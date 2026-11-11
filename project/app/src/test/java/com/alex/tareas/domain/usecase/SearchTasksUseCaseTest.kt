package com.alex.tareas.domain.usecase

import com.alex.tareas.domain.model.Priority
import com.alex.tareas.domain.model.Task
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchTasksUseCaseTest {
    private val useCase = SearchTasksUseCase()

    @Test
    fun searchTasks_withBlankQuery_returnsOriginalTaskCollection() {
        val tasks = listOf(task(id = "1", title = "Comprar leche"))

        val result = useCase(tasks, "   ")

        assertEquals(tasks, result)
    }

    @Test
    fun searchTasks_matchesTitleAndDescriptionWithoutCaseSensitivity() {
        val milk = task(id = "milk", title = "Comprar leche")
        val meeting = task(
            id = "meeting",
            title = "Preparar reunión",
            description = "Llevar documentos"
        )
        val tickets = task(
            id = "tickets",
            title = "Reservas",
            description = "COMPRAR billetes de tren"
        )

        val result = useCase(
            tasks = listOf(milk, meeting, tickets),
            query = "comprar"
        )

        assertEquals(listOf(milk, tickets), result)
    }

    @Test
    fun searchTasks_withoutMatches_returnsEmptyList() {
        val result = useCase(
            tasks = listOf(task(id = "1", title = "Comprar leche")),
            query = "farmacia"
        )

        assertEquals(emptyList<Task>(), result)
    }

    private fun task(
        id: String,
        title: String,
        description: String? = null
    ): Task = Task(
        id = id,
        title = title,
        description = description,
        completed = false,
        priority = Priority.MEDIUM,
        dueDate = null,
        createdAt = LocalDateTime.of(2025, 1, 1, 9, 0),
        updatedAt = LocalDateTime.of(2025, 1, 1, 9, 0),
        completedAt = null
    )
}
