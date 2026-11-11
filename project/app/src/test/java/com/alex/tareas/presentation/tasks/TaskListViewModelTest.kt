package com.alex.tareas.presentation.tasks

import androidx.lifecycle.SavedStateHandle
import com.alex.tareas.domain.model.Priority
import com.alex.tareas.domain.model.Task
import com.alex.tareas.domain.model.TaskFilter
import com.alex.tareas.domain.model.TaskSort
import com.alex.tareas.domain.repository.TaskRepository
import com.alex.tareas.domain.usecase.FilterTasksUseCase
import com.alex.tareas.domain.usecase.GetTasksUseCase
import com.alex.tareas.domain.usecase.ToggleTaskUseCase
import com.alex.tareas.testutil.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(
        Instant.parse("2025-01-01T10:00:00Z"),
        ZoneOffset.UTC
    )

    @Test
    fun initialState_defaultsToPendingFilterAndShowsOnlyPendingTasks() = runTest {
        val pending = task(id = "pending", title = "Pendiente")
        val completed = task(
            id = "completed",
            title = "Completada",
            completed = true,
            completedAt = LocalDateTime.of(2025, 1, 1, 9, 30)
        )
        val viewModel = viewModel(RecordingTaskRepository(listOf(pending, completed)))

        val collector = launch { viewModel.uiState.collect {} }
        advanceTimeBy(250)
        runCurrent()

        assertEquals(TaskFilter.PENDING, viewModel.uiState.value.filter)
        assertEquals(listOf(pending), viewModel.uiState.value.tasks)
        assertFalse(viewModel.uiState.value.isLoading)

        collector.cancel()
    }

    @Test
    fun search_event_appliesDebounceAndFindsTitleOrDescriptionMatches() = runTest {
        val milk = task(id = "milk", title = "Comprar leche")
        val tickets = task(
            id = "tickets",
            title = "Reservas",
            description = "Comprar billetes de tren"
        )
        val meeting = task(id = "meeting", title = "Preparar reunión")
        val viewModel = viewModel(
            RecordingTaskRepository(listOf(milk, tickets, meeting))
        )
        val collector = launch { viewModel.uiState.collect {} }
        advanceTimeBy(250)
        runCurrent()

        viewModel.onEvent(TaskListEvent.ChangeFilter(TaskFilter.ALL))
        viewModel.onEvent(TaskListEvent.Search("comprar"))
        runCurrent()

        assertEquals("", viewModel.uiState.value.searchQuery)

        advanceTimeBy(250)
        runCurrent()

        assertEquals("comprar", viewModel.uiState.value.searchQuery)
        assertEquals(setOf(milk, tickets), viewModel.uiState.value.tasks.toSet())

        collector.cancel()
    }

    @Test
    fun filtersAndSortEvents_arePersistedInSavedStateHandle() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = viewModel(
            repository = RecordingTaskRepository(),
            savedStateHandle = savedStateHandle
        )
        val collector = launch { viewModel.uiState.collect {} }

        viewModel.onEvent(TaskListEvent.ChangeFilter(TaskFilter.COMPLETED))
        viewModel.onEvent(TaskListEvent.ChangePriorityFilter(Priority.HIGH))
        viewModel.onEvent(TaskListEvent.ChangeSort(TaskSort.TITLE))
        runCurrent()

        assertEquals(TaskFilter.COMPLETED, savedStateHandle["filter"])
        assertEquals(Priority.HIGH, savedStateHandle["priority"])
        assertEquals(TaskSort.TITLE, savedStateHandle["sort"])

        collector.cancel()
    }

    @Test
    fun toggle_event_updatesMatchingTaskAndAssignsCompletionTimestamp() = runTest {
        val pending = task(id = "pending", title = "Completar")
        val repository = RecordingTaskRepository(listOf(pending))
        val viewModel = viewModel(repository)
        val collector = launch { viewModel.uiState.collect {} }
        advanceTimeBy(250)
        runCurrent()

        viewModel.onEvent(TaskListEvent.ToggleTask("pending"))
        runCurrent()

        val updated = repository.getTask("pending").first()
        requireNotNull(updated)
        assertTrue(updated.completed)
        assertEquals(
            LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC),
            updated.completedAt
        )

        collector.cancel()
    }

    private fun viewModel(
        repository: RecordingTaskRepository,
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ): TaskListViewModel = TaskListViewModel(
        getTasks = GetTasksUseCase(repository),
        toggleTask = ToggleTaskUseCase(repository, clock),
        filterTasks = FilterTasksUseCase(),
        savedStateHandle = savedStateHandle
    )

    private fun task(
        id: String,
        title: String,
        description: String? = null,
        completed: Boolean = false,
        completedAt: LocalDateTime? = null
    ): Task = Task(
        id = id,
        title = title,
        description = description,
        completed = completed,
        priority = Priority.MEDIUM,
        dueDate = null,
        createdAt = LocalDateTime.of(2025, 1, 1, 9, 0),
        updatedAt = LocalDateTime.of(2025, 1, 1, 9, 0),
        completedAt = completedAt
    )
}

private class RecordingTaskRepository(
    initialTasks: List<Task> = emptyList()
) : TaskRepository {
    private val mutableTasks = MutableStateFlow(initialTasks)

    override fun getTasks(): Flow<List<Task>> = mutableTasks

    override fun getTask(id: String): Flow<Task?> =
        mutableTasks.map { tasks -> tasks.firstOrNull { it.id == id } }

    override suspend fun createTask(task: Task) {
        mutableTasks.value = mutableTasks.value + task
    }

    override suspend fun updateTask(task: Task) {
        mutableTasks.value = mutableTasks.value.map { current ->
            if (current.id == task.id) task else current
        }
    }

    override suspend fun deleteTask(task: Task) {
        mutableTasks.value = mutableTasks.value.filterNot { current -> current.id == task.id }
    }
}
