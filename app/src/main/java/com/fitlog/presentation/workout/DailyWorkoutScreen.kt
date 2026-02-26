package com.fitlog.presentation.workout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.fitlog.presentation.components.FitLogTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitlog.domain.model.WorkoutRecord
import com.fitlog.presentation.components.CopyWorkoutBottomSheet
import com.fitlog.presentation.components.Dimens
import com.fitlog.presentation.components.ExerciseSelectBottomSheet
import com.fitlog.util.CategoryVolume
import com.fitlog.util.DateUtils
import com.fitlog.util.WorkoutFormatter
import com.fitlog.util.calculateCategoryVolumes
import com.fitlog.util.formatSummary
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DailyWorkoutScreen(
    date: Long,
    onNavigateBack: () -> Unit,
    onNavigateToSetEdit: (Long) -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExerciseSheet by remember { mutableStateOf(false) }
    var showCopySheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(date) {
        viewModel.loadWorkout(date)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            FitLogTopAppBar(
                title = "운동 기록",
                onNavigateBack = onNavigateBack,
                actions = {
                    if (uiState.records.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val success = viewModel.copyWorkoutForAI()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (success) "운동 기록이 클립보드에 복사되었습니다"
                                        else "복사할 운동 기록이 없습니다"
                                    )
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "AI 질문용 복사"
                            )
                        }
                    }
                    if (uiState.dailyWorkout != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "삭제",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    TextButton(onClick = { viewModel.saveWorkout() }) {
                        Text("저장")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimens.ScreenPadding)
        ) {
            Text(
                text = DateUtils.formatDate(date),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Dimens.SectionSpacing))

            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("오늘의 운동 제목") },
                placeholder = { Text("예: 가슴/삼두") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(Dimens.ItemSpacing))

            OutlinedTextField(
                value = uiState.memo,
                onValueChange = viewModel::updateMemo,
                label = { Text("메모 (선택)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(Dimens.SectionSpacing))

            // 부위별 볼륨 요약
            val categoryVolumes = remember(uiState.records) {
                calculateCategoryVolumes(uiState.records)
            }
            val totalVolume = remember(categoryVolumes) {
                categoryVolumes.sumOf { it.volume }
            }
            if (totalVolume > 0) {
                VolumeSummarySection(
                    categoryVolumes = categoryVolumes,
                    totalVolume = totalVolume
                )
                Spacer(modifier = Modifier.height(Dimens.SectionSpacing))
            }

            val lazyListState = rememberLazyListState()
            val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
                viewModel.moveRecord(from.index, to.index)
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
            ) {
                itemsIndexed(
                    items = uiState.records,
                    key = { _, record -> record.id }
                ) { _, record ->
                    ReorderableItem(reorderableLazyListState, key = record.id) { isDragging ->
                        WorkoutRecordEditCard(
                            record = record,
                            isDragging = isDragging,
                            onEditClick = { onNavigateToSetEdit(record.id) },
                            onDeleteClick = { viewModel.deleteRecord(record.id) },
                            modifier = Modifier.longPressDraggableHandle()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SectionSpacing))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
            ) {
                Button(
                    onClick = { showExerciseSheet = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(" 운동 추가")
                }

                OutlinedButton(
                    onClick = { showCopySheet = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Text(" 이전 기록 복사")
                }
            }
        }
    }

    if (showExerciseSheet) {
        ExerciseSelectBottomSheet(
            exercises = uiState.exercises,
            onDismiss = { showExerciseSheet = false },
            onExerciseSelected = { exercise ->
                viewModel.addExercise(exercise)
                showExerciseSheet = false
            }
        )
    }

    if (showCopySheet) {
        CopyWorkoutBottomSheet(
            recentWorkouts = uiState.recentWorkouts,
            onDismiss = { showCopySheet = false },
            onWorkoutSelected = { workout ->
                viewModel.copyFromPreviousWorkout(workout.id)
                showCopySheet = false
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("운동 기록 삭제") },
            text = { Text("이 날의 운동 기록을 삭제하시겠습니까?\n모든 운동 및 세트 정보가 삭제됩니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDailyWorkout()
                        showDeleteDialog = false
                    }
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
private fun WorkoutRecordEditCard(
    record: WorkoutRecord,
    isDragging: Boolean = false,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isDragging) Modifier.shadow(8.dp, RoundedCornerShape(12.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 드래그 핸들
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "순서 변경",
                modifier = modifier
                    .padding(end = 12.dp)
                    .size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(modifier = Modifier.weight(1f)) {
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
                    Text(
                        text = record.sets.formatSummary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                TextButton(onClick = onEditClick) {
                    Text("편집")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "삭제",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun VolumeSummarySection(
    categoryVolumes: List<CategoryVolume>,
    totalVolume: Double
) {
    Card(
        shape = RoundedCornerShape(Dimens.CardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(Dimens.ScreenPadding)) {
            Text(
                text = "부위별 볼륨",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(Dimens.ItemSpacing))

            categoryVolumes.forEach { cv ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = cv.category.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${WorkoutFormatter.formatVolume(cv.volume)}kg",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                    Text(
                        text = "${cv.setCount}세트",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )

            val totalSets = categoryVolumes.sumOf { it.setCount }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "합계",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${WorkoutFormatter.formatVolume(totalVolume)}kg",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
                Text(
                    text = "${totalSets}세트",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.6f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}
