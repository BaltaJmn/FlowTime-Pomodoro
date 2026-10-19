-- Coop (#64): salas, miembros, invitaciones y el temporizador compartido. docs/coop.md 3 y 4.
-- El cliente no escribe en ninguna de estas tablas: todo pasa por las funciones, que avisan a la sala
-- por Realtime desde la base. Así un cliente modificado no puede inventarse un "Ana ha pausado".

-- --- tablas -------------------------------------------------------------------------------------

create table flowtime.rooms (
  id uuid primary key default gen_random_uuid(),
  -- 6 caracteres sin los que se confunden (0 O 1 I L): unos 30 bits, con el tope de fallos de #63.
  code text not null unique check (code ~ '^[A-HJKMNP-Z2-9]{6}$'),
  owner uuid not null references flowtime.profiles on delete cascade,
  work_ms integer not null check (work_ms between 300000 and 5400000),
  break_ms integer not null check (break_ms between 60000 and 1800000),
  -- El temporizador (docs/coop.md 4): un estado, no ticks. running_since es null salvo en marcha.
  status text not null default 'idle' check (status in ('idle', 'running', 'paused')),
  running_since timestamptz,
  counted_ms bigint not null default 0 check (counted_ms >= 0),
  changed_by uuid,
  created_at timestamptz not null default now(),
  check ((status = 'running') = (running_since is not null))
);

create table flowtime.room_members (
  room_id uuid not null references flowtime.rooms on delete cascade,
  user_id uuid not null references flowtime.profiles on delete cascade,
  joined_at timestamptz not null default now(),
  primary key (room_id, user_id),
  -- Una sala a la vez.
  constraint one_room unique (user_id)
);

create table flowtime.room_invites (
  room_id uuid not null references flowtime.rooms on delete cascade,
  invitee uuid not null references flowtime.profiles on delete cascade,
  invited_by uuid not null references flowtime.profiles on delete cascade,
  created_at timestamptz not null default now(),
  primary key (room_id, invitee)
);
create index room_invites_invitee on flowtime.room_invites (invitee);

-- --- preguntas ----------------------------------------------------------------------------------

create function flowtime.my_room_id() returns uuid
language sql stable security definer set search_path = '' as $$
  select room_id from flowtime.room_members where user_id = auth.uid();
$$;

create function flowtime.shares_room(y uuid) returns boolean
language sql stable security definer set search_path = '' as $$
  select exists (select 1 from flowtime.room_members where room_id = flowtime.my_room_id() and user_id = y);
$$;

-- --- el temporizador ----------------------------------------------------------------------------

-- Lo contado del ciclo hasta ahora, con la hora del servidor.
create function flowtime.elapsed_ms(r flowtime.rooms) returns bigint
language sql stable set search_path = '' as $$
  select r.counted_ms + case
    when r.status = 'running' then (extract(epoch from (now() - r.running_since)) * 1000)::bigint
    else 0
  end;
$$;

-- Una sala en marcha más de 8 ciclos se da por parada: quien se fue sin parar no acumula horas de
-- "estudio" en el historial de los demás. Los móviles hacen la misma cuenta; esto lo deja escrito.
create function flowtime.settle(room uuid) returns void
language sql security definer set search_path = '' as $$
  update flowtime.rooms r set status = 'idle', running_since = null, counted_ms = 0
  where r.id = room and r.status = 'running'
    and flowtime.elapsed_ms(r) >= 8 * (r.work_ms + r.break_ms)::bigint;
$$;

-- --- Realtime -----------------------------------------------------------------------------------

-- Un canal privado por sala, room:<id>, solo de la base hacia los móviles.
create function flowtime.notify_room(room uuid, event text) returns void
language plpgsql security definer set search_path = '' as $$
begin
  perform realtime.send(
    jsonb_build_object('by', auth.uid()), event, 'room:' || room::text, true
  );
exception when others then
  -- Un aviso que no sale no deshace el cambio: los móviles releen la sala al volver.
  raise warning 'notify_room %: %', room, sqlerrm;
end;
$$;

-- Lee el canal quien es miembro de esa sala. Nadie escribe en él: sin política de insert. El case
-- es para que el cast no se intente nunca con el tema de otro canal (el proyecto es compartido).
create function flowtime.can_listen(topic text) returns boolean
language sql stable security definer set search_path = '' as $$
  select case
    when topic ~ '^room:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
      then substring(topic from 6)::uuid = flowtime.my_room_id()
    else false
  end;
$$;

create policy flowtime_room_listen on realtime.messages for select to authenticated
  using (realtime.messages.extension = 'broadcast' and flowtime.can_listen(realtime.topic()));

-- --- salir y entrar -----------------------------------------------------------------------------

-- Sales de tu sala. Si eras el dueño pasa al que más tiempo lleva; si no queda nadie, se borra.
create function flowtime.leave_room() returns void
language plpgsql security definer set search_path = '' as $$
declare
  room uuid := flowtime.my_room_id();
  heir uuid;
begin
  if room is null then return; end if;
  delete from flowtime.room_members where room_id = room and user_id = auth.uid();
  select user_id into heir from flowtime.room_members where room_id = room order by joined_at, user_id limit 1;
  if heir is null then
    delete from flowtime.rooms where id = room;
    return;
  end if;
  update flowtime.rooms set owner = heir where id = room and owner = auth.uid();
  perform flowtime.notify_room(room, 'members');
end;
$$;

-- El código de una sala nueva, al azar y sin repetir.
create function flowtime.new_room_code() returns text
language plpgsql set search_path = '' as $$
declare
  alphabet constant text := 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';
  code text;
begin
  loop
    select string_agg(substr(alphabet, (get_byte(b, i) % 31) + 1, 1), '')
    into code
    from (select extensions.gen_random_bytes(6) as b) x, generate_series(0, 5) i;
    exit when not exists (select 1 from flowtime.rooms where rooms.code = new_room_code.code);
  end loop;
  return code;
end;
$$;

-- Crea una sala y te mete. Si estabas en otra, sales de ella. Devuelve el código.
create function flowtime.create_room(work_ms integer, break_ms integer) returns text
language plpgsql security definer set search_path = '' as $$
declare
  room flowtime.rooms;
begin
  if not exists (select 1 from flowtime.profiles where id = auth.uid()) then raise exception 'no_profile'; end if;
  perform flowtime.leave_room();
  insert into flowtime.rooms (code, owner, work_ms, break_ms)
  values (flowtime.new_room_code(), auth.uid(), create_room.work_ms, create_room.break_ms)
  returning * into room;
  insert into flowtime.room_members (room_id, user_id) values (room.id, auth.uid());
  return room.code;
end;
$$;

-- 'joined', 'full' (8), 'not_found', 'too_many'. Una sala con alguien bloqueado en cualquier sentido
-- contesta y cuenta como un código que no existe: nadie averigua por un código que lo bloquearon.
create function flowtime.join_room(code text) returns text
language plpgsql security definer set search_path = '' as $$
declare
  me uuid := auth.uid();
  room uuid;
begin
  if not exists (select 1 from flowtime.profiles where id = me) then raise exception 'no_profile'; end if;
  if flowtime.too_many_attempts() then return 'too_many'; end if;
  select id into room from flowtime.rooms where rooms.code = upper(trim(join_room.code));
  if room is null or exists (
    select 1 from flowtime.room_members m where m.room_id = room and flowtime.is_blocked(m.user_id)
  ) then
    perform flowtime.count_attempt();
    return 'not_found';
  end if;
  if room = flowtime.my_room_id() then return 'joined'; end if;
  if (select count(*) from flowtime.room_members where room_id = room) >= 8 then return 'full'; end if;

  perform flowtime.leave_room();
  insert into flowtime.room_members (room_id, user_id) values (room, me);
  delete from flowtime.room_invites where room_id = room and invitee = me;
  perform flowtime.notify_room(room, 'members');
  return 'joined';
end;
$$;

-- Solo el dueño, y nunca a sí mismo (para eso, salir).
create function flowtime.remove_member(member uuid) returns void
language plpgsql security definer set search_path = '' as $$
declare
  room uuid := flowtime.my_room_id();
begin
  if member = auth.uid() or not exists (select 1 from flowtime.rooms where id = room and owner = auth.uid()) then
    return;
  end if;
  delete from flowtime.room_members where room_id = room and user_id = member;
  if found then perform flowtime.notify_room(room, 'members'); end if;
end;
$$;

-- --- invitar ------------------------------------------------------------------------------------

-- 'sent', 'no_room', 'not_friend', 'already' (ya está en tu sala).
create function flowtime.invite_to_room(friend uuid) returns text
language plpgsql security definer set search_path = '' as $$
declare
  room uuid := flowtime.my_room_id();
begin
  if room is null then return 'no_room'; end if;
  if not flowtime.is_friend(friend) then return 'not_friend'; end if;
  if flowtime.shares_room(friend) then return 'already'; end if;
  insert into flowtime.room_invites (room_id, invitee, invited_by) values (room, friend, auth.uid())
  on conflict (room_id, invitee) do nothing;
  return 'sent';
end;
$$;

create function flowtime.decline_room_invite(room uuid) returns void
language sql security definer set search_path = '' as $$
  delete from flowtime.room_invites where room_id = room and invitee = auth.uid();
$$;

-- Las invitaciones que me esperan, con el código para entrar: quien invita es amigo y sigue en la
-- sala. Una de alguien que ya no lo es, o que se fue, no se enseña.
create function flowtime.my_room_invites()
returns table (room_id uuid, code text, invited_by uuid, inviter_name text)
language sql stable security definer set search_path = '' as $$
  select r.id, r.code, p.id, p.display_name
  from flowtime.room_invites i
  join flowtime.rooms r on r.id = i.room_id
  join flowtime.profiles p on p.id = i.invited_by
  join flowtime.room_members m on m.room_id = r.id and m.user_id = i.invited_by
  where i.invitee = auth.uid() and flowtime.is_friend(i.invited_by)
  order by i.created_at desc;
$$;

-- --- el temporizador desde la app ---------------------------------------------------------------

-- Mi sala tal como la ven los móviles, o null: la fila, los miembros y la hora del servidor, todo en
-- milisegundos desde 1970 para no depender del formato de fechas del cliente.
create function flowtime.my_room() returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  room uuid := flowtime.my_room_id();
begin
  if room is null then return null; end if;
  perform flowtime.settle(room);
  return (
    select jsonb_build_object(
      'id', r.id,
      'code', r.code,
      'owner', r.owner,
      'work_ms', r.work_ms,
      'break_ms', r.break_ms,
      'status', r.status,
      'counted_ms', r.counted_ms,
      'running_since', (extract(epoch from r.running_since) * 1000)::bigint,
      'changed_by', r.changed_by,
      'server_now', (extract(epoch from now()) * 1000)::bigint,
      'members', (
        select jsonb_agg(jsonb_build_object('id', p.id, 'display_name', p.display_name) order by m.joined_at, p.id)
        from flowtime.room_members m join flowtime.profiles p on p.id = m.user_id
        where m.room_id = r.id
      )
    )
    from flowtime.rooms r where r.id = room
  );
end;
$$;

-- 'start', 'pause', 'resume', 'skip_break', 'stop' sobre mi sala. Lo que no aplica (pausar algo
-- parado) no cambia nada. Devuelve la sala como my_room(). docs/coop.md 4.
create function flowtime.room_control(action text) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  room uuid := flowtime.my_room_id();
  r flowtime.rooms;
  e bigint;
  cycle bigint;
  pos bigint;
begin
  if room is null then return null; end if;
  if action not in ('start', 'pause', 'resume', 'skip_break', 'stop') then raise exception 'bad_action'; end if;
  perform flowtime.settle(room);
  select * into r from flowtime.rooms where id = room for update;
  e := flowtime.elapsed_ms(r);
  cycle := r.work_ms + r.break_ms;
  pos := e % cycle;

  if action = 'start' and r.status = 'idle' then
    update flowtime.rooms set status = 'running', running_since = now(), counted_ms = 0 where id = room;
  elsif action = 'pause' and r.status = 'running' then
    update flowtime.rooms set status = 'paused', running_since = null, counted_ms = e where id = room;
  elsif action = 'resume' and r.status = 'paused' then
    update flowtime.rooms set status = 'running', running_since = now() where id = room;
  elsif action = 'skip_break' and r.status <> 'idle' and pos >= r.work_ms then
    -- Al principio del ciclo siguiente, sin perder lo trabajado de los anteriores.
    update flowtime.rooms
    set counted_ms = e - pos + cycle, running_since = case when r.status = 'running' then now() end
    where id = room;
  elsif action = 'stop' and r.status <> 'idle' then
    update flowtime.rooms set status = 'idle', running_since = null, counted_ms = 0 where id = room;
  else
    return flowtime.my_room();
  end if;

  update flowtime.rooms set changed_by = auth.uid() where id = room;
  perform flowtime.notify_room(room, action);
  return flowtime.my_room();
end;
$$;

-- --- bloquear también saca de la sala -----------------------------------------------------------

-- Como en #63, y además: si soy el dueño de la sala y está dentro, sale. Si no es mi sala, no la
-- toco; el bloqueo ya impide coincidir en otra (join_room).
create or replace function flowtime.block_user(other uuid) returns void
language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null or other = auth.uid() then return; end if;
  delete from flowtime.friendships where a = least(auth.uid(), other) and b = greatest(auth.uid(), other);
  insert into flowtime.blocks (blocker, blocked) values (auth.uid(), other) on conflict do nothing;
  delete from flowtime.room_invites
  where (invitee = other and invited_by = auth.uid()) or (invitee = auth.uid() and invited_by = other);
  perform flowtime.remove_member(other);
end;
$$;

-- --- quién ejecuta qué y RLS --------------------------------------------------------------------

revoke execute on all functions in schema flowtime from public, anon, authenticated;
grant execute on function
  flowtime.request_friend(text), flowtime.accept_friend(uuid), flowtime.decline_friend(uuid),
  flowtime.remove_friend(uuid), flowtime.block_user(uuid), flowtime.unblock_user(uuid),
  flowtime.regenerate_code(), flowtime.my_profile(), flowtime.my_friendships(), flowtime.my_blocks(),
  flowtime.create_room(integer, integer), flowtime.join_room(text), flowtime.leave_room(),
  flowtime.remove_member(uuid), flowtime.invite_to_room(uuid), flowtime.decline_room_invite(uuid),
  flowtime.my_room_invites(), flowtime.my_room(), flowtime.room_control(text),
  -- Las llaman las políticas, con los permisos de quien pregunta.
  flowtime.is_friend(uuid), flowtime.has_open_request(uuid), flowtime.shares_room(uuid),
  flowtime.my_room_id(), flowtime.can_listen(text)
to authenticated;

alter table flowtime.rooms enable row level security;
alter table flowtime.room_members enable row level security;
alter table flowtime.room_invites enable row level security;
revoke all on flowtime.rooms, flowtime.room_members, flowtime.room_invites from public, anon, authenticated;
grant select on flowtime.rooms, flowtime.room_members, flowtime.room_invites to authenticated;
grant all on flowtime.rooms, flowtime.room_members, flowtime.room_invites to service_role;

create policy rooms_read on flowtime.rooms for select to authenticated using (id = flowtime.my_room_id());
create policy room_members_read on flowtime.room_members for select to authenticated
  using (room_id = flowtime.my_room_id());
create policy room_invites_read on flowtime.room_invites for select to authenticated
  using (auth.uid() in (invitee, invited_by));

-- Quien comparte sala conmigo también ve mi nombre, y lo puedo reportar.
drop policy profiles_read on flowtime.profiles;
create policy profiles_read on flowtime.profiles for select to authenticated using (
  id = auth.uid() or flowtime.is_friend(id) or flowtime.has_open_request(id) or flowtime.shares_room(id)
);
drop policy reports_insert on flowtime.reports;
create policy reports_insert on flowtime.reports for insert to authenticated
  with check (reporter = auth.uid() and (flowtime.is_friend(reported) or flowtime.shares_room(reported)));
