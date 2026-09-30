package com.baltajmn.flowtime.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.SystemClock
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.baltajmn.flowtime.FlowTimeActivity
import com.baltajmn.flowtime.R
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.common.extensions.formatSecondsToTime
import com.baltajmn.flowtime.core.design.theme.Appearance
import com.baltajmn.flowtime.core.design.theme.AppearanceRepository
import com.baltajmn.flowtime.core.design.theme.DarkMode
import com.baltajmn.flowtime.data.goal.DayProgress
import com.baltajmn.flowtime.data.goal.GoalRepository
import com.baltajmn.flowtime.data.goal.Streak
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.FocusSnapshot
import com.baltajmn.flowtime.data.timer.TimerAction
import com.baltajmn.flowtime.session.SessionReceiver
import com.baltajmn.flowtime.session.label
import com.baltajmn.flowtime.session.title
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import com.baltajmn.flowtime.core.design.R as DesignR

// Con SizeMode.Responsive, LocalSize es el mayor de estos que cabe: 2x1, 4x1 y 4x2.
private val SMALL = DpSize(110.dp, 40.dp)
private val WIDE = DpSize(250.dp, 40.dp)
private val MEDIUM = DpSize(250.dp, 110.dp)
private val NARROW_WIDTH = 180.dp

/**
 * El widget de la pantalla de inicio (#41): empezar, pausar y parar sin abrir la app, con el tiempo,
 * el objetivo de hoy y la racha. Los botones van a [SessionReceiver], como los de la notificación, y
 * el reloj lo anima el sistema: no hay que actualizarlo cada segundo.
 */
class FocusWidget :
    GlanceAppWidget(),
    KoinComponent {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE, MEDIUM))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val engine = get<FocusEngine>()
        val goals = get<GoalRepository>()
        val appearance = get<AppearanceRepository>()
        provideContent {
            val state by engine.state.collectAsState()
            val today by goals.today.collectAsState(null)
            val streak by goals.streak.collectAsState(null)
            val look by appearance.appearance.collectAsState()
            val (light, dark) = remember(look) { schemes(context, look) }
            GlanceTheme(colors = ColorProviders(light = light, dark = dark)) {
                Content(
                    context = context,
                    snapshot = engine.snapshot(state),
                    idleMillis = engine.workMillis(state.mode),
                    today = today,
                    streak = streak,
                    timeColors = light.onSurface to dark.onSurface
                )
            }
        }
    }

    companion object {
        /** Tras cada cambio de la sesión o del progreso de hoy. Sin widgets puestos, no hace nada. */
        suspend fun update(context: Context) = FocusWidget().updateAll(context)
    }
}

class FocusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FocusWidget()
}

/** Los colores de la app, con su modo claro u oscuro forzado si lo tiene, o los del fondo de pantalla. */
private fun schemes(context: Context, look: Appearance): Pair<ColorScheme, ColorScheme> {
    val (light, dark) = if (look.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicLightColorScheme(context) to dynamicDarkColorScheme(context)
    } else {
        look.theme.colorScheme(dark = false) to look.theme.colorScheme(dark = true)
    }
    return when (look.darkMode) {
        DarkMode.LIGHT -> light to light
        DarkMode.DARK -> dark to dark
        DarkMode.SYSTEM -> light to dark
    }
}

