# Sonidos de la app

## Sonidos ambientales

Los 12 sonidos del mezclador se generan en tiempo real en el propio movil. No hay ficheros de
audio ni se descarga nada: funcionan sin conexion y no dependen de ningun servidor.

| Sonido | Como se genera | Codigo |
|---|---|---|
| Lluvia | Ruido rosa filtrado, golpecitos de gotas y alguna gota en un charco | `Rain` |
| Tormenta | La lluvia mas fuerte y truenos de ruido marron que retumban | `Storm` |
| Fuego | Rumor grave, siseo y chasquidos de la madera en grupos | `Fire` |
| Olas | Tres olas desfasadas: ruido que sube, rompe y se retira | `Waves` |
| Viento | Ruido por un filtro que se mueve con las rafagas, y un silbido | `Wind` |
| Pajaros | Cuatro pajaros sinteticos que cantan frases cortas, y hojas | `Birds` |
| Calor | Zumbido de chicharras (ruido filtrado a pulsos) y brisa | `Heat` |
| Cafeteria | Voces lejanas (tono y formantes), tazas y el rumor de la sala | `CoffeeHouse` |
| Meditacion | Acorde grave que respira y cuencos tibetanos | `Meditation` |
| Ruido marron, rosa y blanco | Ruido generado directamente | `BrownNoise`, `PinkNoise`, `WhiteNoise` |

Todo el codigo esta en `src/main/kotlin/com/baltajmn/flowtime/core/design/sound/` y es propio de
la app. No hay audio de terceros, asi que no hay ninguna licencia externa que cumplir.

Hasta la version 2.0.4 se reproducian en directo desde `mynoise.world`, sin licencia ni caché.

## Avisos del temporizador

| Fichero | Origen |
|---|---|
| `src/main/res/raw/start.wav` | Añadido en 2023 (`f6d613d`). Origen sin documentar: pendiente de confirmar. |
| `src/main/res/raw/confirmation.wav` | Añadido en 2023 (`f6d613d`). Origen sin documentar: pendiente de confirmar. |
