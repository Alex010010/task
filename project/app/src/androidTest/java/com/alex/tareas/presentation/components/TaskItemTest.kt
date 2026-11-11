package com.alex.tareas.presentation.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.alex.tareas.domain.model.Priority
import com.alex.tareas.domain.model.Task
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TaskItemTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun taskItem_checkboxToggleDoesNotOpenTaskDetail() {
        var toggleCalls = 0
        var openCalls = 0

        composeRule.setContent {
            MaterialTheme {
                TaskItem(
                    task = task(),
                    onToggle = { toggleCalls += 1 },
                    onOpen = { openCalls += 1 }
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Marcar como completada Comprar leche")
            .assertExists()
            .performClick()

        assertEquals(1, toggleCalls)
        assertEquals(0, openCalls)
    }

    @Test
    fun taskItem_taskContainerProvidesIndependentDetailAction() {
        var openCalls = 0

        composeRule.setContent {
            MaterialTheme {
                TaskItem(
                    task = task(),
                    onToggle = {},
                    onOpen = { openCalls += 1 }
                )
            }
        }

        composeRule
            .onNodeWithText("Comprar leche")
            .assertHasClickAction()
            .performClick()

        assertEquals(1, openCalls)
    }

    @Test
    fun completedTask_exposesReopenActionAndTextualPriorityForTalkBack() {
        composeRule.setContent {
            MaterialTheme {
                TaskItem(
                    task = task(completed = true),
                    onToggle = {},
                    onOpen = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Reabrir tarea Comprar leche")
            .assertExists()

        composeRule
            .onNodeWithContentDescription("Prioridad Alta")
            .assertExists()
    }

    private fun task(completed: Boolean = false): Task = Task(
        id = "task-1",
        title = "Comprar leche",
        description = "Ir al supermercado",
        completed = completed,
        priority = Priority.HIGH,
        dueDate = null,
        createdAt = LocalDateTime.of(2025, 1, 1, 10, 0),
        updatedAt = LocalDateTime.of(2025, 1, 1, 10, 0),
        completedAt = if (completed) {
            LocalDateTime.of(2025, 1, 1, 11, 0)
        } else {
            null
        }
    )
}
