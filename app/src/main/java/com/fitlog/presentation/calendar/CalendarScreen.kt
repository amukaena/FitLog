package com.fitlog.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitlog.domain.model.DailyWorkout
import com.fitlog.domain.model.WorkoutRecord
import com.fitlog.presentation.components.Dimens
import com.fitlog.presentation.components.FitLogCard
import com.fitlog.presentation.components.FitLogTopAppBar
import com.fitlog.presentation.theme.CalendarSelected
import com.fitlog.presentation.theme.CalendarToday
import com.fitlog.presentation.theme.WorkoutMarker
import com.fitlog.util.DAYS_OF_WEEK_KOREAN
import com.fitlog.util.DateUtils
import com.fitlog.util.formatSummary
import java.time.LocalDate

@Composable
fun CalendarScreen(
    onNavigateToWorkout: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            FitLogTopAppBar(
                title = "FitLog",
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "설정")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToWorkout(uiState.selectedDate) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "운동 추가")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CalendarHeader(
                year = uiState.currentYear,
                month = uiState.currentMonth,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth
            )

            CalendarGrid(
                year = uiState.currentYear,
                month = uiState.currentMonth,
                selectedDate = uiState.selectedDate,
                workoutDates = uiState.workoutDates,
                onDateSelected = viewModel::selectDate
            )

            Spacer(modifier = Modifier.height(Dimens.SectionSpacing))

            SelectedDateWorkout(
                selectedDate = uiState.selectedDate,
                workout = uiState.selectedDayWorkout,
                onEditClick = { onNavigateToWorkout(uiState.selectedDate) }
            )
        }
    }
}

@Composable
private fun CalendarHeader(
    year: Int,
    month: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.ItemSpacing),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "이전 달"
            )
        }

        Text(
            text = "${year}년 ${month}월",
            style = MaterialTheme.typography.titleLarge
        )

        IconButton(onClick = onNextMonth) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "다음 달"
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    year: Int,
    month: Int,
    selectedDate: Long,
    workoutDates: Set<Long>,
    onDateSelected: (Long) -> Unit
) {
    val daysOfWeek = DAYS_OF_WEEK_KOREAN
    val daysInMonth = DateUtils.getDaysInMonth(year, month)
    val firstDayOfWeek = DateUtils.getFirstDayOfWeekInMonth(year, month)
    val today = DateUtils.todayEpochMillis()

    Column(modifier = Modifier.padding(horizontal = Dimens.ItemSpacing)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.ItemSpacing))

        val totalCells = firstDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val day = cellIndex - firstDayOfWeek + 1

                    if (day in 1..daysInMonth) {
                        val date = DateUtils.localDateToEpochMillis(LocalDate.of(year, month, day))
                        val isSelected = date == selectedDate
                        val isToday = date == today
                        val hasWorkout = workoutDates.contains(date)

                        CalendarDay(
                            day = day,
                            isSelected = isSelected,
                            isToday = isToday,
                            hasWorkout = hasWorkout,
                            modifier = Modifier.weight(1f),
                            onClick = { onDateSelected(date) }
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasWorkout: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> CalendarSelected
                    isToday -> CalendarToday.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
            )
            if (hasWorkout && !isSelected) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(WorkoutMarker)
                )
            }
        }
    }
}

@Composable
private fun SelectedDateWorkout(
    selectedDate: Long,
    workout: DailyWorkout?,
    onEditClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = DateUtils.formatDateWithDayOfWeek(selectedDate),
                style = MaterialTheme.typography.titleMedium
            )

            if (workout != null) {
                Text(
                    text = workout.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.ItemSpacing))

        if (workout == null || workout.records.isEmpty()) {
            FitLogCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditClick)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "운동 기록이 없습니다\n탭하여 추가하세요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
            ) {
                items(workout.records) { record ->
                    WorkoutRecordCard(record = record)
                }
            }
        }
    }
}

@Composable
private fun WorkoutRecordCard(record: WorkoutRecord) {
    FitLogCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Dimens.ScreenPadding)) {
            Text(
                text = record.exercise.name,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${record.sets.size}세트",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (record.sets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = record.sets.formatSummary(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
