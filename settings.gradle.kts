rootProject.name = "FlowTime"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

//app
include(
    ":app",
    // La UI de la app, comun a Android y al iPhone: iosApp la enlaza como Shared.framework.
    ":shared",
)

//Core
include(
    ":core:common",
    ":core:design",
    ":core:navigation",
    ":core:persistence",
    ":core:data",
    ":core:database"
)

//Features
include(
    ":features:screens"
)
