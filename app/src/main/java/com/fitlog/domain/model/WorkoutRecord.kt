package com.fitlog.domain.model

data class WorkoutRecord(
    val id: Long = 0,
    val dailyWorkoutId: Long,
    val exercise: Exercise,
    val order: Int,
    val sets: List<WorkoutSet> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
