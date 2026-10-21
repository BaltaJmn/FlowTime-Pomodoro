#!/usr/bin/env python3
"""Las imagenes de los In-App Events de App Store (#82): store/events/<evento>/card.png y details.png.

    python3 tools/store/eventos.py

Sin texto: Apple pone encima el nombre y la descripcion de cada idioma, y una imagen con letras las
taparia o las repetiria. Es el icono (design/icon.svg) a lo grande y con los colores de un tema de
Pro, para que cada evento se distinga a simple vista en la ficha: el azul de siempre, la noche y la
arena. Los textos y las fechas viven en store/events/<evento>/event.json, y los sube
`appstore.py eventos` de BaltaJmn/ci.
"""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1] / "ios"))
from capturas import RAIZ, render  # noqa: E402
from iconos import colores  # noqa: E402

TEMAS = {"examenes-diciembre": "flowtime", "examenes-enero": "night", "propositos-ano-nuevo": "sand"}
# Las dos imagenes que pide Apple: la tarjeta de la ficha y la cabecera de la pagina del evento.
# La tarjeta es apaisada y la cabecera de la pagina, vertical (9:16).
TAMANOS = {"card": (1920, 1080), "details": (1080, 1920)}


def arte(ancho, alto, fondo, pista, arco, ola):
    # El icono a la derecha y mas grande que la imagen, con aros que se pierden hacia los lados. La
    # parte baja queda tranquila: ahi Apple pone el titulo.
    apaisada = ancho > alto
    k = min(alto * 0.95, ancho) / 108
    cx, cy = (ancho * 0.70, alto * 0.40) if apaisada else (ancho * 0.50, alto * 0.38)
    aros = "".join(
        '<circle cx="%d" cy="%d" r="%d" fill="none" stroke="%s" stroke-width="%d" stroke-opacity="%.2f"/>'
        % (cx, cy, min(alto, ancho * 1.1) * f, pista, alto * 0.012, o) for f, o in ((0.62, 0.55), (0.82, 0.38), (1.08, 0.24)))
    return """<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">
  <defs><radialGradient id="g" cx="{gx}" cy="{gy}" r="0.9">
    <stop offset="0" stop-color="{pista}"/><stop offset="1" stop-color="{fondo}"/>
  </radialGradient></defs>
  <rect width="{W}" height="{H}" fill="{fondo}"/>
  <rect width="{W}" height="{H}" fill="url(#g)" fill-opacity="0.55"/>
  {aros}
  <g transform="translate({x} {y}) scale({k})">
    <circle cx="54" cy="54" r="25" fill="none" stroke="{pista}" stroke-width="7"/>
    <path d="M54,29 A25,25 0 1 1 32.35,41.5" fill="none" stroke="{arco}" stroke-width="7" stroke-linecap="round"/>
    <path d="M36.76,57 C42,50 48,50 54,56 S66,63 71.24,57 A17.5,17.5 0 0 1 36.76,57 Z" fill="{ola}"/>
  </g>
</svg>""".format(W=ancho, H=alto, fondo=fondo, pista=pista, arco=arco, ola=ola, aros=aros,
                 x=cx - 54 * k, y=cy - 54 * k, k=k, gx=cx / ancho, gy=cy / alto)


def main():
    for evento, tema in TEMAS.items():
        carpeta = RAIZ / "store" / "events" / evento
        carpeta.mkdir(parents=True, exist_ok=True)
        for nombre, (ancho, alto) in TAMANOS.items():
            render(arte(ancho, alto, *colores(tema)), carpeta / (nombre + ".png"), ancho, alto)


if __name__ == "__main__":
    main()
