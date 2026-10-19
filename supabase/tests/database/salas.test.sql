-- Coop #64: una sala solo la ve quien está dentro, el temporizador es una cuenta y no ticks, el dueño
-- pasa al siguiente, un bloqueo impide coincidir, y el canal de Realtime es solo de los miembros.
-- Con: supabase test db
begin;
create extension if not exists pgtap with schema extensions;
select plan(38);

insert into auth.users (id, email) values
  ('00000000-0000-0000-0000-00000000000a', 'a@test'),
  ('00000000-0000-0000-0000-00000000000b', 'b@test'),
  ('00000000-0000-0000-0000-00000000000c', 'c@test'),
  ('00000000-0000-0000-0000-00000000000d', 'd@test');
insert into flowtime.profiles (id, display_name, invite_code) values
  ('00000000-0000-0000-0000-00000000000a', 'Ana', 'aaaaaaaaaa'),
  ('00000000-0000-0000-0000-00000000000b', 'Bea', 'bbbbbbbbbb'),
  ('00000000-0000-0000-0000-00000000000c', 'Carla', 'cccccccccc'),
  ('00000000-0000-0000-0000-00000000000d', 'Dani', 'dddddddddd');
-- Ana y Carla son amigas.
insert into flowtime.friendships (a, b, requested_by, status) values
  ('00000000-0000-0000-0000-00000000000a', '00000000-0000-0000-0000-00000000000c', '00000000-0000-0000-0000-00000000000a', 'accepted');

create temporary table t (code text, room uuid);
grant all on t to authenticated;

-- --- Ana crea una sala --------------------------------------------------------------------------

set local role authenticated;
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000a", "role": "authenticated"}';
select throws_ok($$ select flowtime.create_room(60000, 300000) $$, '23514', null, 'la sala tiene duraciones con sentido');
insert into t (code) select flowtime.create_room(1500000, 300000);
update t set room = flowtime.my_room_id();
select matches((select code from t), '^[A-HJKMNP-Z2-9]{6}$', 'con un código de 6 sin letras que se confunden');
select is((flowtime.my_room() ->> 'status'), 'idle', 'parada al crearla');
select is(jsonb_array_length(flowtime.my_room() -> 'members'), 1, 'con su creadora dentro');
select throws_ok(
  $$ insert into flowtime.room_members (room_id, user_id) select room, '00000000-0000-0000-0000-00000000000b' from t $$,
  '42501', null, 'nadie mete a nadie sin las funciones'
);
select throws_ok($$ update flowtime.rooms set status = 'running' $$, '42501', null, 'ni toca el temporizador');

-- Empezar: corre desde ahora (now() es la hora de la transacción, así que lo contado es 0).
select is((flowtime.room_control('start') ->> 'status'), 'running', 'empezar');
select is((flowtime.room_control('start') ->> 'changed_by'), '00000000-0000-0000-0000-00000000000a', 'empezar dos veces no cambia nada');
select is(flowtime.invite_to_room('00000000-0000-0000-0000-00000000000b'), 'not_friend', 'solo se invita a amigos');
select is(flowtime.invite_to_room('00000000-0000-0000-0000-00000000000c'), 'sent', 'a una amiga sí');

-- --- Bea entra con el código --------------------------------------------------------------------

set local role authenticated;
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000b", "role": "authenticated"}';
select is((select count(*) from flowtime.rooms)::int, 0, 'desde fuera no se ve ninguna sala');
select is(flowtime.my_room(), null, 'ni la propia, que no hay');
select is(flowtime.join_room(lower((select code from t)) || ' '), 'joined', 'se entra con el código, en minúsculas o con espacios');
select is((select count(*) from flowtime.rooms)::int, 1, 'dentro se ve la sala');
select is((select count(*) from flowtime.profiles)::int, 2, 'y el nombre de quien está');
select lives_ok(
  $$ insert into flowtime.reports (reporter, reported, reason) values (auth.uid(), '00000000-0000-0000-0000-00000000000a', 'name') $$,
  'a quien comparte sala se le puede reportar'
);

