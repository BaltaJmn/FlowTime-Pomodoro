package com.baltajmn.flowtime.features.screens.fakes

import com.baltajmn.flowtime.data.stats.StatsPeriod
import com.baltajmn.flowtime.data.stats.StatsRepository
import com.baltajmn.flowtime.data.stats.StatsSummary
import com.baltajmn.flowtime.data.stats.TaskTime
import java.time.LocalDate

/** Siempre el mismo resumen. */
class FakeStats(private val summary: StatsSummary = StatsSummary()) : StatsRepository {

    override suspend fun summary(period: StatsPeriod, today: LocalDate) = summary

    override suspend fun change(period: StatsPeriod, today: LocalDate): Float? = null

    override suspend fun byHour(period: StatsPeriod, today: LocalDate) = List(24) { 0L }

    override suspend fun byMode(period: StatsPeriod, today: LocalDate) = emptyMap<String, Long>()

    override suspend fun byTag(period: StatsPeriod, today: LocalDate) = emptyMap<Long?, Long>()

    override suspend fun topTasks(period: StatsPeriod, today: LocalDate) = emptyList<TaskTime>()

    override suspend fun csv(tagNames: Map<Long, String>, taskTitles: Map<Long, String>) = ""
}
