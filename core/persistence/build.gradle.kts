plugins {
    alias(libs.plugins.flowtime.android.library)
}

android {
    namespace = Config.Core.Persistence
}

dependencies {
    implementation(libs.gson)
}