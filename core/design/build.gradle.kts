plugins {
    id("flowtime.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("flowtime.compose.library")
}

android {
    namespace = Config.Core.Design
}

dependencies {
    implementation(projects.core.persistence)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.material.kolor)

    // Testing dependencies
    testImplementation(libs.junit)
    testImplementation(libs.mock)
    testImplementation(libs.coroutines.test)
    testImplementation(kotlin("test"))
}