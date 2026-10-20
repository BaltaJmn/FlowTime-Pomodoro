# Formularios de Play Console para la 2.1.0

> Lo de hoy, 20-10-2026: "Para enviar por la manana", al final. Antes, "Respuesta a Apple 2.5.4" y
> "2.4.0 (62): notas para el revisor de App Store".

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

## App Store: privacidad de la app

*App Store Connect > FlowTime > Privacidad de la app.* Es lo mismo que Play (`store/privacy/README.md`) con los nombres de Apple, y
tiene que coincidir con `iosApp/iosApp/PrivacyInfo.xcprivacy`.

Hechos del código en los que se apoya cada respuesta:

- **Red:** la única dependencia que habla con un servidor es RevenueCat (`purchases-kmp` 3.2.1 en
  `gradle/libs.versions.toml`). No hay Ktor, Supabase, Firebase, analítica, informes de fallos ni publicidad.
- **El iPhone habla con RevenueCat:** `REVENUECAT_IOS_API_KEY` lleva la `appl_` de la app "FlowTime (App Store)"
  desde el 05-10-2026, así que `platformHasStore` es `true` y la app configura RevenueCat, igual que en Android.
- **Sin seguimiento:** `NSPrivacyTracking` en `false`, sin dominios de seguimiento y sin `NSUserTrackingUsageDescription`
  (no hay ATT). RevenueCat se usa sin `appUserID` propio, con el identificador anónimo que asigna él.
- **Notificaciones solo locales:** `UNUserNotificationCenter` con avisos de fin de fase y recordatorio diario
  programados en el propio iPhone (`PhaseNotifications.kt`, `ReminderNotifications.kt`). No hay notificaciones push, ni
  `aps-environment`, ni `remote-notification`. El permiso se pide al empezar la primera sesión.
- **Live Activity local:** `Activity.request` sin `pushType` (`FocusActivities.swift`), y el widget lee el estado por el
  App Group `group.com.baltajmn.flowtime`, sin red.
- **Los ficheros que exporta el usuario** (copia de seguridad y CSV) los guarda donde elige con el selector del sistema:
  no se marcan.
- **La valoración** es `SKStoreReviewController`, del sistema: la app no ve qué se puntuó.

| Pregunta | Respuesta |
|---|---|
| ¿Recoges datos de esta app? | Sí |
| Compras > Historial de compras | Recogido. Finalidades: funcionalidad de la app y análisis de datos, las dos que pide la documentación de RevenueCat. **No** vinculado a la identidad. **No** usado para rastreo. Rellenado en App Store Connect el 05-10-2026 |
| Identificadores | No se marcan. RevenueCat pide *ID de usuario* solo con IDs propios e *ID de dispositivo* solo con integraciones que usen el IDFA, y la app usa su ID anónimo. Así quedaron las cuatro apps de la familia. `PrivacyInfo.xcprivacy` declara además `UserID`: de más, no de menos |
| El resto de tipos | No recogidos |

El manifiesto de la app declara exactamente esos dos tipos, sin vincular y sin seguimiento, más las APIs de razón
obligatoria: `UserDefaults` (`CA92.1`, `1C8F.1`), marcas de tiempo de ficheros (`C617.1`, `0A2A.1`), espacio en disco
(`E174.1`) y hora de arranque (`35F9.1`). El de la extensión del widget (`FlowTimeWidgets/PrivacyInfo.xcprivacy`) no
declara datos y solo `UserDefaults` (`1C8F.1`).

Si el informe de privacidad de Xcode sobre el primer archivo añade algo, se añade en los dos sitios.

Duda a revisar antes de enviar: la documentación de RevenueCat menciona también el identificador de dispositivo
(IDFV) entre lo que su SDK puede recoger. Ni Chroma ni este manifiesto lo declaran, y no se activa ninguna recogida
extra (ni IDFA ni atributos), pero si se quiere el margen más conservador se marcaría además
*Identificadores > ID de dispositivo* con la misma finalidad, sin vincular y sin rastreo, y se añadiría al manifiesto.

## App Store: el resto de la ficha

