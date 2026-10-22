# Coop: estudiar juntos

Diseño de la v1, 19-10-2026, aprobado ese día. Lo que cambió al implementarlo está en cada sección.

## 1. Qué es

Una sala privada donde varias personas estudian con el mismo temporizador. Se entra por enlace o
código, o porque un amigo te invita. Se ve quien está y si la sala está concentrada o en descanso.

Fuera de v1: chat, salas públicas, reacciones, rankings, contadores, notificaciones push.

**La app entera sigue sin cuenta.** Iniciar sesión (Apple o Google) solo se pide al abrir Coop. El
correo con contraseña existe únicamente para la cuenta de App Review (sección 5). Sin
proyecto de Supabase configurado (`SupabaseConfig.url` nulo) la entrada a Coop no existe, igual que
Amigos en Chroma.

## 2. Decisiones que hay que aprobar

Las que cambian el producto. El resto del documento las da por buenas.

1. **Solo Pomodoro en sala.** La sala tiene su trabajo y su descanso (por defecto, los del Pomodoro
   de quien la crea; de 5 a 90 y de 1 a 30 minutos). FlowTime y Porcentaje no tienen sentido en
   grupo: el descanso de cada uno depende de lo que trabajó él.
2. **Cualquiera de la sala controla el temporizador** (empezar, pausar, seguir, saltar descanso,
   parar). Son salas privadas entre conocidos, y así no hay "anfitrion" del que dependa nada: si
   quien la creó se va, la sala sigue igual. Cada cambio sale en la sala como "Ana ha pausado".
3. **Una sala a la vez por persona, 8 personas como mucho por sala, 50 amigos.** Una sala vacía se
   borra sola.
4. **La fase de cada persona es la de la sala.** El temporizador es uno, así que "concentrado" o
   "descanso" es el mismo para todos los que están dentro. No se usa Realtime presence: en iPhone el
   sistema corta la conexión con el móvil bloqueado, que es justo cuando se estudia, y la presencia
   dirÃ­a "fuera" de quien más concentrado estÃ¡. Estar en la sala es una fila en la base; irse es
   pulsar "Salir". Si más adelante se quiere un punto de "con la app abierta", presence se añade sin
   tocar nada de esto.
5. **Invitar a un amigo no avisa por push.** La invitación aparece al abrir Coop (y como un punto en
   la entrada a Coop). Push pide FCM y APNs con servidor: fuera de v1.
6. **La entrada a Coop está en la pantalla de Enfoque**, un boton junto al selector de modo, no una
   quinta pestaña.
7. **Cuenta compartida con Chroma** (decidido el 19-10): un solo proyecto, un solo `auth.users`. Quien
   entra con el mismo Apple o Google en las dos apps es el mismo usuario, pero con perfil, nombre y
   amigos distintos en cada una. Borrar la cuenta desde FlowTime borra lo de FlowTime y solo borra
   el usuario si no tiene nada en Chroma (sección 6). Esto va en la política de privacidad.

## 3. Servidor

### 3.1 Un proyecto, dos apps

Proyecto compartido con Chroma (lo crea la sesión de Chroma; plan Free, región eu-central-1).
Chroma vive en `public`; FlowTime en el esquema `flowtime`, expuesto en la API (*Settings > API >
Exposed schemas*, y `[api] schemas` en `supabase/config.toml` para local). El cliente usa
`Postgrest { defaultSchema = "flowtime" }`.

**Migraciones.** `supabase db push` compara con una sola tabla de historial por proyecto y falla si
el remoto tiene versiones que no están en local: con dos repos empujando al mismo proyecto, cada uno
rompería el `push` del otro. Por eso:

- `supabase/migrations/` de este repo es la fuente de FlowTime y sirve tal cual para `supabase start`
  y `supabase test db` en local y en CI.
