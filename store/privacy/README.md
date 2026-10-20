# Política de privacidad

`index.html` es la política, en inglés y español en la misma página. Un solo fichero, sin fuentes ni
scripts de fuera: la publicación en Play depende de que esta URL responda.

Este fichero es el **original**. Lo que se publica es una copia.

## Dónde se publica

**https://flowtime.baltajmn.dev/**, la misma dirección en tres sitios que tienen que coincidir:

- Play Console, *Contenido de la aplicación > Política de privacidad*.
- App Store Connect, *Información de la app > URL de la política de privacidad* (todos los idiomas).
- La app, *Ajustes > Otros > Política de privacidad* (`PRIVACY_URL` en `SettingsScreen.kt`).
- Esta carpeta.

Este repo es privado, y GitHub Pages en un repo privado es de pago. Por eso, como MoodTraker, se sirve
desde un repo público propio, `BaltaJmn/flowtime-privacy`, con `index.html` y `CNAME`. Cloudflare
sirve la zona `baltajmn.dev`: el registro es `CNAME flowtime` a `baltajmn.github.io`, con proxy
(`~/keys/LEEME.md`).

## Cambiarla

1. Editar `index.html` aquí, con la fecha nueva arriba en los dos idiomas.
2. Copiarlo tal cual al repo público:

   ```bash
   gh repo clone BaltaJmn/flowtime-privacy /tmp/flowtime-privacy
   cp store/privacy/index.html /tmp/flowtime-privacy/ && git -C /tmp/flowtime-privacy commit -am "Actualizar la politica" && git -C /tmp/flowtime-privacy push
   ```

3. Si cambia lo que sale del móvil, cambiar también la seguridad de los datos (abajo) en Play Console.

## Seguridad de los datos

