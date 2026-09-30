package com.baltajmn.flowtime.data.backup

import com.baltajmn.flowtime.core.database.datasource.BackupDao
import com.baltajmn.flowtime.core.database.model.SessionDb
import com.baltajmn.flowtime.core.database.model.TagDb
import com.baltajmn.flowtime.core.database.model.TaskDb
import com.baltajmn.flowtime.core.design.sound.Ambience
import com.baltajmn.flowtime.core.design.sound.PlayerType
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.design.theme.AppearanceRepository
import com.baltajmn.flowtime.core.design.theme.DarkMode
import com.baltajmn.flowtime.core.design.theme.TagPalette
import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.core.persistence.model.TimerDefaults
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.FLOW_TIME_RANGE
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.KEEP_SCREEN_ON
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.LAST_BACKUP_AT
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.PERCENTAGE_RANGE
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.POMODORO_RANGE
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.SHOW_ALERT
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.SHOW_SOUND
import com.baltajmn.flowtime.data.goal.GoalChange
import com.baltajmn.flowtime.data.goal.GoalRepository
import com.baltajmn.flowtime.data.reminder.ReminderRepository
import com.baltajmn.flowtime.data.timer.TimerMode
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Lo que ha hecho una importación. */
data class RestoreResult(
    val sessionsAdded: Int,
    val sessionsExisting: Int,
    val tasksAdded: Int,
    val tagsAdded: Int = 0
)

sealed interface BackupRead {
    data class Valid(val backup: Backup) : BackupRead
    data object NotABackup : BackupRead
    data object TooNew : BackupRead
}

/**
 * La copia de seguridad a fichero: sesiones, tareas y ajustes en JSON. Importar solo añade lo que
 * falta y los ajustes, si se piden; nunca borra nada.
 */
interface BackupRepository {
    /** Cuándo se exportó por última vez, o null si nunca. */
    val lastExportAt: Long?

    suspend fun export(): String

    fun markExported()

    /** Lee el fichero sin escribir nada. */
    suspend fun read(text: String): BackupRead

    /** Las filas que no tienen sentido (una fecha que no existe, un día de más de 24 horas) se saltan. */
    suspend fun restore(backup: Backup, withSettings: Boolean): RestoreResult
}

