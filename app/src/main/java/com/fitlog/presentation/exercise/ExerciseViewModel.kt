package com.fitlog.presentation.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.domain.model.Exercise
import com.fitlog.domain.model.ExerciseCategory
import com.fitlog.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseManageUiState(
    val exercises: List<Exercise> = emptyList(),
    val selectedCategory: ExerciseCategory? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseManageUiState())
    val uiState: StateFlow<ExerciseManageUiState> = _uiState.asStateFlow()

    init {
        loadExercises()
    }

    private fun loadExercises() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val category = _uiState.value.selectedCategory
            val flow = if (category == null) {
                exerciseRepository.getAllExercises()
            } else {
                exerciseRepository.getExercisesByCategory(category)
            }

            flow.collect { exercises ->
                _uiState.update {
                    it.copy(
                        exercises = exercises,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectCategory(category: ExerciseCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
        loadExercises()
    }

    fun addExercise(name: String, category: ExerciseCategory) {
        viewModelScope.launch {
            val newExercise = Exercise(
                name = name,
                category = category,
                isCustom = true
            )
            exerciseRepository.addExercise(newExercise)
        }
    }

    fun updateExerciseName(exercise: Exercise, newName: String) {
        viewModelScope.launch {
            if (exercise.isCustom) {
                exerciseRepository.updateExercise(exercise.copy(name = newName))
            }
        }
    }
}
