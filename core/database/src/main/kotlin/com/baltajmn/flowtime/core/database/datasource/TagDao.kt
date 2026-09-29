package com.baltajmn.flowtime.core.database.datasource

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.baltajmn.flowtime.core.database.model.TagDb
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TagDao {

    /** Todas, las activas primero y cada grupo en su orden. */
    @Query("SELECT * FROM tag ORDER BY archived, position, id")
    abstract fun all(): Flow<List<TagDb>>

    @Query("SELECT * FROM tag ORDER BY archived, position, id")
    abstract suspend fun allOnce(): List<TagDb>

    @Query("SELECT COUNT(*) FROM tag WHERE archived = 0")
    abstract suspend fun countActive(): Int

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM tag")
    abstract suspend fun nextPosition(): Int

    @Insert
    abstract suspend fun insert(tag: TagDb): Long

    @Query("UPDATE tag SET name = :name WHERE id = :id")
    abstract suspend fun rename(id: Long, name: String)

    @Query("UPDATE tag SET color = :color WHERE id = :id")
    abstract suspend fun recolor(id: Long, color: Int)

    @Query("UPDATE tag SET archived = :archived WHERE id = :id")
    abstract suspend fun setArchived(id: Long, archived: Boolean)

    @Query("UPDATE tag SET position = :position WHERE id = :id")
    protected abstract suspend fun setPosition(id: Long, position: Int)

    /** Las etiquetas en el orden de [ids]. */
    @Transaction
    open suspend fun reorder(ids: List<Long>) {
        ids.forEachIndexed { position, id -> setPosition(id, position) }
    }
}
