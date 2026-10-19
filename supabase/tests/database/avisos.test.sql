-- Coop: una solicitud de amistad o una invitación a sala avisa al canal user:<id> de quien la recibe,
-- y ese canal solo lo lee esa persona.
-- Con: supabase test db
begin;
create extension if not exists pgtap with schema extensions;
select plan(6);

insert into auth.users (id, email) values
  ('00000000-0000-0000-0000-00000000000a', 'a@test'),
  ('00000000-0000-0000-0000-00000000000b', 'b@test');
insert into flowtime.profiles (id, display_name, invite_code) values
  ('00000000-0000-0000-0000-00000000000a', 'Ana', 'aaaaaaaaaa'),
  ('00000000-0000-0000-0000-00000000000b', 'Bea', 'bbbbbbbbbb');

set local role authenticated;
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000a", "role": "authenticated"}';
select is(flowtime.request_friend('bbbbbbbbbb'), 'sent', 'Ana pide a Bea');

-- Bea lee su canal: le llegó la solicitud.
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000b", "role": "authenticated"}';
select set_config('realtime.topic', 'user:00000000-0000-0000-0000-00000000000b', true);
select ok(
  (select count(*) from realtime.messages where event = 'friends') > 0,
  'la solicitud avisa a Bea'
);
select lives_ok($$ select flowtime.accept_friend('00000000-0000-0000-0000-00000000000a') $$, 'Bea acepta');

-- Ana tiene sala e invita a Bea: a Bea le llega "invites".
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000a", "role": "authenticated"}';
select flowtime.create_room(1500000, 300000);
select is(flowtime.invite_to_room('00000000-0000-0000-0000-00000000000b'), 'sent', 'Ana invita a Bea');

set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000b", "role": "authenticated"}';
select ok(
  (select count(*) from realtime.messages where event = 'invites') > 0,
  'la invitación avisa a Bea'
);

-- Ana no lee el canal de Bea.
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000a", "role": "authenticated"}';
select is((select count(*) from realtime.messages)::int, 0, 'el canal de Bea es solo suyo');

select * from finish();
rollback;
