package com.alex.tareas.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.alex.tareas.presentation.screens.TaskDetailScreen
import com.alex.tareas.presentation.screens.TaskEditorScreen
import com.alex.tareas.presentation.screens.TaskListScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object AppDestination {
    const val TASKS = "tasks"
    const val CREATE = "task/editor"
    const val DETAIL = "task/{taskId}"
    const val EDIT = "task/{taskId}/editor"

    fun detail(taskId: String): String = "task/${taskId.urlEncode()}"

    fun edit(taskId: String): String = "task/${taskId.urlEncode()}/editor"

    private fun String.urlEncode(): String =
        URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.TASKS,
        modifier = modifier
    ) {
        composable(AppDestination.TASKS) {
            TaskListScreen(
                onCreate = { navController.navigate(AppDestination.CREATE) },
                onOpen = { taskId ->
                    navController.navigate(AppDestination.detail(taskId))
                }
            )
        }

        composable(AppDestination.CREATE) {
            TaskEditorScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = AppDestination.DETAIL,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.StringType
                }
            )
        ) {
            TaskDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { taskId ->
                    navController.navigate(AppDestination.edit(taskId))
                }
            )
        }

        composable(
            route = AppDestination.EDIT,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.StringType
                }
            )
        ) {
            TaskEditorScreen(onBack = { navController.popBackStack() })
        }
    }
}
