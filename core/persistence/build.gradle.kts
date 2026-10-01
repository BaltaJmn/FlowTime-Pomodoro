plugins {
    id("flowtime.kmp.library")
    id("kotlinx-serialization")
}

kotlin {
    android {
        namespace = Config.Core.Persistence
    }

    sourceSets {
        commonMain.dependencies {
            // getObject y setObject son inline: quien los llama compila contra kotlinx.serialization.
            api(libs.kotlinx.serialization.json)
            // DayKeys: las claves de cada día.
            api(libs.kotlinx.datetime)
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
        getByName("androidHostTest").dependencies {
            // Para comprobar que se lee lo que guardaba la app con Gson.
            implementation(libs.gson)
        }
    }
}
