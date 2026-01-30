package com.fitlog.domain.model

data class DailyWorkout(
    val id: Long = 0,
    val date: Long,
    val title: String,
    val memo: String? = null,
    val records: List<WorkoutRecord> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
