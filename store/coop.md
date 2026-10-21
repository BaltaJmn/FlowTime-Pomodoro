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

## Panel, Google y Apple (hecho el 19-10-2026, por la API de gestion y el navegador)

1. *Integrations > Data API > Settings > Exposed schemas*: anadir `flowtime`. Sin esto la app recibe
   406 en todo lo de Coop.
2. *Authentication > URL Configuration > Redirect URLs*: anadir `com.baltajmn.flowtime://login`.
3. *Authentication > Sign In / Providers > Apple > Client IDs*: anadir `com.baltajmn.flowtime` (el
   bundle de iOS). El Services ID de Chroma sirve para el flujo web de las dos apps.
4. Google, hecho el 19-10-2026: proyecto propio de Google Cloud `flowtime-baltajmn` (con el de Chroma
   la pantalla de consentimiento diria "Chroma"). Cliente web "FlowTime Supabase" (redirect
   `https://djfbmvagflfptqhabdln.supabase.co/auth/v1/callback`), anadido al final de *Client IDs* de
   Google en Supabase (el de Chroma sigue el primero: es el que usa el flujo web) y en
   `SupabaseConfig.googleServerClientId`. Tres clientes Android `com.baltajmn.flowtime`: firma de
   Play, clave de subida y debug. Sin cliente iOS: en el iPhone Coop entra solo con Apple.
   **Pendiente:** el consentimiento esta en "Prueba" (solo usuarios de prueba). Publicarlo en
   *Google Auth Platform > Publico > Publicar app*; con solo email y perfil no pide verificacion.
5. App ID `com.baltajmn.flowtime` (`5VT8J4B49Z`) con *Sign in with Apple* (primaria) y *Associated
   Domains* (#72): hecho el 19-10-2026 por la API de App Store Connect (`APPLE_ID_AUTH` y
   `ASSOCIATED_DOMAINS` en `/v1/bundleIdCapabilities`). Los perfiles los rehace
   `-allowProvisioningUpdates` en el siguiente archive.

## Prueba entre dos dispositivos (#75, 19-10-2026)

iPhone fisico (Apple, build de desarrollo) y emulator-5554 (Google), sala VG2VC5. Bien: crear sala y
entrar por codigo, empezar, pausar y reanudar desde los dos lados (mismo tiempo en los dos), salir y
entrar (la lista de miembros cambia en vivo), solicitud y aceptacion de amistad, invitacion a sala
que aparece sola con la hoja abierta, parar (los dos vuelven a cero y cada uno guarda sus minutos).

Arreglado durante la prueba: un aviso perdido cuando el canal fallaba al unirse (MissingPartition)
ahora se recupera al volver a estar suscrito; amistades e invitaciones llegan en vivo por el canal
`user:<id>` (antes hacia falta reabrir Coop).

Visto una vez y sin repetirse: en el iPhone, Salir no llego a llamar a `leave_room`. Si vuelve, mirar
el registro con `xcrun devicectl device process launch --console`.

Sin probar: notificaciones con la app cerrada (no las hay; propuesta en `docs/coop-push.md`, rama
aparte), bloquear y reportar entre los dos, borrar la cuenta.

## Actuar sobre un reporte

El correo trae nombre e id de las dos personas. Apple da 24 h. Desde el panel: cambiar
`display_name` en `flowtime.profiles`, o borrar la fila (cae todo lo de FlowTime en cascada). Para
borrar la cuenta entera, ademas el usuario en *Authentication > Users*, solo si no usa Chroma
(`select public.used_by_other_app('<id>')` en Chroma no sirve aqui: mirar `public.profiles`).

## La web de los enlaces (#72)

`store/web-coop/` lleva lo que va en el repo publico `BaltaJmn/flowtime-privacy`, junto a la politica:
`.nojekyll` (sin el, GitHub Pages no sirve `.well-known`), `.well-known/assetlinks.json` (SHA-256 de la
clave de firma de apps de Play, leido de la API el 19-10-2026), `.well-known/apple-app-site-association`
(Team ID `BR5ZH8XYP6`) y `404.html`, que GitHub Pages sirve para `/f/<codigo>` y `/r/<codigo>` sin la
app instalada y lleva a las tiendas (App Store `6819302003`). Publicarlo es un cambio de la web: con el
si del usuario.

### Cada movil a su tienda (#84, 21-10-2026)

Antes la pagina ensenaba las dos tiendas a todos. Ahora `404.html` mira el movil:

- Android: redirige a Play (`com.baltajmn.flowtime`).
- iPhone y iPad (tambien el iPad que pide el sitio de escritorio: se hace pasar por Mac, pero con
  pantalla tactil): pregunta a la busqueda de iTunes (`lookup?id=6819302003`, con CORS abierto) si la
  app esta a la venta. Si resultCount > 0, redirige a la App Store. Si no, ensena "FlowTime llega
  pronto al iPhone" en el idioma del movil (en, es, pt, de, fr) y no redirige a una pagina de Apple que
  diria que no existe. Si la busqueda falla (sin red), va a la App Store igualmente. La busqueda se
  cachea un dia en Apple: la hora va en la direccion (`&t=`), asi que el aviso se va en una hora
  desde que la app se publica, sin tocar la web.
- Ordenador: las dos tiendas, sin redirigir.
- Una ruta que no es de enlace: "Esta pagina no existe", sin tiendas.

Comprobar sin movil: `node store/web-coop.test.mjs` (ejecuta el script de la pagina con un navegador de
mentira para cada caso). Tras publicar, `curl -sL https://flowtime.baltajmn.dev/r/ABCDEF` debe traer
`id="apple"`. Los enlaces de la app, abiertos con la app instalada, nunca pasan por aqui.

Comprobar: `adb shell pm verify-app-links --re-verify com.baltajmn.flowtime` y despues
`adb shell pm get-app-links com.baltajmn.flowtime` dice `verified`.
