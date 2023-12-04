import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.baltajmn.flowtime.buildlogic"

plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

gradlePlugin {
    plugins {
        register("flowtimeAndroidApplication") {
            id = "flowtime.android.application"
            implementationClass = "plugins.AndroidApplicationPlugin"
        }

        register("flowtimeAndroidLibrary") {
            id = "flowtime.android.library"
            implementationClass = "plugins.AndroidLibraryPlugin"
        }

        register("flowtimeComposeLibrary") {
            id = "flowtime.compose.library"
            implementationClass = "plugins.ComposePlugin"
        }

        register("flowtimeRoomLibrary") {
            id = "flowtime.room.library"
            implementationClass = "plugins.AndroidRoomPlugin"
        }
    }
}

repositories {
    google()
    mavenCentral()
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }
    gradlePluginPortal()

}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(libs.plugin.android)
    implementation(libs.plugin.gradle)
    implementation(libs.plugin.ktlint)
    implementation(libs.plugin.kotlin.serialization)
    implementation(libs.plugin.kotlin.ksp)
}