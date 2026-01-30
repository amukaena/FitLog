package com.fitlog.domain.model

data class Exercise(
    val id: Long = 0,
    val name: String,
    val category: ExerciseCategory,
    val isCustom: Boolean = false,
    val lastUsedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ExerciseCategory(val displayName: String) {
    CHEST("가슴"),
    BACK("등"),
    SHOULDER("어깨"),
    ARMS("팔"),
    LEGS("하체"),
    CORE("코어");

    companion object {
        fun fromDisplayName(name: String): ExerciseCategory {
            return entries.find { it.displayName == name } ?: CHEST
        }
    }
}