- Al proyecto compartido se aplican con `supabase/aplicar.sh`, que pasa con `psql` cada fichero que
  no este en `public.flowtime_migraciones` (una tabla sin permisos para nadie; en `public` porque tiene que existir antes que el esquema) y lo apunta, todo en una transacción
  por fichero. No toca `supabase_migrations`, así que el `push` de Chroma sigue funcionando.
- Las funciones Edge de FlowTime llevan prefijo `flowtime-` (el espacio de nombres es del proyecto).

### 3.2 Tablas

Copiadas del modelo de Chroma (`../color/docs/tecnico.md` 8) donde sirve, en `flowtime`:

```sql
create table flowtime.profiles (
  id uuid primary key references auth.users on delete cascade,
  display_name text not null check (char_length(display_name) between 1 and 30),
  invite_code text not null unique default encode(extensions.gen_random_bytes(5), 'hex'),
  created_at timestamptz not null default now()
);

-- Igual que en Chroma: una fila por pareja, tope de 50 aceptados por trigger.
create table flowtime.friendships (a, b, requested_by, status, created_at);
create table flowtime.blocks (blocker, blocked);
create table flowtime.invite_attempts (user_id, created_at);   -- enlaces y códigos que no valen

create table flowtime.rooms (
  id uuid primary key default gen_random_uuid(),
  code text not null unique,                 -- 6 letras sin ambiguas (sin 0/O, 1/I/L): ~30 bits
  owner uuid not null references flowtime.profiles on delete cascade,
  work_ms int not null check (work_ms between 300000 and 5400000),
  break_ms int not null check (break_ms between 60000 and 1800000),
  -- El temporizador (sección 4). running_since null: parado o en pausa.
  status text not null default 'idle' check (status in ('idle', 'running', 'paused')),
  running_since timestamptz,
  counted_ms bigint not null default 0,
  changed_by uuid,                           -- quien tocó el temporizador por última vez
  created_at timestamptz not null default now()
);

create table flowtime.room_members (
  room_id uuid not null references flowtime.rooms on delete cascade,
  user_id uuid not null references flowtime.profiles on delete cascade,
  joined_at timestamptz not null default now(),
  primary key (room_id, user_id),
  unique (user_id)                           -- una sala a la vez
);

create table flowtime.room_invites (         -- un amigo invita a otro a su sala
  room_id uuid not null references flowtime.rooms on delete cascade,
  invitee uuid not null references flowtime.profiles on delete cascade,
  invited_by uuid not null references flowtime.profiles on delete cascade,
  created_at timestamptz not null default now(),
  primary key (room_id, invitee)
);

create table flowtime.reports (
  id bigint generated always as identity primary key,
  reporter uuid not null references flowtime.profiles on delete cascade,
  reported uuid not null,                    -- sin clave: puede irse antes de que se lea
  reason text not null check (reason in ('name', 'other')),
  created_at timestamptz not null default now(),
  notified_at timestamptz,
  unique (reporter, reported)
);
```

Lo único que otro usuario ve de ti es tu nombre. No hay fotos ni texto libre: no hace falta Storage
ni la purga de Chroma.

### 3.3 Funciones y RLS

El cliente no escribe directamente en amistades, bloqueos, salas ni miembros: todo por funciones
`security definer` con `auth.uid()`, como en Chroma. Cada migracion revoca `execute` a `anon` y a
`public` y se lo da a `authenticated` solo en las que llama la app.

