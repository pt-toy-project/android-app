package com.example.test.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.test.data.entity.RoutineEntity

@Dao
interface RoutineDao {

    @Insert
    suspend fun insertRoutine(routine: RoutineEntity)

    @Query("SELECT * FROM routine WHERE hero_id = :heroId")
    suspend fun getRoutinesByHero(heroId: Int): List<RoutineEntity>

    @Query("SELECT * FROM routine WHERE is_active = 1")
    suspend fun getActiveRoutines(): List<RoutineEntity>

    @Query("SELECT * FROM routine WHERE routine_id = :routineId")
    suspend fun getRoutineById(routineId: Int): RoutineEntity?
}