package com.alex.tareas.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.tareas.domain.model.Task
import com.alex.tareas.domain.usecase.DeleteTaskUseCase
import com.alex.tareas.domain.usecase.GetTaskUseCase
import com.alex.tareas.domain.usecase.ToggleTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskDetailUiState(
    val task: Task? = null,
    val isLoading: Boolean = true,
    val showDeleteConfirmation: Boolean = false
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    getTask: GetTaskUseCase,
    private val toggleTask: ToggleTaskUseCase,
    private val deleteTask: DeleteTaskUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val taskId: String = checkNotNull(savedStateHandle["taskId"])

    val messages = MutableSharedFlow<String>()
    val navigateBack = MutableSharedFlow<Unit>()

    val uiState: StateFlow<TaskDetailUiState> = getTask(taskId)
        .map { task ->
            TaskDetailUiState(
                task = task,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TaskDetailUiState()
        )

    fun toggleCompletion() {
        viewModelScope.launch {
            val task = uiState.value.task ?: return@launch
            runCatching { toggleTask(task) }
                .onFailure {
                    messages.emit("No se ha podido actualizar la tarea.")
                }
        }
    }

    fun delete() {
        viewModelScope.launch {
            val task = uiState.value.task ?: return@launch
            runCatching { deleteTask(task) }
                .onSuccess {
                    navigateBack.emit(Unit)
                }
                .onFailure {
                    messages.emit("No se ha podido eliminar la tarea.")
                }
        }
    }
}
