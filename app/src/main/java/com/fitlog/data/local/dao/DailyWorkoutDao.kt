package com.fitlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fitlog.data.local.entity.DailyWorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyWorkoutDao {
    @Query("SELECT * FROM daily_workouts WHERE date = :date")
    suspend fun getDailyWorkoutByDate(date: Long): DailyWorkoutEntity?

    @Query("SELECT * FROM daily_workouts WHERE date = :date")
    fun getDailyWorkoutByDateFlow(date: Long): Flow<DailyWorkoutEntity?>

    @Query("SELECT * FROM daily_workouts WHERE id = :id")
    suspend fun getDailyWorkoutById(id: Long): DailyWorkoutEntity?

    @Query("SELECT * FROM daily_workouts WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getDailyWorkoutsInRange(startDate: Long, endDate: Long): Flow<List<DailyWorkoutEntity>>

    @Query("SELECT date FROM daily_workouts WHERE date BETWEEN :startDate AND :endDate")
    fun getWorkoutDatesInRange(startDate: Long, endDate: Long): Flow<List<Long>>

    @Query("SELECT date FROM daily_workouts WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getWorkoutDatesInRangeSync(startDate: Long, endDate: Long): List<Long>

    @Query("SELECT * FROM daily_workouts ORDER BY date DESC LIMIT :limit")
    fun getRecentDailyWorkouts(limit: Int): Flow<List<DailyWorkoutEntity>>

    @Query("SELECT * FROM daily_workouts ORDER BY date DESC")
    fun getAllDailyWorkouts(): Flow<List<DailyWorkoutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dailyWorkout: DailyWorkoutEntity): Long

    @Update
    suspend fun update(dailyWorkout: DailyWorkoutEntity)

    @Query("DELETE FROM daily_workouts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM daily_workouts")
    suspend fun deleteAll()
}
