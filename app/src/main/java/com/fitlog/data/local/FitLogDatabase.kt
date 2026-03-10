package com.fitlog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fitlog.data.local.dao.DailyWorkoutDao
import com.fitlog.data.local.dao.ExerciseDao
import com.fitlog.data.local.dao.WorkoutRecordDao
import com.fitlog.data.local.dao.WorkoutSetDao
import com.fitlog.data.local.entity.DailyWorkoutEntity
import com.fitlog.data.local.entity.ExerciseEntity
import com.fitlog.data.local.entity.WorkoutRecordEntity
import com.fitlog.data.local.entity.WorkoutSetEntity

@Database(
    entities = [
        ExerciseEntity::class,
        DailyWorkoutEntity::class,
        WorkoutRecordEntity::class,
        WorkoutSetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FitLogDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun dailyWorkoutDao(): DailyWorkoutDao
    abstract fun workoutRecordDao(): WorkoutRecordDao
    abstract fun workoutSetDao(): WorkoutSetDao

    companion object {
        const val DATABASE_NAME = "fitlog_database"
    }
}
