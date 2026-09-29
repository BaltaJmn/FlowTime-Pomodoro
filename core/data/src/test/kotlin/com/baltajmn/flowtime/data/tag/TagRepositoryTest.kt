package com.baltajmn.flowtime.data.tag

import com.baltajmn.flowtime.core.database.datasource.TagDao
import com.baltajmn.flowtime.core.database.model.TagDb
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.TAGS_SEEDED
import com.baltajmn.flowtime.data.fakes.FakeDataProvider
import com.baltajmn.flowtime.data.pro.NoPurchases
import com.baltajmn.flowtime.data.pro.ProGate
import com.baltajmn.flowtime.data.pro.PurchasesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagRepositoryTest {

    /** La tabla en memoria, con el orden de la consulta de verdad. */
    private class FakeTagDao : TagDao() {
        val rows = MutableStateFlow<List<TagDb>>(emptyList())
        private var nextId = 1L

        override fun all() = rows.map { tags -> tags.sortedWith(compareBy({ it.archived }, { it.position }, { it.id })) }

        override suspend fun allOnce() = all().first()

        override suspend fun countActive() = rows.value.count { !it.archived }

        override suspend fun nextPosition() = (rows.value.maxOfOrNull { it.position } ?: -1) + 1

        override suspend fun insert(tag: TagDb): Long {
            val id = nextId++
            rows.update { it + tag.copy(id = id) }
            return id
        }

        override suspend fun rename(id: Long, name: String) = change(id) { copy(name = name) }

        override suspend fun recolor(id: Long, color: Int) = change(id) { copy(color = color) }

        override suspend fun setArchived(id: Long, archived: Boolean) = change(id) { copy(archived = archived) }

        override suspend fun setPosition(id: Long, position: Int) = change(id) { copy(position = position) }

        private fun change(id: Long, edit: TagDb.() -> TagDb) =
            rows.update { tags -> tags.map { if (it.id == id) it.edit() else it } }
    }

    private class FakePurchases(pro: Boolean) : PurchasesRepository {
        override val isPro = MutableStateFlow(pro)
    }

    private val dao = FakeTagDao()
    private val prefs = FakeDataProvider()
    private val defaults = listOf("Estudio", "Trabajo", "Lectura", "Casa")

    /** Con Pro ya publicado ([ProGate] encendido), salvo que se diga lo contrario. */
    private fun TestScope.repository(purchases: PurchasesRepository, enabled: Boolean = true) =
        DefaultTagRepository(dao, prefs, ProGate(purchases, enabled), scope = backgroundScope, clock = { 0 })

    private fun names() = dao.rows.value.filterNot { it.archived }.sortedBy { it.position }.map { it.name }

    @Test
    fun `las sugeridas se crean una sola vez`() = runTest {
        val tags = repository(FakePurchases(pro = false))

        tags.ensureDefaults(defaults)
        tags.ensureDefaults(defaults)

        assertEquals(defaults, names())
        assertEquals(listOf(0, 1, 2, 3), dao.rows.value.map { it.color })
        assertEquals(true, prefs.values[TAGS_SEEDED.name])
    }

    @Test
    fun `sin Pro no se puede tener una quinta activa`() = runTest {
        val tags = repository(FakePurchases(pro = false))
        tags.ensureDefaults(defaults)

        assertEquals(TagResult.LimitReached, tags.create("Correr"))
        assertEquals(defaults, names())
    }

    @Test
    fun `archivar una libera su hueco`() = runTest {
        val tags = repository(FakePurchases(pro = false))
        tags.ensureDefaults(defaults)

        tags.archive(dao.rows.value.first { it.name == "Casa" }.id)

        assertTrue(tags.create("Correr") is TagResult.Done)
        // Recuperarla sería tener cinco activas.
        assertEquals(TagResult.LimitReached, tags.unarchive(dao.rows.value.first { it.name == "Casa" }.id))
    }

    @Test
    fun `con Pro no hay limite`() = runTest {
        val tags = repository(FakePurchases(pro = true))
        tags.ensureDefaults(defaults)

        assertTrue(tags.create("Correr") is TagResult.Done)
        assertTrue(tags.create("Cocinar") is TagResult.Done)
        assertEquals(6, names().size)
    }

    @Test
    fun `perder Pro no bloquea las que ya existen`() = runTest {
        val purchases = FakePurchases(pro = true)
        val tags = repository(purchases)
        tags.ensureDefaults(defaults)
        tags.create("Correr")
        tags.create("Cocinar")

        // Un reembolso, por ejemplo.
        purchases.isPro.value = false
        val running = dao.rows.value.first { it.name == "Correr" }.id

        assertTrue(tags.rename(running, "Correr por la mañana") is TagResult.Done)
        tags.recolor(running, 7)
        runCurrent()
        assertEquals(6, tags.active.first().size)
        assertEquals(TagResult.LimitReached, tags.create("Leer cómics"))
    }

    @Test
    fun `mientras Pro no exista no hay limite`() = runTest {
        val tags = DefaultTagRepository(dao, prefs, ProGate(NoPurchases()), scope = backgroundScope)
        tags.ensureDefaults(defaults)

        assertTrue(tags.create("Correr") is TagResult.Done)
    }

    @Test
    fun `sin nombres vacios ni repetidos`() = runTest {
        val tags = repository(FakePurchases(pro = true))
        tags.ensureDefaults(defaults)
        tags.archive(dao.rows.value.first { it.name == "Casa" }.id)

        assertEquals(TagResult.Invalid, tags.create("   "))
        assertEquals(TagResult.Duplicate, tags.create(" estudio "))
        // Las archivadas también cuentan: se recupera la que había.
        assertEquals(TagResult.Duplicate, tags.create("CASA"))
        assertEquals(TagResult.Duplicate, tags.rename(dao.rows.value.first { it.name == "Trabajo" }.id, "Lectura"))
    }

    @Test
    fun `una nueva coge el primer color libre`() = runTest {
        val tags = repository(FakePurchases(pro = true))
        tags.ensureDefaults(defaults)

        tags.create("Correr")

        assertEquals(4, dao.rows.value.first { it.name == "Correr" }.color)
    }

    @Test
    fun `se ordenan como se diga`() = runTest {
        val tags = repository(FakePurchases(pro = true))
        tags.ensureDefaults(defaults)
        val ids = dao.rows.value.associate { it.name to it.id }

        tags.reorder(listOf("Casa", "Estudio", "Lectura", "Trabajo").map(ids::getValue))
        runCurrent()

        assertEquals(listOf("Casa", "Estudio", "Lectura", "Trabajo"), tags.active.first().map { it.name })
    }
}
