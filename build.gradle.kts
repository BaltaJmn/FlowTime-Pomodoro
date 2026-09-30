// Una sola copia del plugin de Kotlin para todos los modulos: con un modulo multiplataforma (#61) y
// otros que piden el plugin de Compose con su version, cada uno cargaba la suya y Gradle no puede
// compartir entre ellas el servicio de Kotlin/Native.
plugins {
    id("flowtime.kmp.library") apply false
    alias(libs.plugins.flowtime.kotlin.plugin.compose) apply false
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