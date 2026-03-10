package com.fitlog.presentation.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitlog.data.local.dao.ExerciseDateStat
import com.fitlog.domain.model.ExerciseCategory
import com.fitlog.presentation.components.FitLogCard
import com.fitlog.presentation.components.FitLogTopAppBar
import com.fitlog.util.DateUtils
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseStatsScreen(
    onBack: () -> Unit,
    viewModel: ExerciseStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            FitLogTopAppBar(
                title = "운동 통계",
                onNavigateBack = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Category filter
            CategoryFilterRow(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = viewModel::selectCategory
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Exercise dropdown
            ExerciseDropdown(
                exercises = uiState.filteredExercises,
                selectedExercise = uiState.selectedExercise,
                onExerciseSelected = viewModel::selectExercise
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Period filter
            PeriodFilterRow(
                selectedPeriod = uiState.selectedPeriod,
                onPeriodSelected = viewModel::selectPeriod
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Chart
            if (uiState.chartData.isNotEmpty()) {
                StatsLineChart(
                    data = uiState.chartData,
                    selectedTab = uiState.selectedTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            } else if (!uiState.isLoading) {
                FitLogCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "기록이 없습니다",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab selector
            TabRow(
                selectedTab = uiState.selectedTab,
                onTabSelected = viewModel::selectTab
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Summary
            if (uiState.summary.totalDays > 0) {
                StatsSummaryCard(summary = uiState.summary)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: ExerciseCategory?,
    onCategorySelected: (ExerciseCategory?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            label = { Text("전체") }
        )
        ExerciseCategory.entries.forEach { category ->
            FilterChip(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                label = { Text(category.displayName) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseDropdown(
    exercises: List<com.fitlog.domain.model.Exercise>,
    selectedExercise: com.fitlog.domain.model.Exercise?,
    onExerciseSelected: (com.fitlog.domain.model.Exercise) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedExercise?.name ?: "운동을 선택하세요",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            exercises.forEach { exercise ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(exercise.name)
                            Text(
                                text = exercise.category.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onExerciseSelected(exercise)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PeriodFilterRow(
    selectedPeriod: StatsPeriod,
    onPeriodSelected: (StatsPeriod) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatsPeriod.entries.forEach { period ->
            FilterChip(
                selected = period == selectedPeriod,
                onClick = { onPeriodSelected(period) },
                label = { Text(period.label) }
            )
        }
    }
}

@Composable
private fun TabRow(
    selectedTab: StatsTab,
    onTabSelected: (StatsTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatsTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatsLineChart(
    data: List<ExerciseDateStat>,
    selectedTab: StatsTab,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val density = LocalDensity.current
    val textSizePx = with(density) { 10.sp.toPx() }

    FitLogCard(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (data.isEmpty()) return@Canvas

            val values = when (selectedTab) {
                StatsTab.MAX_WEIGHT -> data.map { it.maxWeight }
                StatsTab.TOTAL_VOLUME -> data.map { it.totalVolume }
            }

            val paddingLeft = 50f
            val paddingBottom = 30f
            val paddingTop = 10f
            val paddingRight = 10f
            val chartWidth = size.width - paddingLeft - paddingRight
            val chartHeight = size.height - paddingBottom - paddingTop

            val minVal = (values.min() * 0.9f).coerceAtLeast(0f)
            val maxVal = values.max() * 1.1f
            val valueRange = if (maxVal - minVal < 0.01f) 1f else maxVal - minVal

            // Grid lines
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = paddingTop + chartHeight * (1f - i.toFloat() / gridLines)
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(size.width - paddingRight, y),
                    strokeWidth = 1f
                )
                val labelValue = minVal + valueRange * i / gridLines
                drawYLabel(labelValue, paddingLeft - 8f, y, textColor, textSizePx, selectedTab)
            }

            // Data points and line
            if (data.size == 1) {
                val x = paddingLeft + chartWidth / 2
                val y = paddingTop + chartHeight * (1f - (values[0] - minVal) / valueRange)
                drawCircle(lineColor, 5f, Offset(x, y))
                drawXLabel(data[0].date, x, size.height - 4f, textColor, textSizePx)
            } else {
                val path = Path()
                val points = mutableListOf<Offset>()

                data.forEachIndexed { index, _ ->
                    val x = paddingLeft + chartWidth * index / (data.size - 1).toFloat()
                    val value = values[index]
                    val y = paddingTop + chartHeight * (1f - (value - minVal) / valueRange)
                    points.add(Offset(x, y))

                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(path, lineColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))

                points.forEach { point ->
                    drawCircle(lineColor, 4f, point)
                }

                // X-axis labels (show max 5)
                val labelCount = minOf(5, data.size)
                val step = if (data.size <= labelCount) 1 else (data.size - 1) / (labelCount - 1)
                for (i in 0 until data.size step maxOf(1, step)) {
                    val x = paddingLeft + chartWidth * i / (data.size - 1).toFloat()
                    drawXLabel(data[i].date, x, size.height - 4f, textColor, textSizePx)
                }
            }
        }
    }
}

private fun DrawScope.drawYLabel(
    value: Float,
    x: Float,
    y: Float,
    color: Color,
    textSize: Float,
    tab: StatsTab
) {
    val formatter = if (tab == StatsTab.TOTAL_VOLUME && value >= 1000) {
        DecimalFormat("#,##0")
    } else {
        DecimalFormat("#.#")
    }
    val text = formatter.format(value)
    val paint = android.graphics.Paint().apply {
        this.color = color.hashCode()
        this.textSize = textSize
        textAlign = android.graphics.Paint.Align.RIGHT
    }
    drawContext.canvas.nativeCanvas.drawText(text, x, y + textSize / 3, paint)
}

private fun DrawScope.drawXLabel(
    epochMillis: Long,
    x: Float,
    y: Float,
    color: Color,
    textSize: Float
) {
    val text = DateUtils.formatDate(epochMillis, "M/d")
    val paint = android.graphics.Paint().apply {
        this.color = color.hashCode()
        this.textSize = textSize
        textAlign = android.graphics.Paint.Align.CENTER
    }
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}

@Composable
private fun StatsSummaryCard(summary: ExerciseStatsSummary) {
    val numberFormat = remember { DecimalFormat("#,##0.#") }

    FitLogCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "기록 요약",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            SummaryRow(
                label = "최고 중량",
                value = "${numberFormat.format(summary.allTimeMax)}kg" +
                        if (summary.allTimeMaxDate != null) " (${DateUtils.formatDate(summary.allTimeMaxDate, "M/d")})" else ""
            )
            SummaryRow(
                label = "최근 중량",
                value = "${numberFormat.format(summary.latestWeight)}kg"
            )
            SummaryRow(
                label = "총 수행 일수",
                value = "${summary.totalDays}일"
            )
            if (summary.firstRecordDate != null) {
                SummaryRow(
                    label = "처음 기록",
                    value = DateUtils.formatDate(summary.firstRecordDate, "yyyy-MM-dd")
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
