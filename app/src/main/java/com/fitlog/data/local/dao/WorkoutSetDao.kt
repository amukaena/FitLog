package com.fitlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fitlog.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSetDao {
    @Query("SELECT * FROM workout_sets WHERE workoutRecordId = :workoutRecordId ORDER BY setNumber ASC")
    fun getSetsByRecordId(workoutRecordId: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE workoutRecordId = :workoutRecordId ORDER BY setNumber ASC")
    suspend fun getSetsByRecordIdSync(workoutRecordId: Long): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets WHERE id = :id")
    suspend fun getSetById(id: Long): WorkoutSetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workoutSet: WorkoutSetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(workoutSets: List<WorkoutSetEntity>)

    @Update
    suspend fun update(workoutSet: WorkoutSetEntity)

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM workout_sets WHERE workoutRecordId = :workoutRecordId")
    suspend fun deleteByRecordId(workoutRecordId: Long)

    @Query("DELETE FROM workout_sets")
    suspend fun deleteAll()
}
