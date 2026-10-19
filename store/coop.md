# Coop: el servidor, paso a paso

Coop usa el proyecto de Supabase de Chroma, `baltajmn-social` (ref `djfbmvagflfptqhabdln`,
eu-central-1, plan Free), con el esquema `flowtime`. Diseno: `docs/coop.md`.

## Hecho (19-10-2026)

- Migraciones de `supabase/migrations/` aplicadas y apuntadas en `public.flowtime_migraciones`. Las
  siguientes, con `supabase/aplicar.sh` (o una a una desde el editor SQL, apuntando la version): nunca
  con `supabase db push`, que es de Chroma.
- `public.used_by_other_app` redefinida: borrar la cuenta en Chroma no borra el usuario de quien usa Coop.
- Funciones `flowtime-delete-account` (con JWT) y `flowtime-report-notify` (sin JWT, la protege
  `x-webhook-secret`) desplegadas. Usan los secretos que ya puso Chroma (`RESEND_API_KEY`,
  `REPORT_TO`, `REPORT_WEBHOOK_SECRET`) y el Vault (`project_url`, `report_webhook_secret`).
- Tarea `flowtime-report-retry` de `pg_cron`, minuto 43 de cada hora.
- URL y clave anon en `core/data/.../coop/SupabaseConfig.kt`.

## Falta, en el panel (usuario)

1. *Integrations > Data API > Settings > Exposed schemas*: anadir `flowtime`. Sin esto la app recibe
   406 en todo lo de Coop.
2. *Authentication > URL Configuration > Redirect URLs*: anadir `com.baltajmn.flowtime://login`.
3. *Authentication > Sign In / Providers > Apple > Client IDs*: anadir `com.baltajmn.flowtime` (el
   bundle de iOS). El Services ID de Chroma sirve para el flujo web de las dos apps.
4. Google (cuando se configure tambien para Chroma): un cliente OAuth Android con el paquete
   `com.baltajmn.flowtime` y los SHA-1 de la firma de Play y de depuracion, y uno iOS con el bundle;
   los dos en *Client IDs* de Google en Supabase. El cliente web es el mismo que el de Chroma y va en
   `SupabaseConfig.googleServerClientId`.
5. App ID `com.baltajmn.flowtime` en developer.apple.com con *Sign in with Apple* y *Associated
   Domains* (#72).

## Actuar sobre un reporte

El correo trae nombre e id de las dos personas. Apple da 24 h. Desde el panel: cambiar
`display_name` en `flowtime.profiles`, o borrar la fila (cae todo lo de FlowTime en cascada). Para
borrar la cuenta entera, ademas el usuario en *Authentication > Users*, solo si no usa Chroma
(`select public.used_by_other_app('<id>')` en Chroma no sirve aqui: mirar `public.profiles`).
