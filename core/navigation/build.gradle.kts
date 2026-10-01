plugins {
    id("flowtime.kmp.library")
}

kotlin {
    android {
        namespace = Config.Core.Navigation
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.cmp.navigation.compose)
        }
    }
}
