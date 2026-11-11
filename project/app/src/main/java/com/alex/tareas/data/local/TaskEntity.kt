package com.alex.tareas.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.alex.tareas.domain.model.Priority
import com.alex.tareas.domain.model.Task
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val completed: Boolean,
    val priority: String,
    @ColumnInfo(name = "due_date") val dueDate: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "completed_at") val completedAt: Long?
)

private fun LocalDateTime.toEpochMillis(): Long =
    atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun Long.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    description = description,
    completed = completed,
    priority = priority.name,
    dueDate = dueDate?.toEpochMillis(),
    createdAt = createdAt.toEpochMillis(),
    updatedAt = updatedAt.toEpochMillis(),
    completedAt = completedAt?.toEpochMillis()
)

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    description = description,
    completed = completed,
    priority = Priority.valueOf(priority),
    dueDate = dueDate?.toLocalDateTime(),
    createdAt = createdAt.toLocalDateTime(),
    updatedAt = updatedAt.toLocalDateTime(),
    completedAt = completedAt?.toLocalDateTime()
)
