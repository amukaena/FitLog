package com.fitlog.di

import android.content.Context
import androidx.room.Room
import com.fitlog.data.local.FitLogDatabase
import com.fitlog.data.local.dao.DailyWorkoutDao
import com.fitlog.data.local.dao.ExerciseDao
import com.fitlog.data.local.dao.WorkoutRecordDao
import com.fitlog.data.local.dao.WorkoutSetDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFitLogDatabase(
        @ApplicationContext context: Context
    ): FitLogDatabase {
        return Room.databaseBuilder(
            context,
            FitLogDatabase::class.java,
            FitLogDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideExerciseDao(database: FitLogDatabase): ExerciseDao {
        return database.exerciseDao()
    }

    @Provides
    @Singleton
    fun provideDailyWorkoutDao(database: FitLogDatabase): DailyWorkoutDao {
        return database.dailyWorkoutDao()
    }

    @Provides
    @Singleton
    fun provideWorkoutRecordDao(database: FitLogDatabase): WorkoutRecordDao {
        return database.workoutRecordDao()
    }

    @Provides
    @Singleton
    fun provideWorkoutSetDao(database: FitLogDatabase): WorkoutSetDao {
        return database.workoutSetDao()
    }
}
