package com.baltajmn.flowtime.data.tag

import com.baltajmn.flowtime.core.database.datasource.TagDao
import com.baltajmn.flowtime.core.database.model.TagDb
import com.baltajmn.flowtime.core.design.theme.TagPalette
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.TAGS_SEEDED
import com.baltajmn.flowtime.data.pro.Limits
import com.baltajmn.flowtime.data.pro.ProGate
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class Tag(
    val id: Long,
    val name: String,
    /** Índice en [TagPalette]. */
    val color: Int,
    val archived: Boolean = false
)

sealed interface TagResult {
    data class Done(val id: Long) : TagResult

    /** Un nombre vacío. */
    data object Invalid : TagResult

    /** Ya hay otra con ese nombre, activa o archivada, sin contar mayúsculas. */
    data object Duplicate : TagResult

    /** Sin Pro, ya hay [Limits.FREE_TAGS] activas. */
    data object LimitReached : TagResult
}

/**
 * Las etiquetas de las sesiones. Sin Pro caben [Limits.FREE_TAGS] activas; las archivadas no cuentan,
 * y quien pierde Pro sigue usando todas las que ya tiene: solo no puede tener más.
 */
interface TagRepository {
    /** Todas, también las archivadas: una sesión antigua puede tener cualquiera. Vacía hasta que se leen. */
    val all: StateFlow<List<Tag>>

    /** Solo emite ya leídas: una lista vacía es que no hay ninguna. */
    val active: Flow<List<Tag>>

    val archived: Flow<List<Tag>>

    /** Las sugeridas, en el idioma del móvil, solo la primera vez. */
    suspend fun ensureDefaults(names: List<String>)

    suspend fun create(name: String, color: Int? = null): TagResult

    suspend fun rename(id: Long, name: String): TagResult

    suspend fun recolor(id: Long, color: Int)

    /** Las activas, en el orden de [ids]. */
    suspend fun reorder(ids: List<Long>)

    suspend fun archive(id: Long)

    /** Recuperar una archivada cuenta como tener una activa más. */
    suspend fun unarchive(id: Long): TagResult
}

class DefaultTagRepository(
    private val dao: TagDao,
    private val dataProvider: DataProvider,
    private val gate: ProGate,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) : TagRepository {
    private val rows: Flow<List<Tag>> = dao.all().map { rows -> rows.map { Tag(it.id, it.name, it.color, it.archived) } }

    override val all: StateFlow<List<Tag>> = rows.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val active: Flow<List<Tag>> = rows.map { tags -> tags.filterNot { it.archived } }

    override val archived: Flow<List<Tag>> = rows.map { tags -> tags.filter { it.archived } }

    override suspend fun ensureDefaults(names: List<String>) {
        if (dataProvider.getBoolean(TAGS_SEEDED, false)) return
        if (dao.allOnce().isEmpty()) {
            names.forEachIndexed { index, name ->
                dao.insert(TagDb(name = name, color = index % TagPalette.COUNT, position = index, createdAt = clock()))
            }
        }
        dataProvider.setBoolean(TAGS_SEEDED, true)
    }

    override suspend fun create(name: String, color: Int?): TagResult {
        val clean = name.clean() ?: return TagResult.Invalid
        val tags = dao.allOnce()
        if (tags.any { it.name.equals(clean, ignoreCase = true) }) return TagResult.Duplicate
        if (!gate.allowsOneMore(tags.count { !it.archived }, Limits.FREE_TAGS)) return TagResult.LimitReached
        val id = dao.insert(
            TagDb(
                name = clean,
                color = color?.mod(TagPalette.COUNT) ?: nextColor(tags),
                position = dao.nextPosition(),
                createdAt = clock()
            )
        )
        return TagResult.Done(id)
    }

    override suspend fun rename(id: Long, name: String): TagResult {
        val clean = name.clean() ?: return TagResult.Invalid
        val others = dao.allOnce().filter { it.id != id }
        if (others.any { it.name.equals(clean, ignoreCase = true) }) return TagResult.Duplicate
        dao.rename(id, clean)
        return TagResult.Done(id)
    }

    override suspend fun recolor(id: Long, color: Int) = dao.recolor(id, color.mod(TagPalette.COUNT))

    override suspend fun reorder(ids: List<Long>) = dao.reorder(ids)

    override suspend fun archive(id: Long) = dao.setArchived(id, true)

    override suspend fun unarchive(id: Long): TagResult {
        if (!gate.allowsOneMore(dao.countActive(), Limits.FREE_TAGS)) return TagResult.LimitReached
        dao.setArchived(id, false)
        return TagResult.Done(id)
    }

    private fun String.clean() = trim().take(MAX_NAME).takeIf { it.isNotEmpty() }

    /** El primer color que no usa ninguna activa; si están todos, el siguiente en la rueda. */
    private fun nextColor(tags: List<TagDb>): Int {
        val used = tags.filterNot { it.archived }.map { it.color }.toSet()
        return (0 until TagPalette.COUNT).firstOrNull { it !in used } ?: tags.size % TagPalette.COUNT
    }

    private companion object {
        const val MAX_NAME = 30
    }
}
