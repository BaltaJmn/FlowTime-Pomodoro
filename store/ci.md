# Publicar desde GitHub Actions

Igual que las hermanas (Chroma, MoodTraker, Purl): la secuencia de Android vive en `BaltaJmn/ci`
(`android-play-release.yml`) y `.github/workflows/release.yml` solo la llama con el paquete, el CN
de la firma, la tarea de Gradle y la carpeta de notas. `tests.yml` corre `./gradlew build` en cada
push a `main` y en cada pull request.

## Cómo se dispara

**Por etiqueta**, no en cada push a `main`. FlowTime ya está en producción, así que la etiqueta
publica **en producción**, no en `alpha` como las hermanas.

1. Subir `versionCode` (y `versionName`) en `build-logic/plugins/src/main/java/Config.kt`. Tiene
   que ser mayor que el último de Play: `~/keys/play.sh estado com.baltajmn.flowtime`.
2. Reescribir `store/whatsnew/whatsnew-es-ES` (tope 500). La ficha solo tiene `es-ES`.
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
