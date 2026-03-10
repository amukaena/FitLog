package com.fitlog.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.data.local.dao.ExerciseDateStat
import com.fitlog.data.local.dao.StatsDao
import com.fitlog.domain.model.Exercise
import com.fitlog.domain.repository.ExerciseRepository
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
    val selectedExercise: Exercise? = null,
    val selectedPeriod: StatsPeriod = StatsPeriod.WEEKS_8,
    val selectedTab: StatsTab = StatsTab.MAX_WEIGHT,
    val chartData: List<ExerciseDateStat> = emptyList(),
    val summary: ExerciseStatsSummary = ExerciseStatsSummary(),
    val isLoading: Boolean = false
)

@HiltViewModel
class ExerciseStatsViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val statsDao: StatsDao
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

            val chartData = statsDao.getExerciseStatsByDate(exercise.id, startDate)
            val allTimeMax = statsDao.getExerciseAllTimeMax(exercise.id) ?: 0f
            val allTimeMaxDate = statsDao.getExerciseAllTimeMaxDate(exercise.id)
            val latestWeight = statsDao.getExerciseLatestMaxWeight(exercise.id) ?: 0f
            val totalDays = statsDao.getExerciseTotalDays(exercise.id)
            val firstRecordDate = statsDao.getExerciseFirstRecordDate(exercise.id)

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
