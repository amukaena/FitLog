package com.fitlog.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitlog.domain.model.Exercise
import com.fitlog.domain.model.ExerciseCategory
import com.fitlog.domain.model.ExerciseRecentRecord
import com.fitlog.util.DateUtils
import com.fitlog.util.WorkoutFormatter
import com.fitlog.util.formatSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSelectBottomSheet(
    exercises: List<Exercise>,
    recentSummaries: Map<Long, String> = emptyMap(),
    recentHistories: Map<Long, List<ExerciseRecentRecord>> = emptyMap(),
    onDismiss: () -> Unit,
    onExerciseSelected: (Exercise) -> Unit,
    onRequestRecentHistory: (Long) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var expandedExerciseId by remember { mutableStateOf<Long?>(null) }

    val categories = listOf("전체") + ExerciseCategory.entries.map { it.displayName }

    val filteredExercises = exercises.filter { exercise ->
        val matchesSearch = searchQuery.isEmpty() ||
                exercise.name.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategoryIndex == 0 ||
                exercise.category.displayName == categories[selectedCategoryIndex]
        matchesSearch && matchesCategory
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "운동 선택",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("검색...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            ScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                edgePadding = 16.dp
            ) {
                categories.forEachIndexed { index, category ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = { selectedCategoryIndex = index },
                        text = { Text(category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(filteredExercises, key = { it.id }) { exercise ->
                    val isExpanded = expandedExerciseId == exercise.id
                    ExerciseSelectItem(
                        exercise = exercise,
                        summary = recentSummaries[exercise.id],
                        isExpanded = isExpanded,
                        history = recentHistories[exercise.id],
                        onClick = { onExerciseSelected(exercise) },
                        onToggleHistory = {
                            expandedExerciseId = if (isExpanded) null else exercise.id
                            if (!isExpanded) onRequestRecentHistory(exercise.id)
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ExerciseSelectItem(
    exercise: Exercise,
    summary: String?,
    isExpanded: Boolean,
    history: List<ExerciseRecentRecord>?,
    onClick: () -> Unit,
    onToggleHistory: () -> Unit
) {
    Column {
        ListItem(
            headlineContent = { Text(exercise.name) },
            supportingContent = {
                if (summary != null) {
                    Text(
                        text = "${exercise.category.displayName} · $summary",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(exercise.category.displayName)
                }
            },
            trailingContent = {
                IconButton(onClick = onToggleHistory) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.History,
                        contentDescription = if (isExpanded) "최근 기록 닫기" else "최근 기록 보기",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            modifier = Modifier.clickable(onClick = onClick)
        )

        AnimatedVisibility(visible = isExpanded) {
            RecentHistorySection(history = history)
        }
    }
}

@Composable
private fun RecentHistorySection(history: List<ExerciseRecentRecord>?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.ItemSpacing)
    ) {
        when {
            history == null -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }

            history.isEmpty() -> {
                Text(
                    text = "최근 기록이 없습니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> {
                Text(
                    text = "최근 기록 ${history.size}건",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Dimens.ItemSpacing))

                history.forEachIndexed { index, record ->
                    // 목록은 최신순이므로, 다음 항목이 직전(더 오래된) 기록이다.
                    val previous = history.getOrNull(index + 1)
                    RecentHistoryRow(record = record, previous = previous)
                    if (index != history.lastIndex) {
                        Spacer(modifier = Modifier.height(Dimens.ItemSpacing))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentHistoryRow(
    record: ExerciseRecentRecord,
    previous: ExerciseRecentRecord?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = DateUtils.formatRelativeDate(record.date),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.width(56.dp)
        )

        Spacer(modifier = Modifier.width(Dimens.ItemSpacing))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.sets.formatSummary(),
                style = MaterialTheme.typography.bodySmall
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "볼륨 ${WorkoutFormatter.formatVolume(record.totalVolume)}kg",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val previousVolume = previous?.totalVolume
                if (previousVolume != null && previousVolume > 0.0) {
                    val diff = record.totalVolume - previousVolume
                    val percent = diff / previousVolume * 100
                    val isUp = diff > 0
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (diff == 0.0) "(유지)"
                        else "(${if (isUp) "▲" else "▼"} ${String.format("%.0f", kotlin.math.abs(percent))}%)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            diff > 0 -> MaterialTheme.colorScheme.primary
                            diff < 0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}
