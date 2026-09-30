#!/usr/bin/env python3
"""Enmarca las capturas de la ficha: el fondo del icono, un titular y la pantalla con las esquinas
redondeadas (#54).

    ./gradlew :features:screens:recordRoborazziDebug --tests '*Store*'
    python3 tools/store/capturas.py

Las pantallas de la app salen de Roborazzi (StoreScreenshotsTest): sin emulador y siempre iguales. El
widget y la notificacion no los pinta la app, asi que son capturas del emulador, guardadas en
store/screenshots/raw/<idioma>/. El movil va en 1200x2100 porque Play rechaza una captura cuyo lado
largo pase del doble del corto; la tablet, en el 16:9 que pide Play para ensenarla en tablets.
"""
import base64
import pathlib
import subprocess
import tempfile

from PIL import Image

RAIZ = pathlib.Path(__file__).resolve().parents[2]
ROBORAZZI = RAIZ / "features" / "screens" / "build" / "outputs" / "roborazzi" / "store"
CRUDAS = RAIZ / "store" / "screenshots" / "raw"
EMULADOR = {"06_widget", "07_notification"}

# Los colores del icono (design/icon.svg): el fondo, el anillo y el agua.
FONDO, TINTA, AGUA = "#15506A", "#EAF6FB", "#8ECAE6"

FORMATOS = {
    "play": {"origen": "phone", "lienzo": (1200, 2100), "letra": 62, "lineas": (170, 252), "arriba": 360, "abajo": 90,
             "radio": 46, "capturas": ["01_focus", "02_dark", "03_stats", "04_tasks", "05_sounds",
                                       "06_widget", "07_notification"]},
    "tablet": {"origen": "tablet", "lienzo": (2560, 1440), "letra": 72, "lineas": (150, 236), "arriba": 300, "abajo": 60,
               "radio": 36, "capturas": ["01_focus", "02_stats", "03_tasks", "04_settings"]},
}

# Dos lineas por escena. La escena es el nombre de la captura sin el numero.
TITULARES = {
    "es-ES": {
        "focus": ["Concéntrate a tu ritmo", "con Pomodoro o FlowTime"],
        "dark": ["FlowTime: trabaja sin cortes", "y descansa lo que toca"],
        "stats": ["Tu racha, tu objetivo", "y a qué dedicas el tiempo"],
        "tasks": ["Las tareas de hoy:", "lo pendiente pasa a mañana"],
        "sounds": ["Lluvia, cafetería, fuego:", "mezcla tus sonidos"],
        "widget": ["Empieza desde", "la pantalla de inicio"],
        "notification": ["Sigue contando", "con la app cerrada"],
        "settings": ["A tu gusto: objetivo,", "recordatorio y etiquetas"],
    },
    "en-US": {
        "focus": ["Focus at your own pace", "with Pomodoro or FlowTime"],
        "dark": ["FlowTime: no interruptions,", "just the break you earned"],
        "stats": ["Your streak, your goal", "and where your time goes"],
        "tasks": ["Today's tasks:", "unfinished ones roll over"],
        "sounds": ["Rain, coffee shop, fire:", "mix your own sounds"],
        "widget": ["Start right from", "your home screen"],
        "notification": ["Keeps counting", "with the app closed"],
        "settings": ["Made yours: goal,", "reminder and tags"],
    },
    "de-DE": {
        "focus": ["In deinem Tempo:", "Pomodoro oder FlowTime"],
        "dark": ["FlowTime: ohne Countdown,", "die Pause passt sich an"],
        "stats": ["Serie, Tagesziel und", "wohin deine Zeit geht"],
        "tasks": ["Aufgaben für heute:", "Offenes kommt morgen wieder"],
        "sounds": ["Regen, Café, Feuer:", "misch deine Klänge"],
        "widget": ["Direkt vom", "Startbildschirm starten"],
        "notification": ["Zählt weiter, auch", "mit geschlossener App"],
        "settings": ["Wie du willst: Ziel,", "Erinnerung und Labels"],
    },
    "it-IT": {
        "focus": ["Concentrati al tuo ritmo", "con Pomodoro o FlowTime"],
        "dark": ["FlowTime: lavora senza stop,", "la pausa si adatta"],
        "stats": ["Serie, obiettivo", "e dove va il tuo tempo"],
        "tasks": ["Le attività di oggi:", "il resto passa a domani"],
        "sounds": ["Pioggia, caffè, fuoco:", "mescola i tuoi suoni"],
        "widget": ["Parti direttamente", "dalla schermata Home"],
        "notification": ["Continua a contare", "anche con l'app chiusa"],
        "settings": ["A modo tuo: obiettivo,", "promemoria ed etichette"],
    },
    "ru-RU": {
        "focus": ["Работайте в своём ритме:", "помодоро или FlowTime"],
        "dark": ["FlowTime: без помех,", "перерыв подстроится"],
        "stats": ["Серия, цель и то,", "на что уходит время"],
        "tasks": ["Задачи на сегодня:", "остаток перейдёт на завтра"],
        "sounds": ["Дождь, кафе, огонь:", "смешивайте звуки"],
        "widget": ["Запуск прямо", "с главного экрана"],
        "notification": ["Считает, даже когда", "приложение закрыто"],
        "settings": ["Под вас: цель,", "напоминание и метки"],
    },
    "hi-IN": {
        "focus": ["अपनी रफ़्तार से ध्यान लगाएँ", "पोमोडोरो या FlowTime के साथ"],
        "dark": ["FlowTime: बिना रुकावट काम,", "ब्रेक अपने-आप तय"],
        "stats": ["लगातार दिन, लक्ष्य और", "आपका समय कहाँ जाता है"],
        "tasks": ["आज के काम:", "बाकी कल फिर दिखेंगे"],
        "sounds": ["बारिश, कैफ़े, आग:", "अपनी ध्वनियाँ मिलाएँ"],
        "widget": ["होम स्क्रीन से", "सीधे शुरू करें"],
        "notification": ["ऐप बंद होने पर भी", "गिनती जारी"],
        "settings": ["आपकी पसंद से: लक्ष्य,", "रिमाइंडर और टैग"],
    },
}


