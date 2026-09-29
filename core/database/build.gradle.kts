plugins {
    alias(libs.plugins.flowtime.android.library)
    alias(libs.plugins.flowtime.android.room.library)
}

android {
    namespace = Config.Core.Database

    // Los esquemas exportados sirven a MigrationTestHelper para crear bases de datos de versiones antiguas.
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    androidTestImplementation(libs.bundles.instrumentationTest)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.coroutines.test)
}
