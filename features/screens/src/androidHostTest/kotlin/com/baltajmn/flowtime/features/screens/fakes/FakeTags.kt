package com.baltajmn.flowtime.features.screens.fakes

import com.baltajmn.flowtime.data.tag.Tag
import com.baltajmn.flowtime.data.tag.TagRepository
import com.baltajmn.flowtime.data.tag.TagResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Etiquetas en memoria, sin límite. */
class FakeTags(vararg names: String) : TagRepository {

    private val tags = MutableStateFlow(
        names.mapIndexed { index, name -> Tag(id = index + 1L, name = name, color = index) }
    )

    override val all: StateFlow<List<Tag>> = tags

    override val active: Flow<List<Tag>> = tags.map { list -> list.filterNot { it.archived } }

    override val archived: Flow<List<Tag>> = tags.map { list -> list.filter { it.archived } }

    override suspend fun ensureDefaults(names: List<String>) = Unit

    override suspend fun create(name: String, color: Int?): TagResult {
        val id = (tags.value.maxOfOrNull { it.id } ?: 0) + 1
        tags.update { it + Tag(id, name, color ?: 0) }
        return TagResult.Done(id)
    }

    override suspend fun rename(id: Long, name: String): TagResult {
        change(id) { copy(name = name) }
        return TagResult.Done(id)
    }

    override suspend fun recolor(id: Long, color: Int) = change(id) { copy(color = color) }

    override suspend fun reorder(ids: List<Long>) = Unit

    override suspend fun archive(id: Long) = change(id) { copy(archived = true) }

    override suspend fun unarchive(id: Long): TagResult {
        change(id) { copy(archived = false) }
        return TagResult.Done(id)
    }

    private fun change(id: Long, edit: Tag.() -> Tag) =
        tags.update { list -> list.map { if (it.id == id) it.edit() else it } }
}
