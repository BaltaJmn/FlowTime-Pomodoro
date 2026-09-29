package com.baltajmn.flowtime.data.repository

import com.baltajmn.flowtime.core.database.datasource.SessionDao
import com.baltajmn.flowtime.core.database.model.SessionDb
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DayKeys
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.SESSIONS_IMPORTED
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Los bloques de trabajo terminados. Los totales van en segundos; cada pantalla redondea como quiera. */
interface SessionRepository {
    /** No espera a la base de datos: lo llama el motor, que no puede bloquearse. */
    fun record(mode: String, startedAt: Long, endedAt: Long, focusSeconds: Long)

    fun secondsOn(day: LocalDate): Flow<Long>

    suspend fun secondsByDay(from: LocalDate, to: LocalDate): Map<LocalDate, Long>

    suspend fun totalSeconds(): Long

    /** Tiempo sin sesiones que lo expliquen (un texto importado); con [replace], sustituye el del día. */
    suspend fun addToDays(secondsByDay: Map<LocalDate, Long>, replace: Boolean)

    /** El historial de antes de las sesiones pasa a la base de datos, una sola vez. */
    fun importLegacyOnce()
}

class DefaultSessionRepository(
    private val dao: SessionDao,
    private val dataProvider: DataProvider
) : SessionRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val zone get() = ZoneId.systemDefault()

    override fun record(mode: String, startedAt: Long, endedAt: Long, focusSeconds: Long) {
        val session = SessionDb(
            startedAt = startedAt,
            endedAt = endedAt,
            localDate = Instant.ofEpochMilli(startedAt).atZone(zone).toLocalDate().toString(),
            mode = mode,
            focusSeconds = focusSeconds
        )
        scope.launch { dao.insert(session) }
    }

    override fun secondsOn(day: LocalDate): Flow<Long> = dao.secondsOn(day.toString())

    override suspend fun secondsByDay(from: LocalDate, to: LocalDate): Map<LocalDate, Long> =
        dao.secondsByDay(from.toString(), to.toString()).associate { LocalDate.parse(it.localDate) to it.seconds }

    override suspend fun totalSeconds(): Long = dao.totalSeconds()

    override suspend fun addToDays(secondsByDay: Map<LocalDate, Long>, replace: Boolean) =
        dao.addToDays(secondsByDay, replace, zone)

    // Sustituye cada día en vez de sumar: si el proceso muere antes de marcarlo como hecho y se repite,
    // no duplica nada. Las claves antiguas se quedan, por si hay que volver a una versión anterior.
    override fun importLegacyOnce() {
        if (dataProvider.getBoolean(SESSIONS_IMPORTED, false)) return
        scope.launch {
            val days = dataProvider.getStudyTimeMap().mapNotNull { (key, minutes) ->
                DayKeys.parse(key)?.let { it to minutes * 60 }
            }.toMap()
            dao.addToDays(days, replace = true, zone = zone)
            dataProvider.setBoolean(SESSIONS_IMPORTED, true)
        }
    }
}
