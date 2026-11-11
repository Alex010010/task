package com.alex.tareas.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {
    private lateinit var database: TaskDatabase
    private lateinit var dao: TaskDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TaskDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = database.taskDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertTask_persistsEveryDatabaseFieldAndCanBeReadById() = runBlocking {
        val entity = pendingTask(
            id = "inserted",
            title = "Comprar leche",
            description = "Supermercado",
            priority = "HIGH",
            dueDate = 1_735_728_000_000
        )

        dao.insertTask(entity)

        assertEquals(entity, dao.getTaskById("inserted").first())
        assertEquals(listOf(entity), dao.getAllTasks().first())
        assertEquals(listOf(entity), dao.getPendingTasks().first())
    }

    @Test
    fun updateTask_movesTaskBetweenPendingAndCompletedQueries() = runBlocking {
        val pending = pendingTask(id = "toggle", title = "Preparar reunión")
        dao.insertTask(pending)

        val completed = pending.copy(
            completed = true,
            updatedAt = 1_735_729_000_000,
            completedAt = 1_735_729_000_000
        )
        dao.updateTask(completed)

        assertEquals(emptyList<TaskEntity>(), dao.getPendingTasks().first())
        assertEquals(listOf(completed), dao.getCompletedTasks().first())
        assertEquals(completed, dao.getTaskById("toggle").first())
    }

    @Test
    fun searchTasks_findsTitleAndDescriptionWithoutCaseSensitivity() = runBlocking {
        val milk = pendingTask(
            id = "milk",
            title = "Comprar leche",
            description = "Supermercado"
        )
        val tickets = pendingTask(
            id = "tickets",
            title = "Reservas",
            description = "COMPRAR billetes de tren"
        )
        val meeting = pendingTask(
            id = "meeting",
            title = "Preparar reunión",
            description = "Sala azul"
        )
        dao.insertTask(milk)
        dao.insertTask(tickets)
        dao.insertTask(meeting)

        val results = dao.searchTasks("comprar").first()

        assertEquals(setOf(milk, tickets), results.toSet())
    }

    @Test
    fun deleteTask_removesTaskFromIdAndAllTasksFlows() = runBlocking {
        val entity = pendingTask(id = "delete", title = "Eliminarme")
        dao.insertTask(entity)

        dao.deleteTask(entity)

        assertNull(dao.getTaskById("delete").first())
        assertEquals(emptyList<TaskEntity>(), dao.getAllTasks().first())
    }

    private fun pendingTask(
        id: String,
        title: String,
        description: String? = null,
        priority: String = "MEDIUM",
        dueDate: Long? = null
    ): TaskEntity = TaskEntity(
        id = id,
        title = title,
        description = description,
        completed = false,
        priority = priority,
        dueDate = dueDate,
        createdAt = 1_735_700_000_000,
        updatedAt = 1_735_700_000_000,
        completedAt = null
    )
}
