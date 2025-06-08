plugins {
    alias(libs.plugins.flowtime.android.library)
    alias(libs.plugins.flowtime.kotlin.plugin.compose)
    alias(libs.plugins.flowtime.android.compose.library)
}

android {
    namespace = Config.Feature.Screens
}

dependencies {
    implementation(libs.gson)
    implementation(projects.core.persistence)
    implementation(projects.core.data)
}