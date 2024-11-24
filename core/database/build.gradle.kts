plugins {
    alias(libs.plugins.flowtime.android.library)
    alias(libs.plugins.flowtime.android.room.library)
}

android {
    namespace = Config.Core.Database
}
