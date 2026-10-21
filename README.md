## Pomodoro | Flowtime

Pomodoro | Flowtime es una aplicación Android diseñada para ayudarte a gestionar tu tiempo de estudio o trabajo de manera efectiva, utilizando las técnicas de temporizador Pomodoro y Flowtime. Además, la aplicación te permite personalizar los tiempos de trabajo y descanso, cambiar el tema de la interfaz y llevar un registro del tiempo dedicado a tus actividades a lo largo de la semana.

### Características Principales

1. **Temporizador Pomodoro y Flowtime:**
   - Establece intervalos de trabajo y descanso utilizando las técnicas Pomodoro y Flowtime para mejorar tu productividad.
   - Visualiza el tiempo restante en cada sesión para mantenerte enfocado.

2. **Ventana de Ajustes:**
   - Personaliza los tiempos de trabajo y descanso según tus preferencias.
   - Cambia el tema de la aplicación para adaptarse a tus gustos visuales.

3. **Historial de Seguimiento:**
   - Accede a un historial semanal que muestra las horas estudiadas o trabajadas cada día.
   - Analiza tu progreso a lo largo del tiempo y ajusta tu enfoque según tus objetivos.

### Tecnologías Utilizadas

- **Jetpack Compose:**
  - La interfaz de usuario está totalmente construida utilizando Jetpack Compose, proporcionando una experiencia moderna y receptiva.

- **Koin - Inyector de Dependencias:**
  - Se utiliza Koin como inyector de dependencias para una gestión eficiente de los componentes y la modularización del código.

- **StateFlow y ViewModels:**
  - StateFlow junto con ViewModels se emplea para gestionar el estado de la aplicación y garantizar una coherencia en la presentación de datos.

- **Modularización:**
  - La aplicación está estructurada en módulos para facilitar el mantenimiento y la escalabilidad del código.

- **Room:**
  - Room se utiliza como una base de datos local para almacenar el historial de seguimiento, permitiendo un acceso rápido y eficiente a los datos.
 
### Enlace Google Play
- ** https://play.google.com/store/apps/details?id=com.baltajmn.flowtime **

## Petición de reseña

Dentro de la app, con el diálogo del sistema (Play In-App Review en Android, `SKStoreReviewController` en iOS); la tienda decide si lo enseña.

- Momento: al terminar una sesión o cerrar la celebración del objetivo, sin sesión en marcha (`calmMoments`).
- Regla: 3 sesiones, 2 días instalada, 15 minutos trabajados hoy (parar a los pocos segundos no es un buen momento) y 120 días desde la última petición (`ReviewPolicy`, con tests).
- Nunca tras un error, en el paywall ni sobre otra capa; se marca como pedida aunque la tienda no la enseñe.


## Momento de Pro (D9, #86)

Pro se ofrece una sola vez, sin bloquear nada: se abre la hoja de Pro de siempre, que se cierra con la X. Coop sigue gratis y el precio no cambia (1,99 el vitalicio, 0,99 al mes).

- Cuándo: **5 sesiones de trabajo** hechas en total, **15 minutos hoy** (el mismo buen momento que la valoración) y en un momento tranquilo (`calmMoments`: al terminar una sesión o cerrar la celebración, nunca al abrir ni con una en marcha). Va después de la petición de valoración y no a la vez: si esa acaba de salir (menos de una hora), espera al siguiente momento.
- A quién: a quien no es Pro, no lo fue (mensual caducado) y tiene Pro a la venta. Quien ya tiene 5 sesiones al actualizar lo ve una vez, en su próximo buen momento.
- Dónde vive: `ProOfferPolicy` (con tests), `ProLauncher.offerIfDue`, y `PRO_OFFERED_AT` en las preferencias. Para cambiar el número: `ProOfferPolicy.MIN_SESSIONS`.

## Tarjetas para compartir (D7, #83)

Estadísticas, periodo "Hoy", botón Compartir: una tarjeta 4:5 con los minutos de foco, las sesiones y la racha, que se comparte como imagen con la hoja nativa (Android: `FileProvider` `<applicationId>.share`; iPhone: `UIActivityViewController`). La imagen sale de la propia tarjeta dibujada (`ShareCardSheet`, `GraphicsLayer`), no hay una copia aparte.

La firma de cada tarjeta, y el texto que la acompaña, es la búsqueda completa de la app en el idioma del móvil: la cadena `app_search_name`, que es el `store/listings/<idioma>/title.txt` de ese idioma. `SearchNameTest` falla si una y otro se separan; al cambiar el título de una ficha, cambiar también `app_search_name`.
