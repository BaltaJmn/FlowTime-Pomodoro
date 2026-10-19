-- Coop: avisos push con la app cerrada (docs/coop-push.md). Una solicitud de amistad, una amistad
-- aceptada o una invitacion a sala llaman a la Edge Function flowtime-push, que manda el aviso por
-- FCM (Android) o APNs (iOS). Los avisos por Realtime (20261019160000_avisos.sql) siguen igual.

-- Sin politicas: nadie la lee ni la escribe salvo por las funciones de abajo y la service role.
create table flowtime.push_tokens (
  token text primary key check (char_length(token) between 1 and 4096),
  user_id uuid not null references flowtime.profiles on delete cascade,
  platform text not null check (platform in ('android', 'ios', 'ios-sandbox')),
  lang text not null default 'en' check (lang ~ '^[a-z]{2}$'),
  updated_at timestamptz not null default now()
);
create index push_tokens_user on flowtime.push_tokens (user_id);
alter table flowtime.push_tokens enable row level security;
revoke all on flowtime.push_tokens from public, anon, authenticated;

-- Un movil que cambia de cuenta deja de avisar a la anterior: el token es de quien lo registro ultimo.
create function flowtime.register_push_token(token text, platform text, lang text) returns void
language sql security definer set search_path = '' as $$
  insert into flowtime.push_tokens (token, user_id, platform, lang)
  values (token, auth.uid(), platform, lang)
  on conflict on constraint push_tokens_pkey do update
    set user_id = excluded.user_id, platform = excluded.platform, lang = excluded.lang, updated_at = now();
$$;

create function flowtime.unregister_push_token(token text) returns void
language sql security definer set search_path = '' as $$
  delete from flowtime.push_tokens t where t.token = unregister_push_token.token and t.user_id = auth.uid();
$$;

-- Como notify_report: la base llama a la funcion, y un aviso que no sale no deshace el cambio.
create function flowtime.push(person uuid, kind text, actor uuid) returns void
language plpgsql security definer set search_path = '' as $$
declare
  project_url text;
  hook_secret text;
begin
  if not exists (select 1 from flowtime.push_tokens where user_id = person) then return; end if;
  select decrypted_secret into project_url from vault.decrypted_secrets where name = 'project_url';
  select decrypted_secret into hook_secret from vault.decrypted_secrets where name = 'report_webhook_secret';
  if project_url is null or hook_secret is null then
    raise warning 'flowtime.push %: faltan project_url y report_webhook_secret en el Vault', person;
    return;
  end if;
  perform net.http_post(
    url => project_url || '/functions/v1/flowtime-push',
    headers => jsonb_build_object('Content-Type', 'application/json', 'x-webhook-secret', hook_secret),
    body => jsonb_build_object('person', person, 'kind', kind, 'actor', actor),
    timeout_milliseconds => 10000
  );
exception when others then
  raise warning 'flowtime.push %: %', person, sqlerrm;
end;
$$;

create function flowtime.friendships_push() returns trigger
language plpgsql security definer set search_path = '' as $$
declare
  other uuid := case when new.requested_by = new.a then new.b else new.a end;
begin
  if tg_op = 'INSERT' and new.status = 'pending' then
    perform flowtime.push(other, 'friend_request', new.requested_by);
  elsif tg_op = 'UPDATE' and old.status = 'pending' and new.status = 'accepted' then
    perform flowtime.push(new.requested_by, 'friend_accepted', other);
  end if;
  return null;
end;
$$;

create trigger friendships_push after insert or update on flowtime.friendships
  for each row execute function flowtime.friendships_push();

create function flowtime.room_invites_push() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  perform flowtime.push(new.invitee, 'room_invite', new.invited_by);
  return null;
end;
$$;

create trigger room_invites_push after insert on flowtime.room_invites
  for each row execute function flowtime.room_invites_push();

revoke all on function flowtime.register_push_token(text, text, text), flowtime.unregister_push_token(text),
  flowtime.push(uuid, text, uuid), flowtime.friendships_push(), flowtime.room_invites_push()
  from public, anon, authenticated;
grant execute on function flowtime.register_push_token(text, text, text), flowtime.unregister_push_token(text)
  to authenticated;