| Función | Que |
|---|---|
| `my_profile`, `my_friendships`, `my_blocks`, `request_friend(code)`, `accept_friend`, `decline_friend`, `remove_friend`, `block_user`, `unblock_user`, `regenerate_code` | Las de Chroma, mismás respuestas (`sent`, `accepted`, `already`, `self`, `blocked`, `not_found`, `limit`, `too_many`) |
| `create_room(work_ms, break_ms)` | Crea la sala con código nuevo y te mete. Si estabas en otra, sales de ella |
| `join_room(code)` | `joined`, `full`, `not_found`, `too_many`. Un código que no existe cuenta en `invite_attempts` (20 por hora entre los dos tipos de código). Si el dueño de la sala te bloqueó o tú a él, `not_found` |
| `leave_room()` | Sales. Si eras el dueño pasa al que más tiempo lleva; si no queda nadie, la sala se borra |
| `remove_member(user)` | Solo el dueño |
| `invite_to_room(friend)` | Solo a un amigo (`is_friend`) y solo a tu sala actual |
| `decline_room_invite(room)` | Borra la invitación |
| `room_control(action)` | `start`, `pause`, `resume`, `skip_break`, `stop`, sobre tu sala. Hace la aritmética de la sección 4 con `now()` del servidor y devuelve la fila |
| `my_room()` | Tu sala con sus miembros (id y nombre) y `server_now` |
| `my_room_invites()` | Invitaciones recibidas con el nombre de la sala (el de su dueño) y de quien invita |

| Tabla | select | escribir |
|---|---|---|
| `profiles` | yo, amigos, solicitudes abiertas, y quien comparte sala conmigo; solo `id` y `display_name` | solo mi fila, solo `display_name` |
| `rooms`, `room_members` | solo la sala en la que estoy | solo por funciones |
| `room_invites` | las que recibo o mandó | solo por funciones |
| `friendships`, `blocks` | las mias | solo por funciones |
| `reports` | nadie | insert con `reporter = auth.uid()` y el reportado amigo o de mi sala; 20 al día |
| `invite_attempts`, `flowtime_migraciones` | nadie | nadie |

Bloquear a alguien de tu sala: si eres el dueño, lo saca; si no, no lo saca (no es tu sala) pero
deja de poder invitarte y tú a él, y no podéis volver a coincidir por código en una sala de uno de
los dos. Bloquear también borra la amistad, como en Chroma.

### 3.4 Realtime

Un canal **privado** por sala, `room:<id>`, solo con broadcast. Las politicas sobre
`realtime.messages` dejan leer el canal a quien es miembro de esa sala (`realtime.topic()`) y
escribir a nadie: los mensajes los manda la base con `realtime.send(...)` desde las funciones de
la tabla anterior, así que un cliente modificado no puede inventarse un "Ana ha pausado".

Eventos: `timer` (la fila del temporizador y quien lo cambio) y `members` (alguien entra o sale). El
cliente, al recibir `members`, vuelve a pedir `my_room()`. Al conectarse o volver de segundo plano
siempre pide `my_room()`: lo que se perdió mientras no escuchaba no se reproduce, se relee.

## 4. El temporizador compartido

Nunca se mandan ticks. La sala guarda un estado y cada móvil calcula el resto con su reloj.

- `status`: `idle`, `running`, `paused`.
- `counted_ms`: tiempo del ciclo contado antes de la última pausa.
- `running_since`: hora del servidor en que empezo a contar (null en pausa o parada).
- `work_ms`, `break_ms`: un ciclo es trabajo seguido de descanso, sin descanso largo (FlowTime no lo
  tiene).

```
transcurrido = counted_ms + (running ? ahora_servidor - running_since : 0)
pos          = transcurrido mod (work_ms + break_ms)
fase         = pos < work_ms ? trabajo : descanso
restante     = fase == trabajo ? work_ms - pos : work_ms + break_ms - pos
```

Los cambios de fase no los escribe nadie: salen de la cuenta. Por eso no hace falta anfitrion, y
una sala sigue bien aunque todos tengan el móvil bloqueado.

- **Saltar descanso**: `counted_ms` pasa al principio del ciclo siguiente.
- **Parar**: `idle`, `counted_ms = 0`.
- **Tope**: una sala en marcha más de 8 ciclos seguidos (unas 4 h a 25+5) se lee como parada en
  todos los móviles, y la siguiente llamada a `room_control` o `my_room` la deja `idle`. Evita que
  alguien que se fue sin parar acumule horas de "estudio" en el historial de los demas.

