plugins {
    id("flowtime.kmp.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    android {
        namespace = Config.Shared
    }

    // La app del iPhone (iosApp) enlaza este módulo, con todos los demás dentro, como Shared.framework.
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.design)
            implementation(projects.core.navigation)
            implementation(projects.core.data)
            implementation(projects.features.screens)
            implementation(libs.cmp.material3.navigation.suite)
        }
        iosMain.dependencies {
            implementation(projects.core.database)
            implementation(projects.core.persistence)
            implementation(libs.cmp.lifecycle.runtime.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.asProvider())
        }
    }
}
