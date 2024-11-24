package com.baltajmn.flowtime.core.database.datasource

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Update
import com.baltajmn.flowtime.core.database.converter.ItemConverter
import com.baltajmn.flowtime.core.database.model.TodoListDB

@Database(
    entities = [TodoListDB::class],
    version = 1
)
@TypeConverters(ItemConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoListDao(): TodoListDao
}

@Dao
interface TodoListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodoList(todoListDB: TodoListDB)

    @Query("SELECT * FROM todoList WHERE date = :date")
    suspend fun getTodoListByDate(date: String): TodoListDB?

    @Update
    suspend fun updateTodoList(todoListDB: TodoListDB)
}
