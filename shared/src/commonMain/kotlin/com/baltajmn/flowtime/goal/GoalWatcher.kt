package com.baltajmn.flowtime.goal

import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.GOAL_CELEBRATED_ON
import com.baltajmn.flowtime.data.goal.Celebration
import com.baltajmn.flowtime.data.goal.GoalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Celebra el objetivo diario una sola vez al día, al cumplirlo: con la app a la vista ([visible]),
 * dentro de la app; si no, con una notificación. [notify] dice si ha podido avisar: si no, se ve al
 * volver a la app.
 */
class GoalWatcher(
    private val goals: GoalRepository,
    private val dataProvider: DataProvider,
    private val visible: () -> Boolean,
    private val notify: suspend (Celebration) -> Boolean
) {
    private val _celebration = MutableStateFlow<Celebration?>(null)

    /** La que la app tiene que enseñar, hasta que se cierre. */
    val celebration: StateFlow<Celebration?> = _celebration.asStateFlow()

    fun watch(scope: CoroutineScope) {
        scope.launch {
            goals.today.filter { it.met }.collect { progress ->
                val day = progress.day.toString()
                if (dataProvider.getString(GOAL_CELEBRATED_ON) == day) return@collect
                dataProvider.setString(GOAL_CELEBRATED_ON, day)
                val celebration = Celebration(progress.goalMinutes, goals.streak.first().current)
                if (visible() || !notify(celebration)) _celebration.value = celebration
            }
        }
    }

    fun onShown() {
        _celebration.value = null
    }
}
