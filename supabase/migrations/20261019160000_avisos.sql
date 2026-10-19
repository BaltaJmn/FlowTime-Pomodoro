-- Coop: avisos en vivo de amistades e invitaciones. Cada persona escucha su canal privado
-- `user:<id>`; un cambio en sus amistades o en sus invitaciones a sala le manda "friends" o "invites",
-- y la app relee esa lista. Sin esto, una solicitud o una invitacion solo se veian al reabrir Coop.

create function flowtime.notify_user(person uuid, event text) returns void
language plpgsql security definer set search_path = '' as $$
begin
  perform realtime.send(jsonb_build_object('by', auth.uid()), event, 'user:' || person::text, true);
exception when others then
  -- Igual que notify_room: un aviso que no sale no deshace el cambio.
  raise warning 'notify_user %: %', person, sqlerrm;
end;
$$;

create function flowtime.friendships_notify() returns trigger
language plpgsql security definer set search_path = '' as $$
declare
  pair flowtime.friendships := coalesce(new, old);
begin
  perform flowtime.notify_user(pair.a, 'friends');
  perform flowtime.notify_user(pair.b, 'friends');
  return null;
end;
$$;

create trigger friendships_notify after insert or update or delete on flowtime.friendships
  for each row execute function flowtime.friendships_notify();

create function flowtime.room_invites_notify() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  perform flowtime.notify_user((coalesce(new, old)).invitee, 'invites');
  return null;
end;
$$;

create trigger room_invites_notify after insert or delete on flowtime.room_invites
  for each row execute function flowtime.room_invites_notify();

-- La sala, para quien es miembro; `user:<id>`, solo para esa persona.
create or replace function flowtime.can_listen(topic text) returns boolean
language sql stable security definer set search_path = '' as $$
  select case
    when topic ~ '^room:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
      then substring(topic from 6)::uuid = flowtime.my_room_id()
    when topic ~ '^user:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
      then substring(topic from 6)::uuid = auth.uid()
    else false
  end;
$$;

revoke all on function flowtime.notify_user(uuid, text) from public, anon, authenticated;
revoke all on function flowtime.friendships_notify() from public, anon, authenticated;
revoke all on function flowtime.room_invites_notify() from public, anon, authenticated;
