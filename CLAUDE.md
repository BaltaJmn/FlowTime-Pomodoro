# FlowTime

Lo carga Claude Code en cada sesión sobre este repositorio: lo que hay que saber siempre. El
proceso de publicar es el de la familia (`BaltaJmn/ci`); lo de FlowTime, en `store/ci.md`.

## Versiones: el tren de los lunes

- Nadie toca `versionName` ni `versionCode` (`build-logic/plugins/src/main/java/Config.kt`) ni pone
  etiquetas entre semana: el tren semanal (`.github/workflows/tren.yml`) numera y sube el domingo y
  saca la versión el lunes en las dos tiendas (`store/ci.md`, "Publicar una versión").
- Quien cambia algo visible reescribe `store/whatsnew/whatsnew-<idioma>` en los catorce idiomas en el
  mismo commit (tope 500). Valen para Play y App Store: nada que solo exista en una, ni el nombre de
  la otra tienda.
- Un arreglo urgente (patch), a mano y solo con el sí del usuario.
