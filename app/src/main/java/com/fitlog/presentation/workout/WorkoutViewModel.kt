package com.fitlog.presentation.workout

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.domain.model.DailyWorkout
import com.fitlog.domain.model.Exercise
import com.fitlog.domain.model.WorkoutRecord
import com.fitlog.domain.model.WorkoutSet
import com.fitlog.domain.repository.ExerciseRepository
import com.fitlog.domain.repository.WorkoutRepository
import com.fitlog.util.WorkoutFormatter
import com.fitlog.widget.FitLogWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DailyWorkoutUiState(
    val date: Long = 0,
    val dailyWorkout: DailyWorkout? = null,
    val title: String = "",
    val memo: String = "",
    val records: List<WorkoutRecord> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val recentWorkouts: List<DailyWorkout> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyWorkoutUiState())
    val uiState: StateFlow<DailyWorkoutUiState> = _uiState.asStateFlow()

    fun loadWorkout(date: Long) {
        _uiState.update { it.copy(date = date, isLoading = true) }

        viewModelScope.launch {
            val existingWorkout = workoutRepository.getDailyWorkoutByDateSync(date)
            if (existingWorkout != null) {
                _uiState.update {
                    it.copy(
                        dailyWorkout = existingWorkout,
                        title = existingWorkout.title,
                        memo = existingWorkout.memo ?: "",
                        records = existingWorkout.records,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }

            exerciseRepository.getAllExercises().collect { exercises ->
                _uiState.update { it.copy(exercises = exercises) }
            }
        }

        viewModelScope.launch {
            workoutRepository.getRecentDailyWorkouts(10).collect { recent ->
                _uiState.update { it.copy(recentWorkouts = recent.filter { w -> w.date != date }) }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateMemo(memo: String) {
        _uiState.update { it.copy(memo = memo) }
    }

    fun addExercise(exercise: Exercise) {
        viewModelScope.launch {
            val currentState = _uiState.value
            var dailyWorkoutId = currentState.dailyWorkout?.id ?: 0L

            if (dailyWorkoutId == 0L) {
                val newWorkout = DailyWorkout(
                    date = currentState.date,
                    title = currentState.title.ifBlank { "운동" }
                )
                dailyWorkoutId = workoutRepository.saveDailyWorkout(newWorkout)
                val savedWorkout = workoutRepository.getDailyWorkoutById(dailyWorkoutId)
                _uiState.update { it.copy(dailyWorkout = savedWorkout) }
            }

            val newRecord = WorkoutRecord(
                dailyWorkoutId = dailyWorkoutId,
                exercise = exercise,
                order = currentState.records.size,
                sets = listOf(
                    WorkoutSet(workoutRecordId = 0, setNumber = 1, weight = 0f, reps = 0)
                )
            )

            val recordId = workoutRepository.saveWorkoutRecord(newRecord)
            val defaultSet = WorkoutSet(
                workoutRecordId = recordId,
                setNumber = 1,
                weight = 0f,
                reps = 0
            )
            workoutRepository.saveWorkoutSet(defaultSet)

            refreshRecords(dailyWorkoutId)
            FitLogWidget.updateWidget(context)
        }
    }

    fun deleteRecord(recordId: Long) {
        viewModelScope.launch {
            workoutRepository.deleteWorkoutRecord(recordId)
            _uiState.value.dailyWorkout?.id?.let { refreshRecords(it) }
        }
    }

    fun deleteDailyWorkout() {
        viewModelScope.launch {
            _uiState.value.dailyWorkout?.id?.let { id ->
                workoutRepository.deleteDailyWorkout(id)
                FitLogWidget.updateWidget(context)
                _uiState.update { it.copy(isDeleted = true) }
            }
        }
    }

    private var reorderJob: Job? = null

    fun moveRecord(fromIndex: Int, toIndex: Int) {
        val currentRecords = _uiState.value.records.toMutableList()
        if (fromIndex < 0 || fromIndex >= currentRecords.size ||
            toIndex < 0 || toIndex >= currentRecords.size) {
            return
        }

        val item = currentRecords.removeAt(fromIndex)
        currentRecords.add(toIndex, item)

        // UI 즉시 업데이트 (optimistic update)
        _uiState.update { it.copy(records = currentRecords.toList()) }

        // DB 저장은 debounce 적용 (연속 드래그 시 마지막 상태만 저장)
        reorderJob?.cancel()
        reorderJob = viewModelScope.launch {
            delay(300)
            updateRecordOrder(currentRecords)
        }
    }

    fun updateRecordOrder(records: List<WorkoutRecord>) {
        viewModelScope.launch {
            records.forEachIndexed { index, record ->
                workoutRepository.updateWorkoutRecord(record.copy(order = index))
            }
            _uiState.update { it.copy(records = records) }
        }
    }

    fun saveWorkout() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val dailyWorkout = currentState.dailyWorkout

            if (dailyWorkout != null) {
                workoutRepository.updateDailyWorkout(
                    dailyWorkout.copy(
                        title = currentState.title.ifBlank { "운동" },
                        memo = currentState.memo.ifBlank { null }
                    )
                )
                FitLogWidget.updateWidget(context)
            }

            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun copyFromPreviousWorkout(sourceDailyWorkoutId: Long) {
        viewModelScope.launch {
            val currentDate = _uiState.value.date
            workoutRepository.copyDailyWorkout(sourceDailyWorkoutId, currentDate)
            loadWorkout(currentDate)
            FitLogWidget.updateWidget(context)
        }
    }

    private suspend fun refreshRecords(dailyWorkoutId: Long) {
        val workout = workoutRepository.getDailyWorkoutById(dailyWorkoutId)
        _uiState.update {
            it.copy(
                dailyWorkout = workout,
                records = workout?.records ?: emptyList()
            )
        }
    }

    fun copyWorkoutForAI(): Boolean {
        val currentState = _uiState.value
        val workout = currentState.dailyWorkout?.copy(
            title = currentState.title.ifBlank { "운동" },
            memo = currentState.memo.ifBlank { null },
            records = currentState.records
        ) ?: return false

        if (workout.records.isEmpty()) return false

        val formattedText = WorkoutFormatter.formatForAI(workout)
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("운동 기록", formattedText)
        clipboardManager.setPrimaryClip(clip)
        return true
    }
}
