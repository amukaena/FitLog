package com.fitlog.data.repository

import com.fitlog.data.local.dao.StatsDao
import com.fitlog.domain.model.ExerciseDateStat
import com.fitlog.domain.repository.StatsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepositoryImpl @Inject constructor(
    private val statsDao: StatsDao
) : StatsRepository {

    override suspend fun getExerciseStatsByDate(exerciseId: Long, startDate: Long): List<ExerciseDateStat> {
        return statsDao.getExerciseStatsByDate(exerciseId, startDate).map { it.toDomain() }
    }

    override suspend fun getExerciseAllTimeMax(exerciseId: Long): Float? {
        return statsDao.getExerciseAllTimeMax(exerciseId)
    }

    override suspend fun getExerciseAllTimeMaxDate(exerciseId: Long): Long? {
        return statsDao.getExerciseAllTimeMaxDate(exerciseId)
    }

    override suspend fun getExerciseLatestMaxWeight(exerciseId: Long): Float? {
        return statsDao.getExerciseLatestMaxWeight(exerciseId)
    }

    override suspend fun getExerciseTotalDays(exerciseId: Long): Int {
        return statsDao.getExerciseTotalDays(exerciseId)
    }

    override suspend fun getExerciseFirstRecordDate(exerciseId: Long): Long? {
        return statsDao.getExerciseFirstRecordDate(exerciseId)
    }
}

private fun com.fitlog.data.local.dao.ExerciseDateStat.toDomain() = ExerciseDateStat(
    date = date,
    maxWeight = maxWeight,
    totalVolume = totalVolume,
    totalSets = totalSets
)
