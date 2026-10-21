package com.baltajmn.flowtime.features.screens.recap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.design.components.FlowCard
import com.baltajmn.flowtime.core.design.resources.Res
import com.baltajmn.flowtime.core.design.resources.recap_best_streak
import com.baltajmn.flowtime.core.design.resources.recap_best_weekday
import com.baltajmn.flowtime.core.design.resources.recap_days
import com.baltajmn.flowtime.core.design.resources.recap_see
import com.baltajmn.flowtime.core.design.resources.recap_text
import com.baltajmn.flowtime.core.design.resources.recap_title
import com.baltajmn.flowtime.core.design.resources.share_focus
import com.baltajmn.flowtime.core.design.resources.stats_best_day
import com.baltajmn.flowtime.core.design.resources.stats_sessions
import com.baltajmn.flowtime.core.design.resources.streak_days
import com.baltajmn.flowtime.core.design.theme.SmallTitle
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.data.goal.GoalRepository
import com.baltajmn.flowtime.data.stats.PeriodKind
import com.baltajmn.flowtime.data.stats.StatsPeriod
import com.baltajmn.flowtime.data.stats.StatsRepository
import com.baltajmn.flowtime.data.stats.YearRecap
import com.baltajmn.flowtime.data.stats.recapYear
import com.baltajmn.flowtime.data.stats.yearRecap
import com.baltajmn.flowtime.features.screens.platform.Formats
import com.baltajmn.flowtime.features.screens.share.BigStat
import com.baltajmn.flowtime.features.screens.share.ShareCardFrame
import com.baltajmn.flowtime.features.screens.share.ShareCardSheet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import com.baltajmn.flowtime.data.goal.today as systemToday
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * "Tu 2026 en FlowTime" (#85): del 1 de diciembre al 15 de enero, con los datos de este móvil. Fuera
 * de esas fechas, o sin foco en el año, [recap] es null y no se enseña nada.
 */
class RecapViewModel(
    private val stats: StatsRepository,
    private val goals: GoalRepository,
    private val today: () -> LocalDate = { systemToday() }
) : ViewModel() {

    private val _recap = MutableStateFlow<YearRecap?>(null)
    val recap: StateFlow<YearRecap?> = _recap.asStateFlow()

    init {
        load()
    }

    /** Al abrirlo también: puede haber sesiones nuevas desde que se creó. */
    fun load() {
        viewModelScope.launch {
            val now = today()
            val year = recapYear(now)
            _recap.value = year?.let {
                yearRecap(it, stats.summary(StatsPeriod(PeriodKind.YEAR, now.year - it), now), goals.history.value)
            }
        }
    }
}

/** La entrada en Estadísticas. */
@Composable
fun RecapCard(recap: YearRecap, onOpen: () -> Unit) {
    FlowCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 4.dp)
        ) {
            Text(
                text = stringResource(Res.string.recap_title, recap.year),
                style = SmallTitle.copy(color = MaterialTheme.colorScheme.onSurface)
            )
            Text(
                text = stringResource(Res.string.recap_text),
                modifier = Modifier.padding(top = 4.dp, end = 8.dp),
                style = SubBody.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            TextButton(onClick = onOpen, modifier = Modifier.align(Alignment.End)) {
                Text(text = stringResource(Res.string.recap_see))
            }
        }
    }
}

/** Las tarjetas del año, para pasarlas de lado y compartir la que se vea. */
@Composable
fun RecapSheet(recap: YearRecap, onDismiss: () -> Unit) {
    ShareCardSheet(
        onDismiss = onDismiss,
        cards = listOf({ YearTotalsCard(recap) }, { YearRecordsCard(recap) })
    )
}

/** Horas de foco, sesiones y días con foco. */
@Composable
internal fun YearTotalsCard(recap: YearRecap) {
    ShareCardFrame {
        CardTitle(recap.year)
        BigStat(value = focusText(recap.totalSeconds), label = stringResource(Res.string.share_focus), size = 56)
        Spacer(modifier = Modifier.size(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            BigStat(value = recap.sessions.toString(), label = stringResource(Res.string.stats_sessions))
            BigStat(value = recap.activeDays.toString(), label = stringResource(Res.string.recap_days))
        }
    }
}

/** La mejor racha, el día de la semana más productivo y el mejor día. */
@Composable
internal fun YearRecordsCard(recap: YearRecap) {
    ShareCardFrame {
        CardTitle(recap.year)
        if (recap.bestStreak > 0) {
            BigStat(
                value = pluralStringResource(Res.plurals.streak_days, recap.bestStreak, recap.bestStreak),
                label = stringResource(Res.string.recap_best_streak)
            )
            Spacer(modifier = Modifier.size(20.dp))
        }
        BigStat(
            value = Formats.weekday(recap.bestWeekday).replaceFirstChar { it.uppercase() },
            label = stringResource(Res.string.recap_best_weekday)
        )
        Spacer(modifier = Modifier.size(20.dp))
        BigStat(
            value = (recap.bestDaySeconds / 60).formatMinutesStudying(),
            label = "${stringResource(Res.string.stats_best_day)}: ${Formats.mediumDate(recap.bestDay)}"
        )
    }
}

@Composable
private fun CardTitle(year: Int) {
    Text(
        text = stringResource(Res.string.recap_title, year),
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
        fontSize = 16.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

/** "142 h"; con menos de una hora, en minutos. */
private fun focusText(seconds: Long): String =
    if (seconds >= 3600) "${seconds / 3600} h" else (seconds / 60).formatMinutesStudying()
