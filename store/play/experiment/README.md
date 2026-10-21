# Experimento de icono (#80)

Hipotesis: un icono verde (el tema `mint` de Pro) convierte mas visitas de la ficha en instalaciones que
el azul de siempre, en una categoria de temporizadores con mucho rojo y azul. Una sola variable.

- Play: experimento de graficos predeterminados, icono. Variante: `icon-mint-512.png` (de
  `iosApp/.../AppIcon-mint.appiconset`, a 512). Control: `store/play/icon-512.png`.
- App Store: Product Page Optimization con el icono alternativo `AppIcon-mint`, que ya va en el binario
  de la 2.4.0 (`ASSETCATALOG_COMPILER_INCLUDE_ALL_APPICON_ASSETS`). Solo se puede crear con la app a la venta.
- Lectura: la que dan las consolas (Play avisa de los instalaciones que hacen falta; Apple, la confianza
  del 90 %). Con 2 visitas al dia en Play (59 en 28 dias) no concluye hasta que lleguen los picos del plan.
- Se aplica el ganador y no se abre otro experimento en esa tienda hasta cerrar este.
