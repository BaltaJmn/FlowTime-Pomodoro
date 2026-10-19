-- Coop #63: sin amistad no se lee nada, el código de invitación es de su dueño, un bloqueo corta en
-- los dos sentidos sin decirlo, los fallos tienen tope, y nadie ejecuta lo que no debe.
-- Con: supabase test db
begin;
create extension if not exists pgtap with schema extensions;
select plan(41);

insert into auth.users (id, email) values
  ('00000000-0000-0000-0000-00000000000a', 'a@test'),
  ('00000000-0000-0000-0000-00000000000b', 'b@test'),
  ('00000000-0000-0000-0000-00000000000c', 'c@test'),
  ('00000000-0000-0000-0000-00000000000d', 'd@test');
insert into flowtime.profiles (id, display_name, invite_code) values
  ('00000000-0000-0000-0000-00000000000a', 'Ana', 'aaaaaaaaaa'),
  ('00000000-0000-0000-0000-00000000000b', 'Bea', 'bbbbbbbbbb'),
  ('00000000-0000-0000-0000-00000000000c', 'Carla', 'cccccccccc');

-- --- quién ejecuta qué --------------------------------------------------------------------------

select is(
  (select array_agg(p.proname::text order by p.proname)
   from pg_proc p join pg_namespace n on n.oid = p.pronamespace
   where n.nspname = 'flowtime' and has_function_privilege('authenticated', p.oid, 'execute')),
  array['accept_friend', 'block_user', 'decline_friend', 'has_open_request', 'is_friend', 'my_blocks',
        'my_friendships', 'my_profile', 'regenerate_code', 'remove_friend', 'request_friend', 'unblock_user'],
  'con sesión solo se ejecutan las funciones de la app'
);
select is(
  (select count(*) from pg_proc p join pg_namespace n on n.oid = p.pronamespace
   where n.nspname = 'flowtime' and has_function_privilege('anon', p.oid, 'execute'))::int,
  0, 'sin sesión, ninguna'
);
select ok(not has_schema_privilege('anon', 'flowtime', 'usage'), 'anon ni entra en el esquema');

-- --- Dani aún no tiene perfil -------------------------------------------------------------------

set local role authenticated;
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000d", "role": "authenticated"}';
select is((select count(*) from flowtime.my_profile())::int, 0, 'sin perfil, my_profile está vacío');
select throws_ok(
  $$ insert into flowtime.profiles (id, display_name) values ('00000000-0000-0000-0000-00000000000a', 'Falsa') $$,
  '42501', null, 'nadie crea un perfil a nombre de otro'
);
select throws_ok(
  $$ insert into flowtime.profiles (id, display_name, invite_code) values ('00000000-0000-0000-0000-00000000000d', 'Dani', 'aaaaaaaaab') $$,
  '42501', null, 'nadie elige su código'
);
select throws_ok(
  $$ insert into flowtime.profiles (id, display_name) values ('00000000-0000-0000-0000-00000000000d', '') $$,
  '23514', null, 'el nombre no puede estar vacío'
);
select lives_ok(
  $$ insert into flowtime.profiles (id, display_name) values ('00000000-0000-0000-0000-00000000000d', 'Dani') $$,
  'cada uno crea su perfil'
);
select is(char_length((select invite_code from flowtime.my_profile())), 10, 'con un código de 10 caracteres');

-- --- Ana --------------------------------------------------------------------------------------

set local role authenticated;
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000a", "role": "authenticated"}';
select is((select invite_code from flowtime.my_profile()), 'aaaaaaaaaa', 'cada uno lee su código');
select throws_ok($$ select invite_code from flowtime.profiles $$, '42501', null, 'pero no de la tabla');
select is((select count(*) from flowtime.profiles)::int, 1, 'sin amigos solo se lee el propio perfil');
select lives_ok(
  $$ update flowtime.profiles set display_name = 'Ana M' where id = auth.uid() $$, 'cada uno cambia su nombre'
);
update flowtime.profiles set display_name = 'Hack' where id = '00000000-0000-0000-0000-00000000000b';
select throws_ok(
  $$ insert into flowtime.friendships (a, b, requested_by, status) values ('00000000-0000-0000-0000-00000000000a', '00000000-0000-0000-0000-00000000000b', '00000000-0000-0000-0000-00000000000a', 'accepted') $$,
  '42501', null, 'las amistades solo cambian por las funciones'
);
select throws_ok(
  $$ insert into flowtime.blocks (blocker, blocked) values ('00000000-0000-0000-0000-00000000000b', '00000000-0000-0000-0000-00000000000a') $$,
  '42501', null, 'y los bloqueos también'
);
select throws_ok(
  $$ insert into flowtime.reports (reporter, reported, reason) values (auth.uid(), '00000000-0000-0000-0000-00000000000b', 'name') $$,
  '42501', null, 'no se reporta a quien no es amigo'
);

