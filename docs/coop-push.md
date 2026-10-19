# Coop: avisos con la app cerrada

Hoy los avisos de Coop van por Realtime (`user:<id>`, `room:<id>`) y solo llegan con la app abierta.
Esto anade notificaciones push para lo que pide una respuesta de la otra persona.

## Que se avisa

| Cambio | A quien | Texto (es) |
|---|---|---|
| `friendships` insert, `status = 'pending'` | el que no la pidio | "Ana quiere ser tu amiga en FlowTime" |
| `friendships` update `pending` a `accepted` | `requested_by` | "Ana ha aceptado tu solicitud" |
| `room_invites` insert | `invitee` | "Ana te invita a su sala" |

Fuera: "alguien ha empezado o pausado la sala". Quien esta en una sala suele tenerla abierta, y un
aviso por cada pausa de cada miembro es ruido. Si se echa en falta, es otro tipo mas en la misma
funcion.

Sin reintentos: un push que no sale no se repite. La lista dentro de la app sigue siendo la verdad, y
Realtime la refresca al abrir.

Tocar el aviso abre la app en Coop.

## Proveedor

- **Android: FCM** (HTTP v1). No hay otra via sin servicios propios. Proyecto de Firebase nuevo,
  `flowtime`, o Firebase anadido a `chroma-baltajmn` (ya es donde vive la cuenta de Play); da igual
  para el codigo.
- **iOS: APNs directo**, sin el SDK de Firebase en el iPhone. El token sale de
  `registerForRemoteNotifications` y la Edge Function firma un JWT ES256 con la clave `.p8`. Ahorra
  una dependencia pesada en iOS y un paso por Google. Riesgo: APNs exige HTTP/2; el `fetch` de Deno
  lo negocia, pero si en el runtime de Supabase fallara, el plan B es FCM tambien en iOS (SDK de
  Firebase y la misma `.p8` subida a Firebase).

## Tokens: `flowtime.push_tokens`

```sql
create table flowtime.push_tokens (
  token text primary key,
  user_id uuid not null references flowtime.profiles on delete cascade,
  platform text not null check (platform in ('android', 'ios', 'ios-sandbox')),
  lang text not null default 'en',
  updated_at timestamptz not null default now()
);
```

- RLS activada y **ninguna politica**: nadie lee ni escribe la tabla directamente. Dos funciones
  `security definer` para `authenticated`:
  - `flowtime.register_push_token(token, platform, lang)`: upsert por `token` y se lo queda quien lo
    registra (un movil que cambia de cuenta no sigue avisando a la anterior).
  - `flowtime.unregister_push_token(token)`: al cerrar sesion en Coop.
- `on delete cascade` desde `profiles`: borrar la cuenta (`delete_my_data`) borra los tokens.
- `ios-sandbox`: builds de Xcode (APNs de desarrollo); TestFlight y App Store son `ios`.
- `lang`: el idioma de la app al registrar. Los textos se eligen en el servidor con el.
- La Edge Function borra los tokens que APNs (410, `BadDeviceToken`) o FCM (`UNREGISTERED`) dan por
  muertos.

## Quien envia

Igual que `flowtime.notify_report`:

1. Triggers nuevos `friendships_push` y `room_invites_push` (aparte de los `*_notify` de Realtime, que
   no se tocan) llaman a `flowtime.push(person, kind, actor)`.
2. `flowtime.push` hace `net.http_post` a la Edge Function `flowtime-push` con la URL y el secreto del
   Vault (`project_url`, `report_webhook_secret`: mismo emisor, mismo nivel de confianza, no hace
   falta otro). Si falla, `raise warning` y el cambio sigue adelante.
3. `flowtime-push` lee los tokens de `person` y el `display_name` de `actor` con la service role,
   arma el texto en `lang` y envia a FCM o APNs.

El cuerpo del aviso solo lleva el nombre visible de quien actua; ni ids ni codigos de sala.

Migracion: `supabase/migrations/20261020100000_push.sql`, aplicada a mano y apuntada en
`public.flowtime_migraciones`. Test: `supabase/tests/database/push.test.sql` (pgTAP: RLS cerrada,
registrar y reasignar token, que cada cambio llame a `push` con el destinatario y el tipo
correctos, y que no avise de lo que no toca, p. ej. una amistad borrada).

## Cliente

- **Permiso**: la app ya lo pide para el temporizador (`POST_NOTIFICATIONS` en Android 13+,
  `requestAuthorization` en iOS). No se pide otra vez; sin permiso no hay aviso y Coop sigue igual.
- **Cuando se registra**: al tener sesion en Coop y en cada arranque (el token puede cambiar), y
  `unregister` al cerrar sesion. Una funcion comun en `core/data/.../coop` con un `expect` para
  sacar el token de cada plataforma.