| Campo | Valor |
|---|---|
| Categoría principal | **Productividad** (`INFOPLIST_KEY_LSApplicationCategoryType = public.app-category.productivity` ya la fija) |
| Categoría secundaria | Educación (la ficha habla de estudio). Alternativa si se prefiere no pasar por la revisión de "educativa": Estilo de vida |
| Dispositivos | Solo iPhone (`TARGETED_DEVICE_FAMILY = 1` en la app y en la extensión). iOS 18.2 o posterior |
| Idioma principal | Español (España), como Play. Los 14 idiomas de la ficha están en `store/app-store/<idioma>/` |
| Clasificación por edad | Cuestionario: **ninguno** en todos los contenidos; **no** en contenido generado por usuarios, mensajería, publicidad, acceso web sin restricciones, temas médicos o de bienestar, concursos y apuestas, controles parentales y verificación de edad. Resultado esperado **4+**. No marcar la categoría Niños: la política dice que la app no se dirige a menores de 13 |
| Derechos de contenido | No contiene ni accede a contenido de terceros |
| Cumplimiento de exportación | No pregunta: `ITSAppUsesNonExemptEncryption = false` en `iosApp/iosApp/Info.plist` (solo el HTTPS del sistema). **No está en el `project.pbxproj`**: la app usa `INFOPLIST_FILE = iosApp/Info.plist` y la clave vive ahí, no en un `INFOPLIST_KEY_`. La extensión del widget (`FlowTimeWidgets/Info.plist`) no la lleva; basta con la de la app, pero si App Store Connect la pidiera también ahí, se añade |
| Copyright | `2026 Baltasar Jiménez` |
| URL de soporte | `https://flowtime.baltajmn.dev/` (la política; lleva el correo de contacto `baltax.75@gmail.com`) |
| URL de marketing | Ninguna: la web es solo la política |
| URL de política de privacidad | `https://flowtime.baltajmn.dev/` (`store/privacy/index.html`, repo público `BaltaJmn/flowtime-privacy`). Habla del iPhone, de la App Store como cobrador, de la copia de iCloud y de la valoración de Apple desde el 05-10-2026 |
| Inicio de sesión para la revisión | No hace falta: no hay cuentas |
| Publicación | Manual |

Usos declarados (`NS...UsageDescription`): **ninguno**. El `Info.plist` no declara cámara, fotos, ubicación, micrófono,
contactos ni seguimiento, y no hay `<idioma>.lproj/InfoPlist.strings` en `iosApp/` (no hay nada que traducir ahí).
Las claves que sí hay: `NSSupportsLiveActivities` (la sesión en la pantalla de bloqueo y la Dynamic Island),
`UIBackgroundModes = audio` (los sonidos siguen con la pantalla bloqueada) y `CFBundleLocalizations` con los idiomas
de la app. El permiso de notificaciones se pide por código, no lleva texto de uso. Cuando la interfaz esté traducida a
14 idiomas, `CFBundleLocalizations` tendrá que listar los mismos 14 que la ficha.

Notas para el revisor, en inglés:

Desde el 06-10-2026 son las siete respuestas que Apple pidió a Chroma en su primera revisión (2.1,
*Information Needed*, por ser una cuenta con poco historial), para adelantarse. Les falta el vídeo:
se graba en un iPhone y va como archivo adjunto de la información para la revisión.

```
1. Screen recording
Attached: a recording made on a physical iPhone running the latest iOS, set to Spanish, from launching the app through the typical flow: a focus session, the Live Activity and the widget, tasks and tags, ambient sounds, stats, Settings, buying FlowTime Pro with a sandbox account, a Pro feature, Restore purchases and the tips. The Buy button shows the price StoreKit returns, the US one ($1.99), because the device had not signed in to the sandbox store yet; the purchase sheet then shows the Spanish one (1.99 EUR). FlowTime has no account registration, login or account deletion, and nothing is shared with other users (no user-generated content), so those flows do not exist.

2. Purpose and audience
FlowTime is a focus timer for studying and working. It offers three techniques: Pomodoro (fixed work and break intervals), FlowTime (work while you are focused, then rest) and Percentage (the break is a fixed percentage of the time worked). It is for students and professionals who want to manage focus and breaks, organise sessions with tasks and tags, and see how they spend their time. Everything stays on the device.

3. How to use it
No login, no setup and no sample files are needed.
- Focus tab: pick a mode (Pomodoro, FlowTime or Percentage) and tap Start. The first time a session starts, the app asks for notification permission; it is only used for local alerts at the end of each phase and for the optional daily reminder. The session also shows on the Lock Screen as a Live Activity, and the Home Screen widget can start, pause and resume it.
- Sounds panel on the Focus tab: ambient sounds generated on the device, which keep playing with the screen locked (this is why the audio background mode is declared).
- Tasks and tags organise the sessions; the Stats tab shows the history.
- Settings: durations, themes and icons, reminder, Pro, and Support FlowTime (tips and Restore purchases).

4. External services
- Apple In-App Purchase (StoreKit), for the purchases.
- RevenueCat (revenuecat.com), to validate those purchases and know whether FlowTime Pro is active. It receives the purchases and an anonymous ID, never a name or an email.
Nothing else: no account system, no server of our own, no analytics, no ads and no AI services.

5. Regions
The app works the same in every region. It is available in every App Store country except mainland China, in 14 languages, and FlowTime Pro costs $1.99 (1.99 EUR in Spain), with Apple's regional pricing elsewhere; the optional tips are $1.99, $3.99 and $8.99.

6. Regulated industries and third-party material
Not applicable: FlowTime is not in a regulated industry and includes no protected third-party material. The ambient sounds are generated by the app.

7. In-App Purchase
- FlowTime Pro has two ways to buy, both unlocking the same: com.baltajmn.flowtime.pro_lifetime (non-consumable, the highlighted option) and com.baltajmn.flowtime.pro_monthly (auto-renewing subscription, 0.99 EUR per month, cancel any time in the App Store settings). The purchase sheet shows both prices, the renewal and cancellation terms, and links to the terms of use and privacy policy. Without Pro the app is fully usable with some limits (4 tags, 15 pending tasks, 3 saved sound mixes). Pro removes those limits and unlocks Pro themes and icons, Pro sounds, advanced stats in the Stats tab and CSV export. The purchase sheet opens from Settings > Pro > See Pro, or by tapping any locked item.
- Tips (consumable): small, medium and large (com.baltajmn.flowtime.tip_small, tip_medium, tip_large), in Settings > Support FlowTime > Leave a tip. They do not unlock Pro; they only give a Supporter badge and theme.
- Restore purchases: Settings > Support FlowTime > Restore purchases, and on the Pro purchase sheet itself.
```

