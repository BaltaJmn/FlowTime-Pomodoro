// Una sola copia del plugin de Kotlin para todos los modulos: con un modulo multiplataforma (#61),
// cada modulo que lo cargaba por su lado tenia la suya y Gradle no puede compartir entre ellas el
// servicio de Kotlin/Native. Los plugins de Kotlin, Compose, KSP y Room llegan todos por build-logic.
plugins {
    id("flowtime.kmp.library") apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}