**Reloj.** Cada móvil calcula `desfase = server_now - (t_envio + t_respuesta) / 2` con la respuestá de
`my_room()` y `room_control`, y usa `ahora_local + desfase`. Un error de unos cientos de
milisegundos entre móviles no se nota en una pantalla de segundos.

**En el móvil.** La sala no es un temporizador aparte: mueve el `FocusEngine` de siempre. `FocusState`
gana un campo `coop` (id de la sala y sus `work_ms` y `break_ms`); con el, el motor toma esas
duraciones en vez de las de Ajustes, sigue tras el descanso aunque el ajuste diga que no, y sus
acciones van a `room_control` en vez de cambiar el estado directamente. Lo que llega de la sala se
convierte en un `FocusState` anclado (`anchorWall`, `anchorElapsed`) con el desfase aplicado. Así la
notificación, las alarmás de fin de fase, No molestar, el sonido y el historial funcionan igual que
solo, y cada persona guarda su propio trabajo en su propio móvil, como siempre. Al salir de la sala el
motor se para con `stop()` (guarda lo trabajado).

## 5. Pantallas

1. **Entrada**: boton en Enfoque. Sin cuenta, abre la presentación de Coop con los dos botones de
   inicio de sesión y una línea: "El resto de la app sigue sin cuenta". Debajo, un enlace discreto
   "Iniciar sesión con correo" (20-10-2026) que abre correo y contraseña. **Existe solo para la cuenta
   de App Review**, que no puede usar Apple ni Google en un dispositivo ajeno (pide SMS o un segundo
   correo). Solo entra, nunca da de alta (`CoopAccount.signInWithEmail`), y el servidor rechaza
   cualquier otro alta por correo: un hook de Supabase (Before User Created, en el repo de Chroma)
   deja pasar solo el correo de revisión, que se crea a mano en el panel. Un fallo dice "correo o
   contraseña incorrectos, usa Apple o Google". El enlace está a la vista y no es un gesto oculto
   (Apple 2.3.1). No recoge datos nuevos: la política de privacidad no cambia.
2. **Primera vez**: la casilla de 16 años o mas (obligatoria, la de Chroma) esta en la entrada y activa los botones
   de Apple, Google y correo. Despues de entrar el perfil se crea solo, sin pedir nombre ni correo (Apple 4: la
   2.4.0 de iPhone y la 1.2 de Chroma lo pedian): el primer nombre que trae la cuenta (Google lo da; Apple casi
   nunca) o "Focus" con cuatro cifras (`suggestedName`). Se cambia en Ajustes > Cuenta de Coop > Cambiar nombre.
   Si hay sesion sin perfil y la casilla no esta marcada (quien entro con una version anterior y se quedo en
   el nombre), la hoja solo pide la edad.
3. **Coop sin sala**: "Crear sala", "Unirme con código", invitaciones recibidas, y la lista de amigos
   con "Añadir amigo" (enlace y QR, como Chroma).
4. **Sala**: el anillo de siempre con la fase de la sala, los miembros (nombre y, al mantener
   pulsado, reportar o bloquear), compartir enlace o código, invitar amigos, salir. Los cambios de
   otros salen como una línea breve ("Ana ha pausado").
5. **Cuenta**, en Ajustes > Cuenta de Coop (solo con sesion): nombre, bloqueados, cerrar sesion, borrar
   cuenta. Primero estuvo al final de la hoja; la critica de diseno del 19-10-2026 la saco para que la
   hoja quede para estudiar juntos. La hoja lo recuerda en una linea al final.

Enlaces: `https://flowtime.baltajmn.dev/r/<código>` (sala) y `/f/<código>` (amistad), en la web que ya
sirve la política (`BaltaJmn/flowtime-privacy`), con `assetlinks.json`,
`apple-app-site-association` y un `404.html` que lleva a las tiendas, como Chroma. Un enlace abierto
sin sesión se guarda en memoria y se usa al terminar de entrar.

