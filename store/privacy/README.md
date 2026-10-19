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

## Coop (#74): lo que cambia, en borrador

Con Coop sale del movil algo mas, solo para quien lo abre (docs/coop.md). La politica ya lo cuenta
(`index.html`, 19-10-2026, sin publicar). Nada de esto se publica ni se cambia en las consolas sin el
si del usuario, y conviene hacerlo con la version que lleve Coop, no antes.

**Play Console, seguridad de los datos.** Cambian:

| Pregunta | Respuesta |
|---|---|
| ¿Permite crear una cuenta? | Si, con OAuth (Apple y Google) |
| ¿Se puede iniciar sesion con cuentas de fuera? | Si |
| ¿Ofreces una forma de pedir que se borren los datos? | Si: desde la app (Coop > Cuenta > Borrar cuenta). Play pide ademas una URL de borrado: hace falta una pagina, como `delete.html` de Chroma |

| Tipo | Recogido | Compartido | Efimero | Obligatorio | Finalidad |
|---|---|---|---|---|---|
| Informacion personal > Nombre | Si | No | No | No (solo Coop) | Funcionalidad de la app |
| Informacion personal > Direccion de correo | Si | No | No | No (solo Coop) | Funcionalidad de la app, gestion de la cuenta |
| Informacion personal > ID de usuario | Si | No | No | No (solo Coop) | Funcionalidad de la app, gestion de la cuenta |
| Mensajes > Otro contenido generado por el usuario | No | | | | Los reportes no llevan texto libre |

Supabase y Resend tratan por cuenta nuestra: no es compartir.

**App Store Connect, App Privacy.** Se anaden, vinculados al usuario, sin rastreo, finalidad
*Funcionalidad de la app*: *Informacion de contacto > Nombre*, *Informacion de contacto > Correo
electronico*, *Identificadores > ID de usuario*. El manifiesto `PrivacyInfo.xcprivacy` declara los
mismos tres tipos (`NSPrivacyCollectedDataTypeName`, `...EmailAddress`, `...UserID`, vinculados, sin
rastreo, `NSPrivacyCollectedDataTypePurposeAppFunctionality`). La sesion de Supabase se guarda en
`NSUserDefaults`, que ya esta declarado (`CA92.1`).

**Clasificacion por edad.** Play (cuestionario IARC): *Los usuarios pueden interactuar* = Si (nombres
visibles entre usuarios, sin chat). App Store: *Contenido generado por usuarios / interaccion* y la
casilla de 16 anos en la app. Apple 1.2 pide reportar, bloquear y actuar en 24 h: hecho
(`store/coop.md`, "Actuar sobre un reporte").

**Iniciar sesion con Apple (4.8).** Google como opcion obliga a ofrecer Apple, igual de visible: hecho.
