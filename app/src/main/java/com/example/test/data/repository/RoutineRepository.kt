package com.example.test.data.repository

import com.example.test.data.dao.RoutineDao
import com.example.test.data.entity.RoutineEntity

class RoutineRepository(private val routineDao: RoutineDao) {

    suspend fun getRoutinesByHero(heroId: Int): List<RoutineEntity> {
        return routineDao.getRoutinesByHero(heroId)
    }

    suspend fun getActiveRoutines(): List<RoutineEntity> {
        return routineDao.getActiveRoutines()
    }

    suspend fun getRoutineById(routineId: Int): RoutineEntity? {
        return routineDao.getRoutineById(routineId)
    }

    suspend fun addRoutine(routine: RoutineEntity) {
        routineDao.insertRoutine(routine)
    }
}