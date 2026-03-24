package com.fitlog.domain.repository

import com.fitlog.domain.model.ExerciseDateStat

interface StatsRepository {
    suspend fun getExerciseStatsByDate(exerciseId: Long, startDate: Long): List<ExerciseDateStat>
    suspend fun getExerciseAllTimeMax(exerciseId: Long): Float?
    suspend fun getExerciseAllTimeMaxDate(exerciseId: Long): Long?
    suspend fun getExerciseLatestMaxWeight(exerciseId: Long): Float?
    suspend fun getExerciseTotalDays(exerciseId: Long): Int
    suspend fun getExerciseFirstRecordDate(exerciseId: Long): Long?
}
