package com.baltajmn.flowtime.core.database.datasource

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull

/** Escrita a mano: con la migración destructiva, un fallo borraba todas las tareas sin avisar. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `session` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `startedAt` INTEGER NOT NULL,
                `endedAt` INTEGER NOT NULL,
                `localDate` TEXT NOT NULL,
                `mode` TEXT NOT NULL,
                `focusSeconds` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_localDate` ON `session` (`localDate`)")
    }
}

/**
 * Las tareas, de un JSON por día (tabla todoList) a una fila cada una. En Kotlin y no con json_each
 * de SQLite, que no está en todos los móviles con Android 8. La tabla todoList se queda una versión
 * más: si algo no se entendiera, no se pierde.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `task` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`title` TEXT NOT NULL, `description` TEXT NOT NULL, `plannedFor` TEXT NOT NULL, " +
                "`doneOn` TEXT, `createdAt` INTEGER NOT NULL, `position` INTEGER NOT NULL, `tagId` INTEGER)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_plannedFor` ON `task` (`plannedFor`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_doneOn` ON `task` (`doneOn`)")
        db.query("SELECT date, todoList FROM todoList ORDER BY date").use { rows ->
            while (rows.moveToNext()) {
                val day = rows.getString(0) ?: continue
                oldTasks(rows.getString(1)).forEachIndexed { position, task ->
                    val values = ContentValues().apply {
                        put("title", task.title)
                        put("description", task.description)
                        put("plannedFor", day)
                        // No se sabía cuándo se completó: el día de la lista es lo más cercano.
                        if (task.done) put("doneOn", day) else putNull("doneOn")
                        put("createdAt", task.id)
                        put("position", position)
                        putNull("tagId")
                    }
                    db.insert("task", SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        }
    }
}

/** Una tarea como la guardaba ItemConverter. */
internal data class OldTask(val id: Long, val title: String, val description: String, val done: Boolean)

/**
 * Las tareas de una lista con el formato que escribía Gson. Lo que falte se rellena; lo que no sea
 * una tarea se salta, y una lista que no se entiende se queda vacía antes que impedir que la app
 * arranque.
 */
internal fun oldTasks(json: String?): List<OldTask> {
    val array = try {
        Json.parseToJsonElement(json ?: return emptyList()) as? JsonArray
    } catch (e: IllegalArgumentException) {
        null
    } ?: return emptyList()
    return array.mapNotNull { element ->
        val item = element as? JsonObject ?: return@mapNotNull null
        OldTask(
            id = item.primitive("id")?.longOrNull ?: 0,
            title = item.primitive("title")?.content.orEmpty(),
            description = item.primitive("description")?.content.orEmpty(),
            done = item.primitive("done")?.booleanOrNull ?: false
        )
    }
}

// Un null de JSON también es un JsonPrimitive: como si no estuviera.
private fun JsonObject.primitive(name: String): JsonPrimitive? =
    (get(name) as? JsonPrimitive)?.takeIf { it !is JsonNull }
