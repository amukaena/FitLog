package com.fitlog.data.local.dao

import androidx.room.Dao
import androidx.room.Query

data class ExerciseDateStat(
    val date: Long,
    val maxWeight: Float,
    val totalVolume: Float,
    val totalSets: Int
)

@Dao
interface StatsDao {

    @Query("""
        SELECT dw.date,
               MAX(ws.weight) as maxWeight,
               SUM(ws.weight * ws.reps) as totalVolume,
               COUNT(ws.id) as totalSets
        FROM daily_workouts dw
        JOIN workout_records wr ON wr.dailyWorkoutId = dw.id
        JOIN workout_sets ws ON ws.workoutRecordId = wr.id
        WHERE wr.exerciseId = :exerciseId AND dw.date >= :startDate
        GROUP BY dw.date
        ORDER BY dw.date ASC
    """)
    suspend fun getExerciseStatsByDate(exerciseId: Long, startDate: Long): List<ExerciseDateStat>

    @Query("""
        SELECT MAX(ws.weight)
        FROM workout_sets ws
        JOIN workout_records wr ON ws.workoutRecordId = wr.id
        WHERE wr.exerciseId = :exerciseId
    """)
    suspend fun getExerciseAllTimeMax(exerciseId: Long): Float?

    @Query("""
        SELECT MIN(dw.date)
        FROM daily_workouts dw
        JOIN workout_records wr ON wr.dailyWorkoutId = dw.id
        WHERE wr.exerciseId = :exerciseId
    """)
    suspend fun getExerciseFirstRecordDate(exerciseId: Long): Long?

    @Query("""
        SELECT COUNT(DISTINCT dw.date)
        FROM daily_workouts dw
        JOIN workout_records wr ON wr.dailyWorkoutId = dw.id
        WHERE wr.exerciseId = :exerciseId
    """)
    suspend fun getExerciseTotalDays(exerciseId: Long): Int

    @Query("""
        SELECT MAX(ws.weight)
        FROM workout_sets ws
        JOIN workout_records wr ON ws.workoutRecordId = wr.id
        JOIN daily_workouts dw ON wr.dailyWorkoutId = dw.id
        WHERE wr.exerciseId = :exerciseId
          AND dw.date = (
            SELECT MAX(dw2.date)
            FROM daily_workouts dw2
            JOIN workout_records wr2 ON wr2.dailyWorkoutId = dw2.id
            WHERE wr2.exerciseId = :exerciseId
          )
    """)
    suspend fun getExerciseLatestMaxWeight(exerciseId: Long): Float?

    @Query("""
        SELECT dw.date
        FROM daily_workouts dw
        JOIN workout_records wr ON wr.dailyWorkoutId = dw.id
        JOIN workout_sets ws ON ws.workoutRecordId = wr.id
        WHERE wr.exerciseId = :exerciseId AND ws.weight = (
            SELECT MAX(ws2.weight)
            FROM workout_sets ws2
            JOIN workout_records wr2 ON ws2.workoutRecordId = wr2.id
            WHERE wr2.exerciseId = :exerciseId
        )
        ORDER BY dw.date DESC
        LIMIT 1
    """)
    suspend fun getExerciseAllTimeMaxDate(exerciseId: Long): Long?
}
