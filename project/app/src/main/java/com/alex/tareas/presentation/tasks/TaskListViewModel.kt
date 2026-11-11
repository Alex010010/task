package com.alex.tareas.presentation.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.tareas.domain.model.Priority
import com.alex.tareas.domain.model.Task
import com.alex.tareas.domain.model.TaskFilter
import com.alex.tareas.domain.model.TaskSort
import com.alex.tareas.domain.usecase.FilterTasksUseCase
import com.alex.tareas.domain.usecase.GetTasksUseCase
import com.alex.tareas.domain.usecase.ToggleTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskListUiState(
    val allTasks: List<Task> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val filter: TaskFilter = TaskFilter.PENDING,
    val priorityFilter: Priority? = null,
    val sort: TaskSort = TaskSort.DEFAULT,
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

sealed interface TaskListEvent {
    data class ToggleTask(val taskId: String) : TaskListEvent
    data class Search(val query: String) : TaskListEvent
    data class ChangeFilter(val filter: TaskFilter) : TaskListEvent
    data class ChangePriorityFilter(val priority: Priority?) : TaskListEvent
    data class ChangeSort(val sort: TaskSort) : TaskListEvent
}

@OptIn(FlowPreview::class)
@HiltViewModel
class TaskListViewModel @Inject constructor(
    getTasks: GetTasksUseCase,
    private val toggleTask: ToggleTaskUseCase,
    private val filterTasks: FilterTasksUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val searchQuery = MutableStateFlow(savedStateHandle[SEARCH_KEY] ?: "")
    private val filter = MutableStateFlow(savedStateHandle[FILTER_KEY] ?: TaskFilter.PENDING)
    private val priority = MutableStateFlow<Priority?>(savedStateHandle[PRIORITY_KEY])
    private val sort = MutableStateFlow(savedStateHandle[SORT_KEY] ?: TaskSort.DEFAULT)

    val messages = MutableSharedFlow<String>()

    val uiState = combine(
        getTasks(),
        filter,
        priority,
        sort,
        searchQuery.debounce(250)
    ) { tasks, selectedFilter, selectedPriority, selectedSort, query ->
        TaskListUiState(
            allTasks = tasks,
            tasks = filterTasks(
                tasks = tasks,
                filter = selectedFilter,
                priority = selectedPriority,
                query = query,
                sort = selectedSort
            ),
            filter = selectedFilter,
            priorityFilter = selectedPriority,
            sort = selectedSort,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskListUiState()
    )

    fun onEvent(event: TaskListEvent) {
        when (event) {
            is TaskListEvent.Search -> {
                savedStateHandle[SEARCH_KEY] = event.query
                searchQuery.value = event.query
            }

            is TaskListEvent.ChangeFilter -> {
                savedStateHandle[FILTER_KEY] = event.filter
                filter.value = event.filter
            }

            is TaskListEvent.ChangePriorityFilter -> {
                savedStateHandle[PRIORITY_KEY] = event.priority
                priority.value = event.priority
            }

            is TaskListEvent.ChangeSort -> {
                savedStateHandle[SORT_KEY] = event.sort
                sort.value = event.sort
            }

            is TaskListEvent.ToggleTask -> toggle(event.taskId)
        }
    }

    private fun toggle(taskId: String) {
        viewModelScope.launch {
            val task = uiState.value.allTasks.firstOrNull { it.id == taskId }
                ?: return@launch

            runCatching { toggleTask(task) }
                .onFailure {
                    messages.emit("No se ha podido actualizar la tarea.")
                }
        }
    }

    private companion object {
        const val SEARCH_KEY = "searchQuery"
        const val FILTER_KEY = "filter"
        const val PRIORITY_KEY = "priority"
        const val SORT_KEY = "sort"
    }
}