def marco(png, lineas, formato, salida):
    ancho, alto = formato["lienzo"]
    crudo_w, crudo_h = Image.open(png).size
    arriba, radio, letra = formato["arriba"], formato["radio"], formato["letra"]
    alto_p = alto - arriba - formato["abajo"]
    ancho_p = round(alto_p * crudo_w / crudo_h)
    x = (ancho - ancho_p) // 2
    texto = "".join(
        '<text x="%d" y="%d" text-anchor="middle" font-family="Helvetica Neue, Helvetica, Arial, sans-serif" '
        'font-size="%d" font-weight="600" fill="%s">%s</text>'
        % (ancho // 2, y, letra, color, linea.replace("&", "&amp;").replace("<", "&lt;"))
        for y, color, linea in zip(formato["lineas"], (TINTA, AGUA), lineas)
    )
    # rsvg no abre ficheros fuera del directorio del SVG, asi que la captura viaja dentro.
    datos = "data:image/png;base64," + base64.b64encode(png.read_bytes()).decode("ascii")
    svg = """<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"
  width="{W}" height="{H}" viewBox="0 0 {W} {H}">
  <rect width="{W}" height="{H}" fill="{fondo}"/>
  {texto}
  <defs><clipPath id="r"><rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}"/></clipPath></defs>
  <image xlink:href="{datos}" x="{x}" y="{y}" width="{w}" height="{h}" clip-path="url(#r)"/>
  <rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="none" stroke="{tinta}" stroke-opacity="0.3"
        stroke-width="3"/>
</svg>""".format(W=ancho, H=alto, fondo=FONDO, tinta=TINTA, texto=texto, datos=datos,
                 x=x, y=arriba, w=ancho_p, h=alto_p, r=radio)
    render(svg, salida, ancho, alto)


def render(svg, salida, ancho, alto):
    """rsvg pinta; PIL quita el canal alfa, que Play no acepta en capturas ni en el grafico."""
    with tempfile.NamedTemporaryFile("w", suffix=".svg", encoding="utf-8") as f:
        f.write(svg)
        f.flush()
        subprocess.run(["rsvg-convert", "-w", str(ancho), "-h", str(alto), f.name, "-o", str(salida)],
                       check=True)
    Image.open(salida).convert("RGB").save(salida, optimize=True)
    print(salida.relative_to(RAIZ))


def main():
    for nombre, formato in FORMATOS.items():
        for idioma, titulares in TITULARES.items():
            salida = RAIZ / "store" / "screenshots" / nombre / idioma
            salida.mkdir(parents=True, exist_ok=True)
            # play.sh sube todo lo que haya en la carpeta: una captura que ya no se usa no puede quedarse.
            for viejo in salida.glob("*.png"):
                viejo.unlink()
            for i, captura in enumerate(formato["capturas"]):
                origen = CRUDAS / idioma if captura in EMULADOR else ROBORAZZI / formato["origen"] / idioma
                escena = captura.split("_", 1)[1]
                marco(origen / (captura + ".png"), titulares[escena], formato, salida / ("%02d.png" % (i + 1)))


if __name__ == "__main__":
    main()
