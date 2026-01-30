package com.fitlog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_records",
    foreignKeys = [
        ForeignKey(
            entity = DailyWorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["dailyWorkoutId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["dailyWorkoutId"]),
        Index(value = ["exerciseId"])
    ]
)
data class WorkoutRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dailyWorkoutId: Long,
    val exerciseId: Long,
    val order: Int,
    val createdAt: Long = System.currentTimeMillis()
)
