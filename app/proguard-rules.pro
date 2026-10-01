# Reglas de R8 para la release (#62). Las librerias traen las suyas (kotlinx.serialization, Room,
# RevenueCat, Koin...) y la app no usa reflexion, asi que aqui solo va lo que R8 pida de verdad.

# Anotacion de compilacion de Play Services que cita la extension de valoraciones de Play
# (review-ktx) y que no viaja en ninguna dependencia: en ejecucion no hace falta.
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite

# Glance 1.1.1 trae WorkManager 2.7.1, que crea su InputMerger por reflexion con el constructor
# vacio y no lo protege: R8 se lo quita y el widget se queda cargando. Es la regla que ya lleva
# WorkManager 2.10; sobra cuando Glance traiga esa version o una posterior.
-keepclassmembers class * extends androidx.work.InputMerger { public <init>(); }
