plugins {
    alias(libs.plugins.flowtime.android.library)
    alias(libs.plugins.flowtime.kotlin.plugin.compose)
    alias(libs.plugins.flowtime.android.compose.library)
}

android {
    namespace = Config.Core.Design
}

dependencies {
    implementation(projects.core.persistence)
}