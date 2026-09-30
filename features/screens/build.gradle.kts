plugins {
    id("flowtime.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("flowtime.compose.library")
    alias(libs.plugins.roborazzi)
}

android {
    namespace = Config.Feature.Screens

    // Robolectric necesita los recursos de verdad para pintar las pantallas en las capturas.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(projects.core.persistence)
    implementation(projects.core.data)

    testImplementation(libs.mock)

    // Capturas de pantalla sin emulador: ./gradlew :features:screens:recordRoborazziDebug
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test)
    debugImplementation(libs.androidx.compose.ui.testManifest)
}

// Las capturas solo con las tareas de Roborazzi: en ./gradlew build ni siquiera se arranca Robolectric,
// que descargaría Android entero en cada ejecución de CI.
tasks.withType<Test>().configureEach {
    if (gradle.startParameter.taskNames.none { it.contains("Roborazzi") }) {
        exclude("**/screenshots/**")
    }
}
