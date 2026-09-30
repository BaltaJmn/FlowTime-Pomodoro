import java.util.Properties

plugins {
    id("flowtime.android.application")
}

/**
 * Credenciales de la clave de subida. El fichero esta en .gitignore y no sale de tu maquina; en CI
 * lo escribe el workflow compartido a partir de los secretos del repositorio.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            // Sin keystore.properties cae a la clave de debug: se puede instalar y probar en local,
            // pero Play la rechaza, asi que una build hecha asi no llega a la tienda por error.
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.persistence)
    implementation(projects.core.data)
    implementation(projects.core.database)
    implementation(projects.core.navigation)
    implementation(projects.core.design)

    implementation(projects.features.screens)

    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.suite)
    implementation(libs.review.ktx)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.androidx.foundation.layout.android)
}