package com.fitlog.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

object DateUtils {
    private val zoneId = ZoneId.systemDefault()
    private val koreanLocale = Locale.KOREAN
    private val weekFields = WeekFields.of(Locale.KOREAN)

    fun localDateToEpochMillis(localDate: LocalDate): Long {
        return localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun epochMillisToLocalDate(epochMillis: Long): LocalDate {
        return Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
    }

    fun todayEpochMillis(): Long {
        return localDateToEpochMillis(LocalDate.now())
    }

    fun formatDate(epochMillis: Long, pattern: String = "yyyy년 M월 d일"): String {
        val localDate = epochMillisToLocalDate(epochMillis)
        return localDate.format(DateTimeFormatter.ofPattern(pattern, koreanLocale))
    }

    fun formatDateWithDayOfWeek(epochMillis: Long): String {
        val localDate = epochMillisToLocalDate(epochMillis)
        val formatter = DateTimeFormatter.ofPattern("M월 d일 (E)", koreanLocale)
        return localDate.format(formatter)
    }

    fun getMonthStartAndEnd(year: Int, month: Int): Pair<Long, Long> {
        val startDate = LocalDate.of(year, month, 1)
        val endDate = startDate.plusMonths(1).minusDays(1)
        return localDateToEpochMillis(startDate) to localDateToEpochMillis(endDate)
    }

    fun getWeekRange(date: LocalDate): Pair<Long, Long> {
        val startOfWeek = date.with(weekFields.dayOfWeek(), 1)
        val endOfWeek = startOfWeek.plusDays(6)
        return localDateToEpochMillis(startOfWeek) to localDateToEpochMillis(endOfWeek)
    }

    fun getTwoWeeksRange(): Pair<Long, Long> {
        val today = LocalDate.now()
        val startOfThisWeek = today.with(weekFields.dayOfWeek(), 1)
        val startOfLastWeek = startOfThisWeek.minusWeeks(1)
        val endOfThisWeek = startOfThisWeek.plusDays(6)
        return localDateToEpochMillis(startOfLastWeek) to localDateToEpochMillis(endOfThisWeek)
    }

    fun getDaysInMonth(year: Int, month: Int): Int {
        return LocalDate.of(year, month, 1).lengthOfMonth()
    }

    fun getFirstDayOfWeekInMonth(year: Int, month: Int): Int {
        val firstDay = LocalDate.of(year, month, 1)
        return firstDay.dayOfWeek.value % 7
    }

    fun formatRelativeDate(epochMillis: Long): String {
        val today = LocalDate.now()
        val date = epochMillisToLocalDate(epochMillis)
        val daysDiff = today.toEpochDay() - date.toEpochDay()

        return when {
            daysDiff == 0L -> "오늘"
            daysDiff == 1L -> "어제"
            daysDiff < 7 -> "${daysDiff}일 전"
            daysDiff < 30 -> "${daysDiff / 7}주 전"
            else -> formatDate(epochMillis, "M월 d일")
        }
    }
}
