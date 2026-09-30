package com.baltajmn.flowtime.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.baltajmn.flowtime.core.database.datasource.AppDatabase
import com.baltajmn.flowtime.core.database.datasource.MIGRATION_1_2
import com.baltajmn.flowtime.core.database.datasource.MIGRATION_3_4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun de_la_1_a_la_2_se_conservan_las_tareas_y_aparece_la_tabla_de_sesiones() {
        helper.createDatabase(DB, 1).use { db ->
            db.execSQL("INSERT INTO todoList (date, todoList) VALUES ('29092026', '[]')")
        }

        // Comprueba además que las tablas quedan igual que las que espera Room en la versión 2.
        helper.runMigrationsAndValidate(DB, 2, true, MIGRATION_1_2).use { db ->
            db.query("SELECT COUNT(*) FROM todoList").use {
                it.moveToFirst()
                assertEquals(1, it.getInt(0))
            }
            db.execSQL(
                "INSERT INTO session (startedAt, endedAt, localDate, mode, focusSeconds) " +
                    "VALUES (0, 1000, '2026-09-29', 'POMODORO', 1)"
            )
        }
    }

    @Test
    fun de_la_2_a_la_3_las_sesiones_se_conservan_sin_etiqueta() {
        helper.createDatabase(DB, 2).use { db ->
            db.execSQL(
                "INSERT INTO session (startedAt, endedAt, localDate, mode, focusSeconds) " +
                    "VALUES (0, 1500000, '2026-09-29', 'POMODORO', 1500)"
            )
        }

        // La migración automática: la tabla de etiquetas y session.tagId, que admite nulos.
        helper.runMigrationsAndValidate(DB, 3, true).use { db ->
            db.query("SELECT focusSeconds, tagId FROM session").use {
                it.moveToFirst()
                assertEquals(1500, it.getInt(0))
                assertEquals(true, it.isNull(1))
            }
            db.execSQL(
                "INSERT INTO tag (name, color, position, archived, createdAt) " +
                    "VALUES ('Estudio', 0, 0, 0, 0)"
            )
        }
    }

    @Test
    fun de_la_3_a_la_4_cada_tarea_pasa_a_su_fila_sin_perder_ninguna() {
        helper.createDatabase(DB, 3).use { db ->
            db.execSQL(
                "INSERT INTO todoList (date, todoList) VALUES ('2026-09-28', " +
                    "'[{\"id\":1,\"title\":\"Repasar\",\"description\":\"Tema 4\",\"done\":true}," +
                    "{\"id\":2,\"title\":\"Leer\",\"description\":\"\",\"done\":false}]')"
            )
            db.execSQL(
                "INSERT INTO todoList (date, todoList) VALUES ('2026-09-29', " +
                    "'[{\"id\":3,\"title\":\"Correr\",\"description\":\"\",\"done\":false}]')"
            )
            db.execSQL("INSERT INTO todoList (date, todoList) VALUES ('2026-09-30', '[]')")
        }

        helper.runMigrationsAndValidate(DB, 4, true, MIGRATION_3_4).use { db ->
            db.query("SELECT title, plannedFor, doneOn, createdAt, position FROM task ORDER BY createdAt").use {
                val rows = mutableListOf<String>()
                while (it.moveToNext()) {
                    rows += listOf(it.getString(0), it.getString(1), it.getString(2), it.getLong(3), it.getInt(4))
                        .joinToString("|")
                }
                assertEquals(
                    listOf(
                        "Repasar|2026-09-28|2026-09-28|1|0",
                        "Leer|2026-09-28|null|2|1",
                        "Correr|2026-09-29|null|3|0"
                    ),
                    rows
                )
            }
            // La tabla antigua se queda, por si hubiera que volver atrás.
            db.query("SELECT COUNT(*) FROM todoList").use {
                it.moveToFirst()
                assertEquals(3, it.getInt(0))
            }
        }
    }

    private companion object {
        const val DB = "migration-test"
    }
}
