#!/usr/bin/env python3
"""Grafico destacado de Play (1024x500) y el icono de 512 de la ficha (#54).

    python3 tools/store/cabecera.py

play.sh sube el grafico solo en el idioma por defecto y Play lo ensena en todos, asi que no lleva nada
que traducir: el nombre, "Pomodoro" y el anillo del icono, con sus colores (design/icon.svg).
"""
import pathlib

from capturas import AGUA, FONDO, RAIZ, TINTA, render

ICONO = RAIZ / "design" / "icon.svg"


def main():
    play = RAIZ / "store" / "play"
    play.mkdir(parents=True, exist_ok=True)
    icono = ICONO.read_text(encoding="utf-8")
    # El icono entero, a escala 6: su fondo es el del grafico, asi que solo se ve el anillo con el agua.
    dibujo = icono[icono.index(">") + 1:icono.rindex("</svg>")]
    svg = """<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="500" viewBox="0 0 1024 500">
  <rect width="1024" height="500" fill="{fondo}"/>
  <g transform="translate(476 -74) scale(6)">{dibujo}</g>
  <text x="72" y="262" font-family="Helvetica Neue, Helvetica, Arial, sans-serif" font-size="104"
        font-weight="700" fill="{tinta}">FlowTime</text>
  <text x="76" y="330" font-family="Helvetica Neue, Helvetica, Arial, sans-serif" font-size="44"
        fill="{agua}">Pomodoro</text>
</svg>""".format(fondo=FONDO, tinta=TINTA, agua=AGUA, dibujo=dibujo)
    render(svg, play / "feature-1024x500.png", 1024, 500)
    # Play pone la mascara del icono: va cuadrado y a sangre, como el SVG.
    render(icono, play / "icon-512.png", 512, 512)


if __name__ == "__main__":
    main()
