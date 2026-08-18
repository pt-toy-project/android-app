package com.example.test.data.repository

import com.example.test.data.dao.HeroDao
import com.example.test.data.entity.HeroEntity

class HeroRepository(private val heroDao: HeroDao) { /* "HeroRepository를 만들 때, HeroDao 하나를 필수로 받는다"라는 의미
                                                        Repository는 Dao없이는 아무것도 못하니깐 만들 때, 항상 Dao를 같이 넘겨야 함
                                                        private은 이 값은 이 클래스 안에서만 쓴다는 의미
                                                        */
    suspend fun getAllHeroes(): List<HeroEntity> {
        return heroDao.getAllHeroes()
    }

    suspend fun getHeroById(heroId: Int): HeroEntity? {
        return heroDao.getHeroById(heroId)
    }

    suspend fun insertHero(hero: HeroEntity) {
        heroDao.insertHero(hero)
    }
}