@Composable
private fun Content(
    context: Context,
    snapshot: FocusSnapshot,
    idleMillis: Long,
    today: DayProgress?,
    streak: Streak?,
    timeColors: Pair<Color, Color>
) {
    val state = snapshot.state
    val medium = LocalSize.current.height >= MEDIUM.height
    // A 2 columnas "03:00" no cabía y se partía en dos líneas: menos margen, botón y letra.
    val narrow = LocalSize.current.width < NARROW_WIDTH
    val colors = GlanceTheme.colors
    val primary = when {
        !state.isActive -> TimerAction.START
        state.isPaused -> TimerAction.RESUME
        else -> TimerAction.PAUSE
    }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(colors.widgetBackground)
            .cornerRadius(20.dp)
            .padding(horizontal = if (narrow) 10.dp else 16.dp, vertical = 8.dp)
            .clickable(actionStartActivity<FlowTimeActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (medium) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val status = listOfNotNull(
                    context.getString(state.mode.label),
                    state.takeIf { it.isActive }?.let { context.getString(it.title) }
                )
                Text(
                    text = status.joinToString(" · "),
                    style = TextStyle(
                        color = colors.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight()
                )
                val days = streak?.current ?: 0
                if (days > 0) {
                    Text(
                        text = context.resources.getQuantityString(
                            DesignR.plurals.streak_days,
                            days,
                            days
                        ),
                        style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp),
                        maxLines = 1
                    )
                }
            }
        }
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Time(
                context,
                snapshot,
                idleMillis,
                size = when {
                    medium -> 36f
                    narrow -> 20f
                    else -> 26f
                },
                timeColors,
                GlanceModifier.defaultWeight()
            )
            val button = if (narrow) 36.dp else 44.dp
            if (medium && state.isActive) {
                ActionButton(context, TimerAction.STOP, filled = false, button)
                Spacer(modifier = GlanceModifier.width(8.dp))
            }
            ActionButton(context, primary, filled = true, button)
        }
        if (medium && today != null) {
            Text(
                text = context.getString(
                    DesignR.string.goal_today,
                    (today.seconds / 60).formatMinutesStudying(),
                    today.goalMinutes.toLong().formatMinutesStudying()
                ),
                style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            LinearProgressIndicator(
                progress = today.fraction,
                modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                color = colors.primary,
                backgroundColor = colors.surfaceVariant
            )
        }
    }
}

/**
 * Con la sesión contando, un `Chronometer` que anima el sistema, con la misma hora que la
 * notificación. Parada o en pausa, el tiempo fijo; sin sesión, lo que dura el trabajo del modo.
 */
@Composable
private fun Time(
    context: Context,
    snapshot: FocusSnapshot,
    idleMillis: Long,
    size: Float,
    colors: Pair<Color, Color>,
    modifier: GlanceModifier
) {
    val state = snapshot.state
    if (!state.running) {
        val seconds = if (state.isActive) snapshot.displaySeconds else idleMillis / 1000
        Text(
            text = seconds.formatSecondsToTime(),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = size.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            modifier = modifier
        )
        return
    }
    val offset = if (state.countsDown) snapshot.remainingMillis else -snapshot.elapsedMillis
    val (day, night) = colors
    val views = RemoteViews(context.packageName, R.layout.widget_time).apply {
        setChronometer(R.id.time, SystemClock.elapsedRealtime() + offset, null, true)
        setChronometerCountDown(R.id.time, state.countsDown)
        setTextViewTextSize(R.id.time, TypedValue.COMPLEX_UNIT_SP, size)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setColorInt(R.id.time, "setTextColor", day.toArgb(), night.toArgb())
        } else {
            val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            setTextColor(
                R.id.time,
                (if (nightMode == Configuration.UI_MODE_NIGHT_YES) night else day).toArgb()
            )
        }
    }
    AndroidRemoteViews(remoteViews = views, modifier = modifier)
}

@Composable
private fun ActionButton(context: Context, action: TimerAction, filled: Boolean, diameter: Dp) {
    val colors = GlanceTheme.colors
    val icon = when (action) {
        TimerAction.PAUSE -> DesignR.drawable.ic_pause
        TimerAction.STOP -> DesignR.drawable.ic_stop
        else -> DesignR.drawable.ic_play
    }
    Box(
        modifier = GlanceModifier
            .size(diameter)
            .cornerRadius(diameter / 2)
            .background(if (filled) colors.primary else colors.secondaryContainer)
            .clickable(
                actionSendBroadcast(
                    Intent(context, SessionReceiver::class.java).setAction(action.name)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = context.getString(action.label),
            colorFilter = ColorFilter.tint(
                if (filled) colors.onPrimary else colors.onSecondaryContainer
            ),
            modifier = GlanceModifier.size(diameter / 2)
        )
    }
}
