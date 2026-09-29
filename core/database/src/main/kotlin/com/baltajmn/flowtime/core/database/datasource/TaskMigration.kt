package com.baltajmn.flowtime.core.database.datasource

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

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
        JsonParser.parseString(json ?: return emptyList()).takeIf { it.isJsonArray }?.asJsonArray
    } catch (e: RuntimeException) {
        null
    } ?: return emptyList()
    return array.mapNotNull { element ->
        val item = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
        OldTask(
            id = item.primitive("id")?.let { runCatching { it.asLong }.getOrNull() } ?: 0,
            title = item.primitive("title")?.asString.orEmpty(),
            description = item.primitive("description")?.asString.orEmpty(),
            done = item.primitive("done")?.let { runCatching { it.asBoolean }.getOrNull() } ?: false
        )
    }
}

private fun JsonObject.primitive(name: String): JsonElement? = get(name)?.takeIf { it.isJsonPrimitive }
