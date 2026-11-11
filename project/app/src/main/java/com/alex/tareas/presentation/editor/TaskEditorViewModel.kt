package com.alex.tareas.presentation.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.tareas.domain.model.Priority
import com.alex.tareas.domain.model.Task
import com.alex.tareas.domain.usecase.CreateTaskUseCase
import com.alex.tareas.domain.usecase.GetTaskUseCase
import com.alex.tareas.domain.usecase.TaskValidator
import com.alex.tareas.domain.usecase.UpdateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TaskEditorUiState(
    val taskId: String? = null,
    val title: String = "",
    val description: String = "",
    val priority: Priority = Priority.MEDIUM,
    val dueDate: LocalDateTime? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val titleError: String? = null,
    val isEditMode: Boolean = false
)

@HiltViewModel
class TaskEditorViewModel @Inject constructor(
    private val createTask: CreateTaskUseCase,
    private val updateTask: UpdateTaskUseCase,
    private val getTask: GetTaskUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val taskId: String? = savedStateHandle.get<String>(TASK_ID_KEY)
    private var original: Task? = null
    private var isInitialTaskLoaded = taskId == null
    
    private val mutableState = MutableStateFlow(
        TaskEditorUiState(
            taskId = taskId,
            title = savedStateHandle.get<String>(TITLE_KEY) ?: "",
            description = savedStateHandle.get<String>(DESCRIPTION_KEY) ?: "",
            priority = savedStateHandle.get<String>(PRIORITY_KEY)
                ?.let { Priority.valueOf(it) }
                ?: Priority.MEDIUM,
            dueDate = savedStateHandle.get<String>(DUE_DATE_KEY)
                ?.let { parseDueDate(it) },
            isEditMode = taskId != null
        )
    )

    val uiState = mutableState.asStateFlow()
    val messages = MutableSharedFlow<String>()
    val navigateBack = MutableSharedFlow<Unit>()

    init {
        if (taskId != null) {
            loadTask(taskId)
        }
    }

    fun update(transform: (TaskEditorUiState) -> TaskEditorUiState) {
        val updated = transform(mutableState.value).copy(titleError = null)
        mutableState.value = updated
        persistDraft(updated)
    }

    fun save() {
        viewModelScope.launch {
            val state = mutableState.value
            val validTitle = runCatching { TaskValidator.title(state.title) }
                .getOrElse {
                    mutableState.value = state.copy(titleError = "Introduce un título válido.")
                    return@launch
                }

            mutableState.value = state.copy(
                title = validTitle,
                isSaving = true,
                titleError = null
            )

            runCatching {
                val existing = original
                if (existing == null) {
                    createTask(
                        title = validTitle,
                        description = state.description,
                        priority = state.priority,
                        dueDate = state.dueDate
                    )
                } else {
                    updateTask(
                        original = existing,
                        title = validTitle,
                        description = state.description,
                        priority = state.priority,
                        dueDate = state.dueDate
                    )
                }
            }.onSuccess {
                clearDraft()
                navigateBack.emit(Unit)
            }.onFailure {
                mutableState.value = mutableState.value.copy(isSaving = false)
                messages.emit("No se ha podido guardar la tarea.")
            }
        }
    }

    fun hasUnsavedChanges(): Boolean {
        val state = mutableState.value
        val initial = original

        if (initial == null) {
            return state.title.isNotBlank() ||
                state.description.isNotBlank() ||
                state.priority != Priority.MEDIUM ||
                state.dueDate != null
        }

        if (!isInitialTaskLoaded) {
            return false
        }

        return state.title.trim() != initial.title ||
            state.description.trim().ifEmpty { null } != initial.description ||
            state.priority != initial.priority ||
            state.dueDate != initial.dueDate
    }

    private fun loadTask(id: String) {
        viewModelScope.launch {
            val task = getTask(id).first()
            if (task == null) {
                isInitialTaskLoaded = true
                return@launch
            }

            original = task
            isInitialTaskLoaded = true

            val hasRestoredDraft = savedStateHandle.contains(TITLE_KEY)
            if (!hasRestoredDraft) {
                val state = mutableState.value.copy(
                    title = task.title,
                    description = task.description.orEmpty(),
                    priority = task.priority,
                    dueDate = task.dueDate
                )
                mutableState.value = state
                persistDraft(state)
            }
        }
    }

    private fun persistDraft(state: TaskEditorUiState) {
        savedStateHandle[TITLE_KEY] = state.title
        savedStateHandle[DESCRIPTION_KEY] = state.description
        savedStateHandle[PRIORITY_KEY] = state.priority.name
        savedStateHandle[DUE_DATE_KEY] = state.dueDate?.format(dueDateFormatter)
    }

    private fun clearDraft() {
        savedStateHandle.remove<String>(TITLE_KEY)
        savedStateHandle.remove<String>(DESCRIPTION_KEY)
        savedStateHandle.remove<String>(PRIORITY_KEY)
        savedStateHandle.remove<String>(DUE_DATE_KEY)
    }

    private companion object {
        val dueDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        const val TASK_ID_KEY = "taskId"
        const val TITLE_KEY = "editorTitle"
        const val DESCRIPTION_KEY = "editorDescription"
        const val PRIORITY_KEY = "editorPriority"
        const val DUE_DATE_KEY = "editorDueDate"

        fun parseDueDate(value: String): LocalDateTime? =
            runCatching {
                LocalDateTime.parse(value, dueDateFormatter)
            }.getOrNull()
    }
}
