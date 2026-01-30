package com.fitlog.domain.repository

import com.fitlog.domain.model.DailyWorkout
import com.fitlog.domain.model.WorkoutRecord
import com.fitlog.domain.model.WorkoutSet
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun getDailyWorkoutByDate(date: Long): Flow<DailyWorkout?>
    fun getWorkoutDatesInRange(startDate: Long, endDate: Long): Flow<List<Long>>
    fun getRecentDailyWorkouts(limit: Int): Flow<List<DailyWorkout>>
    fun getAllDailyWorkouts(): Flow<List<DailyWorkout>>

    suspend fun getDailyWorkoutByDateSync(date: Long): DailyWorkout?
    suspend fun getDailyWorkoutById(id: Long): DailyWorkout?
    suspend fun saveDailyWorkout(dailyWorkout: DailyWorkout): Long
    suspend fun updateDailyWorkout(dailyWorkout: DailyWorkout)
    suspend fun deleteDailyWorkout(id: Long)

    suspend fun getWorkoutRecordById(id: Long): WorkoutRecord?
    suspend fun saveWorkoutRecord(record: WorkoutRecord): Long
    suspend fun updateWorkoutRecord(record: WorkoutRecord)
    suspend fun deleteWorkoutRecord(id: Long)

    suspend fun saveWorkoutSet(set: WorkoutSet): Long
    suspend fun updateWorkoutSet(set: WorkoutSet)
    suspend fun deleteWorkoutSet(id: Long)

    suspend fun copyDailyWorkout(sourceDailyWorkoutId: Long, targetDate: Long): Long

    suspend fun clearAllData()
}
