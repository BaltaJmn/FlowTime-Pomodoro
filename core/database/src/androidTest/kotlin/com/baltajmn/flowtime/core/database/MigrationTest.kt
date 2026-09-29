package com.baltajmn.flowtime.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.baltajmn.flowtime.core.database.datasource.AppDatabase
import com.baltajmn.flowtime.core.database.datasource.MIGRATION_1_2
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

    private companion object {
        const val DB = "migration-test"
    }
}
