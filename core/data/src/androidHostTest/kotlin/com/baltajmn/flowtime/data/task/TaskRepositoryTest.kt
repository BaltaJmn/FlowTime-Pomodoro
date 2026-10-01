package com.baltajmn.flowtime.data.task

import com.baltajmn.flowtime.core.database.datasource.TaskDao
import com.baltajmn.flowtime.core.database.model.SessionDb
import com.baltajmn.flowtime.core.database.model.TaskDb
import com.baltajmn.flowtime.data.fakes.FakeSessionDao
import com.baltajmn.flowtime.data.pro.ProGate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class TaskRepositoryTest {

    /** La tabla en memoria, con la misma consulta que la de verdad. */
    private class FakeTaskDao : TaskDao() {
        val rows = MutableStateFlow<List<TaskDb>>(emptyList())
        private var nextId = 1L

        override fun around(day: String, today: String) = rows.map { tasks ->
            tasks.filter { it.plannedFor == day || it.doneOn == day || (it.doneOn == null && it.plannedFor < today) }
                .sortedWith(compareBy({ it.plannedFor }, { it.position }, { it.id }))
        }

        override suspend fun get(id: Long) = rows.value.firstOrNull { it.id == id }

        override suspend fun countPending() = rows.value.count { it.doneOn == null }

        override suspend fun nextPosition(day: String) =
            (rows.value.filter { it.plannedFor == day }.maxOfOrNull { it.position } ?: -1) + 1

        override suspend fun insert(task: TaskDb): Long {
            val id = if (task.id != 0L) task.id else nextId++
            rows.update { it + task.copy(id = id) }
            return id
        }

        override suspend fun update(task: TaskDb) = rows.update { tasks -> tasks.map { if (it.id == task.id) task else it } }

        override suspend fun delete(id: Long) = rows.update { tasks -> tasks.filterNot { it.id == id } }

        override suspend fun setPosition(id: Long, position: Int) =
            rows.update { tasks -> tasks.map { if (it.id == id) it.copy(position = position) else it } }
    }

    private val today = LocalDate(2026, 9, 29)
    private val yesterday = today.minus(1, DateTimeUnit.DAY)
    private val dao = FakeTaskDao()
    private val sessions = FakeSessionDao()

    private fun repository(isPro: MutableStateFlow<Boolean> = MutableStateFlow(false), enabled: Boolean = false) =
        DefaultTaskRepository(dao, sessions, ProGate(isPro, enabled), clock = { 0 })

    private suspend fun TaskRepository.titles(day: LocalDate) = day(day, today).first().map { it.title }

    private suspend fun TaskRepository.addDone(title: String, planned: LocalDate, done: LocalDate) {
        val id = (add(title, "", planned) as TaskResult.Done).id
        setDone(id, true, done)
    }

    @Test
    fun `una pendiente de ayer aparece hoy`() = runTest {
        val tasks = repository()
        tasks.add("Repasar", "", yesterday)

        val shown = tasks.day(today, today).first().single()

        assertEquals("Repasar", shown.title)
        assertEquals(1L, shown.daysLate(today))
        // Y en ayer sigue estando, en su día.
        assertEquals(listOf("Repasar"), tasks.titles(yesterday))
    }

    @Test
    fun `una completada ayer no aparece hoy`() = runTest {
        val tasks = repository()
        tasks.addDone("Leer", planned = yesterday, done = yesterday)

        assertEquals(emptyList<String>(), tasks.titles(today))
        assertEquals(listOf("Leer"), tasks.titles(yesterday))
    }

    @Test
    fun `una de ayer completada hoy aparece hoy como hecha, y tambien en su dia`() = runTest {
        val tasks = repository()
        tasks.addDone("Correr", planned = yesterday, done = today)

        assertTrue(tasks.day(today, today).first().single().done)
        assertEquals(listOf("Correr"), tasks.titles(yesterday))
    }

    @Test
    fun `un dia pasado no ensena lo que arrastraba y uno futuro solo lo suyo`() = runTest {
        val tasks = repository()
        tasks.add("Antigua", "", today.minus(5, DateTimeUnit.DAY))
        tasks.add("Mañana", "", today.plus(1, DateTimeUnit.DAY))

        assertEquals(emptyList<String>(), tasks.titles(yesterday))
        assertEquals(listOf("Mañana"), tasks.titles(today.plus(1, DateTimeUnit.DAY)))
        assertEquals(listOf("Antigua"), tasks.titles(today))
    }

    @Test
    fun `sin Pro la tarea 16 no se crea y completar una deja crear otra`() = runTest {
        val tasks = repository(MutableStateFlow(false), enabled = true)
        repeat(15) { tasks.add("Tarea $it", "", today) }

        assertEquals(TaskResult.LimitReached, tasks.add("Una más", "", today))

        tasks.setDone(dao.rows.value.first().id, true, today)
        assertTrue(tasks.add("Una más", "", today) is TaskResult.Done)
    }

    @Test
    fun `con Pro no hay limite`() = runTest {
        val tasks = repository(MutableStateFlow(true), enabled = true)

        repeat(20) { assertTrue(tasks.add("Tarea $it", "", today) is TaskResult.Done) }
    }

    @Test
    fun `quien ya tenia 20 pendientes las conserva y puede editarlas y completarlas`() = runTest {
        dao.rows.value = (1L..20L).map { TaskDb(it, "Tarea $it", "", today.toString(), null, 0, it.toInt()) }
        val tasks = repository(MutableStateFlow(false), enabled = true)

        assertEquals(20, tasks.day(today, today).first().size)
        assertTrue(tasks.edit(3, "Tarea tres", "") is TaskResult.Done)
        tasks.setDone(4, true, today)
        tasks.moveTo(5, today.plus(1, DateTimeUnit.DAY))
        assertEquals(TaskResult.LimitReached, tasks.add("Otra", "", today))
        assertEquals("Tarea tres", dao.get(3)?.title)
    }

    @Test
    fun `mientras Pro no exista no hay limite`() = runTest {
        val tasks = repository()

        repeat(20) { assertTrue(tasks.add("Tarea $it", "", today) is TaskResult.Done) }
    }

    @Test
    fun `deshacer un borrado la deja como estaba`() = runTest {
        val tasks = repository()
        tasks.addDone("Leer", planned = yesterday, done = today)
        val before = dao.rows.value.single()

        val deleted = tasks.delete(before.id)!!
        assertEquals(emptyList<TaskDb>(), dao.rows.value)
        tasks.restore(deleted)

        assertEquals(listOf(before), dao.rows.value)
    }

    @Test
    fun `mover a hoy la pone al final del dia`() = runTest {
        val tasks = repository()
        tasks.add("De hoy", "", today)
        tasks.add("De ayer", "", yesterday)

        tasks.moveTo(dao.rows.value.first { it.title == "De ayer" }.id, today)

        assertEquals(listOf("De hoy", "De ayer"), tasks.titles(today))
        assertEquals(0L, tasks.day(today, today).first().last().daysLate(today))
    }

    @Test
    fun `el tiempo de una tarea es la suma de sus sesiones`() = runTest {
        val tasks = repository()
        val id = (tasks.add("Repasar", "", today) as TaskResult.Done).id
        listOf(25L, 25L, 10L).forEach { minutes ->
            sessions.rows += SessionDb(0, 0, 0, today.toString(), "POMODORO", minutes * 60, taskId = id)
        }
        sessions.rows += SessionDb(0, 0, 0, today.toString(), "POMODORO", 30 * 60)

        assertEquals(60 * 60L, tasks.day(today, today).first().single().focusSeconds)
    }

    @Test
    fun `sin titulo no hay tarea`() = runTest {
        val tasks = repository()

        assertEquals(TaskResult.Invalid, tasks.add("   ", "algo", today))
        assertEquals(emptyList<TaskDb>(), dao.rows.value)
    }
}
