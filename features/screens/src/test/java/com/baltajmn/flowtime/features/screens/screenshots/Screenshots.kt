package com.baltajmn.flowtime.features.screens.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.design.theme.FlowTimeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Las capturas solo se hacen con las tareas de Roborazzi (record, compare o verify): con
 * `./gradlew build` se saltan, para no alargar cada compilación ni descargar Robolectric en CI.
 * Tiene que ir antes que la regla de Compose (`order = 0`), que abre una actividad al empezar.
 */
class RoborazziOnly : TestRule {
    override fun apply(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            assumeTrue(
                listOf("record", "compare", "verify").any { System.getProperty("roborazzi.test.$it") == "true" }
            )
            base.evaluate()
        }
    }
}

/** Pinta [content] con el tema y el modo pedidos, deja pasar las animaciones y guarda la captura. */
fun ComposeContentTestRule.capture(
    name: String,
    theme: AppTheme = AppTheme.Blue,
    dark: Boolean = false,
    content: @Composable () -> Unit
) {
    setContent {
        FlowTimeTheme(theme = theme, dark = dark) {
            Surface(color = MaterialTheme.colorScheme.background) {
                Box(modifier = Modifier.fillMaxSize()) { content() }
            }
        }
    }
    mainClock.advanceTimeBy(2_000)
    onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
}
