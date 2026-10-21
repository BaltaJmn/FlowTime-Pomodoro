#!/usr/bin/env python3
"""Enmarca las capturas de la ficha: el fondo del icono, un titular y la pantalla con las esquinas
redondeadas (#54).

    ./gradlew :features:screens:recordRoborazziAndroidHostTest --tests '*Store*'
    python3 tools/store/capturas.py

Las pantallas de la app salen de Roborazzi (StoreScreenshotsTest): sin emulador y siempre iguales. El
widget y la notificacion no los pinta la app, asi que son capturas del emulador, guardadas en
store/screenshots/raw/<idioma>/. El movil va en 1200x2100 porque Play rechaza una captura cuyo lado
largo pase del doble del corto; la tablet, en el 16:9 que pide Play para ensenarla en tablets.
"""
import base64
import os
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

# La letra de la app. render() hace que rsvg la busque con fontconfig (en macOS Pango iria a CoreText,
# que no la tiene) y le suma la carpeta de fuentes del repo, sin instalar nada. El hindi sale de una
# del sistema.
LETRA = "Mulish, sans-serif"
FONTS_CONF = """<?xml version="1.0"?>
<fontconfig>
  <include ignore_missing="yes">/opt/homebrew/etc/fonts/fonts.conf</include>
  <include ignore_missing="yes">/etc/fonts/fonts.conf</include>
  <dir>%s</dir>
</fontconfig>
""" % (RAIZ / "core" / "design" / "src" / "commonMain" / "composeResources" / "font")

FORMATOS = {
    # Las tres primeras son las que se ven en los resultados de busqueda: el beneficio, en grande.
    # "letra" es el tope; marco() la baja si el titular no cabe en el idioma.
    "play": {"origen": "phone", "lienzo": (1200, 2100), "letra": 82, "lineas": (192, 290), "arriba": 390, "abajo": 90,
             "radio": 46, "capturas": ["02_dark", "01_focus", "03_stats", "04_tasks", "05_sounds",
                                       "06_widget", "07_notification"]},
    # App Store Connect pide el tamano exacto del iPhone de 6,9". El widget y la notificacion son del
    # emulador de Android, asi que el iPhone se queda con las pantallas de la app.
    "iphone": {"origen": "phone", "lienzo": (1320, 2868), "letra": 92, "lineas": (232, 342), "arriba": 440, "abajo": 110,
               "radio": 50, "capturas": ["02_dark", "01_focus", "03_stats", "04_tasks", "05_sounds"]},
    "tablet": {"origen": "tablet", "lienzo": (2560, 1440), "letra": 72, "lineas": (150, 236), "arriba": 300, "abajo": 60,
               "radio": 36, "capturas": ["01_focus", "02_stats", "03_tasks", "04_settings"]},
}

