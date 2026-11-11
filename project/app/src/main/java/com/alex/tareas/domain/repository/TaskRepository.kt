package com.alex.tareas.domain.repository

import com.alex.tareas.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getTasks(): Flow<List<Task>>

    fun getTask(id: String): Flow<Task?>

    suspend fun createTask(task: Task)

    suspend fun updateTask(task: Task)

    suspend fun deleteTask(task: Task)
}
