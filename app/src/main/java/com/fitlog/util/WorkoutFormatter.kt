package com.fitlog.util

import com.fitlog.domain.model.DailyWorkout
import com.fitlog.domain.model.ExerciseCategory
import com.fitlog.domain.model.WorkoutRecord
import com.fitlog.domain.model.WorkoutSet

data class CategoryVolume(
    val category: ExerciseCategory,
    val volume: Double,
    val setCount: Int,
    val exerciseCount: Int
)

fun calculateCategoryVolumes(records: List<WorkoutRecord>): List<CategoryVolume> {
    return records.groupBy { it.exercise.category }
        .map { (category, categoryRecords) ->
            CategoryVolume(
                category = category,
                volume = categoryRecords.sumOf { record ->
                    record.sets.sumOf { set -> (set.weight * set.reps).toDouble() }
                },
                setCount = categoryRecords.sumOf { it.sets.size },
                exerciseCount = categoryRecords.size
            )
        }
        .sortedByDescending { it.volume }
}

fun WorkoutSet.formatDisplay(): String = "${formatWeight(weight)}kg x $reps"

fun List<WorkoutSet>.formatSummary(): String = joinToString(" / ") { it.formatDisplay() }

private fun formatWeight(weight: Float): String {
    return if (weight == weight.toLong().toFloat()) {
        weight.toLong().toString()
    } else {
        weight.toString()
    }
}

object WorkoutFormatter {

    fun formatForAI(workout: DailyWorkout): String {
        val sb = StringBuilder()

        // 헤더
        sb.appendLine("## 오늘의 운동 기록")
        sb.appendLine("- 날짜: ${DateUtils.formatDateForAI(workout.date)}")
        sb.appendLine("- 제목: ${workout.title}")
        sb.appendLine()

        // 운동 목록
        sb.appendLine("### 운동 목록")
        sb.appendLine()

        if (workout.records.isEmpty()) {
            sb.appendLine("(기록된 운동 없음)")
        } else {
            workout.records.forEachIndexed { index, record ->
                sb.appendLine("${index + 1}. ${record.exercise.name} (${record.exercise.category.displayName})")
                record.sets.forEach { set ->
                    sb.appendLine("   - ${set.setNumber}세트: ${formatWeight(set.weight)}kg × ${set.reps}회")
                }
                sb.appendLine()
            }
        }

        // 요약
        sb.appendLine("### 요약")
        val totalSets = workout.records.sumOf { it.sets.size }
        val totalVolume = workout.records.sumOf { record ->
            record.sets.sumOf { set -> (set.weight * set.reps).toDouble() }
        }
        val categoryCount = workout.records.groupBy { it.exercise.category }
            .map { (category, records) -> "${category.displayName} ${records.size}종목" }
            .joinToString(", ")

        sb.appendLine("- 총 운동 종목: ${workout.records.size}개")
        sb.appendLine("- 총 세트 수: ${totalSets}세트")
        sb.appendLine("- 총 볼륨: ${formatVolume(totalVolume)}kg (무게 × 반복수 합계)")
        if (categoryCount.isNotEmpty()) {
            sb.appendLine("- 부위별: $categoryCount")
        }

        // 메모
        if (!workout.memo.isNullOrBlank()) {
            sb.appendLine()
            sb.appendLine("### 메모")
            sb.appendLine(workout.memo)
        }

        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine("위 운동 기록에 대해 질문해주세요:")

        return sb.toString()
    }

    private fun formatWeight(weight: Float): String = com.fitlog.util.formatWeight(weight)

    fun formatVolume(volume: Double): String {
        return if (volume == volume.toLong().toDouble()) {
            String.format("%,d", volume.toLong())
        } else {
            String.format("%,.1f", volume)
        }
    }
}
