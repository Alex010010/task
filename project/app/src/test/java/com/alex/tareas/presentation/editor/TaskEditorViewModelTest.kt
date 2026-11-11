package com.alex.tareas.presentation.editor

import androidx.lifecycle.SavedStateHandle
import com.alex.tareas.domain.model.Priority
import com.alex.tareas.domain.model.Task
import com.alex.tareas.domain.repository.TaskRepository
import com.alex.tareas.domain.usecase.CreateTaskUseCase
import com.alex.tareas.domain.usecase.GetTaskUseCase
import com.alex.tareas.domain.usecase.UpdateTaskUseCase
import com.alex.tareas.testutil.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskEditorViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(
        Instant.parse("2025-01-01T10:00:00Z"),
        ZoneOffset.UTC
    )

    @Test
    fun save_withValidNewTask_createsNormalizedTaskAndClearsDraft() = runTest {
        val repository = RecordingRepository()
        val savedStateHandle = SavedStateHandle()
        val viewModel = editorViewModel(
            repository = repository,
            savedStateHandle = savedStateHandle
        )

        viewModel.update { state ->
            state.copy(
                title = " Comprar leche ",
                description = "  Supermercado  ",
                priority = Priority.HIGH
            )
        }
        viewModel.save()
        advanceUntilIdle()

        val savedTask = repository.tasks.value.single()
        assertEquals("Comprar leche", savedTask.title)
        assertEquals("Supermercado", savedTask.description)
        assertEquals(Priority.HIGH, savedTask.priority)
        assertFalse(savedTask.completed)
        assertNull(savedTask.completedAt)
        assertFalse(savedStateHandle.contains("editorTitle"))
        assertFalse(savedStateHandle.contains("editorDescription"))
        assertFalse(savedStateHandle.contains("editorPriority"))
        assertFalse(savedStateHandle.contains("editorDueDate"))
    }

    @Test
    fun save_withBlankTitle_showsValidationErrorAndDoesNotCreateTask() = runTest {
        val repository = RecordingRepository()
        val viewModel = editorViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle()
        )

        viewModel.update { state -> state.copy(title = "   ") }
        viewModel.save()
        advanceUntilIdle()

        assertTrue(repository.tasks.value.isEmpty())
        assertEquals(
            "Introduce un título válido.",
            viewModel.uiState.value.titleError
        )
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun hasUnsavedChanges_forNewTask_ignoresWhitespaceOnlyDescription() = runTest {
        val viewModel = editorViewModel(
            repository = RecordingRepository(),
            savedStateHandle = SavedStateHandle()
        )

        viewModel.update { state -> state.copy(description = "   ") }

        assertFalse(viewModel.hasUnsavedChanges())
    }

    @Test
    fun editedTask_withNormalizedEquivalentFields_hasNoUnsavedChanges() = runTest {
        val original = task(
            id = "existing",
            title = "Comprar leche",
            description = "Supermercado",
            priority = Priority.MEDIUM
        )
        val repository = RecordingRepository(listOf(original))
        val viewModel = editorViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(mapOf("taskId" to original.id))
        )
        advanceUntilIdle()

        viewModel.update { state ->
            state.copy(
                title = "  Comprar leche  ",
                description = " Supermercado "
            )
        }

        assertFalse(viewModel.hasUnsavedChanges())
    }

    @Test
    fun save_existingTask_updatesEditableFieldsAndPreservesCompletionState() = runTest {
        val original = task(
            id = "existing",
            title = "Título anterior",
            completed = true,
            completedAt = LocalDateTime.of(2025, 1, 1, 9, 30)
        )
        val repository = RecordingRepository(listOf(original))
        val viewModel = editorViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(mapOf("taskId" to original.id))
        )
        advanceUntilIdle()

        viewModel.update { state ->
            state.copy(
                title = "Título nuevo",
                description = "Nueva descripción",
                priority = Priority.LOW
            )
        }
        viewModel.save()
        advanceUntilIdle()

        val updated = repository.tasks.value.single()
        assertEquals(original.id, updated.id)
        assertEquals(original.createdAt, updated.createdAt)
        assertTrue(updated.completed)
        assertEquals(original.completedAt, updated.completedAt)
        assertEquals("Título nuevo", updated.title)
        assertEquals("Nueva descripción", updated.description)
        assertEquals(Priority.LOW, updated.priority)
    }

    private fun editorViewModel(
        repository: RecordingRepository,
        savedStateHandle: SavedStateHandle
    ): TaskEditorViewModel = TaskEditorViewModel(
        createTask = CreateTaskUseCase(
            repository = repository,
            clock = clock,
            idGenerator = { "generated-id" }
        ),
        updateTask = UpdateTaskUseCase(repository, clock),
        getTask = GetTaskUseCase(repository),
        savedStateHandle = savedStateHandle
    )

    private fun task(
        id: String,
        title: String,
        description: String? = null,
        completed: Boolean = false,
        completedAt: LocalDateTime? = null,
        priority: Priority = Priority.MEDIUM
    ): Task = Task(
        id = id,
        title = title,
        description = description,
        completed = completed,
        priority = priority,
        dueDate = null,
        createdAt = LocalDateTime.of(2025, 1, 1, 9, 0),
        updatedAt = LocalDateTime.of(2025, 1, 1, 9, 0),
        completedAt = completedAt
    )
}

private class RecordingRepository(
    initialTasks: List<Task> = emptyList()
) : TaskRepository {
    val tasks = MutableStateFlow(initialTasks)

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
        tasks.value = tasks.value.filterNot { current -> current.id == task.id }
    }
}
