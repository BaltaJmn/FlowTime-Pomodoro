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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Los bloques de trabajo terminados. Los totales van en segundos; cada pantalla redondea como quiera. */
interface SessionRepository {
    /** No espera a la base de datos: lo llama el motor, que no puede bloquearse. */
    fun record(
        mode: String,
        startedAt: Long,
        endedAt: Long,
        focusSeconds: Long,
        tagId: Long? = null,
        taskId: Long? = null
    )

    fun secondsOn(day: LocalDate): Flow<Long>

    suspend fun secondsByDay(from: LocalDate, to: LocalDate): Map<LocalDate, Long>

    suspend fun totalSeconds(): Long

    /** Sesiones de trabajo de verdad entre esos días, sin el tiempo importado. */
    suspend fun countSessions(from: LocalDate, to: LocalDate): Int

    /** Tiempo sin sesiones que lo expliquen (un texto importado); con [replace], sustituye el del día. */
    suspend fun addToDays(secondsByDay: Map<LocalDate, Long>, replace: Boolean)

    /** El historial de antes de las sesiones pasa a la base de datos, una sola vez. */
    fun importLegacyOnce()
}

class DefaultSessionRepository(
    private val dao: SessionDao,
    private val dataProvider: DataProvider,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val zone: () -> ZoneId = { ZoneId.systemDefault() }
) : SessionRepository {

    // Lo que se lee o se guarda espera a la importación: la primera vez tras actualizar, una pantalla
    // podía leer antes de que terminara y enseñar el historial vacío.
    @Volatile
    private var legacyImport: Job? = null

    override fun record(
        mode: String,
        startedAt: Long,
        endedAt: Long,
        focusSeconds: Long,
        tagId: Long?,
        taskId: Long?
    ) {
        val session = SessionDb(
            startedAt = startedAt,
            endedAt = endedAt,
            localDate = Instant.ofEpochMilli(startedAt).atZone(zone()).toLocalDate().toString(),
            mode = mode,
            focusSeconds = focusSeconds,
            tagId = tagId,
            taskId = taskId
        )
        scope.launch {
            legacyImport?.join()
            dao.insert(session)
        }
    }

    override fun secondsOn(day: LocalDate): Flow<Long> = dao.secondsOn(day.toString())

    override suspend fun secondsByDay(from: LocalDate, to: LocalDate): Map<LocalDate, Long> {
        legacyImport?.join()
        return dao.secondsByDay(from.toString(), to.toString())
            .associate { LocalDate.parse(it.localDate) to it.seconds }
    }

    override suspend fun totalSeconds(): Long {
        legacyImport?.join()
        return dao.totalSeconds()
    }

    override suspend fun countSessions(from: LocalDate, to: LocalDate): Int {
        legacyImport?.join()
        return dao.countSessions(from.toString(), to.toString())
    }

    override suspend fun addToDays(secondsByDay: Map<LocalDate, Long>, replace: Boolean) {
        legacyImport?.join()
        dao.addToDays(secondsByDay, replace, zone())
    }

    // Solo sustituye el tiempo LEGACY de cada día: si el proceso muere antes de marcarlo como hecho y
    // se repite, no duplica nada, y una sesión nueva guardada mientras tanto no se borra. Si falla, se
    // reintenta en el siguiente arranque en vez de cerrar la app. Las claves antiguas se quedan, por
    // si hay que volver a una versión anterior.
    override fun importLegacyOnce() {
        if (dataProvider.getBoolean(SESSIONS_IMPORTED, false)) return
        legacyImport = scope.launch {
            runCatching {
                val days = dataProvider.getStudyTimeMap().mapNotNull { (key, minutes) ->
                    DayKeys.parse(key)?.let { it to minutes * 60 }
                }.toMap()
                dao.importLegacy(days, zone())
            }.onSuccess { dataProvider.setBoolean(SESSIONS_IMPORTED, true) }
        }
    }
}
