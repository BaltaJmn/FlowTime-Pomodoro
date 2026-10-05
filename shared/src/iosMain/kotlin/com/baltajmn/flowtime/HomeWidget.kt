package com.baltajmn.flowtime

import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.common.extensions.formatSecondsToTime
import com.baltajmn.flowtime.core.design.resources.Res
import com.baltajmn.flowtime.core.design.resources.goal_today
import com.baltajmn.flowtime.core.design.resources.streak_days
import com.baltajmn.flowtime.core.design.theme.Appearance
import com.baltajmn.flowtime.core.design.theme.AppearanceRepository
import com.baltajmn.flowtime.core.design.theme.DarkMode
import com.baltajmn.flowtime.data.goal.DayProgress
import com.baltajmn.flowtime.data.goal.Streak
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.FocusState
import com.baltajmn.flowtime.data.timer.TimerAction
import com.baltajmn.flowtime.features.screens.common.composable.components.label
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.time.Clock
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

/** El widget de la pantalla de inicio, que también es de Swift (FocusWidget.swift). */
interface HomeWidget {
    /**
     * [status]: el modo, y con sesión cómo va. [date] y [time], como en LiveActivity.show; sin sesión,
     * [time] es lo que dura el trabajo del modo. [primary]: START, RESUME o PAUSE, y [stopLabel], con
     * sesión. [goal]: "1 h 25 min de 2 h", con [goalFraction]. [light] y [dark]: los colores del tema
     * de la app con el iPhone en claro y en oscuro (iguales si la app fuerza uno).
     */
    fun update(
        status: String,
        streak: String?,
        countsDown: Boolean,
        date: Double?,
        time: String,
        primary: String,
        primaryLabel: String,
        stopLabel: String?,
        goal: String?,
        goalFraction: Float,
        light: WidgetColors,
        dark: WidgetColors
    )
}

/** Los colores del widget, en ARGB: los mismos papeles que GlanceTheme en el widget de Android. */
class WidgetColors(
    val background: Int,
    /** El reloj. */
    val text: Int,
    /** El modo, la racha y el objetivo. */
    val secondaryText: Int,
    /** Empezar, pausar o seguir, y la barra del objetivo. */
    val primary: Int,
    val onPrimary: Int,
    /** Parar. */
    val secondary: Int,
    val onSecondary: Int,
    /** El fondo de la barra del objetivo. */
    val track: Int
)

/**
 * Lo mismo que el widget de Android: el modo y cómo va, la racha, el reloj, empezar, pausar o seguir
 * y parar, y el objetivo de hoy, con los colores del tema de la app. Se actualiza con cada cambio de la
 * sesión, del progreso de hoy y del aspecto.
 */
internal class HomeWidgetUpdater(
    private val engine: FocusEngine,
    private val appearance: AppearanceRepository,
    private val widget: HomeWidget
) {
    suspend fun update(state: FocusState, today: DayProgress?, streak: Streak?) {
        val status = listOfNotNull(
            getString(state.mode.label),
            state.takeIf { it.isActive }?.let { getString(it.title) }
        ).joinToString(" · ")
        val days = streak?.current ?: 0
        val streakText = if (days > 0) getPluralString(Res.plurals.streak_days, days, days) else null
        val primary = when {
            !state.isActive -> TimerAction.START
            state.isPaused -> TimerAction.RESUME
            else -> TimerAction.PAUSE
        }
        val primaryLabel = getString(primary.label)
        val stopLabel = if (state.isActive) getString(TimerAction.STOP.label) else null
        val goal = today?.let {
            getString(
                Res.string.goal_today,
                (it.seconds / 60).formatMinutesStudying(),
                it.goalMinutes.toLong().formatMinutesStudying()
            )
        }
        // Sin suspender desde aquí: el reloj, con la hora de ahora.
        val snapshot = engine.snapshot(state)
        val now = Clock.System.now().toEpochMilliseconds()
        val date = when {
            !state.running -> null
            state.countsDown -> now + snapshot.remainingMillis
            else -> now - snapshot.elapsedMillis
        }
        val seconds = if (state.isActive) snapshot.displaySeconds else engine.workMillis(state.mode) / 1000
        val (light, dark) = appearance.appearance.value.widgetColors()
        widget.update(
            status = status,
            streak = streakText,
            countsDown = state.countsDown,
            date = date?.let { it / 1000.0 },
            time = seconds.formatSecondsToTime(),
            primary = primary.name,
            primaryLabel = primaryLabel,
            stopLabel = stopLabel,
            goal = goal,
            goalFraction = today?.fraction ?: 0f,
            light = light,
            dark = dark
        )
    }
}

