plugins {
    alias(libs.plugins.flowtime.android.library)
}

android {
    namespace = Config.Core.Data
}

dependencies {
    implementation(projects.core.database)
    implementation(projects.core.design)
    implementation(libs.androidx.workmanager)
}