# Dos lineas por escena. La escena es el nombre de la captura sin el numero.
TITULARES = {
    "es-ES": {
        "focus": ["Tres modos de foco,", "sin anuncios ni cuenta"],
        "dark": ["El descanso se adapta", "a lo que has trabajado"],
        "stats": ["Mantén tu racha y", "cumple tu objetivo"],
        "tasks": ["Las tareas de hoy:", "lo pendiente pasa a mañana"],
        "sounds": ["Lluvia, cafetería, fuego:", "mezcla tus sonidos"],
        "widget": ["Empieza desde", "la pantalla de inicio"],
        "notification": ["Sigue contando", "con la app cerrada"],
        "settings": ["A tu gusto: objetivo,", "recordatorio y etiquetas"],
    },
    "en-US": {
        "focus": ["Three focus modes,", "no ads, no account"],
        "dark": ["Breaks that adapt", "to how long you worked"],
        "stats": ["Keep your streak,", "hit your daily goal"],
        "tasks": ["Today's tasks:", "unfinished ones roll over"],
        "sounds": ["Rain, coffee shop, fire:", "mix your own sounds"],
        "widget": ["Start right from", "your home screen"],
        "notification": ["Keeps counting", "with the app closed"],
        "settings": ["Made yours: goal,", "reminder and tags"],
    },
    "de-DE": {
        "focus": ["Drei Fokus-Modi,", "ohne Werbung und Konto"],
        "dark": ["Die Pause passt sich an,", "wie lange du gearbeitet hast"],
        "stats": ["Halte deine Serie,", "erreiche dein Tagesziel"],
        "tasks": ["Aufgaben für heute:", "Offenes kommt morgen wieder"],
        "sounds": ["Regen, Café, Feuer:", "misch deine Klänge"],
        "widget": ["Direkt vom", "Startbildschirm starten"],
        "notification": ["Zählt weiter, auch", "mit geschlossener App"],
        "settings": ["Wie du willst: Ziel,", "Erinnerung und Labels"],
    },
    "it-IT": {
        "focus": ["Tre modalità di focus,", "senza pubblicità"],
        "dark": ["La pausa si adatta", "a quanto hai lavorato"],
        "stats": ["Mantieni la serie,", "raggiungi l'obiettivo"],
        "tasks": ["Le attività di oggi:", "il resto passa a domani"],
        "sounds": ["Pioggia, caffè, fuoco:", "mescola i tuoi suoni"],
        "widget": ["Parti direttamente", "dalla schermata Home"],
        "notification": ["Continua a contare", "anche con l'app chiusa"],
        "settings": ["A modo tuo: obiettivo,", "promemoria ed etichette"],
    },
    "ru-RU": {
        "focus": ["Три режима фокуса,", "без рекламы и аккаунта"],
        "dark": ["Перерыв подстраивается", "под вашу работу"],
        "stats": ["Держите серию и", "достигайте цели дня"],
        "tasks": ["Задачи на сегодня:", "остаток перейдёт на завтра"],
        "sounds": ["Дождь, кафе, огонь:", "смешивайте звуки"],
        "widget": ["Запуск прямо", "с главного экрана"],
        "notification": ["Считает, даже когда", "приложение закрыто"],
        "settings": ["Под вас: цель,", "напоминание и метки"],
    },
    "hi-IN": {
        "focus": ["फ़ोकस के तीन मोड,", "बिना विज्ञापन, बिना अकाउंट"],
        "dark": ["ब्रेक आपके काम के", "हिसाब से अपने-आप ढलता है"],
        "stats": ["लगातार दिन बनाए रखें,", "रोज़ का लक्ष्य पूरा करें"],
        "tasks": ["आज के काम:", "बाकी कल फिर दिखेंगे"],
        "sounds": ["बारिश, कैफ़े, आग:", "अपनी ध्वनियाँ मिलाएँ"],
        "widget": ["होम स्क्रीन से", "सीधे शुरू करें"],
        "notification": ["ऐप बंद होने पर भी", "गिनती जारी"],
        "settings": ["आपकी पसंद से: लक्ष्य,", "रिमाइंडर और टैग"],
    },
    "fr-FR": {
        "focus": ["Trois modes de focus,", "sans pub ni compte"],
        "dark": ["La pause s'adapte", "à votre travail"],
        "stats": ["Gardez votre série,", "visez votre objectif"],
        "tasks": ["Les tâches du jour :", "le reste passe à demain"],
        "sounds": ["Pluie, café, feu :", "mélangez vos sons"],
    },
    "id": {
        "focus": ["Tiga mode fokus,", "tanpa iklan dan akun"],
        "dark": ["Jeda menyesuaikan", "lamanya kamu bekerja"],
        "stats": ["Jaga streak-mu,", "capai target harianmu"],
        "tasks": ["Tugas hari ini:", "sisanya pindah ke besok"],
        "sounds": ["Hujan, kafe, api:", "racik suaramu sendiri"],
    },
    "ja-JP": {
        "focus": ["集中モードは3種類", "広告なし、アカウント不要"],
        "dark": ["休憩は作業時間に合わせて", "自動で変わる"],
        "stats": ["連続記録を伸ばして", "1日の目標を達成"],
        "tasks": ["今日のタスク", "未完了は明日に繰り越し"],
        "sounds": ["雨、カフェ、焚き火", "好きな音をミックス"],
    },
    "ko-KR": {
        "focus": ["집중 모드 3가지,", "광고 없음, 계정 필요 없음"],
        "dark": ["휴식은 일한 시간에 맞춰", "알아서 달라집니다"],
        "stats": ["연속 기록을 이어가고", "하루 목표를 달성하세요"],
        "tasks": ["오늘의 할 일:", "못 끝낸 일은 내일로"],
        "sounds": ["비, 카페, 모닥불:", "나만의 소리를 섞어 보세요"],
    },
    "nl-NL": {
        "focus": ["Drie focusmodi,", "zonder reclame of account"],
        "dark": ["Pauzes die meebewegen", "met je werktijd"],
        "stats": ["Houd je reeks vast,", "haal je dagdoel"],
        "tasks": ["De taken van vandaag:", "de rest gaat naar morgen"],
        "sounds": ["Regen, koffiebar, vuur:", "mix je eigen geluiden"],
    },
    "pl-PL": {
        "focus": ["Trzy tryby skupienia,", "bez reklam i konta"],
        "dark": ["Przerwa dopasuje się", "do czasu Twojej pracy"],
        "stats": ["Utrzymaj passę", "i osiągnij cel dnia"],
        "tasks": ["Zadania na dziś:", "reszta przejdzie na jutro"],
        "sounds": ["Deszcz, kawiarnia, ogień:", "zmiksuj własne dźwięki"],
    },
    "pt-BR": {
        "focus": ["Três modos de foco,", "sem anúncios e sem conta"],
        "dark": ["A pausa se adapta", "ao seu trabalho"],
        "stats": ["Mantenha a sequência", "e bata sua meta do dia"],
        "tasks": ["As tarefas de hoje:", "o resto fica para amanhã"],
        "sounds": ["Chuva, cafeteria, fogo:", "misture seus próprios sons"],
    },
    "tr-TR": {
        "focus": ["Üç odak modu,", "reklamsız, hesapsız"],
        "dark": ["Mola, çalıştığın süreye", "göre ayarlanır"],
        "stats": ["Serini koru,", "günlük hedefine ulaş"],
        "tasks": ["Bugünün görevleri:", "kalanlar yarına geçer"],
        "sounds": ["Yağmur, kafe, ateş:", "kendi seslerini karıştır"],
    },
}