class DefaultBackupRepository(
    private val dao: BackupDao,
    private val dataProvider: DataProvider,
    private val appearance: AppearanceRepository,
    private val ambience: Ambience,
    private val goals: GoalRepository,
    private val reminders: ReminderRepository,
    private val appVersion: String,
    private val clock: () -> Long = System::currentTimeMillis
) : BackupRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    override val lastExportAt: Long? get() = dataProvider.getLong(LAST_BACKUP_AT).takeIf { it > 0 }

    // Fuera del hilo principal: con años de sesiones, el JSON pasa de unos pocos megas.
    override suspend fun export(): String = withContext(Dispatchers.Default) {
        val backup = Backup(
            exportedAt = clock(),
            appVersion = appVersion,
            sessions = dao.sessions().map { it.toBackup() },
            tags = dao.tags().map { BackupTag(it.id, it.name, it.color, it.position, it.archived, it.createdAt) },
            tasks = dao.tasks().map { it.toBackup() },
            settings = readSettings()
        )
        json.encodeToString(Backup.serializer(), backup)
    }

    override fun markExported() = dataProvider.setLong(LAST_BACKUP_AT, clock())

    override suspend fun read(text: String): BackupRead = withContext(Dispatchers.Default) {
        // Los errores de formato también: SerializationException es un IllegalArgumentException.
        val backup = try {
            json.decodeFromString(Backup.serializer(), text)
        } catch (e: IllegalArgumentException) {
            null
        }
        when {
            backup == null || backup.app != Backup.APP -> BackupRead.NotABackup
            backup.format > Backup.FORMAT -> BackupRead.TooNew
            else -> BackupRead.Valid(backup)
        }
    }

    override suspend fun restore(
        backup: Backup,
        withSettings: Boolean
    ): RestoreResult = withContext(Dispatchers.Default) {
        val count = dao.restore(
            sessions = backup.sessions.filter { it.isValid() }.map { it.toDb() },
            tasks = backup.tasks
                .filter { it.title.isNotBlank() && it.plannedFor.isDay() && it.doneOn?.isDay() != false }
                .distinctBy { it.id }
                .map { it.toDb() },
            tags = backup.tags.filter { it.name.isNotBlank() }.distinctBy { it.id }.map { it.toDb() }
        )
        if (withSettings) backup.settings?.let(::applySettings)
        RestoreResult(count.sessionsAdded, count.sessionsExisting, count.tasksAdded, count.tagsAdded)
    }

    private fun readSettings() = BackupSettings(
        theme = appearance.appearance.value.theme.name,
        darkMode = appearance.appearance.value.darkMode.name,
        dynamicColor = appearance.appearance.value.dynamicColor,
        pomodoro = dataProvider.getRangeModel(POMODORO_RANGE)?.toBackup(),
        flowTime = dataProvider.getRangeModelList(FLOW_TIME_RANGE)?.map { it.toBackup() },
        percentage = dataProvider.getLong(PERCENTAGE_RANGE).takeIf { it > 0 },
        continueAfterBreak = TimerMode.entries.associate { mode ->
            mode.name to dataProvider.getCheckValue(mode.continueAfterBreakKey)
        },
        showAlert = dataProvider.getBoolean(SHOW_ALERT, true),
        keepScreenOn = dataProvider.getBoolean(KEEP_SCREEN_ON, true),
        showSound = dataProvider.getBoolean(SHOW_SOUND, true),
        // Solo los que se han tocado alguna vez: los demás siguen con el volumen por defecto.
        soundVolumes = PlayerType.entries
            .associate { it.name to dataProvider.getFloat(it.name, UNSET) }
            .filterValues { it != UNSET },
        dailyGoal = goals.history.value
            .map { BackupGoalChange(it.from.toString(), it.minutes) }
            .takeIf { it.isNotEmpty() },
        reminder = reminders.reminder.value
    )

    // Un valor que esta versión no conoce (un tema que ya no existe) se salta y se queda el actual.
    private fun applySettings(settings: BackupSettings) {
        settings.theme?.let { AppTheme.entries.named(it) }?.let(appearance::setTheme)
        settings.darkMode?.let { DarkMode.entries.named(it) }?.let(appearance::setDarkMode)
        settings.dynamicColor?.let(appearance::setDynamicColor)
        settings.pomodoro?.takeIf { it.isValid() }?.let {
            dataProvider.setObject(POMODORO_RANGE, it.toModel())
        }
        settings.flowTime?.takeIf { ranges -> ranges.isNotEmpty() && ranges.all { it.isValid() } }?.let {
            dataProvider.setObject(FLOW_TIME_RANGE, it.map { range -> range.toModel() }.toMutableList())
        }
        settings.percentage?.let {
            dataProvider.setLong(PERCENTAGE_RANGE, TimerDefaults.percentage(it))
        }
        settings.continueAfterBreak.forEach { (name, value) ->
            TimerMode.entries.named(name)?.let { dataProvider.setCheckValue(it.continueAfterBreakKey, value) }
        }
        settings.showAlert?.let { dataProvider.setBoolean(SHOW_ALERT, it) }
        settings.keepScreenOn?.let { dataProvider.setBoolean(KEEP_SCREEN_ON, it) }
        settings.showSound?.let { dataProvider.setBoolean(SHOW_SOUND, it) }
        settings.dailyGoal?.let { changes ->
            val history = changes
                .filter { it.from.isDay() }
                .map { GoalChange(LocalDate.parse(it.from), it.minutes) }
            goals.restore(history)
        }
        settings.reminder?.takeIf { it.minuteOfDay in 0 until MAX_MINUTES }?.let(reminders::set)
        // Por Ambience y no directamente a las preferencias: tiene los volúmenes en memoria.
        settings.soundVolumes.forEach { (name, volume) ->
            PlayerType.entries.named(name)?.let { ambience.setVolume(it, volume.coerceIn(0f, 1f)) }
        }
    }

    private companion object {
        const val UNSET = -1f
        const val MAX_MINUTES = 24 * 60
        const val MAX_TAG_NAME = 30
    }

    private fun SessionDb.toBackup() =
        BackupSession(startedAt, endedAt, localDate, mode, focusSeconds, tagId, taskId)

    private fun BackupSession.toDb() = SessionDb(
        startedAt = startedAt,
        endedAt = endedAt,
        localDate = localDate,
        mode = mode,
        focusSeconds = focusSeconds,
        tagId = tagId,
        taskId = taskId
    )

    private fun BackupTag.toDb() = TagDb(
        id = id,
        name = name.trim().take(MAX_TAG_NAME),
        color = color.mod(TagPalette.COUNT),
        position = position,
        archived = archived,
        createdAt = createdAt
    )

    private fun BackupSession.isValid() =
        localDate.isDay() && mode.isNotBlank() && focusSeconds in 0..SessionDb.DAY_SECONDS && endedAt >= startedAt

    private fun TaskDb.toBackup() = BackupTask(
        id = id,
        title = title,
        description = description,
        plannedFor = plannedFor,
        doneOn = doneOn,
        createdAt = createdAt,
        position = position,
        tagId = tagId
    )

    private fun BackupTask.toDb() = TaskDb(
        id = id,
        title = title,
        description = description,
        plannedFor = plannedFor,
        doneOn = doneOn,
        createdAt = createdAt,
        position = position,
        tagId = tagId
    )

    private fun RangeModel.toBackup() = BackupRange(totalRange, endRange, rest)

    private fun BackupRange.toModel() = RangeModel(totalRange = totalRange, endRange = endRange, rest = rest)

    private fun BackupRange.isValid() = totalRange >= 0 && endRange in 1..MAX_MINUTES && rest in 0..MAX_MINUTES

    private fun <T : Enum<T>> List<T>.named(name: String) = firstOrNull { it.name == name }

    private fun String.isDay() = try {
        LocalDate.parse(this)
        true
    } catch (e: DateTimeParseException) {
        false
    }
}
