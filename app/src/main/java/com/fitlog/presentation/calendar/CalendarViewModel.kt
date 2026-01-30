package com.fitlog.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.domain.model.DailyWorkout
import com.fitlog.domain.repository.ExerciseRepository
import com.fitlog.domain.repository.WorkoutRepository
import com.fitlog.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CalendarUiState(
    val currentYear: Int = LocalDate.now().year,
    val currentMonth: Int = LocalDate.now().monthValue,
    val selectedDate: Long = DateUtils.todayEpochMillis(),
    val workoutDates: Set<Long> = emptySet(),
    val selectedDayWorkout: DailyWorkout? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        initializeDefaultExercises()
        loadMonthData()
        loadSelectedDayWorkout()
    }

    private fun initializeDefaultExercises() {
        viewModelScope.launch {
            exerciseRepository.initializeDefaultExercises()
        }
    }

    fun selectDate(date: Long) {
        _uiState.update { it.copy(selectedDate = date) }
        loadSelectedDayWorkout()
    }

    fun previousMonth() {
        _uiState.update { state ->
            val newMonth = if (state.currentMonth == 1) 12 else state.currentMonth - 1
            val newYear = if (state.currentMonth == 1) state.currentYear - 1 else state.currentYear
            state.copy(currentYear = newYear, currentMonth = newMonth)
        }
        loadMonthData()
    }

    fun nextMonth() {
        _uiState.update { state ->
            val newMonth = if (state.currentMonth == 12) 1 else state.currentMonth + 1
            val newYear = if (state.currentMonth == 12) state.currentYear + 1 else state.currentYear
            state.copy(currentYear = newYear, currentMonth = newMonth)
        }
        loadMonthData()
    }

    private fun loadMonthData() {
        viewModelScope.launch {
            val (startDate, endDate) = DateUtils.getMonthStartAndEnd(
                _uiState.value.currentYear,
                _uiState.value.currentMonth
            )

            workoutRepository.getWorkoutDatesInRange(startDate, endDate).collect { dates ->
                _uiState.update { it.copy(workoutDates = dates.toSet()) }
            }
        }
    }

    private fun loadSelectedDayWorkout() {
        viewModelScope.launch {
            workoutRepository.getDailyWorkoutByDate(_uiState.value.selectedDate).collect { workout ->
                _uiState.update { it.copy(selectedDayWorkout = workout) }
            }
        }
    }

    fun deleteDailyWorkout(id: Long) {
        viewModelScope.launch {
            workoutRepository.deleteDailyWorkout(id)
        }
    }

    fun refresh() {
        loadMonthData()
        loadSelectedDayWorkout()
    }
}
