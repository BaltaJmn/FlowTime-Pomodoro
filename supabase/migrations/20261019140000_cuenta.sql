-- Coop (#65): borrar la cuenta y el aviso de un reporte. docs/coop.md 6.
--
-- Usa los secretos del Vault que ya puso Chroma en el proyecto compartido (project_url y
-- report_webhook_secret) y las variables de sus funciones (RESEND_API_KEY, REPORT_TO,
-- REPORT_WEBHOOK_SECRET): FlowTime no necesita ninguno nuevo.

create extension if not exists pg_net;
create extension if not exists pg_cron;

-- --- borrar la cuenta ---------------------------------------------------------------------------

-- Lo de FlowTime de quien llama: sale de su sala (la sala pasa a otro o se borra), los reportes sobre
-- él (no tienen clave ajena) y su perfil, y con él en cascada amistades, bloqueos e invitaciones.
-- Devuelve si la misma persona tiene cuenta en Chroma: entonces el usuario de auth.users no se
-- borra, porque es de las dos apps. public.profiles puede no existir (en local no está).
create function flowtime.delete_my_data() returns boolean
language plpgsql security definer set search_path = '' as $$
declare
  in_chroma boolean := false;
begin
  if auth.uid() is null then raise exception 'not_signed_in'; end if;
  perform flowtime.leave_room();
  delete from flowtime.reports where reported = auth.uid();
  delete from flowtime.profiles where id = auth.uid();
  if to_regclass('public.profiles') is not null then
    execute 'select exists (select 1 from public.profiles where id = $1)' into in_chroma using auth.uid();
  end if;
  return in_chroma;
end;
$$;

-- --- el aviso de un reporte ---------------------------------------------------------------------

-- Como en Chroma: el correo sale desde la base y no desde un webhook del panel, para que no se pueda
-- olvidar. Un fallo aquí nunca le cuesta el reporte a nadie: se apunta y el reintento lo manda.
create function flowtime.notify_report(report_id bigint) returns void
language plpgsql security definer set search_path = '' as $$
declare
  rec flowtime.reports;
  project_url text;
  hook_secret text;
begin
  select * into rec from flowtime.reports where id = report_id;
  if not found then return; end if;
  select decrypted_secret into project_url from vault.decrypted_secrets where name = 'project_url';
  select decrypted_secret into hook_secret from vault.decrypted_secrets where name = 'report_webhook_secret';
  if project_url is null or hook_secret is null then
    raise warning 'flowtime.notify_report %: faltan project_url y report_webhook_secret en el Vault', report_id;
    return;
  end if;
  perform net.http_post(
    url => project_url || '/functions/v1/flowtime-report-notify',
    headers => jsonb_build_object('Content-Type', 'application/json', 'x-webhook-secret', hook_secret),
    body => jsonb_build_object('record', to_jsonb(rec)),
    timeout_milliseconds => 10000
  );
exception when others then
  raise warning 'flowtime.notify_report %: %', report_id, sqlerrm;
end;
$$;

create function flowtime.on_report_insert() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  perform flowtime.notify_report(new.id);
  return null;
end;
$$;

create trigger report_notify after insert on flowtime.reports
for each row execute function flowtime.on_report_insert();

-- Cada hora, los que no salieron. Con prefijo: los nombres de pg_cron son del proyecto.
select cron.schedule(
  'flowtime-report-retry',
  '43 * * * *',
  $$
  select flowtime.notify_report(id) from flowtime.reports
  where notified_at is null and created_at < now() - interval '10 minutes'
  order by id limit 50;
  $$
);

revoke execute on function flowtime.delete_my_data(), flowtime.notify_report(bigint), flowtime.on_report_insert()
from public, anon, authenticated;
grant execute on function flowtime.delete_my_data() to authenticated;
