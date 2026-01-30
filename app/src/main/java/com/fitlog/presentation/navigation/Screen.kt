package com.fitlog.presentation.navigation

sealed class Screen(val route: String) {
    object Calendar : Screen("calendar")
    object DailyWorkout : Screen("daily_workout/{date}") {
        fun createRoute(date: Long) = "daily_workout/$date"
    }
    object WorkoutSetEdit : Screen("workout_set_edit/{workoutRecordId}") {
        fun createRoute(workoutRecordId: Long) = "workout_set_edit/$workoutRecordId"
    }
    object ExerciseManage : Screen("exercise_manage")
    object Settings : Screen("settings")
    object GoogleDriveBackup : Screen("google_drive_backup")
}
