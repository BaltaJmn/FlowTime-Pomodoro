-- Coop: cada solicitud, amistad aceptada e invitacion pide un push a flowtime-push para quien la
-- recibe; los tokens no los lee nadie desde la app.
-- Con: supabase test db
begin;
create extension if not exists pgtap with schema extensions;
select plan(11);

-- En local el Vault esta vacio; en el proyecto ya tiene los dos.
select vault.create_secret('http://localhost', 'project_url') where not exists (select 1 from vault.secrets where name = 'project_url');
select vault.create_secret('x', 'report_webhook_secret') where not exists (select 1 from vault.secrets where name = 'report_webhook_secret');

create function pg_temp.pushes() returns setof jsonb language sql as $$
  select convert_from(body, 'utf8')::jsonb from net.http_request_queue where url like '%/flowtime-push'
$$;

insert into auth.users (id, email) values
  ('00000000-0000-0000-0000-00000000000a', 'a@test'),
  ('00000000-0000-0000-0000-00000000000b', 'b@test');
insert into flowtime.profiles (id, display_name, invite_code) values
  ('00000000-0000-0000-0000-00000000000a', 'Ana', 'aaaaaaaaaa'),
  ('00000000-0000-0000-0000-00000000000b', 'Bea', 'bbbbbbbbbb');

set local role authenticated;
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000a", "role": "authenticated"}';
select throws_ok('select * from flowtime.push_tokens', '42501', null, 'la app no lee los tokens');
select lives_ok($$ select flowtime.register_push_token('tok-ana', 'android', 'es') $$, 'Ana registra su token');
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000b", "role": "authenticated"}';
select lives_ok($$ select flowtime.register_push_token('tok-bea', 'ios', 'en') $$, 'Bea registra el suyo');

set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000a", "role": "authenticated"}';
select flowtime.request_friend('bbbbbbbbbb');
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000b", "role": "authenticated"}';
select flowtime.accept_friend('00000000-0000-0000-0000-00000000000a');
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000a", "role": "authenticated"}';
select flowtime.create_room(1500000, 300000);
select flowtime.invite_to_room('00000000-0000-0000-0000-00000000000b');
reset role;

select ok(
  '{"person": "00000000-0000-0000-0000-00000000000b", "kind": "friend_request", "actor": "00000000-0000-0000-0000-00000000000a"}'::jsonb
    in (select * from pg_temp.pushes()),
  'la solicitud avisa a Bea'
);
select ok(
  '{"person": "00000000-0000-0000-0000-00000000000a", "kind": "friend_accepted", "actor": "00000000-0000-0000-0000-00000000000b"}'::jsonb
    in (select * from pg_temp.pushes()),
  'aceptar avisa a Ana'
);
select ok(
  '{"person": "00000000-0000-0000-0000-00000000000b", "kind": "room_invite", "actor": "00000000-0000-0000-0000-00000000000a"}'::jsonb
    in (select * from pg_temp.pushes()),
  'la invitacion avisa a Bea'
);
select is((select count(*) from pg_temp.pushes())::int, 3, 'nada mas');

-- Quitar la amistad no avisa; a quien no tiene token no se le llama.
set local role authenticated;
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000a", "role": "authenticated"}';
select flowtime.remove_friend('00000000-0000-0000-0000-00000000000b');
select flowtime.unregister_push_token('tok-bea');
reset role;
select ok(exists (select 1 from flowtime.push_tokens where token = 'tok-bea'), 'Ana no borra el token de Bea');
set local role authenticated;
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000b", "role": "authenticated"}';
select flowtime.unregister_push_token('tok-bea');
select flowtime.request_friend('aaaaaaaaaa');
reset role;
select ok(not exists (select 1 from flowtime.push_tokens where token = 'tok-bea'), 'Bea borra el suyo');
select is(
  (select count(*) from pg_temp.pushes() p where p->>'person' = '00000000-0000-0000-0000-00000000000a')::int,
  2,
  'quitar la amistad no avisa; pedir de nuevo avisa a Ana'
);

-- El movil de Ana pasa a la cuenta de Bea: el token es de Bea.
set local role authenticated;
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000a", "role": "authenticated"}';
select flowtime.register_push_token('tok-ana', 'android', 'es');
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000b", "role": "authenticated"}';
select flowtime.register_push_token('tok-ana', 'android', 'de');
reset role;
select is(
  (select user_id from flowtime.push_tokens where token = 'tok-ana'),
  '00000000-0000-0000-0000-00000000000b'::uuid,
  'el token es de quien lo registro ultimo'
);

select * from finish();
rollback;
