package com.fitlog.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

// 위젯 색상 정의
private val WidgetBackground = Color(0xFF1A1A1A)
private val WorkoutDayColor = Color(0xFF4CAF50)
private val TodayColor = Color(0xFF81C784)
private val EmptyDayColor = Color(0xFF2D2D2D)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFB0B0B0)

@Composable
fun FitLogWidgetContent(
    widgetData: WidgetData,
    onWidgetClick: Action
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetBackground)
            .cornerRadius(16.dp)
            .clickable(onWidgetClick)
            .padding(12.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            // 헤더
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FitLog",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(TextPrimary)
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "${widgetData.totalWorkoutDays}/14일",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = ColorProvider(WorkoutDayColor)
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // 요일 헤더
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                listOf("월", "화", "수", "목", "금", "토", "일").forEach { day ->
                    Box(
                        modifier = GlanceModifier.defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = ColorProvider(TextSecondary),
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // 첫째 주 (7일)
            if (widgetData.days.size >= 7) {
                WeekRow(days = widgetData.days.take(7))
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // 둘째 주 (7일)
            if (widgetData.days.size >= 14) {
                WeekRow(days = widgetData.days.drop(7).take(7))
            }
        }
    }
}

@Composable
private fun WeekRow(days: List<DayInfo>) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        days.forEach { day ->
            DayCell(
                day = day,
                modifier = GlanceModifier.defaultWeight()
            )
        }
    }
}

@Composable
private fun DayCell(
    day: DayInfo,
    modifier: GlanceModifier
) {
    val backgroundColor = when {
        day.isToday -> TodayColor
        day.hasWorkout -> WorkoutDayColor
        else -> EmptyDayColor
    }

    Box(
        modifier = modifier.padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .size(28.dp)
                .background(backgroundColor)
                .cornerRadius(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.dayOfMonth.toString(),
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                    color = ColorProvider(TextPrimary),
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}
