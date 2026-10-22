# Publicar desde GitHub Actions

Igual en las cuatro apps de la familia: el proceso, los workflows y los scripts viven en
`BaltaJmn/ci` (`README.md` y `PUBLICAR.md`). Aquí, solo lo que es de FlowTime.

| Qué | Valor |
|---|---|
| Paquete de Play y bundle id de Apple | `com.baltajmn.flowtime` |
| Clave de subida | `~/keys/flowtime-upload.jks`, alias `upload`, `CN=BaltaJmn` (el `signer-cn` de `release.yml`) |
| Versión de las dos tiendas | `versionName` y `versionCode` de `build-logic/plugins/src/main/java/Config.kt` |
| Web: contacto de Play, soporte y privacidad de Apple | https://flowtime.baltajmn.dev/ |
| Pro | `pro_lifetime` en Play, `com.baltajmn.flowtime.pro_lifetime` en Apple; 1,99 EUR en las dos |

FlowTime no tiene `androidApp`: el módulo es `:app` y la versión vive en `Config.kt`, por eso `release.yml` pasa `gradle-tasks`, `aab-path` y `version-file`. Ya está en producción en Play: el tren sube a `alpha` como en las demás y el lunes la promociona a producción. Además de Pro tiene tres propinas (`tip_small`, `tip_medium`, `tip_large`) y la compra antigua `support_developer`, fuera de RevenueCat.

## Publicar una versión: el tren de los lunes

Desde el 26-10-2026 las versiones salen solas con el tren semanal (`.github/workflows/tren.yml`; el
proceso, en `BaltaJmn/ci` PUBLICAR.md sección 7):

- **Domingo 19:37** (hora de Madrid): si hay cambios en main desde la última versión (sin contar
  `.md` ni `.github`), sube la minor (`X.Y+1.0`) y el `versionCode`, deja el commit y la etiqueta,
  sube a Play `alpha` y a TestFlight y avisa por ntfy con el botón "Parar esta semana".
- **Lunes 9:07**: producción al 100 % en Play y revisión en App Store (sale sola al aprobarse), con
  la ficha de `store/app-store` y las capturas de iPhone si cambiaron. Una tienda con la versión
  anterior en revisión, rechazada o sin publicar se salta la semana, y el aviso lo dice.
- **Entre semana nadie toca `versionName` ni pone etiquetas.** El `versionCode` solo sube para una
  build de prueba de TestFlight sin etiqueta (`gh workflow run release.yml -f stores=app-store`); el
  tren numera por encima del mayor usado en el fichero, Play o App Store Connect (TestFlight incluido), así que una build de prueba no bloquea nada ni hay que apuntarla. Un arreglo urgente sube la patch
  (`X.Y.Z+1`) a mano y solo con el sí del usuario: commit con la versión, `git tag vX.Y.Z && git
  push origin vX.Y.Z` (alpha y TestFlight) y `gh workflow run tren.yml -f paso=lunes -f seco=false`.
- **Novedades**: quien cambia algo visible reescribe `store/whatsnew/whatsnew-<idioma>` en todos los
  idiomas en el mismo commit (tope 500). Valen para las dos tiendas: nada que solo exista en una, ni
  el nombre de la otra tienda (Apple rechaza que se nombre Google Play). Si nadie las toca, salen las
  genéricas de `store/whatsnew-generic` y el aviso del domingo lo dice.
- **Avisos**: lo que sale bien llega en silencio; suenan los fallos y las tiendas que se saltan la
  semana. Cada paso tiene una ejecución de reserva (domingo 21:37, lunes 4:17 y 11:07) que reintenta lo
  pasajero antes de que suene.
- Prueba sin publicar nada: `gh workflow run tren.yml -f paso=domingo` (seco por defecto) y después
  `-f paso=lunes`.

Otro canal o una sola tienda, a mano: `gh workflow run release.yml -f stores=play -f track=internal`
(`-f status=draft` mientras la app no haya publicado nada en Play).

## Ficha

Textos, gráficos y capturas en `store/`, con la estructura del README de `ci`. Cada push a `store/`
la comprueba; para subirla, `gh workflow run listings.yml -f target=play` (o `app-store`, `both`).
La App Store solo acepta cambios con una versión en preparación.

## Fichas de temporada

El texto de la ficha de Play cambia en examenes (30-11 al 20-12) y en enero (28-12 al 31-01), y vuelve a la
normal: `store/temporada/README.md` (ordenes, fechas y condiciones) y `temporada.yml`. Un push solo las
comprueba; subirlas va con el si del usuario, como cualquier cambio de la ficha.

## Secretos del repositorio

Los cinco de Play salen de la clave de esta app y de la cuenta de servicio que publica. Se ponen sin
que el valor pase por la pantalla:

```bash
base64 -i ~/keys/flowtime-upload.jks | gh secret set KEYSTORE_BASE64 -R BaltaJmn/FlowTime-Pomodoro
sed -n 's/^storePassword=//p' keystore.properties | tr -d '\n' | gh secret set KEYSTORE_PASSWORD -R BaltaJmn/FlowTime-Pomodoro
sed -n 's/^keyPassword=//p' keystore.properties | tr -d '\n' | gh secret set KEY_PASSWORD -R BaltaJmn/FlowTime-Pomodoro
printf upload | gh secret set KEY_ALIAS -R BaltaJmn/FlowTime-Pomodoro
gh secret set PLAY_SERVICE_ACCOUNT_JSON -R BaltaJmn/FlowTime-Pomodoro < ~/keys/play-service-account.json
```

Los ocho de Apple (`APPSTORE_*`, `APPLE_*`) son de cuenta, iguales en las cuatro apps: los pone
`~/keys/credenciales.sh sincronizar`, que también renueva `PLAY_SERVICE_ACCOUNT_JSON` al rotarla.
