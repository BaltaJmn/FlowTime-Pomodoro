# Icono de FlowTime

El anillo de progreso del temporizador (#50) con agua que fluye dentro: dice "FlowTime" y no "otro
reloj". Los SVG de esta carpeta son el original; lo demás sale de ellos.

| Fichero | Qué es |
|---|---|
| `icon.svg` | El icono completo, a sangre y sin esquinas. De aquí sale el de 512 px de la ficha. |
| `icon-foreground.svg` | El primer plano del icono adaptativo (108 × 108, todo dentro del círculo seguro de 66). |
| `icon-monochrome.svg` | La capa monocroma para los iconos temáticos de Android 13. |
| `icon-preview.png` | Cómo queda con las máscaras del sistema, a 48 dp, en oscuro y en monocromo. |

## Colores

- Fondo `#15506A`, el azul del tema en oscuro.
- Anillo `#EAF6FB` (8:1 frente al fondo) y su pista `#2B6782`.
- Agua `#8ECAE6` (4,9:1 frente al fondo).

## En la app

Con `minSdk` 26 siempre se usa el icono adaptativo, así que no hay PNG por densidad:

- `core/design/src/main/res/mipmap-anydpi-v26/ic_launcher_flowtime.xml` y su versión `_round`;
- `drawable/ic_launcher_flowtime_foreground.xml` y `ic_launcher_flowtime_monochrome.xml`: los mismos
  trazados que los SVG, escritos como vectores de Android;
- `values/ic_launcher_flowtime_background.xml`: el color de fondo.

Si se cambia un SVG, hay que copiar sus `d` al vector de Android que le corresponde.

## El de la ficha

```sh
rsvg-convert -w 512 -h 512 design/icon.svg -o core/design/src/main/ic_launcher_flowtime-playstore.png
```

El del iPhone, a 1024 px y sin canal alfa (la App Store no lo acepta con él); iOS pone las esquinas:

```sh
rsvg-convert -w 1024 -h 1024 design/icon.svg | python3 -c "import sys, io; from PIL import Image; \
  Image.open(io.BytesIO(sys.stdin.buffer.read())).convert('RGB').save('iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon.png')"
```

Sin `rsvg-convert`, con Chrome:

```sh
chrome --headless=new --window-size=512,512 --default-background-color=00000000 \
  --screenshot=core/design/src/main/ic_launcher_flowtime-playstore.png "file://$PWD/design/icon.svg"
```
