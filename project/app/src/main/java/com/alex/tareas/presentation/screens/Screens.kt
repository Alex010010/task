package com.alex.tareas.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alex.tareas.domain.model.Priority
import com.alex.tareas.domain.model.TaskFilter
import com.alex.tareas.domain.model.TaskSort
import com.alex.tareas.presentation.components.PriorityIndicator
import com.alex.tareas.presentation.components.TaskItem
import com.alex.tareas.presentation.detail.TaskDetailViewModel
import com.alex.tareas.presentation.editor.TaskEditorUiState
import com.alex.tareas.presentation.editor.TaskEditorViewModel
import com.alex.tareas.presentation.tasks.TaskListEvent
import com.alex.tareas.presentation.tasks.TaskListViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var priorityMenuOpen by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Mis tareas") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreate,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Crear tarea"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = "${state.allTasks.count { !it.completed }} pendientes",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { value ->
                    viewModel.onEvent(TaskListEvent.Search(value))
                },
                label = { Text("Buscar tareas") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                TaskFilter.entries.forEachIndexed { index, filter ->
                    SegmentedButton(
                        selected = state.filter == filter,
                        onClick = {
                            viewModel.onEvent(TaskListEvent.ChangeFilter(filter))
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = TaskFilter.entries.size
                        )
                    ) {
                        Text(filter.label)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { priorityMenuOpen = true }) {
                    Text(state.priorityFilter?.label ?: "Todas las prioridades")
                }

                DropdownMenu(
                    expanded = priorityMenuOpen,
                    onDismissRequest = { priorityMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Todas") },
                        onClick = {
                            viewModel.onEvent(
                                TaskListEvent.ChangePriorityFilter(null)
                            )
                            priorityMenuOpen = false
                        }
                    )

                    Priority.entries.forEach { priority ->
                        DropdownMenuItem(
                            text = { Text(priority.label) },
                            onClick = {
                                viewModel.onEvent(
                                    TaskListEvent.ChangePriorityFilter(priority)
                                )
                                priorityMenuOpen = false
                            }
                        )
                    }
                }

                Button(onClick = { sortMenuOpen = true }) {
                    Text("Ordenar")
                }

                DropdownMenu(
                    expanded = sortMenuOpen,
                    onDismissRequest = { sortMenuOpen = false }
                ) {
                    TaskSort.entries.forEach { sort ->
                        DropdownMenuItem(
                            text = { Text(sort.label) },
                            onClick = {
                                viewModel.onEvent(TaskListEvent.ChangeSort(sort))
                                sortMenuOpen = false
                            }
                        )
                    }
                }
            }

            when {
                state.isLoading -> {
                    Text(
                        text = "Cargando…",
                        modifier = Modifier.padding(24.dp)
                    )
                }

                state.tasks.isNotEmpty() -> {
                    LazyColumn {
                        items(
                            items = state.tasks,
                            key = { task -> task.id }
                        ) { task ->
                            TaskItem(
                                task = task,
                                onToggle = {
                                    viewModel.onEvent(
                                        TaskListEvent.ToggleTask(task.id)
                                    )
                                },
                                onOpen = { onOpen(task.id) }
                            )
                        }
                    }
                }

                state.allTasks.isEmpty() -> EmptyTasks(onCreate = onCreate)

                state.filter == TaskFilter.COMPLETED &&
                    state.searchQuery.isBlank() -> {
                    EmptyMessage("Todavía no has completado ninguna tarea.")
                }

                else -> EmptyMessage("No encontramos ninguna tarea")
            }
        }
    }
}

