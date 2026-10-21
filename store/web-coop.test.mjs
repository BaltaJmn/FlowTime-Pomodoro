// Comprueba a donde lleva store/web-coop/404.html en cada movil: node store/web-coop.test.mjs
// Ejecuta el script de la pagina con un navegador de mentira (navigator, location, document, fetch).
import { readFileSync } from "node:fs";
import vm from "node:vm";
import assert from "node:assert/strict";

const html = readFileSync(new URL("./web-coop/404.html", import.meta.url), "utf8");
const script = html.match(/<script>([\s\S]*)<\/script>/)[1];
const PLAY = "https://play.google.com/store/apps/details?id=com.baltajmn.flowtime";
const APPLE = "https://apps.apple.com/app/id6819302003";

async function visit({ ua, path = "/f/0123456789", touch = 0, lang = "en-US", listed = true, offline = false }) {
  const el = () => ({ hidden: false, textContent: "", innerHTML: "" });
  const els = { title: el(), text: el(), play: el(), apple: el(), stores: el() };
  let went = null;
  const sandbox = {
    navigator: { userAgent: ua, language: lang, maxTouchPoints: touch },
    location: { pathname: path, replace: (u) => (went = u) },
    document: { documentElement: {}, getElementById: (id) => els[id] },
    fetch: async () => {
      if (offline) throw new Error("sin red");
      return { json: async () => ({ resultCount: listed ? 1 : 0 }) };
    },
    Date,
    Math,
  };
  vm.runInNewContext(script, sandbox);
  await new Promise((r) => setTimeout(r, 10));
  return { went, els };
}

const ANDROID = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/126 Mobile Safari/537.36";
const IPHONE = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 Version/18.0 Mobile/15E148 Safari/604.1";
const IPAD = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/18.0 Safari/605.1.15";
const MAC = IPAD;

// Android: a Play.
let r = await visit({ ua: ANDROID });
assert.equal(r.went, PLAY);
assert.equal(r.els.apple.hidden, true);

// El codigo de sala tambien (/r/CODIGO).
assert.equal((await visit({ ua: ANDROID, path: "/r/ABCDEF" })).went, PLAY);

// iPhone con la app ya en la App Store: a la App Store.
r = await visit({ ua: IPHONE });
assert.equal(r.went, APPLE);

// iPad con el sitio de escritorio (se hace pasar por Mac, pero es tactil).
assert.equal((await visit({ ua: IPAD, touch: 5 })).went, APPLE);

// iPhone sin la app en la App Store: no se va a ninguna parte y el aviso lo dice, en su idioma.
r = await visit({ ua: IPHONE, listed: false, lang: "es-ES" });
assert.equal(r.went, null);
assert.match(r.els.title.textContent, /llega pronto al iPhone/);
assert.equal(r.els.stores.hidden, true);

// Sin red, mejor la App Store que nada.
assert.equal((await visit({ ua: IPHONE, offline: true })).went, APPLE);

// Un Mac de verdad (sin pantalla tactil): las dos tiendas, sin redirigir.
r = await visit({ ua: MAC });
assert.equal(r.went, null);
assert.equal(r.els.stores.hidden, false);
assert.equal(r.els.play.hidden, false);
assert.equal(r.els.apple.hidden, false);

// Una ruta que no es un enlace: ni tiendas ni redireccion, aunque sea Android.
r = await visit({ ua: ANDROID, path: "/otra/cosa" });
assert.equal(r.went, null);
assert.equal(r.els.stores.hidden, true);

console.log("web-coop: ok");
