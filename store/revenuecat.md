# RevenueCat y los productos de Play

Montado el 30-09-2026 por API (#55), con la clave `revenuecat-flowtime` del Llavero y la cuenta de
servicio de publicar para Play. Cómo se hace cada paso: `~/keys/LEEME.md`.

## Qué hay

| Play (`com.baltajmn.flowtime`) | Tipo en RevenueCat | Derecho | Paquete de la oferta `default` | Precio en España |
|---|---|---|---|---|
| `pro_lifetime` | No consumible | `pro` | `$rc_lifetime` | 2,99 EUR |
| `tip_small` | Consumible | `supporter` | `tip_small` | 1,99 EUR |
| `tip_medium` | Consumible | `supporter` | `tip_medium` | 4,99 EUR |
| `tip_large` | Consumible | `supporter` | `tip_large` | 9,99 EUR |
| `support_developer` (el de antes) | Fuera de RevenueCat | | | 1,19 EUR |

- **Los ids son para siempre.** Un id borrado no se puede volver a usar.
- **Precios:** base sin IVA (2,47 / 1,65 / 4,13 / 8,26 EUR) y conversión de Play con redondeo a los
  174 países. Una rebaja se hace con la del propio producto en Play, nunca con un id nuevo.
- **Cada producto tiene una opción de compra `base`,** compatible con las versiones antiguas de
  Billing (`legacyCompatible`).
- **Nombre y descripción en los 6 idiomas.** La descripción de Pro cuenta lo mismo que la pantalla
  de Pro y la ficha: si cambia lo que incluye Pro, se cambian las tres a la vez.
  Se cambian con `PATCH .../applications/com.baltajmn.flowtime/onetimeproducts/<id>?updateMask=listings&regionsVersion.version=2026%2F01`
  y el recurso entero de `GET .../oneTimeProducts/<id>`: el PATCH va en minúsculas, con
  `oneTimeProducts` da 404.
- **RevenueCat:** proyecto `Flowtime`, app `FlowTime (Play)` con el JSON de la cuenta de solo
  lectura (`revenuecat@`). La primera vez puede tardar hasta 36 horas en validarse.
- **Clave pública** (`goog_`): `REVENUECAT_API_KEY` en `core/data/.../pro/RevenueCatStore.kt`. A
  `null`, la app funciona entera en gratis y sin conectarse a nada.

## En el código

- `PurchasesRepository` es la única comprobación: `isPro` e `isSupporter`, guardados en
  preferencias para funcionar sin conexión desde el primer fotograma.
- Pro sigue a la tienda: un reembolso lo quita. La insignia de supporter no se quita nunca, porque
  con Billing 8 una propina ya consumida no se puede recuperar desde Google.
- El usuario de RevenueCat es anónimo. Sus preferencias (`com_revenuecat_purchases_preferences`)
  van en la copia automática de Android, para que tras reinstalar sea el mismo usuario y conserve las
  propinas. Pro se recupera siempre con "Restaurar compras".
- `ProFeatures.enabled` está encendido desde la 2.1.0. Apagado, no hay límites y no se ofrece Pro.
- El SDK trae el identificador de publicidad; el manifiesto quita el permiso `AD_ID`, que FlowTime
  no usa.

## Antes de subir la versión con RevenueCat

1. **Seguridad de los datos**, en Play Console: el historial de compras y un identificador de la
   app se recogen y se comparten con RevenueCat como proveedor de servicios, para gestionar las
   compras. No se usan para publicidad ni para analítica.
2. **Política de privacidad:** que mencione RevenueCat.

## Probar

1. *Ajustes > Monetización > Licencia para testing* (nivel de cuenta): el correo de la cuenta de
   Google de prueba. Compra con el diálogo real y sin cargo.
2. Instalar **desde el canal de prueba interna**: una compra no funciona instalando con `adb`.
3. Dejar una propina: sale el agradecimiento, la insignia en Ajustes y el tema Supporter.
4. Comprar Pro, reembolsarlo desde Play Console (Pro tiene que
   desaparecer), reinstalar y usar "Restaurar compras".
5. En RevenueCat, *Customer History*: el evento y el derecho activo.

## Cuando la versión nueva esté publicada

- Desactivar `support_developer` en Play Console. Se desactiva, no se borra.
