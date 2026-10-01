# Publicar desde GitHub Actions

Igual que las hermanas (Chroma, MoodTraker, Purl): la secuencia de Android vive en `BaltaJmn/ci`
(`android-play-release.yml`) y `.github/workflows/release.yml` solo la llama con el paquete, el CN
de la firma, la tarea de Gradle y la carpeta de notas. `tests.yml` corre `./gradlew build` en cada
push a `main` y en cada pull request, y en macOS los tests de iOS de `core/data` y la compilación de
todo `iosMain`: lo único que demuestra que el iPhone sigue compilando y enlazando. El de macOS cuesta
diez veces más por minuto, y por eso el disparador no incluye ramas sueltas.

## Cómo se dispara

**Por etiqueta**, no en cada push a `main`. FlowTime ya está en producción, así que la etiqueta
publica **en producción**, no en `alpha` como las hermanas.

1. Subir `versionCode` (y `versionName`) en `build-logic/plugins/src/main/java/Config.kt`. Tiene
   que ser mayor que el último de Play: `~/keys/play.sh estado com.baltajmn.flowtime`.
2. Reescribir las notas de `store/whatsnew/whatsnew-<idioma>`, una por idioma de la ficha (tope 500 cada una).
3. Commit, y la etiqueta:

```bash
git tag v2.1.0 && git push origin v2.1.0
```

Disparo manual, para otro canal:

```bash
gh workflow run release.yml --ref main -f track=internal
```

## La firma

Clave de subida en `~/keys/flowtime-upload.jks`, alias `upload`, `CN=BaltaJmn, O=BaltaJmn, C=ES`,
RSA 4096, generada el 29-09-2026 con el comando de `~/keys/LEEME.md`. SHA-256 del certificado:
`5A:BE:9A:97:4F:D0:F0:B2:B3:D6:06:12:48:29:CA:96:FD:3F:09:17:5A:EB:BB:89:61:81:69:5B:6B:BA:19:0B`.

La anterior se perdió: el 29-09-2026 se pidió en Play Console (*Protegida con Play > Firma de
aplicaciones*) el cambio de clave de subida con este certificado. **Hasta que Play lo active**, que
avisa por correo con la fecha, rechaza cualquier AAB firmado con la nueva. Se comprueba en esa
misma pantalla: el SHA-256 del certificado de subida tiene que ser el de arriba.

`release.yml` pasa `signer-cn: CN=BaltaJmn` porque el valor por defecto del workflow compartido es
`CN=Baltasar`. En local, `keystore.properties` en la raíz (git-ignorado); sin él la release cae a
la clave de debug, que Play rechaza.

## Secretos del repositorio

| Secreto | Qué es | De dónde sale |
|---|---|---|
| `KEYSTORE_BASE64` | El `.jks` de subida, en base64 | `~/keys/flowtime-upload.jks` |
| `KEYSTORE_PASSWORD` | La del almacén | `keystore.properties` |
| `KEY_ALIAS` | `upload` | |
| `KEY_PASSWORD` | La de la clave (la misma, PKCS12) | `keystore.properties` |
| `PLAY_SERVICE_ACCOUNT_JSON` | La cuenta de servicio **de publicar** | `~/keys/credenciales.sh`, que tiene este repo en `PLAY_REPOS` |

Se ponen sin que el valor pase por la pantalla:

```bash
R=BaltaJmn/FlowTime-Pomodoro
base64 -i ~/keys/flowtime-upload.jks | gh secret set KEYSTORE_BASE64 -R $R
sed -n 's/^storePassword=//p' keystore.properties | tr -d '\n' | gh secret set KEYSTORE_PASSWORD -R $R
sed -n 's/^keyPassword=//p' keystore.properties | tr -d '\n' | gh secret set KEY_PASSWORD -R $R
printf upload | gh secret set KEY_ALIAS -R $R
gh secret set PLAY_SERVICE_ACCOUNT_JSON -R $R < ~/keys/play-service-account.json
```

Al rotar la cuenta de publicar con `credenciales.sh play`, este secreto se actualiza solo.

## iPhone: TestFlight

`release-ios.yml`, copiado de MoodTraker, se dispara con la misma etiqueta `v*`: una etiqueta
publica en las dos tiendas. Archiva, exporta y sube a TestFlight en un solo `xcodebuild`
(`destination: upload` en el `ExportOptions.plist`, que evita pasar el ipa por `altool`).

Corre en `macos-26` y no en `macos-15`: Compose enlaza clases de UIKit que solo existen desde el SDK
de iOS 26, y la imagen vieja falla con símbolos indefinidos.

Mientras no existan los secretos de Apple el trabajo se salta solo y deja un aviso, en vez de salir
rojo en cada etiqueta.

La versión que se ve en la App Store es `MARKETING_VERSION` en `iosApp/iosApp.xcodeproj` (a la par
que `versionName` de Android); el número de build lo pone el workflow desde `github.run_number`. Las
dos van en el proyecto y no en cada target: la app y su extensión (`FlowTimeWidgets`, la Live
Activity) tienen que llevar las mismas, y App Store Connect lo comprueba al subir.

### Antes de la primera subida (usuario)

1. Cuenta de Apple Developer y su Team ID.
2. La app en App Store Connect con el identificador `com.baltajmn.flowtime`, que es para siempre.
   El de la extensión, `com.baltajmn.flowtime.widgets`, lo registra Xcode al firmar
   (`-allowProvisioningUpdates`).
3. Los productos de compras integradas. En la App Store los ids son únicos en toda la cuenta (los
   de MoodTraker cuentan), así que van con el de la app delante y lo de Play detrás:
   `com.baltajmn.flowtime.pro_lifetime`, `.tip_small`, `.tip_medium` y `.tip_large`; la app se queda
   con lo que va tras el último punto. Después, la app de iOS en el proyecto de RevenueCat con los
   derechos `pro` y `supporter`, y su clave `appl_` en `REVENUECAT_IOS_API_KEY` (`core/data`,
   `iosMain`). Sin ella el iPhone no tiene tienda, y no se publica: Pro saldría gratis
   (`ProFeatures`).
4. Los secretos de abajo, y la clave de compras integradas para RevenueCat (`~/keys/LEEME.md`).

### Secretos de Apple

| Secreto | Qué es |
|---|---|
| `APPSTORE_KEY_ID` | El Key ID de la clave de la App Store Connect API |
| `APPSTORE_ISSUER_ID` | El Issuer ID, el mismo para todas las claves de la cuenta |
| `APPSTORE_PRIVATE_KEY` | El contenido del `.p8`, entero, con sus líneas `BEGIN`/`END` |
| `APPLE_TEAM_ID` | El Team ID de la cuenta de desarrollador |

La clave `.p8` se descarga **una sola vez**. Necesita rol *App Manager* o superior para que
`-allowProvisioningUpdates` pueda crear el certificado y el perfil por su cuenta: es lo que evita
meter un `.p12` en un secreto.
