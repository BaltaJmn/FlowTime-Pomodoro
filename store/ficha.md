# Ficha de Google Play

Todo lo que se ve en la tienda sale de este repo (#54), en los 6 idiomas de la app.

| Qué | Dónde | De dónde sale |
|---|---|---|
| Título y descripciones | `store/listings/<idioma>/{title,short,full}.txt` | A mano. `listings.yml` comprueba los topes de las dos tiendas en cada push a `store/` (`python3 ../ci/tienda/comprobar.py .` en local) |
| Capturas de móvil | `store/screenshots/play/<idioma>/01.png` a `07.png` | Roborazzi (`StoreScreenshotsTest`) y `tools/store/capturas.py` |
| Capturas de tablet | `store/screenshots/tablet/<idioma>/01.png` a `04.png` | Igual |
| Widget y notificación | `store/screenshots/raw/<idioma>/06_widget.png` y `07_notification.png` | Emulador: idioma por app (`cmd locale set-app-locales`), una sesión Pomodoro en marcha y el fondo de pantalla azul, para que el sistema tome los colores de la ficha |
| Gráfico destacado e icono | `store/play/feature-1024x500.png` y `icon-512.png` | `tools/store/cabecera.py`, con los colores de `design/icon.svg` |

La descripción larga tiene que cuadrar con la pantalla de Pro (#57): lo que es gratis y lo que añade
el pago único o el mensual (0,99 EUR). Si cambia una, cambia la otra en el mismo commit.

## Regenerar

```bash
./gradlew :features:screens:recordRoborazziAndroidHostTest --tests '*Store*'
python3 tools/store/capturas.py
python3 tools/store/cabecera.py
```

`capturas.py` borra antes las capturas de cada carpeta: `play.sh` sube todo lo que encuentre.

## Subir

Con la versión nueva ya en producción, no antes: la ficha enseña pantallas que la 2.0.3 no tiene.
Confirmar la edición cuenta como publicar, así que va con el sí expreso del usuario.

```bash
gh workflow run listings.yml -f target=play
/Users/baltajmn/keys/play.sh ficha com.baltajmn.flowtime . https://flowtime.baltajmn.dev/   # lo mismo, desde el Mac
```

- En una sola edición: los textos de los 6 idiomas, el icono, el gráfico y las capturas de móvil.
- `<correo>` es el de contacto que ya tiene la ficha. La ficha no tiene web: `""` la deja vacía.
- Las capturas de tablet no las sube `play.sh`. Van por la API en otra edición, con el token de
  `~/keys/LEEME.md` (subidas el 30-09-2026): por idioma, `DELETE .../edits/<id>/listings/<idioma>/tenInchScreenshots`
  y un `POST https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/com.baltajmn.flowtime/edits/<id>/listings/<idioma>/tenInchScreenshots?uploadType=media`
  por imagen (`Content-Type: image/png`). Al final `POST .../edits/<id>:commit` con
  `Content-Length: 0`: sin esa cabecera Google responde 404. En zsh, `${E}:commit` con llaves, porque
  `$E:c` es un modificador y se come la `c`.
- El gráfico va solo en el idioma por defecto (es-ES) y Play lo enseña en todos: por eso no lleva
  texto que traducir.

## App Store

Solo iPhone (`TARGETED_DEVICE_FAMILY = 1`): `store/screenshots/iphone/<idioma>/01.png` a `05.png`, a
1320x2868 (6,9"), salen del mismo `capturas.py`. En Apple las capturas son de una versión, así que
solo se cambian con una versión en preparación (`appstore.py estado` dice si la hay):

```bash
python3 ../ci/tienda/appstore.py capturas com.baltajmn.flowtime . --dry --reemplazar   # qué cambiaría
python3 ../ci/tienda/appstore.py capturas com.baltajmn.flowtime . --reemplazar
```

Sin `--reemplazar` solo rellena los idiomas que aún no tienen capturas.

## Como anuncio (#79)

Plan de crecimiento, fase 0 (`plan-apps/0-plan.md`): cada pico de trafico convierte en la ficha.

- Las tres primeras capturas son las que salen en la busqueda y llevan el beneficio, en grande: 01 el
  descanso que se adapta (pantalla FlowTime), 02 tres modos sin anuncios ni cuenta, 03 racha y objetivo.
  `capturas.py` baja la letra hasta que el titular cabe en el 90 % del ancho, en cualquier idioma.
- Subtitulo de 30 caracteres y palabras clave de 100 **bytes** (un acento son dos; el hindi, el japones
  y el coreano, tres), de la mas a la menos buscada y sin repetir palabras del nombre ni del subtitulo:
  Apple las junta, repetirlas solo gasta sitio. `comprobar.py` lo vigila.
- La descripcion de iPhone lleva la seccion de Coop (la de Play, con Apple ademas de Google para entrar)
  y Coop en la lista de lo gratis.
- Play: los textos no cambian; las capturas nuevas se suben con el resto de la ficha cuando la version
  en produccion este aprobada. App Store: el subtitulo, las claves, la descripcion y las capturas van
  en la siguiente version (`appstore.py nueva`), porque la 2.4.0 esta en revision y no admite cambios.
