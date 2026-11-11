package com.alex.tareas.domain.model

import java.time.LocalDateTime

data class Task(
    val id: String,
    val title: String,
    val description: String?,
    val completed: Boolean,
    val priority: Priority,
    val dueDate: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val completedAt: LocalDateTime?
)

enum class Priority(
    val label: String,
    val symbol: String
) {
    LOW(label = "Baja", symbol = "↓"),
    MEDIUM(label = "Media", symbol = "—"),
    HIGH(label = "Alta", symbol = "↑")
}

enum class TaskFilter(val label: String) {
    ALL("Todas"),
    PENDING("Pendientes"),
    COMPLETED("Completadas")
}

enum class TaskSort(val label: String) {
    DEFAULT("Predeterminado"),
    CREATED_AT("Fecha de creación"),
    DUE_DATE("Fecha límite"),
    PRIORITY("Prioridad"),
    TITLE("Título")
}
