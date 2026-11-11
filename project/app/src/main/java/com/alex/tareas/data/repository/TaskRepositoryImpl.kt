package com.alex.tareas.data.repository

import com.alex.tareas.data.local.TaskDao
import com.alex.tareas.data.local.toDomain
import com.alex.tareas.data.local.toEntity
import com.alex.tareas.domain.model.Task
import com.alex.tareas.domain.repository.TaskRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {
    override fun getTasks(): Flow<List<Task>> =
        taskDao.getAllTasks().map { entities ->
            entities.map { entity -> entity.toDomain() }
        }

    override fun getTask(id: String): Flow<Task?> =
        taskDao.getTaskById(id).map { entity -> entity?.toDomain() }

    override suspend fun createTask(task: Task) {
        taskDao.insertTask(task.toEntity())
    }

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }

    override suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task.toEntity())
    }
}