-- La aritmética de docs/coop.md 4, con la sala movida a mano 26 minutos atrás: en el descanso.
reset role;
update flowtime.rooms set running_since = now() - interval '26 minutes' where id = (select room from t);
set local role authenticated;
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000b", "role": "authenticated"}';
select is(
  (flowtime.room_control('pause') ->> 'counted_ms')::bigint, 1560000::bigint, 'pausar guarda lo contado'
);
select is((flowtime.my_room() ->> 'running_since'), null, 'y en pausa no corre');
select is(
  (flowtime.room_control('skip_break') ->> 'counted_ms')::bigint, 1800000::bigint,
  'saltar el descanso lleva al principio del ciclo siguiente'
);
select is(
  (flowtime.room_control('skip_break') ->> 'counted_ms')::bigint, 1800000::bigint,
  'y en el trabajo no hace nada'
);
select is((flowtime.room_control('resume') ->> 'status'), 'running', 'seguir');
select is((flowtime.my_room() ->> 'changed_by'), '00000000-0000-0000-0000-00000000000b', 'con quién lo tocó');
select throws_ok($$ select flowtime.room_control('explode') $$, 'P0001', 'bad_action', 'una acción que no existe');

-- Realtime: el canal de la sala lo leen sus miembros, y nadie escribe en él.
select set_config('realtime.topic', 'room:' || (select room::text from t), true);
select ok((select count(*) from realtime.messages) > 0, 'cada cambio llega al canal de la sala');
select throws_ok(
  $$ insert into realtime.messages (topic, extension, event, payload, private) values (current_setting('realtime.topic'), 'broadcast', 'pause', '{}', true) $$,
  '42501', null, 'y desde el móvil nadie se inventa un aviso'
);

-- Dani, de fuera, ni con el tema del canal.
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000d", "role": "authenticated"}';
select is((select count(*) from realtime.messages)::int, 0, 'quien no está no lee el canal');
select is(flowtime.room_control('stop'), null, 'ni para una sala ajena');

-- Más de 8 ciclos en marcha se da por parada.
reset role;
update flowtime.rooms set running_since = now() - interval '5 hours', counted_ms = 0 where id = (select room from t);
set local role authenticated;
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000b", "role": "authenticated"}';
select is((flowtime.my_room() ->> 'status'), 'idle', 'una sala olvidada en marcha se para sola');

-- --- Carla entra por la invitación, y Ana se va -------------------------------------------------

set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000c", "role": "authenticated"}';
select is((select inviter_name from flowtime.my_room_invites()), 'Ana', 'la invitación llega con quien invita');
select is(flowtime.join_room((select code from flowtime.my_room_invites())), 'joined', 'y con su código se entra');
select is((select count(*) from flowtime.my_room_invites())::int, 0, 'entrar gasta la invitación');

set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000a", "role": "authenticated"}';
select flowtime.leave_room();
select is(flowtime.my_room(), null, 'salir');
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000b", "role": "authenticated"}';
select is((flowtime.my_room() ->> 'owner'), '00000000-0000-0000-0000-00000000000b', 'la sala pasa a quien más tiempo lleva');

-- Bea, ahora dueña, bloquea a Carla: sale de la sala y no puede volver con el código.
select flowtime.block_user('00000000-0000-0000-0000-00000000000c');
select is(jsonb_array_length(flowtime.my_room() -> 'members'), 1, 'bloquear desde la sala propia saca a la otra');
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000c", "role": "authenticated"}';
select is(flowtime.join_room((select code from t)), 'not_found', 'y el código contesta como si no existiera');

-- Llena: 8 como mucho.
reset role;
insert into auth.users (id, email) select ('00000000-0000-0000-0000-0000000001' || lpad(g::text, 2, '0'))::uuid, g || '@test' from generate_series(1, 7) g;
insert into flowtime.profiles (id, display_name) select ('00000000-0000-0000-0000-0000000001' || lpad(g::text, 2, '0'))::uuid, 'P' || g from generate_series(1, 7) g;
insert into flowtime.room_members (room_id, user_id)
select (select room from t), ('00000000-0000-0000-0000-0000000001' || lpad(g::text, 2, '0'))::uuid from generate_series(1, 7) g;
set local role authenticated;
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000d", "role": "authenticated"}';
select is(flowtime.join_room((select code from t)), 'full', 'una sala de 8 está llena');

-- Una sala vacía se borra.
select flowtime.create_room(1500000, 300000);
select flowtime.leave_room();
reset role;
select is((select count(*) from flowtime.rooms where owner = '00000000-0000-0000-0000-00000000000d')::int, 0, 'la última en salir borra la sala');
select is(
  (select count(*) from flowtime.room_members where user_id = '00000000-0000-0000-0000-00000000000d')::int, 0,
  'y no queda en ninguna'
);

select * from finish();
rollback;
