package com.alex.tareas.domain.usecase

import com.alex.tareas.domain.model.Priority
import com.alex.tareas.domain.model.Task
import com.alex.tareas.domain.model.TaskFilter
import com.alex.tareas.domain.model.TaskSort
import com.alex.tareas.domain.repository.TaskRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskUseCasesTest {
    private val now = Instant.parse("2025-01-01T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val expectedNow = LocalDateTime.ofInstant(now, ZoneOffset.UTC)

    @Test
    fun createTask_withValidValues_normalizesFieldsAndAssignsRequiredDefaults() = runBlocking {
        val repository = FakeRepository()

        val task = CreateTaskUseCase(repository, clock) { "id-1" }(
            title = " Comprar leche ",
            description = "  Comprar leche entera  ",
            priority = Priority.HIGH,
            dueDate = LocalDateTime.of(2025, 1, 2, 18, 30)
        )

        assertEquals("id-1", task.id)
        assertEquals("Comprar leche", task.title)
        assertEquals("Comprar leche entera", task.description)
        assertEquals(Priority.HIGH, task.priority)
        assertEquals(LocalDateTime.of(2025, 1, 2, 18, 30), task.dueDate)
        assertFalse(task.completed)
        assertNull(task.completedAt)
        assertEquals(expectedNow, task.createdAt)
        assertEquals(expectedNow, task.updatedAt)
        assertEquals(listOf(task), repository.tasks.value)
    }

    @Test
    fun createTask_withoutOptionalValues_usesMediumPriorityAndNullDescription() = runBlocking {
        val repository = FakeRepository()

        val task = CreateTaskUseCase(repository, clock) { "id-2" }(
            title = "Llamar al banco",
            description = null
        )

        assertEquals(Priority.MEDIUM, task.priority)
        assertNull(task.description)
        assertNull(task.dueDate)
        assertEquals(task, repository.tasks.value.single())
    }

    @Test(expected = IllegalArgumentException::class)
    fun createTask_withBlankTitle_doesNotWriteToRepository() = runBlocking {
        val repository = FakeRepository()

        try {
            CreateTaskUseCase(repository, clock) { "id" }(
                title = "   ",
                description = null
            )
        } finally {
            assertTrue(repository.tasks.value.isEmpty())
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun createTask_withTitleOver120Characters_rejectsInput() = runBlocking {
        CreateTaskUseCase(FakeRepository(), clock) { "id" }(
            title = "a".repeat(TaskValidator.TITLE_MAX_LENGTH + 1),
            description = null
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun createTask_withDescriptionOver1000Characters_rejectsInput() = runBlocking {
        CreateTaskUseCase(FakeRepository(), clock) { "id" }(
            title = "Título válido",
            description = "a".repeat(TaskValidator.DESCRIPTION_MAX_LENGTH + 1)
        )
    }

    @Test
    fun updateTask_withEditableChanges_preservesIdentityCreationAndCompletionState() = runBlocking {
        val repository = FakeRepository()
        val original = task(
            completed = true,
            completedAt = LocalDateTime.of(2025, 1, 1, 9, 30),
            priority = Priority.LOW
        )
        repository.createTask(original)

        UpdateTaskUseCase(repository, clock)(
            original = original,
            title = " Título actualizado ",
            description = " Descripción actualizada ",
            priority = Priority.HIGH,
            dueDate = LocalDateTime.of(2025, 1, 5, 9, 0)
        )

        val updated = repository.tasks.value.single()
        assertEquals(original.id, updated.id)
        assertEquals(original.createdAt, updated.createdAt)
        assertEquals(original.completed, updated.completed)
        assertEquals(original.completedAt, updated.completedAt)
        assertEquals("Título actualizado", updated.title)
        assertEquals("Descripción actualizada", updated.description)
        assertEquals(Priority.HIGH, updated.priority)
        assertEquals(LocalDateTime.of(2025, 1, 5, 9, 0), updated.dueDate)
        assertEquals(expectedNow, updated.updatedAt)
    }

    @Test
    fun toggleTask_fromPendingToCompleted_assignsCompletionAndUpdateTimestamps() = runBlocking {
        val repository = FakeRepository()
        val pendingTask = task(completed = false)
        repository.createTask(pendingTask)

        ToggleTaskUseCase(repository, clock)(pendingTask)

        val completed = repository.tasks.value.single()
        assertTrue(completed.completed)
        assertEquals(expectedNow, completed.completedAt)
        assertEquals(expectedNow, completed.updatedAt)
    }

    @Test
    fun toggleTask_fromCompletedToPending_clearsCompletionTimestamp() = runBlocking {
        val repository = FakeRepository()
        val completedTask = task(
            completed = true,
            completedAt = LocalDateTime.of(2025, 1, 1, 9, 30)
        )
        repository.createTask(completedTask)

        ToggleTaskUseCase(repository, clock)(completedTask)

        val reopened = repository.tasks.value.single()
        assertFalse(reopened.completed)
        assertNull(reopened.completedAt)
        assertEquals(expectedNow, reopened.updatedAt)
    }

    @Test
    fun deleteTask_delegatesTheExactTaskToRepository() = runBlocking {
        val repository = FakeRepository()
        val task = task(id = "delete-me")
        repository.createTask(task)

        DeleteTaskUseCase(repository)(task)

        assertTrue(repository.tasks.value.isEmpty())
        assertEquals(task, repository.lastDeletedTask)
    }

    @Test
    fun filterTasks_combinesStatusPriorityAndCaseInsensitiveSearch() {
        val high = task(
            id = "high",
            title = "Comprar leche",
            priority = Priority.HIGH
        )
        val medium = task(
            id = "medium",
            title = "Preparar reunión",
            priority = Priority.MEDIUM
        )
        val completed = task(
            id = "done",
            title = "Comprar billetes",
            completed = true,
            completedAt = expectedNow,
            priority = Priority.HIGH
        )

        val result = FilterTasksUseCase()(
            tasks = listOf(medium, completed, high),
            filter = TaskFilter.PENDING,
            priority = Priority.HIGH,
            query = "COMPRAR",
            sort = TaskSort.DEFAULT
        )

        assertEquals(listOf(high), result)
    }

    @Test
    fun filterTasks_searchesDescriptionsAndReturnsAllAcceptanceCaseMatches() {
        val milk = task(id = "milk", title = "Comprar leche")
        val meeting = task(id = "meeting", title = "Preparar reunión")
        val tickets = task(
            id = "tickets",
            title = "Reservas",
            description = "Comprar billetes de tren"
        )

        val result = FilterTasksUseCase()(
            tasks = listOf(meeting, tickets, milk),
            filter = TaskFilter.ALL,
            priority = null,
            query = "comprar",
            sort = TaskSort.TITLE
        )

        assertEquals(listOf(milk, tickets), result)
    }

    @Test
    fun filterTasks_defaultOrder_placesPendingHighDueTasksBeforeOtherTasks() {
        val highDue = task(
            id = "high-due",
            priority = Priority.HIGH,
            dueDate = LocalDateTime.of(2025, 1, 2, 10, 0)
        )
        val highWithoutDueDate = task(
            id = "high-none",
            priority = Priority.HIGH,
            dueDate = null
        )
        val medium = task(id = "medium", priority = Priority.MEDIUM)
        val completed = task(
            id = "completed",
            completed = true,
            completedAt = expectedNow,
            priority = Priority.HIGH
        )

        val result = FilterTasksUseCase()(
            tasks = listOf(completed, medium, highWithoutDueDate, highDue),
            filter = TaskFilter.ALL,
            priority = null,
            query = "",
            sort = TaskSort.DEFAULT
        )

        assertEquals(
            listOf(highDue, highWithoutDueDate, medium, completed),
            result
        )
    }

    @Test
    fun filterTasks_dueDateSort_placesTasksWithoutDueDateAtTheEnd() {
        val later = task(
            id = "later",
            dueDate = LocalDateTime.of(2025, 1, 4, 10, 0)
        )
        val none = task(id = "none", dueDate = null)
        val earlier = task(
            id = "earlier",
            dueDate = LocalDateTime.of(2025, 1, 2, 10, 0)
        )

        val result = FilterTasksUseCase()(
            tasks = listOf(none, later, earlier),
            filter = TaskFilter.ALL,
            priority = null,
            query = "",
            sort = TaskSort.DUE_DATE
        )

        assertEquals(listOf(earlier, later, none), result)
    }

    private fun task(
        id: String = "task",
        title: String = "Tarea",
        description: String? = null,
        completed: Boolean = false,
        completedAt: LocalDateTime? = null,
        priority: Priority = Priority.MEDIUM,
        dueDate: LocalDateTime? = null
    ): Task = Task(
        id = id,
        title = title,
        description = description,
        completed = completed,
        priority = priority,
        dueDate = dueDate,
        createdAt = LocalDateTime.of(2025, 1, 1, 9, 0),
        updatedAt = LocalDateTime.of(2025, 1, 1, 9, 0),
        completedAt = completedAt
    )
}

private class FakeRepository : TaskRepository {
    val tasks = MutableStateFlow<List<Task>>(emptyList())
    var lastDeletedTask: Task? = null

    override fun getTasks(): Flow<List<Task>> = tasks

    override fun getTask(id: String): Flow<Task?> =
        tasks.map { current -> current.firstOrNull { it.id == id } }

    override suspend fun createTask(task: Task) {
        tasks.value = tasks.value + task
    }

    override suspend fun updateTask(task: Task) {
        tasks.value = tasks.value.map { current ->
            if (current.id == task.id) task else current
        }
    }

    override suspend fun deleteTask(task: Task) {
        lastDeletedTask = task
        tasks.value = tasks.value.filterNot { current -> current.id == task.id }
    }
}
