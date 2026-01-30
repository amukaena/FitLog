package com.fitlog.data.repository

import com.fitlog.data.local.dao.DailyWorkoutDao
import com.fitlog.data.local.dao.ExerciseDao
import com.fitlog.data.local.dao.WorkoutRecordDao
import com.fitlog.data.local.dao.WorkoutSetDao
import com.fitlog.data.local.entity.WorkoutRecordEntity
import com.fitlog.data.local.entity.WorkoutSetEntity
import com.fitlog.data.local.entity.toDomain
import com.fitlog.data.local.entity.toEntity
import com.fitlog.domain.model.DailyWorkout
import com.fitlog.domain.model.WorkoutRecord
import com.fitlog.domain.model.WorkoutSet
import com.fitlog.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WorkoutRepositoryImpl @Inject constructor(
    private val dailyWorkoutDao: DailyWorkoutDao,
    private val workoutRecordDao: WorkoutRecordDao,
    private val workoutSetDao: WorkoutSetDao,
    private val exerciseDao: ExerciseDao
) : WorkoutRepository {

    override fun getDailyWorkoutByDate(date: Long): Flow<DailyWorkout?> {
        return dailyWorkoutDao.getDailyWorkoutByDateFlow(date).map { entity ->
            entity?.let { loadFullDailyWorkout(it.id) }
        }
    }

    override fun getWorkoutDatesInRange(startDate: Long, endDate: Long): Flow<List<Long>> {
        return dailyWorkoutDao.getWorkoutDatesInRange(startDate, endDate)
    }

    override fun getRecentDailyWorkouts(limit: Int): Flow<List<DailyWorkout>> {
        return dailyWorkoutDao.getRecentDailyWorkouts(limit).map { entities ->
            entities.mapNotNull { loadFullDailyWorkout(it.id) }
        }
    }

    override fun getAllDailyWorkouts(): Flow<List<DailyWorkout>> {
        return dailyWorkoutDao.getAllDailyWorkouts().map { entities ->
            entities.mapNotNull { loadFullDailyWorkout(it.id) }
        }
    }

    override suspend fun getDailyWorkoutByDateSync(date: Long): DailyWorkout? {
        val entity = dailyWorkoutDao.getDailyWorkoutByDate(date)
        return entity?.let { loadFullDailyWorkout(it.id) }
    }

    override suspend fun getDailyWorkoutById(id: Long): DailyWorkout? {
        return loadFullDailyWorkout(id)
    }

    private suspend fun loadFullDailyWorkout(dailyWorkoutId: Long): DailyWorkout? {
        val dailyWorkoutEntity = dailyWorkoutDao.getDailyWorkoutById(dailyWorkoutId) ?: return null
        val recordEntities = workoutRecordDao.getRecordsByDailyWorkoutIdSync(dailyWorkoutId)

        val records = recordEntities.map { recordEntity ->
            val exercise = exerciseDao.getExerciseById(recordEntity.exerciseId)?.toDomain()
                ?: return@map null
            val sets = workoutSetDao.getSetsByRecordIdSync(recordEntity.id).map { it.toDomain() }
            recordEntity.toDomain(exercise, sets)
        }.filterNotNull()

        return dailyWorkoutEntity.toDomain(records)
    }

    override suspend fun saveDailyWorkout(dailyWorkout: DailyWorkout): Long {
        return dailyWorkoutDao.insert(dailyWorkout.toEntity())
    }

    override suspend fun updateDailyWorkout(dailyWorkout: DailyWorkout) {
        dailyWorkoutDao.update(dailyWorkout.toEntity().copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteDailyWorkout(id: Long) {
        dailyWorkoutDao.deleteById(id)
    }

    override suspend fun getWorkoutRecordById(id: Long): WorkoutRecord? {
        val recordEntity = workoutRecordDao.getRecordById(id) ?: return null
        val exercise = exerciseDao.getExerciseById(recordEntity.exerciseId)?.toDomain() ?: return null
        val sets = workoutSetDao.getSetsByRecordIdSync(id).map { it.toDomain() }
        return recordEntity.toDomain(exercise, sets)
    }

    override suspend fun saveWorkoutRecord(record: WorkoutRecord): Long {
        exerciseDao.updateLastUsedAt(record.exercise.id, System.currentTimeMillis())
        return workoutRecordDao.insert(record.toEntity())
    }

    override suspend fun updateWorkoutRecord(record: WorkoutRecord) {
        workoutRecordDao.update(record.toEntity())
    }

    override suspend fun deleteWorkoutRecord(id: Long) {
        workoutRecordDao.deleteById(id)
    }

    override suspend fun saveWorkoutSet(set: WorkoutSet): Long {
        return workoutSetDao.insert(set.toEntity())
    }

    override suspend fun updateWorkoutSet(set: WorkoutSet) {
        workoutSetDao.update(set.toEntity())
    }

    override suspend fun deleteWorkoutSet(id: Long) {
        workoutSetDao.deleteById(id)
    }

    override suspend fun copyDailyWorkout(sourceDailyWorkoutId: Long, targetDate: Long): Long {
        val sourceDailyWorkout = loadFullDailyWorkout(sourceDailyWorkoutId) ?: return -1

        val newDailyWorkout = sourceDailyWorkout.copy(
            id = 0,
            date = targetDate,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val newDailyWorkoutId = dailyWorkoutDao.insert(newDailyWorkout.toEntity())

        sourceDailyWorkout.records.forEachIndexed { index, record ->
            val newRecordEntity = WorkoutRecordEntity(
                id = 0,
                dailyWorkoutId = newDailyWorkoutId,
                exerciseId = record.exercise.id,
                order = index,
                createdAt = System.currentTimeMillis()
            )
            val newRecordId = workoutRecordDao.insert(newRecordEntity)

            val newSets = record.sets.map { set ->
                WorkoutSetEntity(
                    id = 0,
                    workoutRecordId = newRecordId,
                    setNumber = set.setNumber,
                    weight = set.weight,
                    reps = set.reps
                )
            }
            workoutSetDao.insertAll(newSets)
        }

        return newDailyWorkoutId
    }

    override suspend fun clearAllData() {
        workoutSetDao.deleteAll()
        workoutRecordDao.deleteAll()
        dailyWorkoutDao.deleteAll()
    }
}