# Los idiomas que aun no tienen capturas de Play: les faltan el widget y la notificacion del emulador.
# Salen solo para el iPhone, que no las lleva.
SOLO_IPHONE = {"fr-FR", "id", "ja-JP", "ko-KR", "nl-NL", "pl-PL", "pt-BR", "tr-TR"}


_ANCHOS = {}


def ancho_100(linea):
    """Lo que mide la linea a 100 px con la letra de la ficha, medido pintandola: vale igual para el
    hindi, el japones o el ruso, sin tablas de anchos."""
    if linea not in _ANCHOS:
        svg = ('<svg xmlns="http://www.w3.org/2000/svg" width="4000" height="200"><text x="20" y="140" '
               'font-family="%s" font-size="100" font-weight="600">%s</text></svg>' % (LETRA, esc(linea)))
        with tempfile.TemporaryDirectory() as tmp:
            png = pathlib.Path(tmp, "medida.png")
            rsvg(svg, png, 4000, 200)
            caja = Image.open(png).getchannel("A").getbbox()
        _ANCHOS[linea] = caja[2] - caja[0]
    return _ANCHOS[linea]


def esc(linea):
    return linea.replace("&", "&amp;").replace("<", "&lt;")


def marco(png, lineas, formato, salida):
    ancho, alto = formato["lienzo"]
    crudo_w, crudo_h = Image.open(png).size
    arriba, radio = formato["arriba"], formato["radio"]
    # El titular ocupa como mucho el 90 % del ancho: si no cabe a la letra del formato, se baja.
    letra = min(formato["letra"], int(100 * 0.9 * ancho / max(ancho_100(l) for l in lineas)))
    alto_p = alto - arriba - formato["abajo"]
    ancho_p = round(alto_p * crudo_w / crudo_h)
    x = (ancho - ancho_p) // 2
    texto = "".join(
        '<text x="%d" y="%d" text-anchor="middle" font-family="%s" '
        'font-size="%d" font-weight="600" fill="%s">%s</text>'
        % (ancho // 2, y, LETRA, letra, color, esc(linea))
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


def rsvg(svg, salida, ancho, alto):
    with tempfile.TemporaryDirectory() as tmp:
        conf, dibujo = pathlib.Path(tmp, "fonts.conf"), pathlib.Path(tmp, "dibujo.svg")
        conf.write_text(FONTS_CONF, encoding="utf-8")
        dibujo.write_text(svg, encoding="utf-8")
        subprocess.run(["rsvg-convert", "-w", str(ancho), "-h", str(alto), str(dibujo), "-o", str(salida)],
                       check=True, env={**os.environ, "FONTCONFIG_FILE": str(conf), "PANGOCAIRO_BACKEND": "fc"})


def render(svg, salida, ancho, alto):
    """rsvg pinta; PIL quita el canal alfa, que Play no acepta en capturas ni en el grafico."""
    rsvg(svg, salida, ancho, alto)
    Image.open(salida).convert("RGB").save(salida, optimize=True)
    print(salida.relative_to(RAIZ))


def main():
    for nombre, formato in FORMATOS.items():
        for idioma, titulares in TITULARES.items():
            if idioma in SOLO_IPHONE and nombre != "iphone":
                continue
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
