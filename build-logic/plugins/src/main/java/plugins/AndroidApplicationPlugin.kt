package plugins

import Config
import com.android.build.api.dsl.ApplicationExtension
import extensions.configureKotlinAndroid
import extensions.configureKtlint
import extensions.implementation
import extensions.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidApplicationPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply {
            apply("com.android.application")
            apply("org.jetbrains.kotlin.plugin.compose")
        }

        extensions.configure<ApplicationExtension> {
            namespace = Config.baseApplicationId
            compileSdk = libs.versions.compileSdk.get().toInt()

            defaultConfig {
                targetSdk = libs.versions.targetSdk.get().toInt()
                minSdk = libs.versions.minSdk.get().toInt()
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                versionCode = Config.versionCode
                versionName = Config.versionName
            }

            buildFeatures {
                buildConfig = true
            }

            buildTypes {

                // R8 (#62): el mapping y los símbolos nativos viajan dentro del AAB, y Play los coge
                // de ahí para que los fallos de vitals se lean.
                release {
                    isShrinkResources = true
                    isMinifyEnabled = true
                    ndk.debugSymbolLevel = "SYMBOL_TABLE"
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro"
                    )
                }

                debug {
                    enableUnitTestCoverage = true
                }

            }

            testOptions.unitTests.all {
                it.useJUnitPlatform()
                it.testLogging {
                    events("passed", "failed", "skipped", "standardOut", "standardError")
                }
            }

            configureKotlinAndroid(this)
            configureKtlint()
            configureCompose(this)

            dependencies {
                implementation(libs.koin.core)
                implementation(libs.koin.android)
            }
        }
    }
}