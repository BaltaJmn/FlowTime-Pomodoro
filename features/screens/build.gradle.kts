plugins {
    alias(libs.plugins.flowtime.android.library)
    alias(libs.plugins.flowtime.android.compose.library)
}

android {
    namespace = Config.Feature.Screens
}