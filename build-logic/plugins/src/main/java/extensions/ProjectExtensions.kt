package extensions

import com.android.build.api.dsl.CommonExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

val Project.libs get() = the<LibrariesForLibs>()

internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension,
) {
    commonExtension.compileOptions.apply {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    val warningsAsErrors: String? by project

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            allWarningsAsErrors.set(warningsAsErrors.toBoolean())
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.coroutines.FlowPreview",
            )
        }
    }
}

fun Project.configureKtlint() {
    plugins.apply("org.jlleitschuh.gradle.ktlint")

    configure<KtlintExtension> {
        android.set(true)
        ignoreFailures.set(false)
        disabledRules.set(
            setOf(
                "final-newline",
                "no-wildcard-imports",
                "max-line-length",
                "import-ordering"
            )
        )
        reporters {
            reporter(ReporterType.PLAIN)
            reporter(ReporterType.CHECKSTYLE)
            reporter(ReporterType.SARIF)
        }

    }

}