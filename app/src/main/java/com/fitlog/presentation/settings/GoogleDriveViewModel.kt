package com.fitlog.presentation.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.data.remote.BackupInfo
import com.fitlog.data.remote.GoogleAuthManager
import com.fitlog.data.remote.GoogleDriveManager
import com.fitlog.data.remote.GoogleSignInState
import com.fitlog.domain.model.DailyWorkout
import com.fitlog.domain.model.Exercise
import com.fitlog.domain.model.ExerciseCategory
import com.fitlog.domain.model.WorkoutRecord
import com.fitlog.domain.model.WorkoutSet
import com.fitlog.domain.repository.ExerciseRepository
import com.fitlog.domain.repository.WorkoutRepository
import com.fitlog.util.DateUtils
import com.google.gson.GsonBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class GoogleDriveUiState(
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val lastBackupTime: Long? = null,
    val isLoading: Boolean = false,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class GoogleDriveViewModel @Inject constructor(
    private val authManager: GoogleAuthManager,
    private val driveManager: GoogleDriveManager,
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoogleDriveUiState())
    val uiState: StateFlow<GoogleDriveUiState> = _uiState.asStateFlow()

    private val gson = GsonBuilder().setPrettyPrinting().create()

    init {
        observeSignInState()
        authManager.checkCurrentSignIn()
    }

    private fun observeSignInState() {
        viewModelScope.launch {
            authManager.signInState.collect { state ->
                when (state) {
                    is GoogleSignInState.SignedIn -> {
                        driveManager.initialize(state.account)
                        _uiState.update {
                            it.copy(
                                isSignedIn = true,
                                userEmail = state.account.email,
                                isLoading = false
                            )
                        }
                        loadBackupInfo()
                    }
                    is GoogleSignInState.SignedOut -> {
                        driveManager.clear()
                        _uiState.update {
                            it.copy(
                                isSignedIn = false,
                                userEmail = null,
                                lastBackupTime = null,
                                isLoading = false
                            )
                        }
                    }
                    is GoogleSignInState.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is GoogleSignInState.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = state.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun getSignInIntent(): Intent = authManager.getSignInIntent()

    fun handleSignInResult(data: Intent?) {
        authManager.handleSignInResult(data)
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
        }
    }

    fun backup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true, errorMessage = null) }

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

                driveManager.uploadBackup(json).fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                isBackingUp = false,
                                successMessage = "Google Drive에 백업되었습니다",
                                lastBackupTime = System.currentTimeMillis()
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isBackingUp = false,
                                errorMessage = "백업 실패: ${e.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isBackingUp = false,
                        errorMessage = "백업 실패: ${e.message}"
                    )
                }
            }
        }
    }

    fun restore() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true, errorMessage = null) }

            driveManager.downloadBackup().fold(
                onSuccess = { json ->
                    try {
                        val backupData = gson.fromJson(json, BackupData::class.java)

                        // 기존 데이터 삭제
                        workoutRepository.clearAllData()

                        // 운동 종목 복원
                        backupData.exercises.forEach { backupExercise ->
                            val exercise = backupExercise.toDomain()
                            exerciseRepository.addExercise(exercise)
                        }

                        // 운동 이름으로 매핑
                        val exerciseMap = exerciseRepository.getAllExercises().first()
                            .associateBy { it.name }

                        // 일일 운동 기록 복원
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

                        _uiState.update {
                            it.copy(
                                isRestoring = false,
                                successMessage = "복원이 완료되었습니다"
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                isRestoring = false,
                                errorMessage = "복원 실패: ${e.message}"
                            )
                        }
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isRestoring = false,
                            errorMessage = "복원 실패: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    private fun loadBackupInfo() {
        viewModelScope.launch {
            val info = driveManager.getBackupInfo()
            _uiState.update {
                it.copy(lastBackupTime = info?.modifiedTime)
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }

    // 변환 함수들
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
}
