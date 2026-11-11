package com.alex.tareas.domain.usecase

import com.alex.tareas.domain.model.Priority
import com.alex.tareas.domain.model.Task
import com.alex.tareas.domain.model.TaskFilter
import com.alex.tareas.domain.model.TaskSort
import com.alex.tareas.domain.repository.TaskRepository
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.flow.Flow

object TaskValidator {
    fun title(value: String): String {
        val normalized = value.trim()
        require(normalized.isNotEmpty()) { "El título es obligatorio." }
        require(normalized.length <= TITLE_MAX_LENGTH) {
            "El título no puede superar $TITLE_MAX_LENGTH caracteres."
        }
        return normalized
    }

    fun description(value: String?): String? {
        val normalized = value?.trim().orEmpty()
        require(normalized.length <= DESCRIPTION_MAX_LENGTH) {
            "La descripción no puede superar $DESCRIPTION_MAX_LENGTH caracteres."
        }
        return normalized.ifEmpty { null }
    }

    const val TITLE_MAX_LENGTH = 120
    const val DESCRIPTION_MAX_LENGTH = 1000
}

class CreateTaskUseCase(
    private val repository: TaskRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val idGenerator: () -> String = { UUID.randomUUID().toString() }
) {
    suspend operator fun invoke(
        title: String,
        description: String?,
        priority: Priority = Priority.MEDIUM,
        dueDate: LocalDateTime? = null
    ): Task {
        val now = LocalDateTime.now(clock)
        val task = Task(
            id = idGenerator(),
            title = TaskValidator.title(title),
            description = TaskValidator.description(description),
            completed = false,
            priority = priority,
            dueDate = dueDate,
            createdAt = now,
            updatedAt = now,
            completedAt = null
        )
        repository.createTask(task)
        return task
    }
}

class UpdateTaskUseCase(
    private val repository: TaskRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    suspend operator fun invoke(
        original: Task,
        title: String,
        description: String?,
        priority: Priority,
        dueDate: LocalDateTime?
    ) {
        repository.updateTask(
            original.copy(
                title = TaskValidator.title(title),
                description = TaskValidator.description(description),
                priority = priority,
                dueDate = dueDate,
                updatedAt = LocalDateTime.now(clock)
            )
        )
    }
}

class ToggleTaskUseCase(
    private val repository: TaskRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    suspend operator fun invoke(task: Task) {
        val now = LocalDateTime.now(clock)
        repository.updateTask(
            task.copy(
                completed = !task.completed,
                completedAt = if (task.completed) null else now,
                updatedAt = now
            )
        )
    }
}

class DeleteTaskUseCase(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(task: Task) {
        repository.deleteTask(task)
    }
}

class GetTasksUseCase(
    private val repository: TaskRepository
) {
    operator fun invoke(): Flow<List<Task>> = repository.getTasks()
}

class GetTaskUseCase(
    private val repository: TaskRepository
) {
    operator fun invoke(id: String): Flow<Task?> = repository.getTask(id)
}

class FilterTasksUseCase {
    operator fun invoke(
        tasks: List<Task>,
        filter: TaskFilter,
        priority: Priority?,
        query: String,
        sort: TaskSort
    ): List<Task> {
        val normalizedQuery = query.trim()

        return tasks.asSequence()
            .filter { task ->
                when (filter) {
                    TaskFilter.ALL -> true
                    TaskFilter.PENDING -> !task.completed
                    TaskFilter.COMPLETED -> task.completed
                }
            }
            .filter { task -> priority == null || task.priority == priority }
            .filter { task ->
                normalizedQuery.isEmpty() ||
                    task.title.contains(normalizedQuery, ignoreCase = true) ||
                    task.description.orEmpty().contains(normalizedQuery, ignoreCase = true)
            }
            .sortedWith(comparator(sort))
            .toList()
    }

    private fun comparator(sort: TaskSort): Comparator<Task> = when (sort) {
        TaskSort.DEFAULT -> compareBy<Task> { it.completed }
            .thenBy { priorityRank(it.priority) }
            .thenBy { it.dueDate == null }
            .thenBy { it.dueDate }
            .thenBy { it.createdAt }

        TaskSort.CREATED_AT -> compareByDescending { it.createdAt }

        TaskSort.DUE_DATE -> compareBy<Task> { it.dueDate == null }
            .thenBy { it.dueDate }
            .thenBy { it.createdAt }

        TaskSort.PRIORITY -> compareBy<Task> { priorityRank(it.priority) }
            .thenBy { it.title.lowercase() }

        TaskSort.TITLE -> compareBy<Task> { it.title.lowercase() }
    }

    private fun priorityRank(priority: Priority): Int = when (priority) {
        Priority.HIGH -> 0
        Priority.MEDIUM -> 1
        Priority.LOW -> 2
    }
}
