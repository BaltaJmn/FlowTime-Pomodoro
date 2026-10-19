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
            // Coop: las pantallas usan también compose-auth para iniciar sesión.
            api(project.dependencies.platform(libs.supabase.bom))
            api(libs.supabase.auth)
            api(libs.supabase.postgrest)
            api(libs.supabase.realtime)
            api(libs.supabase.functions)
            api(libs.supabase.composeAuth)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.revenuecat)
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.purchases.kmp.core)
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.coroutines.test)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.mock)
        }
    }
}
