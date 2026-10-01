plugins {
    id("flowtime.kmp.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("kotlinx-serialization")
}

kotlin {
    android {
        namespace = Config.Core.Design
        androidResources { enable = true }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.persistence)
            api(libs.cmp.runtime)
            api(libs.cmp.foundation)
            api(libs.cmp.ui)
            api(libs.cmp.material3)
            api(libs.cmp.components.resources)
            implementation(libs.cmp.ui.tooling.preview)
            implementation(libs.cmp.material.icons.core)
            implementation(libs.cmp.lifecycle.runtime.compose)
            implementation(libs.material.kolor)
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.core.ktx)
            implementation(libs.koin.android)
            implementation(libs.compose.lottie)
        }
        iosMain.dependencies {
            implementation(libs.compottie)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.mock)
        }
        commonTest.dependencies {
            implementation(libs.coroutines.test)
        }
    }
}

compose.resources {
    packageOfResClass = "com.baltajmn.flowtime.core.design.resources"
    publicResClass = true
}

/**
 * Los textos, iconos y fuentes de composeResources son también recursos de Android: las
 * notificaciones, el widget y el resto del código de Android sin Compose los siguen leyendo de R.
 * Van copiados porque cada uno escapa distinto: Compose lee los apóstrofos y comillas tal cual, y
 * Android los quiere con \ delante (y no hay forma de escribirlos que valga para los dos).
 */
abstract class AndroidResources : DefaultTask() {
    @get:InputDirectory
    abstract val source: DirectoryProperty

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @TaskAction
    fun copy() {
        val from = source.get().asFile
        val into = output.get().asFile
        into.deleteRecursively()
        from.walkTopDown().filter { it.isFile }.forEach { file ->
            val target = into.resolve(file.relativeTo(from))
            target.parentFile.mkdirs()
            if (file.parentFile.name.startsWith("values")) {
                // Solo el texto entre etiquetas: los atributos llevan comillas de verdad.
                val text = Regex(">([^<]*)<").replace(file.readText()) {
                    ">" + it.groupValues[1].replace("'", "\\'").replace("\"", "\\\"") + "<"
                }
                target.writeText(text)
            } else {
                file.copyTo(target)
            }
        }
    }
}

val androidResources = tasks.register<AndroidResources>("androidResources") {
    source = layout.projectDirectory.dir("src/commonMain/composeResources")
    output = layout.buildDirectory.dir("generated/androidResources")
}

androidComponents {
    onVariants { variant ->
        variant.sources.res?.addGeneratedSourceDirectory(androidResources, AndroidResources::output)
    }
}
