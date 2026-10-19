-- Coop (#63): perfiles, amistades, bloqueos y reportes en el esquema flowtime. docs/coop.md 3.
-- Es el modelo de Amigos de Chroma (../color/supabase), ya con su revisión aplicada, en un esquema
-- propio: el proyecto y auth.users son compartidos, los perfiles y los amigos no.
-- La seguridad vive aquí, en RLS y funciones security definer: un cliente modificado no puede leer ni
-- escribir nada que el oficial no pueda.

create schema flowtime;
grant usage on schema flowtime to authenticated, service_role;

-- --- tablas -------------------------------------------------------------------------------------

create table flowtime.profiles (
  id uuid primary key references auth.users on delete cascade,
  display_name text not null check (char_length(display_name) between 1 and 30),
  invite_code text not null unique default encode(extensions.gen_random_bytes(5), 'hex'),
  created_at timestamptz not null default now()
);

create table flowtime.friendships (
  a uuid not null references flowtime.profiles on delete cascade,
  b uuid not null references flowtime.profiles on delete cascade,
  requested_by uuid not null,
  status text not null check (status in ('pending', 'accepted')),
  created_at timestamptz not null default now(),
  primary key (a, b),
  -- Una fila por pareja, pida quien pida: la pareja se guarda ordenada.
  check (a < b),
  check (requested_by in (a, b))
);
create index friendships_b on flowtime.friendships (b);

create table flowtime.blocks (
  blocker uuid not null references flowtime.profiles on delete cascade,
  blocked uuid not null references flowtime.profiles on delete cascade,
  primary key (blocker, blocked)
);
create index blocks_blocked on flowtime.blocks (blocked);

-- Lo único que otro usuario ve de ti es el nombre: se reporta a la persona, no un contenido.
create table flowtime.reports (
  id bigint generated always as identity primary key,
  reporter uuid not null references flowtime.profiles on delete cascade,
  -- Sin clave ajena: la persona puede irse antes de que se lea el reporte.
  reported uuid not null,
  reason text not null check (reason in ('name', 'other')),
  created_at timestamptz not null default now(),
  -- Lo rellena flowtime-report-notify (#65) cuando el correo salió.
  notified_at timestamptz,
  constraint reports_once unique (reporter, reported)
);

-- Los códigos que no valen, de amistad y de sala. Un código de amistad son 40 bits: lo que impide
-- recorrerlos es cuántos fallos tiene una cuenta.
create table flowtime.invite_attempts (
  user_id uuid not null references auth.users on delete cascade,
  created_at timestamptz not null default now()
);
create index invite_attempts_user on flowtime.invite_attempts (user_id, created_at);

-- --- las preguntas que hace todo -----------------------------------------------------------------

-- Un bloqueo corta en los dos sentidos. Nadie con sesión la ejecuta: un bloqueado podría preguntar
-- si lo está. Solo la usan las funciones de abajo, con los permisos de su dueño.
create function flowtime.is_blocked(y uuid) returns boolean
language sql stable security definer set search_path = '' as $$
  select exists (
    select 1 from flowtime.blocks k
    where (k.blocker = auth.uid() and k.blocked = y) or (k.blocker = y and k.blocked = auth.uid())
  );
$$;

-- Amistad aceptada y ningún bloqueo. Un false no dice cuál de las dos falló: se puede dar al cliente.
create function flowtime.is_friend(y uuid) returns boolean
language sql stable security definer set search_path = '' as $$
  select exists (
    select 1 from flowtime.friendships f
    where f.a = least(auth.uid(), y) and f.b = greatest(auth.uid(), y) and f.status = 'accepted'
  ) and not flowtime.is_blocked(y);
$$;

-- Aceptar necesita saber quién pide.
create function flowtime.has_open_request(y uuid) returns boolean
language sql stable security definer set search_path = '' as $$
  select exists (
    select 1 from flowtime.friendships f
    where f.a = least(auth.uid(), y) and f.b = greatest(auth.uid(), y) and f.status = 'pending'
  ) and not flowtime.is_blocked(y);
$$;

-- --- el tope de 50 amigos ------------------------------------------------------------------------

create function flowtime.accepted_count(u uuid) returns integer
language sql stable security definer set search_path = '' as $$
  select count(*)::integer from flowtime.friendships f where (f.a = u or f.b = u) and f.status = 'accepted';
$$;

create function flowtime.enforce_friend_limit() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  if new.status = 'accepted' and (tg_op = 'INSERT' or old.status <> 'accepted') then
    if flowtime.accepted_count(new.a) >= 50 or flowtime.accepted_count(new.b) >= 50 then
      raise exception 'friend_limit';
    end if;
  end if;
  return new;
end;
$$;

create trigger friend_limit before insert or update on flowtime.friendships
for each row execute function flowtime.enforce_friend_limit();

-- --- intentos fallidos ---------------------------------------------------------------------------

-- 20 fallos en una hora entre los dos tipos de código. Lo de más de una hora ya no cuenta y se borra
-- aquí mismo: sin tarea programada.
create function flowtime.too_many_attempts() returns boolean
language plpgsql security definer set search_path = '' as $$
begin
  delete from flowtime.invite_attempts where user_id = auth.uid() and created_at < now() - interval '1 hour';
  return (select count(*) from flowtime.invite_attempts where user_id = auth.uid()) >= 20;
end;
$$;

create function flowtime.count_attempt() returns void
language sql security definer set search_path = '' as $$
  insert into flowtime.invite_attempts (user_id) values (auth.uid());
$$;

-- --- lo que llama la app -------------------------------------------------------------------------

-- 'sent', 'accepted' (el otro ya lo había pedido), 'already', 'self', 'blocked' (lo bloqueé yo),
-- 'not_found', 'limit' (50 amigos, o 100 solicitudes esperando al otro), 'too_many'.
create function flowtime.request_friend(code text) returns text
language plpgsql security definer set search_path = '' as $$
declare
  me uuid := auth.uid();
  other uuid;
  existing flowtime.friendships;
begin
  if me is null then raise exception 'not_signed_in'; end if;
  if flowtime.too_many_attempts() then return 'too_many'; end if;

  select id into other from flowtime.profiles where invite_code = lower(trim(code));
  -- Un código que no existe y uno cuyo dueño me bloqueó contestan y cuentan igual: la respuesta no
  -- le dice a nadie que lo bloquearon.
  if other is null or exists (select 1 from flowtime.blocks where blocker = other and blocked = me) then
    perform flowtime.count_attempt();
    return 'not_found';
  end if;
  if other = me then return 'self'; end if;
  if exists (select 1 from flowtime.blocks where blocker = me and blocked = other) then return 'blocked'; end if;

  select * into existing from flowtime.friendships where a = least(me, other) and b = greatest(me, other);
  if not found then
    if flowtime.accepted_count(me) >= 50 then return 'limit'; end if;
    if (
      select count(*) from flowtime.friendships
      where (a = other or b = other) and status = 'pending' and requested_by <> other
    ) >= 100 then
      return 'limit';
    end if;
    begin
      insert into flowtime.friendships (a, b, requested_by, status)
      values (least(me, other), greatest(me, other), me, 'pending');
      return 'sent';
    exception when unique_violation then
      -- El otro pidió en el mismo momento: se sigue como si la fila ya estuviera.
      select * into existing from flowtime.friendships where a = least(me, other) and b = greatest(me, other);
      if not found then return 'not_found'; end if;
    end;
  end if;

  if existing.status = 'accepted' then return 'already'; end if;
  if existing.requested_by = me then return 'sent'; end if;
  begin
    update flowtime.friendships set status = 'accepted' where a = existing.a and b = existing.b;
  exception when raise_exception then
    return 'limit';
  end;
  return 'accepted';
end;
$$;

-- Un bloqueo que se cruzó con una solicitud puede dejar una fila pendiente: no debe volverse amistad.
create function flowtime.accept_friend(other uuid) returns void
language plpgsql security definer set search_path = '' as $$
begin
  update flowtime.friendships set status = 'accepted'
  where a = least(auth.uid(), other) and b = greatest(auth.uid(), other)
    and requested_by = other and status = 'pending'
    and not flowtime.is_blocked(other);
end;
$$;

-- Ignorar y quitar borran la fila sin avisar.
create function flowtime.decline_friend(other uuid) returns void
language sql security definer set search_path = '' as $$
  delete from flowtime.friendships
  where a = least(auth.uid(), other) and b = greatest(auth.uid(), other)
    and requested_by = other and status = 'pending';
$$;

create function flowtime.remove_friend(other uuid) returns void
language sql security definer set search_path = '' as $$
  delete from flowtime.friendships where a = least(auth.uid(), other) and b = greatest(auth.uid(), other);
$$;

create function flowtime.block_user(other uuid) returns void
language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null or other = auth.uid() then return; end if;
  delete from flowtime.friendships where a = least(auth.uid(), other) and b = greatest(auth.uid(), other);
  insert into flowtime.blocks (blocker, blocked) values (auth.uid(), other) on conflict do nothing;
end;
$$;

-- Desbloquear no devuelve la amistad: la fila se fue con el bloqueo.
create function flowtime.unblock_user(other uuid) returns void
language sql security definer set search_path = '' as $$
  delete from flowtime.blocks where blocker = auth.uid() and blocked = other;
$$;

-- El enlace viejo deja de valer en cuanto hay uno nuevo.
create function flowtime.regenerate_code() returns text
language sql security definer set search_path = '' as $$
  update flowtime.profiles set invite_code = encode(extensions.gen_random_bytes(5), 'hex')
  where id = auth.uid() returning invite_code;
$$;

-- La única forma de leer un código de invitación, y solo el propio.
create function flowtime.my_profile()
returns table (id uuid, display_name text, invite_code text)
language sql stable security definer set search_path = '' as $$
  select p.id, p.display_name, p.invite_code from flowtime.profiles p where p.id = auth.uid();
$$;

-- Amistades y solicitudes con el nombre ya unido, sin nadie bloqueado en ningún sentido.
create function flowtime.my_friendships()
returns table (id uuid, display_name text, status text, requested_by uuid)
language sql stable security definer set search_path = '' as $$
  select p.id, p.display_name, f.status, f.requested_by
  from flowtime.friendships f
  join flowtime.profiles p on p.id = case when f.a = auth.uid() then f.b else f.a end
  where auth.uid() in (f.a, f.b) and not flowtime.is_blocked(p.id);
$$;

create function flowtime.my_blocks()
returns table (id uuid, display_name text)
language sql stable security definer set search_path = '' as $$
  select p.id, p.display_name
  from flowtime.blocks k
  join flowtime.profiles p on p.id = k.blocked
  where k.blocker = auth.uid();
$$;

-- 20 reportes al día por persona; con reports_once, nadie llena el buzón.
create function flowtime.limit_reports() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  if (select count(*) from flowtime.reports r where r.reporter = new.reporter and r.created_at > now() - interval '1 day') >= 20 then
    raise exception 'report_limit';
  end if;
  return new;
end;
$$;

create trigger report_cap before insert on flowtime.reports
for each row execute function flowtime.limit_reports();

-- --- quién ejecuta qué ---------------------------------------------------------------------------

-- Postgres da execute a public en cada función nueva: aquí nadie tiene nada salvo lo que se nombra.
revoke execute on all functions in schema flowtime from public, anon, authenticated;
grant execute on function
  flowtime.request_friend(text), flowtime.accept_friend(uuid), flowtime.decline_friend(uuid),
  flowtime.remove_friend(uuid), flowtime.block_user(uuid), flowtime.unblock_user(uuid),
  flowtime.regenerate_code(), flowtime.my_profile(), flowtime.my_friendships(), flowtime.my_blocks(),
  -- Las llaman las políticas, con los permisos de quien pregunta.
  flowtime.is_friend(uuid), flowtime.has_open_request(uuid)
to authenticated;

-- --- RLS -----------------------------------------------------------------------------------------

alter table flowtime.profiles enable row level security;
alter table flowtime.friendships enable row level security;
alter table flowtime.blocks enable row level security;
alter table flowtime.reports enable row level security;
alter table flowtime.invite_attempts enable row level security;

revoke all on all tables in schema flowtime from public, anon, authenticated;
-- De los perfiles ajenos, solo id y nombre: el código se lee por my_profile().
grant select (id, display_name), insert (id, display_name), update (display_name) on flowtime.profiles to authenticated;
grant select on flowtime.friendships, flowtime.blocks to authenticated;
grant insert (reporter, reported, reason) on flowtime.reports to authenticated;
grant all on all tables in schema flowtime to service_role;

create policy profiles_read on flowtime.profiles for select to authenticated using (
  id = auth.uid() or flowtime.is_friend(id) or flowtime.has_open_request(id)
);
create policy profiles_insert on flowtime.profiles for insert to authenticated with check (id = auth.uid());
create policy profiles_update on flowtime.profiles for update to authenticated
  using (id = auth.uid()) with check (id = auth.uid());

create policy friendships_read on flowtime.friendships for select to authenticated
  using (auth.uid() in (a, b));

create policy blocks_read on flowtime.blocks for select to authenticated using (blocker = auth.uid());

-- De un amigo, y nunca se lee de vuelta: un reporte es para el desarrollador. #64 lo abre también a
-- quien comparte sala.
create policy reports_insert on flowtime.reports for insert to authenticated
  with check (reporter = auth.uid() and flowtime.is_friend(reported));
