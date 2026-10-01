package plugins

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import extensions.configureKtlint
import extensions.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Un módulo multiplataforma (#61): Android con el plugin de AGP 9 para bibliotecas KMP, y los dos
 * destinos de iPhone (el móvil y el simulador de un Mac con Apple Silicon). Cada módulo pone su
 * `namespace` en `kotlin { android { } }`.
 *
 * En Linux (el CI) las tareas de iOS se saltan solas: allí solo se compila y se prueba Android.
 */
class KmpLibraryPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")

        extensions.configure<KotlinMultiplatformExtension> {
            targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                compileSdk = libs.versions.compileSdk.get().toInt()
                minSdk = libs.versions.minSdk.get().toInt()
                compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
                // Solo se puede crear una vez: con los recursos de Android para todos, que las capturas de
                // features/screens (Robolectric) los necesitan para pintar las pantallas.
                withHostTest { isIncludeAndroidResources = true }
            }
            iosArm64()
            iosSimulatorArm64()

            // Room pide un expect object (AppDatabaseConstructor) y escribe sus actual.
            compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
            // Los mismos que en los módulos solo de Android (configureKotlin).
            compilerOptions.optIn.addAll("kotlinx.coroutines.ExperimentalCoroutinesApi", "kotlinx.coroutines.FlowPreview")

            sourceSets.commonTest.configure {
                dependencies { implementation(kotlin("test")) }
            }
        }

        configureKtlint()
    }
}
