package com.fitlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fitlog.data.local.entity.WorkoutRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutRecordDao {
    @Query("SELECT * FROM workout_records WHERE dailyWorkoutId = :dailyWorkoutId ORDER BY `order` ASC")
    fun getRecordsByDailyWorkoutId(dailyWorkoutId: Long): Flow<List<WorkoutRecordEntity>>

    @Query("SELECT * FROM workout_records WHERE dailyWorkoutId = :dailyWorkoutId ORDER BY `order` ASC")
    suspend fun getRecordsByDailyWorkoutIdSync(dailyWorkoutId: Long): List<WorkoutRecordEntity>

    @Query("SELECT * FROM workout_records WHERE id = :id")
    suspend fun getRecordById(id: Long): WorkoutRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workoutRecord: WorkoutRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(workoutRecords: List<WorkoutRecordEntity>): List<Long>

    @Update
    suspend fun update(workoutRecord: WorkoutRecordEntity)

    @Query("DELETE FROM workout_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM workout_records WHERE dailyWorkoutId = :dailyWorkoutId")
    suspend fun deleteByDailyWorkoutId(dailyWorkoutId: Long)

    @Query("DELETE FROM workout_records")
    suspend fun deleteAll()
}
