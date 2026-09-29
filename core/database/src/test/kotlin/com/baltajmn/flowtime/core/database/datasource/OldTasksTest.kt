package com.baltajmn.flowtime.core.database.datasource

import org.junit.Assert.assertEquals
import org.junit.Test

/** Lo que lee la migración de las tareas (MIGRATION_3_4) del JSON que escribía ItemConverter. */
class OldTasksTest {

    @Test
    fun `una lista como la escribia Gson`() {
        val json = """[{"id":1727600000000,"title":"Repasar","description":"Tema 4","done":true},""" +
            """{"id":1727600001000,"title":"Leer","description":"","done":false}]"""

        assertEquals(
            listOf(
                OldTask(1727600000000, "Repasar", "Tema 4", done = true),
                OldTask(1727600001000, "Leer", "", done = false)
            ),
            oldTasks(json)
        )
    }

    @Test
    fun `lo que falta se rellena`() {
        assertEquals(listOf(OldTask(0, "Leer", "", done = false)), oldTasks("""[{"title":"Leer"}]"""))
    }

    @Test
    fun `lo que no es una tarea se salta y una lista rota se queda vacia`() {
        assertEquals(listOf(OldTask(5, "", "", done = false)), oldTasks("""[3, "hola", {"id":5}]"""))
        assertEquals(emptyList<OldTask>(), oldTasks("[{"))
        assertEquals(emptyList<OldTask>(), oldTasks("{}"))
        assertEquals(emptyList<OldTask>(), oldTasks(null))
        assertEquals(emptyList<OldTask>(), oldTasks("[]"))
    }
}
