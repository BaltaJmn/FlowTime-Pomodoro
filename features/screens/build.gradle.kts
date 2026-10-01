plugins {
    id("flowtime.kmp.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    alias(libs.plugins.roborazzi)
}

kotlin {
    android {
        namespace = Config.Feature.Screens
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.design)
            implementation(projects.core.navigation)
            implementation(projects.core.persistence)
            implementation(projects.core.data)
            implementation(libs.cmp.material.icons.core)
            implementation(libs.cmp.ui.tooling.preview)
            implementation(libs.cmp.lifecycle.runtime.compose)
            implementation(libs.cmp.lifecycle.viewmodel.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.asProvider())
            implementation(libs.koin.compose.viewmodel)
        }
        androidMain.dependencies {
            implementation(libs.androidx.compose.activity)
            implementation(libs.koin.android)
        }
        commonTest.dependencies {
            implementation(libs.coroutines.test)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.mock)

            // Capturas de pantalla sin emulador: ./gradlew :features:screens:recordRoborazziAndroidHostTest
            implementation(libs.robolectric)
            implementation(libs.roborazzi)
            implementation(libs.roborazzi.compose)
            implementation(libs.roborazzi.junit.rule)
            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation(libs.androidx.compose.ui.test)
            implementation(libs.androidx.compose.ui.testManifest)
        }
    }
}

// Las capturas solo con las tareas de Roborazzi: en ./gradlew build ni siquiera se arranca Robolectric,
// que descargaría Android entero en cada ejecución de CI.
tasks.withType<Test>().configureEach {
    if (gradle.startParameter.taskNames.none { it.contains("Roborazzi") }) {
        exclude("**/screenshots/**")
    }
}