- **Android**: `firebase-messaging` en `app`, inicializado con `FirebaseOptions` en codigo (sin el
  plugin de google-services; son valores publicos). Un `FirebaseMessagingService` minimo para
  `onNewToken`. Canal de notificacion `coop` aparte del del temporizador.
- **iOS**: capacidad Push Notifications (`aps-environment`) en el App ID y el entitlement;
  `registerForRemoteNotifications` y el token por el delegate de la app.

## Textos (14 idiomas)

Tres frases con el nombre como unico hueco, en un objeto dentro de `flowtime-push`
(en, es, de, fr, it, pt, nl, pl, ru, tr, ja, ko, hi, id). Si `lang` no esta, ingles. Traduce sonnet
(son textos de cara al publico), no haiku. Sin genero en las que lo marcan: "quiere ser tu amigo/a"
se reescribe como "te ha enviado una solicitud de amistad".

## Privacidad y tiendas

- **Politica** (`store/privacy`): nueva seccion "Notificaciones de Coop": guardamos un identificador
  de notificaciones del dispositivo ligado a la cuenta de Coop, solo para avisar de solicitudes e
  invitaciones; lo entregamos a Google (Firebase Cloud Messaging) o Apple (APNs) para que llegue el
  aviso, con el nombre visible de quien lo provoca; se borra al cerrar sesion o borrar la cuenta.
- **Data Safety (Play)**: anadir "Identificadores del dispositivo u otros": recogido, no compartido
  (proveedor de servicio), ligado a la cuenta, obligatorio para Coop, finalidad "Funcionalidad de la
  app". El SDK de FCM tambien genera el Firebase Installation ID: cae en la misma casilla.
- **App Privacy (App Store)**: "Identificadores > ID de dispositivo", vinculado al usuario, finalidad
  "Funcionalidad de la app", sin rastreo.

## Credenciales (las crea el usuario)

| Que | Donde | Se guarda |
|---|---|---|
| Clave APNs `.p8` (Key ID) | developer.apple.com > Keys > + > Apple Push Notifications service | `~/keys/apns.p8` y secretos de la Edge Function `APNS_KEY`, `APNS_KEY_ID`; Team ID `BR5ZH8XYP6` ya esta |
| Proyecto Firebase con app Android `com.baltajmn.flowtime` | console.firebase.google.com | valores publicos (app id, api key, project id) en el codigo |
| Cuenta de servicio con rol "Firebase Cloud Messaging API Admin" | Google Cloud > IAM > Service Accounts > Keys > JSON | `~/keys/flowtime-fcm.json` y secreto `FCM_SERVICE_ACCOUNT` de la Edge Function |

Ninguna pasa por el chat ni por el repo; se suben a Supabase con `supabase secrets set` leyendo el
fichero.

## Hecho cuando

Migracion con test pgTAP en verde, Android e iOS registrando token, y un aviso real recibido en el
iPhone del usuario y en `emulator-5554` con la app cerrada.

## Estado y lo que falta (19-10-2026)

Hecho: migracion aplicada y apuntada (pgTAP 12/12), `flowtime-push` desplegada (`verify_jwt` off),
Push activado en el App ID, valores publicos de Firebase (`flowtime-pomodoro`) en `CoopPush.kt`,
token de `emulator-5554` (BaltaAnd) ya en `flowtime.push_tokens`. Credenciales en `~/keys`:
`apns.p8` (Key ID `R33CWTQ635`), `flowtime-fcm.json`, `flowtime-google-services.json`.

Falta, solo el usuario (Claude no escribe secretos en un formulario):

1. https://supabase.com/dashboard/project/djfbmvagflfptqhabdln/functions/secrets, tres secretos:
   - `APNS_KEY_ID` = `R33CWTQ635`
   - `APNS_KEY`: `pbcopy < ~/keys/apns.p8` y pegar
   - `FCM_SERVICE_ACCOUNT`: `pbcopy < ~/keys/flowtime-fcm.json` y pegar
   - despues `pbcopy < /dev/null`
2. Rotar las dos claves privadas, que pasaron por el chat: en Firebase (Cuentas de servicio) una
   clave nueva y borrar la `3fb86116...`; en developer.apple.com > Keys una nueva y revocar
   `R33CWTQ635`. Mover los ficheros nuevos a `~/keys` con `mv` y repetir el paso 1.

Prueba (Claude, con los secretos puestos): app en segundo plano y, en SQL,

```sql
select flowtime.push(a.id, 'friend_request', b.id) from flowtime.profiles a, flowtime.profiles b
where a.display_name = 'BaltaAnd' and b.display_name = 'BaltaJmn';
```

Para el iPhone, igual con los nombres cambiados, tras instalar desde Xcode (`ios-sandbox`) la build
de esta rama y abrir la app una vez con la sesion de Coop. Si falla, los logs de `flowtime-push`
en el panel (Edge Functions > Logs) dicen `fcm ...` o `apns <estado> <motivo>`.
