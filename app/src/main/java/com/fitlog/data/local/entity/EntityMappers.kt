package com.fitlog.data.local.entity

import com.fitlog.domain.model.DailyWorkout
import com.fitlog.domain.model.Exercise
import com.fitlog.domain.model.ExerciseCategory
import com.fitlog.domain.model.WorkoutRecord
import com.fitlog.domain.model.WorkoutSet

fun ExerciseEntity.toDomain(): Exercise = Exercise(
    id = id,
    name = name,
    category = ExerciseCategory.fromDisplayName(category),
    isCustom = isCustom,
    lastUsedAt = lastUsedAt,
    createdAt = createdAt
)

fun Exercise.toEntity(): ExerciseEntity = ExerciseEntity(
    id = id,
    name = name,
    category = category.displayName,
    isCustom = isCustom,
    lastUsedAt = lastUsedAt,
    createdAt = createdAt
)

fun DailyWorkoutEntity.toDomain(records: List<WorkoutRecord> = emptyList()): DailyWorkout = DailyWorkout(
    id = id,
    date = date,
    title = title,
    memo = memo,
    records = records,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun DailyWorkout.toEntity(): DailyWorkoutEntity = DailyWorkoutEntity(
    id = id,
    date = date,
    title = title,
    memo = memo,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun WorkoutRecordEntity.toDomain(exercise: Exercise, sets: List<WorkoutSet> = emptyList()): WorkoutRecord = WorkoutRecord(
    id = id,
    dailyWorkoutId = dailyWorkoutId,
    exercise = exercise,
    order = order,
    sets = sets,
    createdAt = createdAt
)

fun WorkoutRecord.toEntity(): WorkoutRecordEntity = WorkoutRecordEntity(
    id = id,
    dailyWorkoutId = dailyWorkoutId,
    exerciseId = exercise.id,
    order = order,
    createdAt = createdAt
)

fun WorkoutSetEntity.toDomain(): WorkoutSet = WorkoutSet(
    id = id,
    workoutRecordId = workoutRecordId,
    setNumber = setNumber,
    weight = weight,
    reps = reps
)

fun WorkoutSet.toEntity(): WorkoutSetEntity = WorkoutSetEntity(
    id = id,
    workoutRecordId = workoutRecordId,
    setNumber = setNumber,
    weight = weight,
    reps = reps
)
