package com.fitlog.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.domain.model.WorkoutRecord
import com.fitlog.domain.model.WorkoutSet
import com.fitlog.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutSetUiState(
    val workoutRecord: WorkoutRecord? = null,
    val sets: List<WorkoutSet> = emptyList(),
    val isLoading: Boolean = false,
    val isDone: Boolean = false
)

@HiltViewModel
class WorkoutSetViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutSetUiState())
    val uiState: StateFlow<WorkoutSetUiState> = _uiState.asStateFlow()

    fun loadRecord(workoutRecordId: Long) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val record = workoutRepository.getWorkoutRecordById(workoutRecordId)
            _uiState.update {
                it.copy(
                    workoutRecord = record,
                    sets = record?.sets ?: emptyList(),
                    isLoading = false
                )
            }
        }
    }

    fun updateSet(setId: Long, weight: Float, reps: Int) {
        _uiState.update { state ->
            val updatedSets = state.sets.map { set ->
                if (set.id == setId) set.copy(weight = weight, reps = reps) else set
            }
            state.copy(sets = updatedSets)
        }
    }

    fun addSet() {
        val currentSets = _uiState.value.sets
        val workoutRecordId = _uiState.value.workoutRecord?.id ?: return

        val lastSet = currentSets.lastOrNull()
        val newSet = WorkoutSet(
            id = 0,
            workoutRecordId = workoutRecordId,
            setNumber = currentSets.size + 1,
            weight = lastSet?.weight ?: 0f,
            reps = lastSet?.reps ?: 0
        )

        viewModelScope.launch {
            val newSetId = workoutRepository.saveWorkoutSet(newSet)
            _uiState.update { state ->
                state.copy(sets = state.sets + newSet.copy(id = newSetId))
            }
        }
    }

    fun deleteSet(setId: Long) {
        viewModelScope.launch {
            workoutRepository.deleteWorkoutSet(setId)
            _uiState.update { state ->
                val remainingSets = state.sets.filter { it.id != setId }
                val reorderedSets = remainingSets.mapIndexed { index, set ->
                    set.copy(setNumber = index + 1)
                }
                reorderedSets.forEach { workoutRepository.updateWorkoutSet(it) }
                state.copy(sets = reorderedSets)
            }
        }
    }

    fun saveAndFinish() {
        viewModelScope.launch {
            _uiState.value.sets.forEach { set ->
                workoutRepository.updateWorkoutSet(set)
            }
            _uiState.update { it.copy(isDone = true) }
        }
    }
}