@Composable
private fun EmptyTasks(onCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Todavía no tienes tareas",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Añade tu primera tarea para empezar.")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onCreate) {
            Text("Crear tarea")
        }
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        style = MaterialTheme.typography.bodyLarge
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.navigateBack.collect { onBack() }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Detalle de tarea") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    state.task?.let { task ->
                        IconButton(onClick = { onEdit(task.id) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar tarea"
                            )
                        }

                        IconButton(
                            onClick = { showDeleteConfirmation = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar tarea"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Text(
                    text = "Cargando…",
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(24.dp)
                )
            }

            state.task == null -> {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(24.dp)
                ) {
                    Text("Esta tarea ya no está disponible.")
                    Button(
                        onClick = onBack,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Volver a la lista")
                    }
                }
            }

            else -> {
                val task = checkNotNull(state.task)
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(24.dp)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    task.description?.let { description ->
                        Text(
                            text = description,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Estado: ${
                            if (task.completed) "Completada" else "Pendiente"
                        }"
                    )
                    PriorityIndicator(priority = task.priority)

                    task.dueDate?.let { dueDate ->
                        Text("Fecha límite: ${dueDate.format(dateTimeFormatter)}")
                    }

                    Text("Creada: ${task.createdAt.format(dateTimeFormatter)}")
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(onClick = viewModel::toggleCompletion) {
                        Text(if (task.completed) "Reabrir" else "Completar")
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("¿Eliminar tarea?") },
            text = {
                Text("Esta acción eliminará la tarea de forma permanente.")
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancelar")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.delete()
                    }
                ) {
                    Text("Eliminar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    onBack: () -> Unit,
    viewModel: TaskEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDiscardConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.navigateBack.collect { onBack() }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BackHandler {
        if (viewModel.hasUnsavedChanges()) {
            showDiscardConfirmation = true
        } else {
            onBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (state.isEditMode) {
                            "Editar tarea"
                        } else {
                            "Nueva tarea"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (viewModel.hasUnsavedChanges()) {
                                showDiscardConfirmation = true
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::save,
                        enabled = !state.isSaving
                    ) {
                        Text("Guardar")
                    }
                }
            )
        }
    ) { paddingValues ->
        EditorForm(
            state = state,
            viewModel = viewModel,
            modifier = Modifier.padding(paddingValues)
        )
    }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text("¿Descartar los cambios?") },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) {
                    Text("Seguir editando")
                }
            },
            confirmButton = {
                TextButton(onClick = onBack) {
                    Text("Descartar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorForm(
    state: TaskEditorUiState,
    viewModel: TaskEditorViewModel,
    modifier: Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        OutlinedTextField(
            value = state.title,
            onValueChange = { value ->
                if (value.length <= 120) {
                    viewModel.update { current -> current.copy(title = value) }
                }
            },
            label = { Text("¿Qué tienes que hacer?") },
            isError = state.titleError != null,
            supportingText = {
                Text(state.titleError ?: "${state.title.length}/120")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.description,
            onValueChange = { value ->
                if (value.length <= 1000) {
                    viewModel.update { current -> current.copy(description = value) }
                }
            },
            label = { Text("Descripción") },
            supportingText = { Text("${state.description.length}/1000") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Prioridad")

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Priority.entries.forEachIndexed { index, priority ->
                SegmentedButton(
                    selected = state.priority == priority,
                    onClick = {
                        viewModel.update { current ->
                            current.copy(priority = priority)
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = Priority.entries.size
                    )
                ) {
                    Text(priority.label)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = state.dueDate?.let {
                "Fecha límite: ${it.format(dateTimeFormatter)}"
            } ?: "Sin fecha límite",
            style = MaterialTheme.typography.bodyLarge
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    selectedDate = state.dueDate?.toLocalDate()
                    showDatePicker = true
                }
            ) {
                Text("Elegir fecha")
            }

            TextButton(
                onClick = {
                    viewModel.update { current -> current.copy(dueDate = null) }
                },
                enabled = state.dueDate != null
            ) {
                Text("Limpiar fecha")
            }
        }
    }

    if (showDatePicker) {
        val initialSelection = state.dueDate
            ?.atZone(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli()

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialSelection
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            showDatePicker = false
                            showTimePicker = true
                        }
                    }
                ) {
                    Text("Siguiente")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker && selectedDate != null) {
        val currentDateTime = state.dueDate ?: LocalDateTime.now()
        val timePickerState = rememberTimePickerState(
            initialHour = currentDateTime.hour,
            initialMinute = currentDateTime.minute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Elegir hora") },
            text = {
                TimePicker(state = timePickerState)
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancelar")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val dueDate = checkNotNull(selectedDate).atTime(
                            timePickerState.hour,
                            timePickerState.minute
                        )
                        viewModel.update { current ->
                            current.copy(dueDate = dueDate)
                        }
                        showTimePicker = false
                    }
                ) {
                    Text("Aceptar")
                }
            }
        )
    }
}
