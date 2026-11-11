package com.alex.tareas.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.alex.tareas.domain.model.Priority
import com.alex.tareas.domain.model.Task
import java.time.format.DateTimeFormatter

private val taskDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

@Composable
fun PriorityIndicator(priority: Priority) {
    Text(
        text = "${priority.symbol} ${priority.label}",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.semantics {
            contentDescription = "Prioridad ${priority.label}"
        }
    )
}

@Composable
fun TaskItem(
    task: Task,
    onToggle: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(
                onClickLabel = "Abrir detalles de ${task.title}",
                onClick = onOpen
            )
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggle() },
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics {
                        contentDescription = if (task.completed) {
                            "Reabrir tarea ${task.title}"
                        } else {
                            "Marcar como completada ${task.title}"
                        }
                    }
            )

            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.completed) {
                        TextDecoration.LineThrough
                    } else {
                        TextDecoration.None
                    },
                    color = if (task.completed) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                task.description?.let { description ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PriorityIndicator(priority = task.priority)

                    task.dueDate?.let { dueDate ->
                        Text(
                            text = "Vence ${dueDate.format(taskDateFormatter)}",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
