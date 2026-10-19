-- Coop #65: borrar la cuenta deja la sala a los demás y no se lleva nada de nadie más; un reporte se
-- guarda aunque el aviso no pueda salir.
-- Con: supabase test db
begin;
create extension if not exists pgtap with schema extensions;
select plan(8);

insert into auth.users (id, email) values
  ('00000000-0000-0000-0000-00000000000a', 'a@test'),
  ('00000000-0000-0000-0000-00000000000b', 'b@test');
insert into flowtime.profiles (id, display_name, invite_code) values
  ('00000000-0000-0000-0000-00000000000a', 'Ana', 'aaaaaaaaaa'),
  ('00000000-0000-0000-0000-00000000000b', 'Bea', 'bbbbbbbbbb');
create temporary table t (code text);
grant all on t to authenticated;

set local role authenticated;
set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000a", "role": "authenticated"}';
insert into t select flowtime.create_room(1500000, 300000);

set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000b", "role": "authenticated"}';
select flowtime.join_room((select code from t));
-- Con el Vault vacío, como un proyecto que olvidó sus secretos: el reporte se guarda igual.
select lives_ok(
  $$ insert into flowtime.reports (reporter, reported, reason) values (auth.uid(), '00000000-0000-0000-0000-00000000000a', 'name') $$,
  'un reporte se guarda aunque el aviso no salga'
);

set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000a", "role": "authenticated"}';
select is(flowtime.delete_my_data(), false, 'sin Chroma, el usuario se puede borrar');
select is((select count(*) from flowtime.my_profile())::int, 0, 'el perfil se fue');

set local request.jwt.claims to '{"sub": "00000000-0000-0000-0000-00000000000b", "role": "authenticated"}';
select is((flowtime.my_room() ->> 'owner'), '00000000-0000-0000-0000-00000000000b', 'la sala sigue, con otra dueña');
select is(jsonb_array_length(flowtime.my_room() -> 'members'), 1, 'sin quien se fue');

reset role;
select is((select count(*) from flowtime.reports)::int, 0, 'los reportes sobre quien se fue, también');
select is((select count(*) from flowtime.profiles where id = '00000000-0000-0000-0000-00000000000b')::int, 1, 'lo de los demás se queda');

set local role anon;
select throws_ok($$ select flowtime.delete_my_data() $$, '42501', null, 'sin sesión no se ejecuta');

select * from finish();
rollback;
