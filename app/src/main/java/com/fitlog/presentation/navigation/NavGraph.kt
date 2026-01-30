package com.fitlog.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitlog.presentation.calendar.CalendarScreen
import com.fitlog.presentation.exercise.ExerciseManageScreen
import com.fitlog.presentation.settings.GoogleDriveBackupScreen
import com.fitlog.presentation.settings.SettingsScreen
import com.fitlog.presentation.workout.DailyWorkoutScreen
import com.fitlog.presentation.workout.WorkoutSetEditScreen

@Composable
fun FitLogNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Calendar.route
    ) {
        composable(Screen.Calendar.route) {
            CalendarScreen(
                onNavigateToWorkout = { date ->
                    navController.navigate(Screen.DailyWorkout.createRoute(date))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.DailyWorkout.route,
            arguments = listOf(
                navArgument("date") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getLong("date") ?: System.currentTimeMillis()
            DailyWorkoutScreen(
                date = date,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSetEdit = { workoutRecordId ->
                    navController.navigate(Screen.WorkoutSetEdit.createRoute(workoutRecordId))
                }
            )
        }

        composable(
            route = Screen.WorkoutSetEdit.route,
            arguments = listOf(
                navArgument("workoutRecordId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val workoutRecordId = backStackEntry.arguments?.getLong("workoutRecordId") ?: 0L
            WorkoutSetEditScreen(
                workoutRecordId = workoutRecordId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ExerciseManage.route) {
            ExerciseManageScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToExerciseManage = {
                    navController.navigate(Screen.ExerciseManage.route)
                },
                onNavigateToGoogleDriveBackup = {
                    navController.navigate(Screen.GoogleDriveBackup.route)
                }
            )
        }

        composable(Screen.GoogleDriveBackup.route) {
            GoogleDriveBackupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