-- Ana pide a Bea con su enlace.
select is(flowtime.request_friend('BBBBBBBBBB '), 'sent', 'un enlace, con mayúsculas o espacios, hace una solicitud');
select is(flowtime.request_friend('bbbbbbbbbb'), 'sent', 'pedir otra vez no duplica');
select is(flowtime.request_friend('aaaaaaaaaa'), 'self', 'el propio enlace no hace nada');
select is((select count(*) from flowtime.profiles)::int, 2, 'con la solicitud abierta se lee el nombre de Bea');

-- --- Bea acepta -------------------------------------------------------------------------------

set local role authenticated;
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000b", "role": "authenticated"}';
select is(
  (select display_name || ':' || status from flowtime.my_friendships()),
  'Ana M:pending', 'la solicitud llega con el nombre de quien pide'
);
select flowtime.accept_friend('00000000-0000-0000-0000-00000000000a');
select is((select status from flowtime.my_friendships()), 'accepted', 'aceptada');
select is(flowtime.request_friend('aaaaaaaaaa'), 'already', 'ya son amigas');
select lives_ok(
  $$ insert into flowtime.reports (reporter, reported, reason) values (auth.uid(), '00000000-0000-0000-0000-00000000000a', 'name') $$,
  'a un amigo sí se le reporta'
);
select throws_ok(
  $$ insert into flowtime.reports (reporter, reported, reason) values (auth.uid(), '00000000-0000-0000-0000-00000000000a', 'name') $$,
  '23505', null, 'una vez'
);
select throws_ok($$ select * from flowtime.reports $$, '42501', null, 'y nadie lee los reportes');

-- Bea bloquea a Ana: la amistad se va y Ana no se entera.
select flowtime.block_user('00000000-0000-0000-0000-00000000000a');
select is((select count(*) from flowtime.my_friendships())::int, 0, 'bloquear borra la amistad');
select is((select display_name from flowtime.my_blocks()), 'Ana M', 'Bea ve a quién bloqueó');
select is(flowtime.request_friend('aaaaaaaaaa'), 'blocked', 'y quien bloquea lo sabe');

set local role authenticated;
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000a", "role": "authenticated"}';
select is((select count(*) from flowtime.profiles)::int, 1, 'Ana ya no lee a Bea');
select is((select count(*) from flowtime.my_blocks())::int, 0, 'ni sabe que la bloquearon');
select is(flowtime.request_friend('bbbbbbbbbb'), 'not_found', 'su enlace contesta como uno que no existe');

-- Bea desbloquea: no vuelve la amistad, pero se puede pedir otra vez.
set local role authenticated;
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000b", "role": "authenticated"}';
select flowtime.unblock_user('00000000-0000-0000-0000-00000000000a');
select is((select count(*) from flowtime.my_friendships())::int, 0, 'desbloquear no devuelve la amistad');
select is(flowtime.request_friend('aaaaaaaaaa'), 'sent', 'pero se puede volver a pedir');

-- El código nuevo deja el viejo sin valor.
set local role authenticated;
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000c", "role": "authenticated"}';
select isnt(flowtime.regenerate_code(), 'cccccccccc', 'regenerar da otro código');
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000d", "role": "authenticated"}';
select is(flowtime.request_friend('cccccccccc'), 'not_found', 'y el viejo ya no vale');

-- 20 fallos en una hora y se corta, también para un código que sí existe.
select is(
  (select count(*) from generate_series(1, 18) g where flowtime.request_friend('ffffffffff') = 'not_found')::int,
  18, 'los fallos cuentan'
);
select is(flowtime.request_friend('eeeeeeeeee'), 'not_found', 'el vigésimo aún contesta');
select is(flowtime.request_friend('aaaaaaaaaa'), 'too_many', 'el siguiente se corta');

-- Borrar el usuario se lleva su perfil y sus amistades.
reset role;
select is(
  (select display_name from flowtime.profiles where id = '00000000-0000-0000-0000-00000000000b'),
  'Bea', 'nadie cambia el nombre de otro'
);
delete from auth.users where id = '00000000-0000-0000-0000-00000000000b';
select is(
  (select count(*) from flowtime.friendships where '00000000-0000-0000-0000-00000000000b' in (a, b))::int,
  0, 'borrar la cuenta borra sus amistades'
);

select * from finish();
rollback;
