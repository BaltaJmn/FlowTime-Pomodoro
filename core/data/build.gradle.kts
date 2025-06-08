plugins {
    alias(libs.plugins.flowtime.android.library)
    alias(libs.plugins.flowtime.kotlin.plugin.compose)
    alias(libs.plugins.flowtime.android.compose.library)
}

android {
    namespace = Config.Core.Data
}

dependencies {
    implementation(projects.core.database)
    implementation(projects.core.design)
    implementation(libs.androidx.workmanager)
}