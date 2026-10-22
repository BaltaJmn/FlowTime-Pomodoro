#!/usr/bin/env python3
"""The Play listing of a season, and the way back to the normal one. Only the text of the listing
changes; the icon, the graphics and the screenshots stay as they are.

    temporada.py comprobar [nombre]        limits, languages and characters; nothing is sent
    temporada.py subir <nombre> [--dry]    title, short and full description in every language, one edit

<nombre> is a folder of store/temporada/ (examenes-diciembre, enero-2027) or "normal": store/listings as it is now.
A season holds short.txt and intro.txt per language. The short description replaces the normal one, and
intro.txt goes above the normal full description, so what is fixed in store/listings reaches the season
too. The title is always the normal one: the search is won on it. `--dry` has Play validate the edit and
throws it away. Writing to Play is chosen by hand (store/ci.md); this file is run by temporada.yml.
The upload itself is play.py of BaltaJmn/ci, in .ci/tienda (Actions) or ../ci/tienda (the Mac).
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
STORE = ROOT / "store"
PACKAGE = "com.baltajmn.flowtime"
LIMITS = {"title": 30, "short": 80, "full": 4000}
# The family's writing rule: no typographic dashes or quotes, no ellipsis character, no hard space.
BANNED = "—–“”‘’… "


def read(path):
    return path.read_text(encoding="utf-8").strip()


def locales():
    return sorted(d.name for d in (STORE / "listings").iterdir() if d.is_dir())


def seasons():
    folder = STORE / "temporada"
    return sorted(d.name for d in folder.iterdir() if d.is_dir()) if folder.is_dir() else []


def texts(name):
    """{language: (title, short, full)} as the listing will read in that season."""
    out = {}
    for loc in locales():
        normal = STORE / "listings" / loc
        title, short, full = read(normal / "title.txt"), read(normal / "short.txt"), read(normal / "full.txt")
        if name != "normal":
            season = STORE / "temporada" / name / loc
            short = read(season / "short.txt")
            full = read(season / "intro.txt") + "\n\n" + full
        out[loc] = (title, short, full)
    return out


def check(name):
    errors = []
    if name != "normal":
        have = {d.name for d in (STORE / "temporada" / name).iterdir() if d.is_dir()}
        if have != set(locales()):
            errors.append(f"{name}: idiomas distintos de store/listings, faltan {sorted(set(locales()) - have)}, sobran {sorted(have - set(locales()))}")
        for loc in sorted(have & set(locales())):
            for f in ("short.txt", "intro.txt"):
                if not (STORE / "temporada" / name / loc / f).is_file():
                    errors.append(f"falta temporada/{name}/{loc}/{f}")
    if errors:
        return errors
    for loc, parts in texts(name).items():
        for kind, text in zip(LIMITS, parts):
            if not text or len(text) > LIMITS[kind]:
                errors.append(f"{name}/{loc} {kind}: {len(text)} caracteres, el tope es {LIMITS[kind]}")
            bad = sorted({c for c in text if c in BANNED})
            if bad:
                errors.append(f"{name}/{loc} {kind}: caracteres tipograficos {[hex(ord(c)) for c in bad]}")
    return errors


def play():
    for folder in (ROOT / ".ci/tienda", ROOT.parent / "ci/tienda"):
        if (folder / "play.py").is_file():
            sys.path.insert(0, str(folder))
            import play  # noqa: E402
            return play
    sys.exit("falta play.py de BaltaJmn/ci: .ci/tienda o ../ci/tienda")


def upload(name, dry):
    errors = check(name)
    if errors:
        sys.exit("\n".join("ERROR " + e for e in errors))
    api = play()
    with api.Edit(PACKAGE, dry) as edit:
        for loc, (title, short, full) in texts(name).items():
            api.call("PUT", f"{edit.url}/listings/{loc}", {
                "language": loc, "title": title, "shortDescription": short, "fullDescription": full})
            print(f"  {loc}: {name}")


def main(argv):
    if argv[:1] == ["comprobar"]:
        names = argv[1:2] or ["normal"] + seasons()
        bad = False
        for name in names:
            errors = check(name)
            bad |= bool(errors)
            print(f"{name}: " + ("ok" if not errors else ""))
            for e in errors:
                print("ERROR", e)
        return 1 if bad else 0
    if argv[:1] == ["subir"] and len(argv) in (2, 3) and re.fullmatch(r"[a-z0-9-]+", argv[1]) and argv[2:] in ([], ["--dry"]):
        if argv[1] != "normal" and argv[1] not in seasons():
            sys.exit(f"no existe store/temporada/{argv[1]}")
        upload(argv[1], dry=argv[2:] == ["--dry"])
        return 0
    sys.exit(__doc__)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
