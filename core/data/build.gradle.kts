plugins {
    id("flowtime.android.library")
    alias(libs.plugins.flowtime.kotlin.plugin.compose)
    id("flowtime.compose.library")
}

android {
    namespace = Config.Core.Data
}

dependencies {
    implementation(projects.core.database)
    implementation(projects.core.design)
    implementation(projects.core.persistence)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.revenuecat)

    testImplementation(libs.mock)
}