### Compras integradas

En la App Store los identificadores son únicos en toda la cuenta, así que llevan el de la app delante
(`store/ci.md`): `com.baltajmn.flowtime.pro_lifetime`. La app y RevenueCat se quedan con lo que va tras el último punto
(`pro_lifetime`), igual que en Play. El identificador no se puede reutilizar si se borra.

| Campo | Valor |
|---|---|
| Nombre de referencia | `FlowTime Pro (lifetime)` |
| ID del producto | `com.baltajmn.flowtime.pro_lifetime` |
| Tipo | No consumible |
| Precio | 1,99 EUR en España, igual que en Play: `compra.py` de `BaltaJmn/ci` |
| Derecho en RevenueCat | `pro` (oferta `default`, paquete `$rc_lifetime`) |
| Captura para la revisión | La hoja de Pro (Ajustes > Pro > Ver Pro), con el botón de compra |
| Notas de la revisión | `Unlocks advanced stats, CSV export, unlimited tags, tasks and mixes, 6 sounds, 6 themes and 6 app icons. Open Settings > Pro > See Pro. Restore Purchases is on that sheet and in Settings > Support FlowTime.` |

Nombre visible (máx. 30) y descripción (máx. 45), por idioma, entre paréntesis los caracteres:

| Idioma | Apple | Nombre visible | Descripción |
|---|---|---|---|
| de-DE | Alemán | `FlowTime Pro` (12) | `Erweiterte Statistiken und mehr, einmalig.` (42) |
| en-US | Inglés (EE. UU.) | `FlowTime Pro` (12) | `Advanced stats and extras. Pay once.` (36) |
| es-ES | Español (España) | `FlowTime Pro` (12) | `Estadísticas avanzadas y más. Un solo pago.` (43) |
| fr-FR | Francés | `FlowTime Pro` (12) | `Statistiques avancées et plus. Un seul achat.` (45) |
| hi-IN | Hindi | `FlowTime Pro` (12) | `उन्नत आँकड़े और बहुत कुछ। एक बार भुगतान।` (40) |
| id | Indonesio | `FlowTime Pro` (12) | `Statistik lanjutan dan lainnya. Bayar sekali.` (45) |
| it-IT | Italiano | `FlowTime Pro` (12) | `Statistiche avanzate e altro. Un pagamento.` (43) |
| ja-JP | Japonés | `FlowTime Pro` (12) | `高度な統計などを開放。一度の購入で永続。` (20) |
| ko-KR | Coreano | `FlowTime Pro` (12) | `고급 통계와 더 많은 기능. 한 번만 결제.` (24) |
| nl-NL | Neerlandés | `FlowTime Pro` (12) | `Uitgebreide statistieken en meer. Eenmalig.` (43) |
| pl-PL | Polaco | `FlowTime Pro` (12) | `Zaawansowane statystyki. Płacisz raz.` (37) |
| pt-BR | Portugués (Brasil) | `FlowTime Pro` (12) | `Estatísticas avançadas e mais. Pague uma vez.` (45) |
| ru-RU | Ruso | `FlowTime Pro` (12) | `Расширенная статистика и другое. Разово.` (40) |
| tr-TR | Turco | `FlowTime Pro` (12) | `Gelişmiş istatistikler ve fazlası. Tek ödeme.` (45) |

