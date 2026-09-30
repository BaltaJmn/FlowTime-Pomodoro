plugins {
    id("flowtime.kmp.library")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

kotlin {
    android {
        namespace = Config.Core.Database
        // Sin esto, los tests de dispositivo no llevan assets: ni los esquemas de abajo.
        androidResources { enable = true }
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.room.runtime)
            // SessionDao y SessionDb.legacy trabajan con días y zonas horarias.
            api(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.koin.android)
            // Para leer las tareas antiguas en la migración de la 3 a la 4.
            implementation(libs.kotlinx.serialization.json)
        }
        iosMain.dependencies {
            implementation(libs.sqlite.bundled)
        }
        iosTest.dependencies {
            implementation(libs.coroutines.test)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.bundles.instrumentationTest)
            implementation(libs.room.testing)
            implementation(libs.coroutines.test)
        }
    }
}

// Los esquemas exportados sirven a MigrationTestHelper para crear bases de datos de versiones antiguas.
room {
    schemaDirectory("$projectDir/schemas")
}
androidComponents {
    onVariants { variant ->
        variant.deviceTests.values.forEach { it.sources.assets?.addStaticSourceDirectory("$projectDir/schemas") }
    }
}

dependencies {
    listOf("kspAndroid", "kspIosArm64", "kspIosSimulatorArm64").forEach { add(it, libs.room.compiler) }
}
