package com.fitlog.domain.model

/**
 * 운동 선택 시 보여주는 특정 운동의 과거 기록 1건.
 * 부하량 증감 비교를 위해 세트 정보를 그대로 유지한다.
 */
data class ExerciseRecentRecord(
    val date: Long,
    val sets: List<WorkoutSet>
) {
    val totalVolume: Double
        get() = sets.sumOf { (it.weight * it.reps).toDouble() }
}
