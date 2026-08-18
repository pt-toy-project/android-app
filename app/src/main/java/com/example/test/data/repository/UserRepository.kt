package com.example.test.data.repository

import com.example.test.data.dao.UserDao
import com.example.test.data.entity.UserEntity

class UserRepository(private val userDao: UserDao) {

    suspend fun login(userId: String, password: String): UserEntity? {
        return userDao.login(userId, password)
    }

    suspend fun getUserByUid(userUid: Int): UserEntity? {
        return userDao.getUserByUid(userUid)
    }

    suspend fun registerUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    suspend fun updateCoinBalance(userUid: Int, newBalance: Int) {
        userDao.updateCoinBalance(userUid, newBalance)
    }

    suspend fun selectHero(userUid: Int, heroId: Int) {
        userDao.updateCurrentHero(userUid, heroId)
    }
}