package com.alex.tareas.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskById(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE completed = 0")
    fun getPendingTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE completed = 1")
    fun getCompletedTasks(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE lower(title) LIKE '%' || lower(:query) || '%'
           OR lower(COALESCE(description, '')) LIKE '%' || lower(:query) || '%'
        """
    )
    fun searchTasks(query: String): Flow<List<TaskEntity>>
}
