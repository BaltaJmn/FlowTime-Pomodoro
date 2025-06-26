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
    implementation(libs.androidx.lifecycle.process)

    // Testing dependencies
    testImplementation(libs.junit)
    testImplementation(libs.mock)
    testImplementation(libs.coroutines.test)
    testImplementation(kotlin("test"))
}