package com.baltajmn.flowtime.data.goal

import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.design.resources.Res
import com.baltajmn.flowtime.core.design.resources.goal_reached_text
import com.baltajmn.flowtime.core.design.resources.streak_days
import com.baltajmn.flowtime.core.design.resources.streak_milestone
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

/** Haber cumplido el objetivo de hoy, con la racha que deja. */
data class Celebration(val goalMinutes: Int, val streak: Int) {
    /** 7, 30 o 100 días seguidos. */
    val milestone get() = streak in MILESTONES

    suspend fun text(): String {
        val days = getPluralString(Res.plurals.streak_days, streak, streak)
        return when {
            milestone -> getString(Res.string.streak_milestone, days)
            streak > 1 -> days
            else -> getString(Res.string.goal_reached_text, goalMinutes.toLong().formatMinutesStudying())
        }
    }

    private companion object {
        val MILESTONES = setOf(7, 30, 100)
    }
}