/** Claro y oscuro, o el mismo dos veces si la app fuerza uno, como schemes() en el widget de Android. */
private fun Appearance.widgetColors(): Pair<WidgetColors, WidgetColors> {
    val light = theme.colorScheme(dark = false).widgetColors()
    val dark = theme.colorScheme(dark = true).widgetColors()
    return when (darkMode) {
        DarkMode.LIGHT -> light to light
        DarkMode.DARK -> dark to dark
        DarkMode.SYSTEM -> light to dark
    }
}

private fun ColorScheme.widgetColors() = WidgetColors(
    background = widgetBackground(secondaryContainer).toArgb(),
    text = onSurface.toArgb(),
    secondaryText = onSurfaceVariant.toArgb(),
    primary = primary.toArgb(),
    onPrimary = onPrimary.toArgb(),
    secondary = secondaryContainer.toArgb(),
    onSecondary = onSecondaryContainer.toArgb(),
    track = surfaceVariant.toArgb()
)

/**
 * El fondo que da Glance a un widget con un ColorScheme: secondaryContainer, 5 de tono más claro si ya
 * es claro y 10 más oscuro si no. Glance mueve el tono de HCT; aquí, la L* de CIELAB, que es el mismo
 * tono, dejando a y b como están.
 */
private fun widgetBackground(color: Color): Color {
    val (l, a, b) = lab(color)
    val tone = (l + if (l > 50) 5f else -10f).coerceIn(0f, 100f)
    return fromLab(tone, a, b, color.alpha)
}

// sRGB y CIELAB con el blanco D65.
private const val XN = 0.95047f
private const val ZN = 1.08883f

private fun linear(c: Float) = if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)

private fun gamma(c: Float) = (if (c <= 0.0031308f) c * 12.92f else 1.055f * c.pow(1 / 2.4f) - 0.055f).coerceIn(0f, 1f)

private fun f(t: Float) = if (t > 216f / 24389f) cbrt(t) else (24389f / 27f * t + 16f) / 116f

private fun fInverse(t: Float) = if (t * t * t > 216f / 24389f) t * t * t else (116f * t - 16f) / (24389f / 27f)

private fun lab(color: Color): Triple<Float, Float, Float> {
    val r = linear(color.red)
    val g = linear(color.green)
    val b = linear(color.blue)
    val x = (0.4124f * r + 0.3576f * g + 0.1805f * b) / XN
    val y = 0.2126f * r + 0.7152f * g + 0.0722f * b
    val z = (0.0193f * r + 0.1192f * g + 0.9505f * b) / ZN
    return Triple(116f * f(y) - 16f, 500f * (f(x) - f(y)), 200f * (f(y) - f(z)))
}

private fun fromLab(l: Float, a: Float, b: Float, alpha: Float): Color {
    val fy = (l + 16f) / 116f
    val x = fInverse(fy + a / 500f) * XN
    val y = fInverse(fy)
    val z = fInverse(fy - b / 200f) * ZN
    return Color(
        red = gamma(3.2406f * x - 1.5372f * y - 0.4986f * z),
        green = gamma(-0.9689f * x + 1.8758f * y + 0.0415f * z),
        blue = gamma(0.0557f * x - 0.2040f * y + 1.0570f * z),
        alpha = alpha
    )
}