La descripción de Pro cuenta lo mismo que la pantalla de Pro y la ficha: si cambia lo que incluye Pro, se cambian las
tres a la vez. En el iPhone no entra el No molestar automático (`hasFocusMode = false`), por eso ninguna descripción de
App Store lo menciona, a diferencia de Play.

### Propinas (consumibles)

El código las ofrece también en el iPhone (`Products.TIPS` y la hoja de propinas, activas en cuanto hay clave `appl_`),
así que hay que crearlas o la hoja sale vacía y la revisión lo verá. Son tres consumibles con el derecho `supporter`:
`com.baltajmn.flowtime.tip_small` (1,99 EUR), `.tip_medium` (4,99 EUR) y `.tip_large` (9,99 EUR), nombre de referencia
`FlowTime tip small`, `FlowTime tip medium` y `FlowTime tip large`. Los nombres visibles son los de la app
(`tip_small`, `tip_medium`, `tip_large` en `strings.xml`) y la descripción, una sola para los tres:

| Idioma | Nombres visibles (pequeña / mediana / grande) | Descripción (máx. 45) |
|---|---|---|
| de-DE | `Kleines Trinkgeld` / `Mittleres Trinkgeld` / `Großes Trinkgeld` | `Danke, dass du FlowTime unterstützt.` (36) |
| en-US | `Small tip` / `Medium tip` / `Large tip` | `Thanks for supporting FlowTime.` (31) |
| es-ES | `Propina pequeña` / `Propina mediana` / `Propina grande` | `Gracias por apoyar FlowTime.` (28) |
| fr-FR | `Petit pourboire` / `Pourboire moyen` / `Gros pourboire` | `Merci de soutenir FlowTime.` (27) |
| hi-IN | `छोटी टिप` / `मध्यम टिप` / `बड़ी टिप` | `FlowTime का साथ देने के लिए धन्यवाद।` (36) |
| id | `Tip kecil` / `Tip sedang` / `Tip besar` | `Terima kasih sudah mendukung FlowTime.` (38) |
| it-IT | `Mancia piccola` / `Mancia media` / `Mancia grande` | `Grazie per sostenere FlowTime.` (30) |
| ja-JP | `小さなチップ` / `中くらいのチップ` / `大きなチップ` | `FlowTimeを応援ありがとうございます。` (22) |
| ko-KR | `작은 팁` / `보통 팁` / `큰 팁` | `FlowTime을 응원해 주셔서 감사합니다.` (24) |
| nl-NL | `Kleine fooi` / `Gemiddelde fooi` / `Grote fooi` | `Bedankt voor je steun aan FlowTime.` (35) |
| pl-PL | `Mały napiwek` / `Średni napiwek` / `Duży napiwek` | `Dziękujemy za wsparcie FlowTime.` (32) |
| pt-BR | `Gorjeta pequena` / `Gorjeta média` / `Gorjeta grande` | `Obrigado por apoiar o FlowTime.` (31) |
| ru-RU | `Маленькие чаевые` / `Средние чаевые` / `Большие чаевые` | `Спасибо, что поддерживаете FlowTime.` (36) |
| tr-TR | `Küçük bahşiş` / `Orta bahşiş` / `Büyük bahşiş` | `FlowTime'ı desteklediğin için teşekkürler.` (42) |

### Huecos a decidir antes de enviar

1. **Clave de RevenueCat para iOS:** resuelto el 05-10-2026. App "FlowTime (App Store)" en RevenueCat con los cuatro
   productos de arriba en los derechos `pro` y `supporter` y en los paquetes de `default`, y su `appl_` en
   `REVENUECAT_IOS_API_KEY`, y la clave de compras integradas de Apple subida (`~/keys/LEEME.md`).
2. **Política de privacidad:** resuelto el 05-10-2026. Habla del iPhone, de la App Store como cobrador, de la copia de
   iCloud y de la valoración de Apple. La tabla de permisos sigue siendo la de Android, con una nota debajo para iOS
   (solo pide notificaciones), igual que la de Quilt.
3. **Idiomas de la app:** las fichas dicen "14 idiomas" (App Store, y las ocho nuevas de Play). Las seis anteriores de
   Play (`store/listings/{de-DE,en-US,es-ES,hi-IN,it-IT,ru-RU}/full.txt`) siguen listando seis, a cambiar cuando la
   interfaz traducida esté en producción.
