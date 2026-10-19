-- Coop (#65): la otra mitad del borrado de cuenta compartido. Chroma define en su repo
-- public.used_by_other_app(uid) devolviendo false, y cada app que se une al proyecto la redefine con su
-- propia pregunta: así borrar la cuenta de Chroma no se lleva el usuario de Auth de quien usa Coop.
-- (Lo contrario lo hace flowtime.delete_my_data mirando public.profiles.)
create or replace function public.used_by_other_app(uid uuid) returns boolean
language sql stable security definer set search_path = '' as $$
  select exists (select 1 from flowtime.profiles where id = uid);
$$;

revoke execute on function public.used_by_other_app(uuid) from public, anon, authenticated;
grant execute on function public.used_by_other_app(uuid) to service_role;
