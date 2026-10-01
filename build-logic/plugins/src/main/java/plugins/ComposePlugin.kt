package plugins

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import extensions.androidTestImplementation
import extensions.androidTestImplementationBom
import extensions.debugImplementation
import extensions.implementation
import extensions.implementationBom
import extensions.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class ComposePlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        val extension = extensions.getByType<LibraryExtension>()
        configureCompose(extension)
    }
}

fun Project.configureCompose(commonExtension: CommonExtension) {
    commonExtension.buildFeatures.compose = true

    // Reglas de lint que Compose 1.8 no tenia y que en la 1.11 salen como error. Se dejan como aviso:
    // subir las herramientas no debe obligar a tocar la UI. Limpieza pendiente: stringResource y LocalLocale.
    commonExtension.lint.warning += setOf("LocalContextGetResourceValueCall", "NonObservableLocale")

    dependencies {
        implementation(project(":core:navigation"))
        if (project.name != "design") {
            implementation(project(":core:design"))
        }
        implementationBom(platform(libs.androidx.compose.bom))
        androidTestImplementationBom(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.compose.activity)
        implementation(libs.androidx.compose.runtime)
        implementation(libs.androidx.compose.foundation)
        implementation(libs.androidx.compose.material3)
        // Material3 1.4 dejo de traer los iconos como dependencia transitiva.
        implementation(libs.androidx.compose.material.icons.core)
        implementation(libs.androidx.lifecycle.viewmodel)
        implementation(libs.androidx.lifecycle.compose)
        implementation(libs.androidx.compose.ui.tooling.preview)
        implementation(libs.koin.compose.asProvider())
        implementation(libs.koin.compose.viewmodel)
        implementation(libs.compose.lottie)

        debugImplementation(libs.androidx.compose.ui.tooling.debug)
        androidTestImplementation(libs.androidx.compose.ui.test)
    }
}

