package com.alex.tareas.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class AppDestinationTest {
    @Test
    fun detail_withPlainIdentifier_buildsDetailRoute() {
        assertEquals(
            "task/task-123",
            AppDestination.detail("task-123")
        )
    }

    @Test
    fun edit_withIdentifierContainingReservedCharacters_encodesArgument() {
        assertEquals(
            "task/tarea+con+espacios%2F%3F/editor",
            AppDestination.edit("tarea con espacios/?")
        )
    }

    @Test
    fun destinationConstants_matchNavigationContract() {
        assertEquals("tasks", AppDestination.TASKS)
        assertEquals("task/editor", AppDestination.CREATE)
        assertEquals("task/{taskId}", AppDestination.DETAIL)
        assertEquals("task/{taskId}/editor", AppDestination.EDIT)
    }
}
