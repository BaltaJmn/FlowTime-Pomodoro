plugins {
    alias(libs.plugins.flowtime.android.library)
    alias(libs.plugins.flowtime.android.compose.library)
}

android {
    namespace = Config.Feature.Screens
}

dependencies {
    implementation(projects.core.persistence)
    implementation(libs.gson)
}