## 6. Cuenta, borrado y moderación

- **Sesión**: `supabase-kt` 3.8.0 con Auth, Postgrest, Realtime y Functions, y `compose-auth`
  (Android: solo Google, nativo. iPhone: Apple nativo y Google con el cliente iOS propio de FlowTime,
  sin SDK, en `GoogleSignInIos.kt`, porque el flujo web de Supabase ensenaria el consentimiento de
  Chroma). Una cuenta de Apple no se abre en Android: se acepta. Se copia `Social.kt` de Chroma casí tal cual, con su manejo de
  sesión perdida.
- **Borrar cuenta** (Apple 5.1.1(v), Play): función `flowtime-delete-account`. Saca a la persona de su
  sala con `leave_room`, borra los reportes sobre ella y su fila de `flowtime.profiles` (lo demás cae
  en cascada). Después borra el usuario de `auth.users` **solo si no tiene fila en
  `public.profiles`** (Chroma). El `delete-account` de Chroma pregunta a
  `public.used_by_other_app(uid)`, que FlowTime redefine para mirar `flowtime.profiles`.
- **Reportar**: inserta en `flowtime.reports`; `flowtime-report-notify` manda el correo por Resend
  como en Chroma (mismos secretos de Resend). Actuar: borrar el perfil desde el panel.
- **Bloquear y desbloquear**: como Chroma, más lo de salas de 3.3.
- **Tienda** (con el si del usuario antes de publicar nada): política de privacidad (cuenta opcional,
  nombre visible, compartida con Chroma, borrado), Data Safety (nombre e identificador de usuario,
  correo de la cuenta), App Privacy (Contact Info y User ID, vinculados, no para seguimiento) y
  clasificación por edad (interacción entre usuarios). Sign in with Apple y Associated Domains en
  `iosApp.entitlements`.

## 7. Coste en plan Free

| Recurso | Límite Free | Coop |
|---|---|---|
| Conexiones Realtime | 200 a la vez (por proyecto) | Una por móvil con Coop abierto. Chroma no usa Realtime |
| Mensajes Realtime | 2 M al mes | Solo al tocar el temporizador o al entrar o salir: unos 5 por sesión y persona |
| Base | 500 MB | Filas de bytes; nada de fotos |
| Usuarios activos | 50 000 al mes | Compartido con Chroma |
| Funciones Edge | 500 000 llamadas | Borrar cuenta y reportes |

El riesgo real del Free: el proyecto se pausa tras 7 días sin actividad. Con usuarios de dos apps no
deberÃ­a pasar, pero si pasa, Coop dice "sin conexión" y el resto de la app no se entera.

## 8. Orden de trabajo (issues)

Cada una, un commit. Builds con el target mínimo del modulo tocado; el completo al final.

1. Servidor: esquema `flowtime`, perfiles, amistades, bloqueos, reportes, `aplicar.sh`, tests de RLS.
2. Servidor: salas, miembros, invitaciones, `room_control`, canal privado de Realtime, tests.
3. Servidor: `flowtime-delete-account` y `flowtime-report-notify`.
4. Cliente de Supabase, sesión, perfil y `SupabaseConfig`, en `core:data/coop` (no hizo falta un módulo:
   el motor que sigue a la sala vive en `core:data`).
5. Motor: `FocusState.coop` y la cuenta de la sección 4, con tests.
6. Pantallas: entrada, inicio de sesión, nombre y edad.
7. Pantallas: amigos, enlace y QR, solicitudes, bloquear, reportar.
8. Pantallas: sala, crear, unirse, invitar, salir, con Realtime.
9. Cuenta y borrar cuenta (Ajustes > Cuenta de Coop).
10. Enlaces universales (web, `assetlinks.json`, AASA) y entitlements de iPhone.
11. Textos en los 14 idiomas.
12. Politica, Data Safety, App Privacy y edad (borradores; publicar, con el si del usuario).
13. Verificación entre dos dispositivos.
