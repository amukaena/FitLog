package com.fitlog.domain.repository

import com.fitlog.domain.model.Exercise
import com.fitlog.domain.model.ExerciseCategory
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getAllExercises(): Flow<List<Exercise>>
    fun getExercisesByCategory(category: ExerciseCategory): Flow<List<Exercise>>
    fun searchExercises(query: String): Flow<List<Exercise>>
    suspend fun getExerciseById(id: Long): Exercise?
    suspend fun addExercise(exercise: Exercise): Long
    suspend fun updateExercise(exercise: Exercise)
    suspend fun updateLastUsedAt(id: Long)
    suspend fun initializeDefaultExercises()
}
