package com.fitlog.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.updateAll
import com.fitlog.MainActivity
import com.fitlog.data.local.FitLogDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class FitLogWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetData = loadWidgetData(context)

        provideContent {
            FitLogWidgetContent(
                widgetData = widgetData,
                onWidgetClick = actionStartActivity<MainActivity>()
            )
        }
    }

    private suspend fun loadWidgetData(context: Context): WidgetData {
        return withContext(Dispatchers.IO) {
            try {
                val db = FitLogDatabase.getDatabase(context)
                val today = LocalDate.now()

                // 이번주 일요일 계산 (요일 헤더 "일월화수목금토"와 일치시키기 위함)
                // Java DayOfWeek: 1=월 ~ 7=일
                val dayOfWeek = today.dayOfWeek.value
                val daysFromSunday = if (dayOfWeek == 7) 0 else dayOfWeek
                val thisWeekSunday = today.minusDays(daysFromSunday.toLong())
                // 지난주 일요일부터 시작 (2주간 표시)
                val startDate = thisWeekSunday.minusWeeks(1)
                // 이번주 토요일까지
                val endDate = thisWeekSunday.plusDays(6)

                val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endMillis = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

                val workoutDates = db.dailyWorkoutDao().getWorkoutDatesInRangeSync(startMillis, endMillis).toSet()

                // 2주간 날짜 목록 생성
                val days = (0..13).map { daysAgo ->
                    val date = startDate.plusDays(daysAgo.toLong())
                    val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    // 해당 날짜에 운동 기록이 있는지 확인
                    val hasWorkout = workoutDates.any { workoutDate ->
                        val workoutLocalDate = java.time.Instant.ofEpochMilli(workoutDate)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        workoutLocalDate == date
                    }

                    DayInfo(
                        date = dateMillis,
                        dayOfMonth = date.dayOfMonth,
                        dayOfWeek = getDayOfWeekKorean(date.dayOfWeek.value),
                        hasWorkout = hasWorkout,
                        isToday = date == today
                    )
                }

                WidgetData(
                    days = days,
                    totalWorkoutDays = days.count { it.hasWorkout }
                )
            } catch (e: Exception) {
                WidgetData(days = emptyList(), totalWorkoutDays = 0)
            }
        }
    }

    private fun getDayOfWeekKorean(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "월"
            2 -> "화"
            3 -> "수"
            4 -> "목"
            5 -> "금"
            6 -> "토"
            7 -> "일"
            else -> ""
        }
    }

    companion object {
        suspend fun updateWidget(context: Context) {
            FitLogWidget().updateAll(context)
        }
    }
}

data class WidgetData(
    val days: List<DayInfo>,
    val totalWorkoutDays: Int
)

data class DayInfo(
    val date: Long,
    val dayOfMonth: Int,
    val dayOfWeek: String,
    val hasWorkout: Boolean,
    val isToday: Boolean
)
