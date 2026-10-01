# Formularios de Play Console para la 2.1.0

La 2.0.3 solo pedía `INTERNET` y `BILLING`. La 2.1.0 añade permisos que Play hace declarar en
*Política y programas > Contenido de la aplicación* antes de enviar la versión a revisión.

| Qué | Por qué | Respuesta |
|---|---|---|
| Política de privacidad | Ahora hay RevenueCat | `https://flowtime.baltajmn.dev/` (`store/privacy/`) |
| Seguridad de los datos | Ahora hay RevenueCat | La tabla de `store/privacy/README.md` |
| Servicios en primer plano | `AmbientService`, tipo `mediaPlayback` (sonidos) | Abajo |
| Permiso de alarma exacta | `USE_EXACT_ALARM` (aviso de fin de fase) | Abajo |

`ACCESS_NOTIFICATION_POLICY`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE` y
`WAKE_LOCK` no llevan declaración.

Las respuestas van en inglés, que es lo que leen los revisores.

**Enviado el 01-10-2026**, con la 2.1.0 en producción. Ninguno de los dos formularios sale con solo
subir el AAB a interna: el de alarma exacta aparece como error al revisar la versión de producción,
y el de servicios en primer plano en *Contenido de la aplicación* después de guardarla. Enviarlo con
la versión ya en revisión reinicia la revisión. Lo que pidió cada uno, en la práctica, es menos que
lo de abajo: alarma exacta, solo elegir *Despertador* o *Calendar*; servicios en primer plano, la
casilla *Reproducción de contenido multimedia* y el enlace del vídeo. Los textos de abajo quedan por
si un revisor los pide.

## Servicios en primer plano: Media playback

- **Caso de uso:** *Continue audio or video playback from the background*.
- **Functionality:** FlowTime is a focus timer. Users can play ambient sounds (rain, fire, waves,
  white, pink and brown noise and others) that the app generates on the device. They start them
  from the sound panel of the focus screen, and the sounds keep playing while they work in other apps
  or with the screen off, until they stop them from the app or from the media notification, or until
  the sleep timer they chose ends.
- **Impact if deferred:** the sound the user has just started would not play until they open the app
  again, so they would have to keep FlowTime on screen, which defeats the purpose of a focus timer.
- **Impact if interrupted:** the ambient sound stops in the middle of a work session and the user has
  to go back to the app to start it again.
- **Video:** `https://flowtime.baltajmn.dev/video/sonidos-segundo-plano.mp4` (27 s, grabado el
  01-10-2026 en el emulador con `adb screenrecord`): abrir la app, abrir los sonidos, reproducir Lluvia,
  salir al inicio, bajar la persiana con la notificación "Ambient sounds, Rain" y pulsar Pause y Stop.
  Vive en el repo público `BaltaJmn/flowtime-privacy`.

## Permiso de alarma exacta: USE_EXACT_ALARM

- **Función principal:** temporizador (Pomodoro). Play lo permite a las apps de alarma y de
  temporizador.
- **Texto:** FlowTime is a Pomodoro and focus timer. At the end of every work and break phase it has
  to alert the user at the exact second, also with the app closed and the phone idle. It schedules
  one exact alarm per phase end (`setExactAndAllowWhileIdle`, `PhaseAlarm.kt`) and cancels it when
  the session is paused or stopped. An inexact alarm would ring minutes late and break the timer.
