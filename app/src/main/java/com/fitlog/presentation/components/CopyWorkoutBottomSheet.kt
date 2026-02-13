package com.fitlog.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitlog.domain.model.DailyWorkout
import com.fitlog.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyWorkoutBottomSheet(
    recentWorkouts: List<DailyWorkout>,
    onDismiss: () -> Unit,
    onWorkoutSelected: (DailyWorkout) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPadding)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "이전 기록 복사",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(Dimens.ItemSpacing))

            Text(
                text = "최근 운동 기록에서 선택하세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Dimens.SectionSpacing))

            if (recentWorkouts.isEmpty()) {
                Text(
                    text = "복사할 수 있는 이전 기록이 없습니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(400.dp)
                ) {
                    items(recentWorkouts) { workout ->
                        RecentWorkoutCard(
                            workout = workout,
                            onClick = { onWorkoutSelected(workout) }
                        )
                        Spacer(modifier = Modifier.height(Dimens.ItemSpacing))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentWorkoutCard(
    workout: DailyWorkout,
    onClick: () -> Unit
) {
    FitLogCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(Dimens.ScreenPadding)) {
            Text(
                text = DateUtils.formatDateWithDayOfWeek(workout.date),
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = workout.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )

            if (workout.records.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = workout.records.joinToString(", ") { it.exercise.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
