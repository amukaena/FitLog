package com.fitlog.domain.model

data class WorkoutSet(
    val id: Long = 0,
    val workoutRecordId: Long,
    val setNumber: Int,
    val weight: Float,
    val reps: Int
)
