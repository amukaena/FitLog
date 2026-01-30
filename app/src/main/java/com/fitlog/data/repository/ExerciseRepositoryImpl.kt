package com.fitlog.data.repository

import com.fitlog.data.local.dao.ExerciseDao
import com.fitlog.data.local.entity.ExerciseEntity
import com.fitlog.data.local.entity.toDomain
import com.fitlog.data.local.entity.toEntity
import com.fitlog.domain.model.Exercise
import com.fitlog.domain.model.ExerciseCategory
import com.fitlog.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao
) : ExerciseRepository {

    override fun getAllExercises(): Flow<List<Exercise>> {
        return exerciseDao.getAllExercises().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getExercisesByCategory(category: ExerciseCategory): Flow<List<Exercise>> {
        return exerciseDao.getExercisesByCategory(category.displayName).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchExercises(query: String): Flow<List<Exercise>> {
        return exerciseDao.searchExercises(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getExerciseById(id: Long): Exercise? {
        return exerciseDao.getExerciseById(id)?.toDomain()
    }

    override suspend fun addExercise(exercise: Exercise): Long {
        return exerciseDao.insert(exercise.toEntity())
    }

    override suspend fun updateExercise(exercise: Exercise) {
        exerciseDao.update(exercise.toEntity())
    }

    override suspend fun updateLastUsedAt(id: Long) {
        exerciseDao.updateLastUsedAt(id, System.currentTimeMillis())
    }

    override suspend fun initializeDefaultExercises() {
        if (exerciseDao.getCount() > 0) return

        val defaultExercises = listOf(
            // 가슴
            ExerciseEntity(name = "벤치프레스", category = "가슴"),
            ExerciseEntity(name = "인클라인 벤치프레스", category = "가슴"),
            ExerciseEntity(name = "덤벨 플라이", category = "가슴"),
            ExerciseEntity(name = "케이블 크로스오버", category = "가슴"),
            ExerciseEntity(name = "푸시업", category = "가슴"),
            // 등
            ExerciseEntity(name = "데드리프트", category = "등"),
            ExerciseEntity(name = "랫풀다운", category = "등"),
            ExerciseEntity(name = "바벨로우", category = "등"),
            ExerciseEntity(name = "시티드로우", category = "등"),
            ExerciseEntity(name = "풀업", category = "등"),
            // 어깨
            ExerciseEntity(name = "오버헤드프레스", category = "어깨"),
            ExerciseEntity(name = "사이드 레터럴 레이즈", category = "어깨"),
            ExerciseEntity(name = "프론트 레이즈", category = "어깨"),
            ExerciseEntity(name = "페이스풀", category = "어깨"),
            // 팔
            ExerciseEntity(name = "바벨컬", category = "팔"),
            ExerciseEntity(name = "덤벨컬", category = "팔"),
            ExerciseEntity(name = "트라이셉스 익스텐션", category = "팔"),
            ExerciseEntity(name = "케이블 푸시다운", category = "팔"),
            ExerciseEntity(name = "해머컬", category = "팔"),
            // 하체
            ExerciseEntity(name = "스쿼트", category = "하체"),
            ExerciseEntity(name = "레그프레스", category = "하체"),
            ExerciseEntity(name = "레그익스텐션", category = "하체"),
            ExerciseEntity(name = "레그컬", category = "하체"),
            ExerciseEntity(name = "런지", category = "하체"),
            ExerciseEntity(name = "카프레이즈", category = "하체"),
            // 코어
            ExerciseEntity(name = "플랭크", category = "코어"),
            ExerciseEntity(name = "크런치", category = "코어"),
            ExerciseEntity(name = "레그레이즈", category = "코어"),
            ExerciseEntity(name = "러시안 트위스트", category = "코어")
        )

        exerciseDao.insertAll(defaultExercises)
    }
}
