package com.alex.tareas.di

import android.content.Context
import androidx.room.Room
import com.alex.tareas.data.local.TaskDao
import com.alex.tareas.data.local.TaskDatabase
import com.alex.tareas.data.repository.TaskRepositoryImpl
import com.alex.tareas.domain.repository.TaskRepository
import com.alex.tareas.domain.usecase.CreateTaskUseCase
import com.alex.tareas.domain.usecase.DeleteTaskUseCase
import com.alex.tareas.domain.usecase.FilterTasksUseCase
import com.alex.tareas.domain.usecase.GetTaskUseCase
import com.alex.tareas.domain.usecase.GetTasksUseCase
import com.alex.tareas.domain.usecase.ToggleTaskUseCase
import com.alex.tareas.domain.usecase.UpdateTaskUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): TaskDatabase = Room.databaseBuilder(
        context,
        TaskDatabase::class.java,
        "tasks.db"
    ).build()

    @Provides
    fun provideTaskDao(database: TaskDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideCreateTaskUseCase(repository: TaskRepository): CreateTaskUseCase =
        CreateTaskUseCase(repository)

    @Provides
    fun provideUpdateTaskUseCase(repository: TaskRepository): UpdateTaskUseCase =
        UpdateTaskUseCase(repository)

    @Provides
    fun provideDeleteTaskUseCase(repository: TaskRepository): DeleteTaskUseCase =
        DeleteTaskUseCase(repository)

    @Provides
    fun provideToggleTaskUseCase(repository: TaskRepository): ToggleTaskUseCase =
        ToggleTaskUseCase(repository)

    @Provides
    fun provideGetTasksUseCase(repository: TaskRepository): GetTasksUseCase =
        GetTasksUseCase(repository)

    @Provides
    fun provideGetTaskUseCase(repository: TaskRepository): GetTaskUseCase =
        GetTaskUseCase(repository)

    @Provides
    fun provideFilterTasksUseCase(): FilterTasksUseCase = FilterTasksUseCase()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        implementation: TaskRepositoryImpl
    ): TaskRepository
}
