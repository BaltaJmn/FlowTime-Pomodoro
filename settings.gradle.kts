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
