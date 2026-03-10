package com.fitlog

import android.app.Application
import com.fitlog.domain.repository.ExerciseRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FitLogApplication : Application() {

    @Inject
    lateinit var exerciseRepository: ExerciseRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            exerciseRepository.initializeDefaultExercises()
        }
    }
}
