plugins {
    alias(libs.plugins.flowtime.android.application)
}

android {
    defaultConfig {
        manifestPlaceholders["redirectSchemeName"] = "flowtime"
        manifestPlaceholders["redirectHostName"] = "auth"
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.persistence)
    implementation(projects.core.navigation)
    implementation(projects.core.design)

    implementation(projects.features.screens)

    implementation(libs.androidx.workmanager)
}