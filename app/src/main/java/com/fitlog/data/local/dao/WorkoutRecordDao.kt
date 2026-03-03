package com.fitlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fitlog.data.local.entity.WorkoutRecordEntity
import kotlinx.coroutines.flow.Flow

data class LatestExerciseRecord(
    val exerciseId: Long,
    val recordId: Long,
    val date: Long
)

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

    @Query("""
        SELECT wr.exerciseId, wr.id as recordId, dw.date
        FROM workout_records wr
        JOIN daily_workouts dw ON wr.dailyWorkoutId = dw.id
        WHERE wr.id = (
            SELECT wr2.id FROM workout_records wr2
            JOIN daily_workouts dw2 ON wr2.dailyWorkoutId = dw2.id
            WHERE wr2.exerciseId = wr.exerciseId
            ORDER BY dw2.date DESC, wr2.id DESC LIMIT 1
        )
    """)
    suspend fun getLatestRecordForAllExercises(): List<LatestExerciseRecord>
}
