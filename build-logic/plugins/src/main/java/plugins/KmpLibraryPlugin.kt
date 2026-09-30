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
                withHostTest { }
            }
            iosArm64()
            iosSimulatorArm64()

            sourceSets.commonTest.configure {
                dependencies { implementation(kotlin("test")) }
            }
        }

        configureKtlint()
    }
}
