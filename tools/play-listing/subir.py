#!/usr/bin/env python3
"""Sube las fichas de tienda de los seis idiomas de una vez.

Play Console no tiene importacion masiva: cada idioma se pega a mano en tres campos, y son seis
idiomas por dieciocho campos. La API de Android Publisher si acepta los seis en una sola edicion, y
ademas la edicion es atomica: o entran todos los idiomas o no entra ninguno, que es justo lo que se
quiere de un cambio de posicionamiento.

Sin argumentos solo comprueba los limites y no toca Play. Con --subir hace el cambio de verdad, y
con --estado no escribe nada: lee en que canal esta cada versionCode, que es la unica forma de saber
desde fuera de la Console si una version llego a su canal o se quedo en borrador.
"""
import json
import os
import pathlib
import sys

# Los tres topes que impone Play. Superarlos no da un error util en la Console: el campo se corta
# o el boton de guardar se queda muerto, asi que se comprueba aqui antes de abrir la edicion.
TOPES = {"title": 30, "short": 80, "full": 4000}
CAMPOS = {"title": "title", "short": "shortDescription", "full": "fullDescription"}
RAIZ = pathlib.Path(__file__).resolve().parents[2] / "store" / "listings"
PAQUETE = os.environ.get("PACKAGE_NAME", "com.baltajmn.flowtime")


def leer():
    fichas = {}
    for d in sorted(p for p in RAIZ.iterdir() if p.is_dir()):
        ficha = {}
        for nombre, tope in TOPES.items():
            texto = (d / (nombre + ".txt")).read_text(encoding="utf-8").strip()
            if len(texto) > tope:
                sys.exit("%s/%s.txt: %d caracteres, el tope es %d" % (d.name, nombre, len(texto), tope))
            if not texto:
                sys.exit("%s/%s.txt esta vacio" % (d.name, nombre))
            ficha[CAMPOS[nombre]] = texto
        fichas[d.name] = ficha
        print("%-6s titulo %2d  corta %2d  larga %4d" % (
            d.name, len(ficha["title"]), len(ficha["shortDescription"]), len(ficha["fullDescription"])))
    return fichas


def conectar():
    from google.oauth2 import service_account
    from googleapiclient.discovery import build

    credenciales = service_account.Credentials.from_service_account_info(
        json.loads(os.environ["PLAY_SERVICE_ACCOUNT_JSON"]),
        scopes=["https://www.googleapis.com/auth/androidpublisher"],
    )
    return build("androidpublisher", "v3", credentials=credenciales, cache_discovery=False).edits()


def estado():
    """Lee los canales. Una edicion que no se confirma no cambia nada y caduca sola."""
    edits = conectar()
    edicion = edits.insert(packageName=PAQUETE, body={}).execute()["id"]
    for canal in edits.tracks().list(packageName=PAQUETE, editId=edicion).execute().get("tracks", []):
        for version in canal.get("releases", []) or [None]:
            if version is None:
                print("%-11s vacio" % canal["track"])
                continue
            print("%-11s versionCode %-6s estado %-11s %s" % (
                canal["track"],
                ",".join(version.get("versionCodes", [])) or "-",
                version.get("status", "?"),
                version.get("name", ""),
            ))


def subir(fichas):
    edits = conectar()
    edicion = edits.insert(packageName=PAQUETE, body={}).execute()["id"]
    for idioma, ficha in fichas.items():
        edits.listings().update(
            packageName=PAQUETE, editId=edicion, language=idioma,
            body=dict(ficha, language=idioma),
        ).execute()
        print("subido %s" % idioma)
    edits.commit(packageName=PAQUETE, editId=edicion).execute()
    print("edicion %s confirmada" % edicion)


if __name__ == "__main__":
    if "--estado" in sys.argv:
        estado()
    elif "--subir" in sys.argv:
        subir(leer())
    else:
        leer()
        print("\nSolo comprobacion. Anade --subir para escribir en Play.")
