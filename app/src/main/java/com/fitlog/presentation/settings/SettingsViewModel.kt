package com.fitlog.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.domain.model.DailyWorkout
import com.fitlog.domain.model.Exercise
import com.fitlog.domain.model.ExerciseCategory
import com.fitlog.domain.model.WorkoutRecord
import com.fitlog.domain.model.WorkoutSet
import com.fitlog.domain.repository.ExerciseRepository
import com.fitlog.domain.repository.WorkoutRepository
import com.fitlog.util.DateUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class SettingsUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val importSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun exportToJson(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null) }

            try {
                val exercises = exerciseRepository.getAllExercises().first()
                val dailyWorkouts = workoutRepository.getAllDailyWorkouts().first()

                val backupData = BackupData(
                    version = 1,
                    exportedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    exercises = exercises.map { it.toBackupExercise() },
                    dailyWorkouts = dailyWorkouts.map { it.toBackupDailyWorkout() }
                )

                val json = gson.toJson(backupData)

                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray())
                    }
                }

                _uiState.update { it.copy(isExporting = false, exportSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isExporting = false, errorMessage = "내보내기 실패: ${e.message}")
                }
            }
        }
    }

    fun importFromJson(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, errorMessage = null) }

            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().readText()
                    } ?: throw Exception("파일을 읽을 수 없습니다")
                }

                val backupData = gson.fromJson(json, BackupData::class.java)

                workoutRepository.clearAllData()

                backupData.exercises.forEach { backupExercise ->
                    val exercise = backupExercise.toDomain()
                    exerciseRepository.addExercise(exercise)
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

                _uiState.update { it.copy(isImporting = false, importSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isImporting = false, errorMessage = "가져오기 실패: ${e.message}")
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(exportSuccess = false, importSuccess = false, errorMessage = null) }
    }
}

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
