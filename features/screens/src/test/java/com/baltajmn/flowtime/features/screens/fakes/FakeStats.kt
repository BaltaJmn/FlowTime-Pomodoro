package com.baltajmn.flowtime.features.screens.fakes

import com.baltajmn.flowtime.data.stats.StatsPeriod
import com.baltajmn.flowtime.data.stats.StatsRepository
import com.baltajmn.flowtime.data.stats.StatsSummary
import com.baltajmn.flowtime.data.stats.TaskTime
import java.time.LocalDate

/** Siempre los mismos números, sea cual sea el periodo. Cuenta las consultas de Pro. */
class FakeStats(
    private val summary: StatsSummary = StatsSummary(),
    private val change: Float? = null,
    private val byHour: List<Long> = List(24) { 0L },
    private val byMode: Map<String, Long> = emptyMap(),
    private val byTag: Map<Long?, Long> = emptyMap(),
    private val topTasks: List<TaskTime> = emptyList()
) : StatsRepository {

    var proQueries = 0
        private set

    override suspend fun summary(period: StatsPeriod, today: LocalDate) = summary

    override suspend fun change(period: StatsPeriod, today: LocalDate): Float? {
        proQueries++
        return change
    }

    override suspend fun byHour(period: StatsPeriod, today: LocalDate) = byHour

    override suspend fun byMode(period: StatsPeriod, today: LocalDate) = byMode

    override suspend fun byTag(period: StatsPeriod, today: LocalDate) = byTag

    override suspend fun topTasks(period: StatsPeriod, today: LocalDate) = topTasks

    override suspend fun csv(tagNames: Map<Long, String>) = ""
}
