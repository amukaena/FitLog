package com.fitlog.di

import com.fitlog.data.repository.ExerciseRepositoryImpl
import com.fitlog.data.repository.StatsRepositoryImpl
import com.fitlog.data.repository.WorkoutRepositoryImpl
import com.fitlog.data.service.BackupServiceImpl
import com.fitlog.domain.repository.ExerciseRepository
import com.fitlog.domain.repository.StatsRepository
import com.fitlog.domain.repository.WorkoutRepository
import com.fitlog.domain.service.BackupService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindExerciseRepository(
        exerciseRepositoryImpl: ExerciseRepositoryImpl
    ): ExerciseRepository

    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(
        workoutRepositoryImpl: WorkoutRepositoryImpl
    ): WorkoutRepository

    @Binds
    @Singleton
    abstract fun bindStatsRepository(
        statsRepositoryImpl: StatsRepositoryImpl
    ): StatsRepository

    @Binds
    @Singleton
    abstract fun bindBackupService(
        backupServiceImpl: BackupServiceImpl
    ): BackupService
}
