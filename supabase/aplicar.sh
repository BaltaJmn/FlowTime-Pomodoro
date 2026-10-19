#!/bin/sh
# Aplica las migraciones de FlowTime al proyecto compartido con Chroma (docs/coop.md 3.1).
# No usa `supabase db push`: el historial de migraciones es uno por proyecto y es de Chroma; con dos
# repos empujando, cada uno romperia el push del otro. Lo aplicado se apunta en flowtime.migraciones.
#
#   DB_URL='postgresql://postgres.<ref>:<clave>@<host>:5432/postgres' supabase/aplicar.sh
set -eu
: "${DB_URL:?Falta DB_URL, la cadena de conexion de la base (Settings > Database)}"
cd "$(dirname "$0")/migrations"

psql "$DB_URL" -v ON_ERROR_STOP=1 -q <<'SQL'
create table if not exists public.flowtime_migraciones (version text primary key, aplicada timestamptz not null default now());
alter table public.flowtime_migraciones enable row level security;
revoke all on public.flowtime_migraciones from public, anon, authenticated;
SQL

for f in *.sql; do
  v="${f%%_*}"
  hecha=$(psql "$DB_URL" -tAc "select 1 from public.flowtime_migraciones where version = '$v'")
  [ -n "$hecha" ] && continue
  echo "Aplicando $f"
  # Todo el fichero y su apunte en una transaccion: o entra entero o no entra.
  { echo "begin;"; cat "$f"; echo "insert into public.flowtime_migraciones (version) values ('$v'); commit;"; } |
    psql "$DB_URL" -v ON_ERROR_STOP=1 -q
done
echo "Al dia."
