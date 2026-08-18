package com.example.test.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.test.data.entity.HeroEntity

@Dao
interface HeroDao {

    @Insert
    suspend fun insertHero(hero: HeroEntity)

    @Query("SELECT * FROM hero")
    suspend fun getAllHeroes(): List<HeroEntity>

    @Query("SELECT * FROM hero WHERE hero_id = :heroId")
    suspend fun getHeroById(heroId: Int): HeroEntity?
}