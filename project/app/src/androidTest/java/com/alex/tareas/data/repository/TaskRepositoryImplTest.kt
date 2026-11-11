package com.alex.tareas.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alex.tareas.data.local.TaskDatabase
import com.alex.tareas.domain.model.Priority
import com.alex.tareas.domain.model.Task
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskRepositoryImplTest {
    private lateinit var database: TaskDatabase
    private lateinit var repository: TaskRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TaskDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = TaskRepositoryImpl(database.taskDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createTask_mapsAndRecoversAllDomainFields() = runBlocking {
        val task = task(
            id = "task-1",
            title = "Comprar leche",
            description = "Leche sin lactosa",
            priority = Priority.HIGH,
            dueDate = LocalDateTime.of(2025, 1, 2, 18, 30)
        )

        repository.createTask(task)

        assertEquals(task, repository.getTask(task.id).first())
        assertEquals(listOf(task), repository.getTasks().first())
    }

    @Test
    fun updateAndDeleteTask_emitCurrentDomainState() = runBlocking {
        val original = task(id = "task-2", title = "Preparar reunión")
        repository.createTask(original)

        val completed = original.copy(
            completed = true,
            updatedAt = LocalDateTime.of(2025, 1, 1, 11, 0),
            completedAt = LocalDateTime.of(2025, 1, 1, 11, 0)
        )
        repository.updateTask(completed)

        assertEquals(completed, repository.getTask(completed.id).first())

        repository.deleteTask(completed)

        assertNull(repository.getTask(completed.id).first())
        assertEquals(emptyList<Task>(), repository.getTasks().first())
    }

    private fun task(
        id: String,
        title: String,
        description: String? = null,
        priority: Priority = Priority.MEDIUM,
        dueDate: LocalDateTime? = null
    ): Task = Task(
        id = id,
        title = title,
        description = description,
        completed = false,
        priority = priority,
        dueDate = dueDate,
        createdAt = LocalDateTime.of(2025, 1, 1, 10, 0),
        updatedAt = LocalDateTime.of(2025, 1, 1, 10, 0),
        completedAt = null
    )
}
