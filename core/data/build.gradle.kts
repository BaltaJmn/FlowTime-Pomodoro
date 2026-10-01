plugins {
    id("flowtime.kmp.library")
    id("kotlinx-serialization")
}

kotlin {
    android {
        namespace = Config.Core.Data
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.database)
            implementation(projects.core.design)
            implementation(projects.core.persistence)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.revenuecat)
        }
        commonTest.dependencies {
            implementation(libs.coroutines.test)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.mock)
        }
    }
}
