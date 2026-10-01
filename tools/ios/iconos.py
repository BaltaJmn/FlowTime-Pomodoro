#!/usr/bin/env python3
"""Los iconos del iPhone: el de siempre y uno por tema de Pro (#56), como los del lanzador de Android.

    python3 tools/ios/iconos.py

Cada uno es design/icon.svg con los cuatro colores del icono de Android de ese tema (fondo, aro, arco
y ola): los de Android son el original. Salen dos tamanos:

- 1024 px y sin canal alfa (la App Store no lo acepta con el) en Assets.xcassets, donde
  ASSETCATALOG_COMPILER_INCLUDE_ALL_APPICON_ASSETS los hace iconos alternativos.
- 144 px en composeResources de iosMain, para elegirlo en Ajustes (rememberAppIconImage).
"""
import io
import json
import pathlib
import re
import subprocess

from PIL import Image

RAIZ = pathlib.Path(__file__).resolve().parents[2]
ICONO = RAIZ / "design" / "icon.svg"
RES = RAIZ / "core" / "design" / "src" / "androidMain" / "res"
ASSETS = RAIZ / "iosApp" / "iosApp" / "Assets.xcassets"
VISTAS = RAIZ / "core" / "design" / "src" / "iosMain" / "composeResources" / "drawable"

# El nombre en Android (ic_launcher_<nombre>) y el de AppIcon en Kotlin.
ICONOS = {
    "flowtime": "default",
    "lavender": "lavender",
    "mint": "mint",
    "coral": "coral",
    "sand": "sand",
    "night": "night",
    "cherry": "cherry",
}


def colores(android):
    """Fondo, aro, arco y ola, en el orden en que los pinta design/icon.svg."""
    fondos = (RES / "values" / "ic_launcher_flowtime_background.xml").read_text(encoding="utf-8")
    fondo = re.search(rf'name="ic_launcher_{android}_background">(#\w+)<', fondos).group(1)
    primer_plano = (RES / "drawable" / f"ic_launcher_{android}_foreground.xml").read_text(encoding="utf-8")
    return [fondo] + re.findall(r'android:(?:strokeColor|fillColor)="(#\w+)"', primer_plano)


def png(svg, lado):
    datos = subprocess.run(
        ["rsvg-convert", "-w", str(lado), "-h", str(lado)], input=svg.encode(), capture_output=True, check=True
    ).stdout
    return Image.open(io.BytesIO(datos))


def main():
    plantilla = ICONO.read_text(encoding="utf-8")
    originales = colores("flowtime")
    VISTAS.mkdir(parents=True, exist_ok=True)
    for android, kotlin in ICONOS.items():
        svg = plantilla
        for original, nuevo in zip(originales, colores(android)):
            svg = svg.replace(original, nuevo)
        juego = ASSETS / ("AppIcon.appiconset" if kotlin == "default" else f"AppIcon-{kotlin}.appiconset")
        juego.mkdir(exist_ok=True)
        png(svg, 1024).convert("RGB").save(juego / "AppIcon.png")
        contenido = {
            "images": [{"filename": "AppIcon.png", "idiom": "universal", "platform": "ios", "size": "1024x1024"}],
            "info": {"author": "xcode", "version": 1},
        }
        # Con el mismo formato que Xcode, para que no lo reescriba al abrir el proyecto.
        json_xcode = json.dumps(contenido, indent=2, separators=(",", " : "))
        (juego / "Contents.json").write_text(json_xcode + "\n", encoding="utf-8")
        png(svg, 144).save(VISTAS / f"app_icon_{kotlin}.png", optimize=True)
        print(juego.name, VISTAS.name, f"app_icon_{kotlin}.png")


if __name__ == "__main__":
    main()
