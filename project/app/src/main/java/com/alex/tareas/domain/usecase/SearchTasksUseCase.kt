package com.alex.tareas.domain.usecase

import com.alex.tareas.domain.model.Task

class SearchTasksUseCase {
    operator fun invoke(
        tasks: List<Task>,
        query: String
    ): List<Task> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) {
            return tasks
        }

        return tasks.filter { task ->
            task.title.contains(normalizedQuery, ignoreCase = true) ||
                task.description.orEmpty().contains(normalizedQuery, ignoreCase = true)
        }
    }
}
