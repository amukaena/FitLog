package com.fitlog.presentation.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.fitlog.presentation.components.Dimens
import com.fitlog.presentation.components.FitLogCard
import com.fitlog.presentation.components.FitLogTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitlog.domain.model.WorkoutSet

@Composable
fun WorkoutSetEditScreen(
    workoutRecordId: Long,
    onNavigateBack: () -> Unit,
    viewModel: WorkoutSetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(workoutRecordId) {
        viewModel.loadRecord(workoutRecordId)
    }

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            FitLogTopAppBar(
                title = uiState.workoutRecord?.exercise?.name ?: "세트 편집",
                onNavigateBack = onNavigateBack,
                actions = {
                    TextButton(onClick = { viewModel.saveAndFinish() }) {
                        Text("완료")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimens.ScreenPadding)
        ) {
            Text(
                text = "세트 기록",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(Dimens.SectionSpacing))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.sets, key = { it.id }) { set ->
                    SetEditCard(
                        set = set,
                        onWeightChange = { weight ->
                            viewModel.updateSet(set.id, weight, set.reps)
                        },
                        onRepsChange = { reps ->
                            viewModel.updateSet(set.id, set.weight, reps)
                        },
                        onDelete = { viewModel.deleteSet(set.id) },
                        canDelete = uiState.sets.size > 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SectionSpacing))

            Button(
                onClick = { viewModel.addSet() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(" 세트 추가")
            }
        }
    }
}

@Composable
private fun SetEditCard(
    set: WorkoutSet,
    onWeightChange: (Float) -> Unit,
    onRepsChange: (Int) -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    var weightText by remember(set.id) {
        mutableStateOf(if (set.weight == 0f) "" else set.weight.toString())
    }
    var repsText by remember(set.id) {
        mutableStateOf(if (set.reps == 0) "" else set.reps.toString())
    }

    FitLogCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Dimens.ScreenPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "세트 ${set.setNumber}",
                    style = MaterialTheme.typography.titleSmall
                )

                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "삭제",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing)
            ) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { value ->
                        weightText = value
                        val weight = value.toFloatOrNull()
                        if (weight != null) {
                            onWeightChange(weight)
                        } else if (value.isEmpty()) {
                            onWeightChange(0f)
                        }
                    },
                    label = { Text("무게") },
                    suffix = { Text("kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = repsText,
                    onValueChange = { value ->
                        repsText = value
                        val reps = value.toIntOrNull()
                        if (reps != null) {
                            onRepsChange(reps)
                        } else if (value.isEmpty()) {
                            onRepsChange(0)
                        }
                    },
                    label = { Text("횟수") },
                    suffix = { Text("회") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }
}
