package com.fitlog.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.domain.model.Exercise
import com.fitlog.domain.model.ExerciseDateStat
import com.fitlog.domain.repository.ExerciseRepository
import com.fitlog.domain.repository.StatsRepository
import com.fitlog.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class StatsPeriod(val label: String, val weeks: Int?) {
    WEEKS_4("4주", 4),
    WEEKS_8("8주", 8),
    WEEKS_12("12주", 12),
    ALL("전체", null)
}

enum class StatsTab(val label: String) {
    MAX_WEIGHT("최고 중량"),
    TOTAL_VOLUME("총 볼륨")
}

data class ExerciseStatsSummary(
    val allTimeMax: Float = 0f,
    val allTimeMaxDate: Long? = null,
    val latestWeight: Float = 0f,
    val totalDays: Int = 0,
    val firstRecordDate: Long? = null
)

data class ExerciseStatsUiState(
    val exercises: List<Exercise> = emptyList(),
    val selectedCategory: com.fitlog.domain.model.ExerciseCategory? = null,
    val selectedExercise: Exercise? = null,
    val selectedPeriod: StatsPeriod = StatsPeriod.WEEKS_8,
    val selectedTab: StatsTab = StatsTab.MAX_WEIGHT,
    val chartData: List<ExerciseDateStat> = emptyList(),
    val summary: ExerciseStatsSummary = ExerciseStatsSummary(),
    val isLoading: Boolean = false
) {
    val filteredExercises: List<Exercise>
        get() = selectedCategory?.let { cat ->
            exercises.filter { it.category == cat }
        } ?: exercises
}

@HiltViewModel
class ExerciseStatsViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseStatsUiState())
    val uiState: StateFlow<ExerciseStatsUiState> = _uiState.asStateFlow()

    init {
        loadExercises()
    }

    private fun loadExercises() {
        viewModelScope.launch {
            val exercises = exerciseRepository.getAllExercises().first()
            _uiState.update { it.copy(exercises = exercises) }
            if (exercises.isNotEmpty() && _uiState.value.selectedExercise == null) {
                selectExercise(exercises.first())
            }
        }
    }

    fun selectCategory(category: com.fitlog.domain.model.ExerciseCategory?) {
        _uiState.update { state ->
            val newState = state.copy(selectedCategory = category)
            // 현재 선택된 운동이 필터에 포함되지 않으면 필터된 목록의 첫 번째로 변경
            val filtered = newState.filteredExercises
            if (state.selectedExercise != null && !filtered.contains(state.selectedExercise)) {
                newState.copy(selectedExercise = filtered.firstOrNull())
            } else {
                newState
            }
        }
        // 선택된 운동이 변경되었을 수 있으므로 통계 다시 로드
        if (_uiState.value.selectedExercise != null) {
            loadStats()
        }
    }

    fun selectExercise(exercise: Exercise) {
        _uiState.update { it.copy(selectedExercise = exercise) }
        loadStats()
    }

    fun selectPeriod(period: StatsPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadStats()
    }

    fun selectTab(tab: StatsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    private fun loadStats() {
        val exercise = _uiState.value.selectedExercise ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val startDate = when (val weeks = _uiState.value.selectedPeriod.weeks) {
                null -> 0L
                else -> DateUtils.localDateToEpochMillis(
                    LocalDate.now().minusWeeks(weeks.toLong())
                )
            }

            val chartData = statsRepository.getExerciseStatsByDate(exercise.id, startDate)
            val allTimeMax = statsRepository.getExerciseAllTimeMax(exercise.id) ?: 0f
            val allTimeMaxDate = statsRepository.getExerciseAllTimeMaxDate(exercise.id)
            val latestWeight = statsRepository.getExerciseLatestMaxWeight(exercise.id) ?: 0f
            val totalDays = statsRepository.getExerciseTotalDays(exercise.id)
            val firstRecordDate = statsRepository.getExerciseFirstRecordDate(exercise.id)

            _uiState.update {
                it.copy(
                    chartData = chartData,
                    summary = ExerciseStatsSummary(
                        allTimeMax = allTimeMax,
                        allTimeMaxDate = allTimeMaxDate,
                        latestWeight = latestWeight,
                        totalDays = totalDays,
                        firstRecordDate = firstRecordDate
                    ),
                    isLoading = false
                )
            }
        }
    }
}
