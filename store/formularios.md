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
- FlowTime Pro (com.baltajmn.flowtime.pro_lifetime): non-consumable, a one-time payment, no subscription. Without it the app is fully usable with some limits (4 tags, 15 pending tasks, 3 saved sound mixes). Pro removes those limits and unlocks Pro themes and icons, Pro sounds, advanced stats in the Stats tab and CSV export. The purchase sheet opens from Settings > Pro > See Pro, or by tapping any locked item.
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