Lo único que sale del móvil es lo de RevenueCat (#55). Respuestas de Play Console:

| Pregunta | Respuesta |
|---|---|
| ¿Recoge o comparte alguno de los tipos de datos obligatorios? | Sí |
| ¿Se cifran en tránsito todos los datos recogidos? | Sí, HTTPS del SDK de RevenueCat |
| ¿Permite crear una cuenta? | No |
| ¿Se puede iniciar sesión con cuentas de fuera? | No |
| ¿Ofreces una forma de pedir que se borren los datos? (opcional) | Sin responder: el "Sí" pide una URL que destaque los pasos, y la política lo explica (por correo, con el número de pedido) en mitad del texto. Con una página de borrado propia, "Sí" y su URL |

| Tipo | Recogido | Compartido | Efímero | Obligatorio | Finalidad |
|---|---|---|---|---|---|
| Información financiera > Historial de compras | Sí | No | No | Sí | Funcionalidad de la app |
| Identificadores de dispositivo u otros identificadores | Sí | No | No | Sí | Funcionalidad de la app |

**Compartido: no**, porque RevenueCat trata los datos por cuenta nuestra como proveedor de
servicios, y Play no cuenta eso como compartir.

Lo que no se marca: el identificador de publicidad (el manifiesto quita `AD_ID`), la valoración
(el diálogo es de Google Play y la app no la ve), la copia de Android (va a la cuenta de Google del
usuario, no a nosotros) y los ficheros que exporta el usuario (los guarda donde elige).

## App Store: privacidad de la app

Lo mismo con los nombres de Apple, como en Purl. Las respuestas de *App Privacy* en App Store
Connect tienen que coincidir con `iosApp/iosApp/PrivacyInfo.xcprivacy`, que lo declara dentro de la
app porque RevenueCat va enlazado en el binario y no trae su manifiesto:

| Tipo | Vinculado al usuario | Rastreo | Finalidad |
|---|---|---|---|
| Compras > Historial de compras | No | No | Funcionalidad de la app y análisis de datos |

Los identificadores no se marcan: RevenueCat pide *ID de usuario* solo con IDs propios e *ID de
dispositivo* solo con integraciones que usen el IDFA, y la app usa su ID anónimo. Así quedaron las
cuatro apps de la familia, publicado en App Store Connect el 05-10-2026.

Si cambia lo que sale del móvil, se cambian las tres: Play, App Store Connect y el manifiesto.

## Coop (#74): respuestas para la 2.4.0

Con Coop sale del movil algo mas, solo para quien lo abre (docs/coop.md). La politica ya lo cuenta
(`index.html`, 19-10-2026). Politica y `delete.html` publicadas el 20-10-2026 en flowtime.baltajmn.dev.
Lo de las consolas se cambia con la version que lleve Coop (2.4.0), no antes: la 2.2.2 de iOS se
reenvia sin tocar App Privacy.

**Revisado contra el codigo el 20-10-2026** (`main` de la 2.4.0, push incluido). Lo que sale del
movil con Coop, y solo con sesion de Coop:

- A Supabase (`baltajmn-social`, UE): el ID y el correo que dan Apple o Google al entrar (con Apple
  puede ser el de reenvio privado), el nombre elegido, el codigo de invitacion, amigos y solicitudes,
  bloqueados, la sala en la que estas, las invitaciones, los reportes (motivo cerrado, sin texto
  libre) y el token de avisos con el idioma de la app.
- A Google (FCM, Android) o Apple (APNs, iPhone): el token y el texto de cada aviso.
- A Resend: el correo de un reporte, con los dos nombres, solo hacia nosotros.
- Lo que no sale: sesiones, tareas, etiquetas y estadisticas (el temporizador de la sala es de la
  sala, y cada uno guarda sus minutos en su movil).

Corregido el 20-10-2026: en Android el SDK de FCM pedia su token al abrir la app a todo el mundo (y
con eso el ID de instalacion de Firebase llegaba a Google aunque no se usara Coop). Desde `bddea97`
arranca solo con sesion de Coop. En el iPhone el token de APNs lo da Apple a todos, pero solo se
sube al servidor con sesion.

**Play Console, seguridad de los datos.** Cambian:

| Pregunta | Respuesta |
|---|---|
| ¿Se cifran en transito todos los datos recogidos? | Si (HTTPS de Supabase, FCM y RevenueCat) |
| ¿Permite crear una cuenta? | Si, con *OAuth* (en Android solo Google; en iPhone Apple y Google) |
| ¿Se puede iniciar sesion con cuentas de fuera? | Si |
| URL para borrar la cuenta | `https://flowtime.baltajmn.dev/delete.html` |
| ¿Ofreces una forma de pedir que se borren los datos? | Si: desde la app (Ajustes > Cuenta de Coop > Borrar cuenta) y en la misma URL |

| Tipo | Recogido | Compartido | Efimero | Obligatorio | Finalidad |
|---|---|---|---|---|---|
| Informacion personal > Nombre | Si | No | No | No (solo Coop) | Funcionalidad de la app |
| Informacion personal > Direccion de correo | Si | No | No | No (solo Coop) | Funcionalidad de la app, gestion de la cuenta |
| Informacion personal > ID de usuario | Si | No | No | No (solo Coop) | Funcionalidad de la app, gestion de la cuenta |
| Contactos | Si | No | No | No (solo Coop) | Funcionalidad de la app. La lista de amigos y solicitudes: Play cuenta como contactos el "grafo social" (nombres de usuario de tus contactos). No se lee la agenda del movil |
| Identificadores de dispositivo u otros identificadores | Si | No | No | **Si** (no cambia) | Funcionalidad de la app. Ya estaba marcada por RevenueCat como obligatoria, y Play pide una sola respuesta por tipo: se queda en obligatoria. Ahora cubre tambien el token de FCM y el ID de instalacion de Firebase, solo con Coop |
| Informacion financiera > Historial de compras | Si | No | No | Si | Sin cambios (RevenueCat) |
| Actividad en apps > Otro contenido generado por el usuario | No | | | | Lo unico que escribe el usuario es su nombre (ya en Nombre). Los reportes son un motivo cerrado |
| Actividad en apps > Otras acciones | No | | | | Quien empieza o pausa la sala es estado de la sala, no un registro de uso: se borra con la sala |

Supabase, Resend, Google (FCM) y Apple (APNs) tratan por cuenta nuestra: no es compartir. Que tus
amigos y tu sala vean tu nombre tampoco: lo provoca el usuario y lo espera (excepcion de Play para
lo que el usuario comparte a proposito).

**Play Console, acceso a la aplicacion.** *Toda o parte de la funcionalidad esta restringida*. Una
instruccion, con la cuenta de Google de prueba (la crea el usuario: hueco en `store/formularios.md`,
"Para enviar por la manana").

**App Store Connect, App Privacy.** Se anaden, vinculados al usuario, sin rastreo, finalidad
*Funcionalidad de la app*: *Informacion de contacto > Nombre*, *Informacion de contacto > Correo
electronico*, *Contactos* (la lista de amigos: Apple pone el "grafo social" como ejemplo),
*Identificadores > ID de usuario* e *Identificadores > ID de dispositivo* (el token de APNs de los
avisos de Coop, docs/coop-push.md). *Compras > Historial de compras* sigue igual: sin vincular,
porque RevenueCat sigue con su ID anonimo (la app no llama a `logIn` con el ID de Coop). El
manifiesto `PrivacyInfo.xcprivacy` declara los mismos cinco tipos (`NSPrivacyCollectedDataTypeName`,
`...EmailAddress`, `...Contacts`, `...UserID`, `...DeviceID`, vinculados, sin rastreo,
`NSPrivacyCollectedDataTypePurposeAppFunctionality`). La sesion de Supabase se guarda en
`NSUserDefaults`, que ya esta declarado (`CA92.1`).

**Clasificacion por edad.** Play (cuestionario IARC): *Los usuarios pueden interactuar* = Si (nombres
visibles entre usuarios, solicitudes de amistad e invitaciones, sin chat); *Comparte la ubicacion* =
No; *Compras digitales* = Si. App Store: *Contenido generado por usuarios* = Si (el nombre visible),
*Mensajeria y chat* = No, el resto igual que en la 2.2.2; la app pide 16 anos o mas para Coop. Apple
1.2 pide reportar, bloquear y actuar en 24 h: hecho (`store/coop.md`, "Actuar sobre un reporte").

**Iniciar sesion con Apple (4.8).** Google como opcion obliga a ofrecer Apple, igual de visible: hecho.

Pendiente fuera de FlowTime: Chroma tiene amigos y grupos con el mismo modelo y no declara
*Contactos* en ninguna de las dos tiendas. Si se acepta aqui, conviene igualarlo alli.