4. **Nombre en la App Store:** resuelto. Cada idioma lleva el de `store/app-store/<idioma>/name.txt` (en español,
   `FlowTime: Pomodoro y estudio`), y App Store Connect coincide con el repo en nombre, subtítulo, descripción,
   palabras clave y texto promocional de los 14 idiomas (comprobado por API el 05-10-2026).
5. **Capturas:** resuelto el 05-10-2026. Las 5 de `store/screenshots/iphone/<idioma>/` en los 14 idiomas, como
   pantalla de 6,9" (`APP_IPHONE_67` en la API, que acepta 1320x2868).
6. **Build:** resuelto el 06-10-2026. La 60 (2.2.2, con Ajustes arreglado en iOS) está en la versión; la 59 se
   cerraba al abrir Ajustes. Las capturas de revisión de las cuatro compras son de esa fecha, a precio de EE. UU.
   (`store/screenshots/iphone/revision-compra.png` y `revision-propinas.png`, 1320x2868).

## Respuesta a Apple 2.5.4 (2.2.2, build 60)

Rechazo del 08-10-2026: el revisor, en un iPad, no encontro audio que justifique `UIBackgroundModes =
audio`. Lo hay (los sonidos ambientales), pero en la 2.2.2 se abren con un boton de solo icono.

**Ojo con el icono:** en la 2.2.2 el boton de sonidos parado es un **ecualizador** (tres barras
verticales, gris), a la izquierda del icono de ajustes (tres deslizadores). La nota musical llego con
la 2.3.0. El texto y el video de abajo son de la 2.2.2, que es la build que se revisa.

**Texto para la respuesta** (App Store Connect > FlowTime > App Review, responder al mensaje):

```
Hello,

Thank you for reviewing FlowTime.

FlowTime declares the audio background mode for one feature: its ambient sounds (rain, coffee house, fire, waves, wind, brown, pink and white noise, and more). They are generated on the device and are meant to keep playing for the whole study or work session, while the user is in other apps or has the screen locked, until they stop them in the app or the sleep timer they chose ends. This is persistent audio: without the audio background mode it would stop as soon as the user leaves the app.

How to hear it:
1. Open FlowTime. On the Focus tab (the first tab), tap the sounds button at the top right of the screen: the equalizer icon (three vertical bars), just to the left of the sliders icon.
2. The Sounds sheet opens with one row per sound: its icon, a volume slider and a play button. Tap the play button on the first row (Rain, the rain cloud icon), or on any row without a lock. Please make sure the device volume is up.
3. Go to the Home Screen, open another app, or lock the device: Rain keeps playing.
4. Come back to FlowTime, tap the sounds button again and tap the same button on the Rain row, now a pause button, to stop it.

FlowTime is an iPhone app. On iPad it runs in iPhone compatibility mode, in a phone-sized window, so the button is small; it is in the same place, at the top right of the Focus tab.

The attached screen recording, made on an iPhone with sound, shows these steps: opening the sounds, starting Rain, going to the Home Screen and to another app while it keeps playing, and coming back to stop it.

Best regards,
Baltasar Jimenez
```

**Guion del video** (iPhone, unos 40 s, con la **2.2.2 (60) de TestFlight**, no con una build de Xcode
de `main`: esa ya trae Coop y la nota musical y no coincide con el texto):

1. Volumen alto. Centro de control > Grabacion de pantalla (mantener pulsado: microfono apagado; el
   audio de la app se graba igual). Esperar la cuenta atras.
2. Abrir FlowTime en la pestana Enfoque. Quieto 2 s para que se vea la pantalla.
3. Tocar el ecualizador de arriba a la derecha. En la hoja de Sonidos, tocar el play de la primera fila
   (Lluvia, la nube; las filas no llevan nombre, solo icono). Esperar 3 s: se oye y el ecualizador se
   mueve.
4. Deslizar hacia arriba al Home Screen. Quieto 5 s (sigue sonando).
5. Abrir otra app (Notas o Safari) y moverse un poco, 5 s.
6. Volver a FlowTime, tocar el ecualizador y la pausa de la primera fila: deja de sonar.
7. Parar la grabacion. No bloquear el movil durante la grabacion: la grabacion de pantalla se corta.

