package com.fitlog.domain.service

import com.fitlog.domain.model.DailyWorkout
import com.fitlog.domain.model.Exercise
import com.fitlog.domain.model.ExerciseCategory
import com.fitlog.domain.model.WorkoutRecord
import com.fitlog.domain.model.WorkoutSet
import com.fitlog.domain.repository.ExerciseRepository
import com.fitlog.domain.repository.WorkoutRepository
import com.fitlog.util.DateUtils
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class BackupData(
    val version: Int,
    val exportedAt: String,
    val exercises: List<BackupExercise>,
    val dailyWorkouts: List<BackupDailyWorkout>
)

data class BackupExercise(
    val id: Long,
    val name: String,
    val category: String,
    val isCustom: Boolean
)

data class BackupDailyWorkout(
    val id: Long,
    val date: String,
    val title: String,
    val memo: String?,
    val records: List<BackupWorkoutRecord>
)

data class BackupWorkoutRecord(
    val id: Long,
    val exerciseName: String,
    val order: Int,
    val sets: List<BackupWorkoutSet>
)

data class BackupWorkoutSet(
    val setNumber: Int,
    val weight: Float,
    val reps: Int
)

@Singleton
class BackupService @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository
) {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun createBackupJson(): String {
        val exercises = exerciseRepository.getAllExercises().first()
        val dailyWorkouts = workoutRepository.getAllDailyWorkouts().first()

        val backupData = BackupData(
            version = 1,
            exportedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            exercises = exercises.map { it.toBackupExercise() },
            dailyWorkouts = dailyWorkouts.map { it.toBackupDailyWorkout() }
        )

        return gson.toJson(backupData)
    }

    suspend fun restoreFromJson(json: String) {
        val backupData = gson.fromJson(json, BackupData::class.java)

        workoutRepository.clearAllData()

        backupData.exercises.forEach { backupExercise ->
            exerciseRepository.addExercise(backupExercise.toDomain())
        }

        val exerciseMap = exerciseRepository.getAllExercises().first()
            .associateBy { it.name }

        backupData.dailyWorkouts.forEach { backupWorkout ->
            val dailyWorkout = backupWorkout.toDomain()
            val dailyWorkoutId = workoutRepository.saveDailyWorkout(dailyWorkout)

            backupWorkout.records.forEachIndexed { index, backupRecord ->
                val exercise = exerciseMap[backupRecord.exerciseName] ?: return@forEachIndexed
                val record = WorkoutRecord(
                    dailyWorkoutId = dailyWorkoutId,
                    exercise = exercise,
                    order = index
                )
                val recordId = workoutRepository.saveWorkoutRecord(record)

                backupRecord.sets.forEach { backupSet ->
                    val set = WorkoutSet(
                        workoutRecordId = recordId,
                        setNumber = backupSet.setNumber,
                        weight = backupSet.weight,
                        reps = backupSet.reps
                    )
                    workoutRepository.saveWorkoutSet(set)
                }
            }
        }
    }
}

private fun Exercise.toBackupExercise() = BackupExercise(
    id = id,
    name = name,
    category = category.displayName,
    isCustom = isCustom
)

private fun BackupExercise.toDomain() = Exercise(
    name = name,
    category = ExerciseCategory.fromDisplayName(category),
    isCustom = isCustom
)

private fun DailyWorkout.toBackupDailyWorkout() = BackupDailyWorkout(
    id = id,
    date = DateUtils.epochMillisToLocalDate(date).format(DateTimeFormatter.ISO_LOCAL_DATE),
    title = title,
    memo = memo,
    records = records.map { record ->
        BackupWorkoutRecord(
            id = record.id,
            exerciseName = record.exercise.name,
            order = record.order,
            sets = record.sets.map { set ->
                BackupWorkoutSet(
                    setNumber = set.setNumber,
                    weight = set.weight,
                    reps = set.reps
                )
            }
        )
    }
)

private fun BackupDailyWorkout.toDomain() = DailyWorkout(
    date = DateUtils.localDateToEpochMillis(LocalDate.parse(date)),
    title = title,
    memo = memo
)
