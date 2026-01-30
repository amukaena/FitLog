package com.fitlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fitlog.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("""
        SELECT * FROM exercises
        ORDER BY
            CASE WHEN lastUsedAt IS NOT NULL THEN 0 ELSE 1 END,
            lastUsedAt DESC,
            createdAt ASC
    """)
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("""
        SELECT * FROM exercises
        WHERE category = :category
        ORDER BY
            CASE WHEN lastUsedAt IS NOT NULL THEN 0 ELSE 1 END,
            lastUsedAt DESC,
            createdAt ASC
    """)
    fun getExercisesByCategory(category: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%'")
    fun searchExercises(query: String): Flow<List<ExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Query("UPDATE exercises SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUsedAt(id: Long, timestamp: Long)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getCount(): Int

    @Query("DELETE FROM exercises")
    suspend fun deleteAll()
}
