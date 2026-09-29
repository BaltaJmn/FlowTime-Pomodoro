package com.baltajmn.flowtime.core.database.datasource

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.baltajmn.flowtime.core.database.model.TaskDb
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TaskDao {

    /**
     * Lo que puede verse en [day]: las apuntadas y las completadas ese día, y las pendientes de antes
     * de [today]. Qué se enseña de verdad lo decide `Task.showsOn`, que tiene sus tests.
     */
    @Query(
        "SELECT * FROM task WHERE plannedFor = :day OR doneOn = :day " +
            "OR (doneOn IS NULL AND plannedFor < :today) ORDER BY plannedFor, position, id"
    )
    abstract fun around(day: String, today: String): Flow<List<TaskDb>>

    @Query("SELECT * FROM task WHERE id = :id")
    abstract suspend fun get(id: Long): TaskDb?

    @Query("SELECT COUNT(*) FROM task WHERE doneOn IS NULL")
    abstract suspend fun countPending(): Int

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM task WHERE plannedFor = :day")
    abstract suspend fun nextPosition(day: String): Int

    /** Con el id que traiga: así deshacer un borrado deja la tarea como estaba. */
    @Insert
    abstract suspend fun insert(task: TaskDb): Long

    @Update
    abstract suspend fun update(task: TaskDb)

    @Query("DELETE FROM task WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Query("UPDATE task SET position = :position WHERE id = :id")
    protected abstract suspend fun setPosition(id: Long, position: Int)

    /** Las tareas en el orden de [ids]. */
    @Transaction
    open suspend fun reorder(ids: List<Long>) {
        ids.forEachIndexed { position, id -> setPosition(id, position) }
    }
}