**Revisado el 20-10-2026 en el simulador** (iPad Air 13", `main`): en iPad la app sale en una ventana
de iPhone a tamano real en el centro de la pantalla, y la fila de iconos de arriba a la derecha
(grupo, nota, deslizadores) queda pequena y sin texto. Se ve, pero quien no conoce la app no tiene por
que adivinar que la nota son los sonidos. **Mejora apuntada para despues de la 2.4.0:** poner texto al
boton ("Sonidos" debajo del icono, como "Empezar" bajo el play) o un acceso con nombre en la pantalla
de Enfoque. No va en la 2.4.0 porque ya esta etiquetada; la 2.4.0 si lleva los sonidos en "En
reproduccion" de la pantalla de bloqueo (`27c6196`), que es lo que un revisor busca para el audio en
segundo plano.

## 2.4.0 (62): notas para el revisor de App Store

Para cuando la 2.2.2 este aprobada y se cree la version 2.4.0 en App Store Connect (no antes). Van en
*Informacion para la revision > Notas*. El hueco `<<...>>` es la cuenta de prueba que crea el usuario.

```
1. Screen recording
Attached: a recording made on a physical iPhone: a focus session, ambient sounds playing in the background and in Now Playing on the Lock Screen, Coop (signing in, creating a room, a second device joining, the shared timer, friends, reporting and blocking from a person's menu, and Settings > Coop account > Delete account), and the purchase sheet with its two options.

2. Purpose and audience
FlowTime is a focus timer for studying and working. It offers three techniques: Pomodoro (fixed work and break intervals), FlowTime (work while you are focused, then rest) and Percentage (the break is a fixed percentage of the time worked). It is for students and professionals who want to manage focus and breaks, organise sessions with tasks and tags, and see how they spend their time. New in 2.4.0, Coop (optional): a private room where friends study with the same Pomodoro timer. The rest of the app needs no account, and sessions, tasks and stats stay on the device.

3. How to use it
The app needs no login. Only Coop asks to sign in.
- Focus tab: pick a mode (Pomodoro, FlowTime or Percentage) and tap Start. The first time a session starts, the app asks for notification permission; it is used for the alerts at the end of each phase, the optional daily reminder and, with a Coop account, friend requests and room invitations. The session also shows on the Lock Screen as a Live Activity, and the Home Screen widget can start, pause and resume it.
- Ambient sounds: the music note icon at the top right of the Focus tab. The sounds are generated on the device and keep playing in other apps and with the screen locked until they are stopped; while they play they appear in Now Playing on the Lock Screen and in Control Center, with play and pause. This is why the audio background mode is declared.
- Coop: the people icon at the top right of the Focus tab, to the left of the music note. Sign in with Apple or Google, choose a name and confirm you are 16 or older. Then create a room (anyone in it can start, pause or stop the shared Pomodoro) or join one with its code or link, and add friends by sharing your link or QR code or pasting a friend's link. Friends can be invited to your room.
- Reporting and blocking (Guideline 1.2): the three-dot menu next to each person, in the room and in the Friends list, has Report name and Block. A report reaches us by email and we act within 24 hours. The only content users create is their chosen name (1 to 30 characters): there is no chat, no photos and no free text.
- Account deletion (Guideline 5.1.1(v)): Settings > Coop account > Delete account, which deletes the Coop account and its data from our server at once. Also explained at https://flowtime.baltajmn.dev/delete.html.
- Tasks and tags organise the sessions; the Stats tab shows the history.
- Settings: durations, themes and icons, reminder, Coop account, Pro, and Support FlowTime (tips and Restore purchases).

Demo account: Sign in with Apple works with any Apple ID. To see Coop with a friend already added, sign in with Google using the account in Sign-In Information (<<CORREO DE LA CUENTA DE PRUEBA>>); it has the Coop name <<NOMBRE EN COOP>> and one accepted friend. Creating a room and sharing its code is enough to try the shared timer from a second device.

4. External services
- Apple In-App Purchase (StoreKit), for the purchases.
- RevenueCat (revenuecat.com), to validate those purchases and know whether FlowTime Pro is active. It receives the purchases and an anonymous ID, never a name or an email.
- Only for Coop: Supabase (supabase.com, servers in the EU) for the account, chosen name, friends, rooms and reports; Sign in with Apple and Google Sign-In to sign in; Apple Push Notification service for friend requests and room invitations; and Resend (resend.com), which emails us when a name is reported.
Nothing else: no analytics, no ads and no AI services.

5. Regions
The app works the same in every region. It is available in every App Store country except mainland China, in 14 languages. FlowTime Pro costs $1.99 once (1.99 EUR in Spain) or 0.99 EUR per month in Spain, with Apple's regional pricing elsewhere; the optional tips are $1.99, $3.99 and $8.99.

6. Regulated industries and third-party material
Not applicable: FlowTime is not in a regulated industry and includes no protected third-party material. The ambient sounds are generated by the app.

7. In-App Purchase
- FlowTime Pro has two ways to buy, both unlocking the same: com.baltajmn.flowtime.pro_lifetime (non-consumable, the highlighted option) and com.baltajmn.flowtime.pro_monthly (auto-renewing subscription, 0.99 EUR per month, cancel any time in the App Store settings). The purchase sheet shows both prices, the renewal and cancellation terms, and links to the terms of use and privacy policy. Without Pro the app is fully usable with some limits (4 tags, 15 pending tasks, 3 saved sound mixes). Pro removes those limits and unlocks Pro themes and icons, Pro sounds, advanced stats in the Stats tab and CSV export. The purchase sheet opens from Settings > Pro > See Pro, or by tapping any locked item.
- Tips (consumable): small, medium and large (com.baltajmn.flowtime.tip_small, tip_medium, tip_large), in Settings > Support FlowTime > Leave a tip. They do not unlock Pro; they only give a Supporter badge and theme.
- Restore purchases: Settings > Support FlowTime > Restore purchases, and on the Pro purchase sheet itself.
```

Lo que cambia en el resto de la ficha de App Store con la 2.4.0 (antes de enviarla):

| Campo | 2.4.0 |
|---|---|
| Inicio de sesion para la revision | Marcar *Se requiere inicio de sesion*: usuario y contrasena de la cuenta de Google de prueba |
| Privacidad de la app | La de `store/privacy/README.md`, "Coop (#74): respuestas para la 2.4.0" |
| Clasificacion por edad | *Contenido generado por usuarios* = Si; *Mensajeria y chat* = No; el resto igual |
| Compras en el envio | Anadir `com.baltajmn.flowtime.pro_monthly` (primera suscripcion: va con la version) |
| Novedades | Las de `store/whatsnew/whatsnew-<idioma>`, mas una linea solo de iPhone: "Los sonidos salen en la pantalla de bloqueo y el Centro de control, con pausa" |
| Descripcion | La del repo ya acaba con el enlace a los terminos (`https://flowtime.baltajmn.dev/terms.html`, guia 3.1.2 por la suscripcion) en los 14 idiomas. Se sube con `python3 ~/AndroidStudioProjects/ci/tienda/appstore.py ficha com.baltajmn.flowtime .` cuando exista la version 2.4.0. La 2.2.2 no lo lleva a proposito: su descripcion dice "not a subscription", como su build |

## Para enviar por la manana (20-10-2026)

En este orden. Lo que va entre `<<...>>` lo pones tu. Nada de esto lo he enviado yo.

### A. App Store: reenviar la 2.2.2 (60)

1. En el iPhone, app TestFlight > FlowTime > *Builds anteriores* > **2.2.2 (60)** > Instalar. Esta
   noche ha subido la 2.4.0 (62) y TestFlight ensena esa primero: para el video hace falta la 60.
2. Grabar el video con el guion de "Respuesta a Apple 2.5.4" (arriba). Pasarlo al Mac por AirDrop.
3. (Opcional, mejor hacerlo) Adjuntarlo tambien a la version, en *Informacion para la revision*:

   ```bash
   python3 ~/AndroidStudioProjects/ci/tienda/appstore.py adjunto com.baltajmn.flowtime ~/Downloads/<<VIDEO>>.MP4
   ```

4. (Opcional) App Store Connect > FlowTime > 2.2.2 > *Informacion para la revision > Notas*: cambiar la
   linea que empieza por `- Sounds panel on the Focus tab` por esta:

   ```
   - Ambient sounds: the equalizer icon (three vertical bars) at the top right of the Focus tab, to the left of the sliders icon. The sounds are generated on the device and keep playing in other apps and with the screen locked until they are stopped (this is why the audio background mode is declared).
   ```

   y Guardar.
5. App Store Connect > FlowTime > *App Review* (menu de la izquierda) > el envio con *Problemas sin
   resolver* > el mensaje de Apple > *Responder*: pegar el texto de "Respuesta a Apple 2.5.4", adjuntar
   el video y Enviar.
6. En ese mismo envio, *Reenviar a App Review*. Comprobar que lleva la version 2.2.2 y las 4 compras
   (`pro_lifetime`, `tip_small`, `tip_medium`, `tip_large`). **No anadir `pro_monthly`:** la build 60
   no la ofrece; va con la 2.4.0.

### B. Google Play: la 2.4.0 (62) en prueba cerrada (alpha)

La etiqueta `v2.4.0` sube la 62 a alpha con las novedades en 14 idiomas (si el release de GitHub
Actions fallo, lo dice el resumen de la sesion). Google la revisa al subirla: rellenar los formularios
cuanto antes, porque la ficha de datos todavia no habla de Coop.

1. **Consentimiento de Google publicado.** console.cloud.google.com > proyecto `flowtime-baltajmn` >
   *Google Auth Platform > Publico*. Si dice *Prueba*, pulsar *Publicar app* (solo pide email y
   perfil: sin verificacion). Si no, solo los usuarios de prueba pueden entrar a Coop con Google, el
   revisor tampoco (`store/coop.md`, punto 4 de "Panel").
2. **Cuenta de prueba.** Crear una cuenta de Google nueva para revisores (`<<CORREO>>`,
   `<<CONTRASENA>>`). En un Android con la 2.4.0: Enfoque > icono de grupo > *Continuar con Google* con
   esa cuenta > nombre `<<NOMBRE EN COOP>>` > casilla de 16 anos > Continuar. Desde tu cuenta de Coop,
   hacerte amigo de ella (comparte tu enlace y acepta). Asi el revisor ve un amigo.
3. Play Console > FlowTime > *Politica y programas > Contenido de la aplicacion*:
   1. *Acceso a la aplicacion* > Gestionar > *Toda o parte de la funcionalidad esta restringida* >
      Anadir instrucciones. Nombre: `Coop (study together)`. Usuario: `<<CORREO>>`. Contrasena:
      `<<CONTRASENA>>`. Otra informacion:

      ```
      Coop is the only part of FlowTime that needs an account. Open it with the people icon at the top right of the Focus tab, tap Continue with Google and sign in with this account. It already has a Coop name and one friend. You can create a room and share its code to try the shared timer from a second device. Report and Block are in the three-dot menu next to each person; account deletion is in Settings > Coop account > Delete account. The rest of the app works without signing in.
      ```

      Guardar.
   2. *Seguridad de los datos* > Editar, con la tabla de `store/privacy/README.md`, "Coop (#74):
      respuestas para la 2.4.0". Resumen de clics: creacion de cuenta *OAuth*; URL de borrar la cuenta
      `https://flowtime.baltajmn.dev/delete.html`; tipos nuevos *Nombre*, *Direccion de correo*, *ID de
      usuario* y *Contactos*, cada uno recogido, no compartido, no efimero, opcional, finalidad
      *Funcionalidad de la app* (correo e ID de usuario, tambien *Gestion de la cuenta*);
      *Identificadores de dispositivo u otros* e *Historial de compras* como estaban. Guardar.
   3. *Clasificacion de contenido* > Editar el cuestionario: *Los usuarios pueden interactuar* = Si
      (solo nombres, solicitudes de amistad e invitaciones; sin chat, voz ni imagenes); *Comparte la
      ubicacion* = No; *Compras digitales* = Si. Guardar y enviar.
4. *Vista general de la publicacion*: si sale *Enviar cambios para revision*, pulsarlo (manda los tres
   formularios con la 62 de alpha).
5. **Antes de produccion** (no es para esta manana, pero va antes del paso 6):
   - Rotar las claves de FCM y APNs y poner los tres secretos en Supabase (`docs/coop-push.md`,
     "Estado y lo que falta"). Sin ellos Coop funciona, pero los avisos con la app cerrada no salen.
   - Probar entre dos moviles lo que quedo sin probar en `store/coop.md`: bloquear, reportar (llega el
     correo), borrar la cuenta y un aviso real.
6. **Promocionar cuando quieras:** *Pruebas > Prueba cerrada > alpha* > la 2.4.0 (62) > *Promocionar
   version > Produccion* > Revisar > *Iniciar lanzamiento a produccion*. (Por la API, produccion
   decia 2.2.2 (59) a las 03:00; si la 2.3.0 sigue en revision, esta la sustituye.)

### C. iPhone: la 2.4.0 (62) en TestFlight

1. Instalar la 62 desde TestFlight, poner un sonido y bloquear: en la pantalla de bloqueo tiene que
   salir el reproductor con el nombre del sonido, "Ambient sounds" (o su idioma) y pausa. Pausar y
   reanudar desde ahi. No lo he podido ver en el simulador: no tenia permiso para tocarlo.
2. La version 2.4.0 de App Store solo cuando la 2.2.2 este aprobada (Apple solo admite una en
   revision). Entonces: crearla, build 62, y lo de "2.4.0 (62): notas para el revisor de App Store"
   (notas, cuenta de prueba, App Privacy, edad, `pro_monthly` en el envio, novedades).
