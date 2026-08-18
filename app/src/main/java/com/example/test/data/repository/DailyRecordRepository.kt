package com.example.test.data.repository

import com.example.test.data.dao.DailyRecordDao
import com.example.test.data.entity.DailyRecordEntity

class DailyRecordRepository(private val dailyRecordDao: DailyRecordDao) {

    suspend fun getTodayRecords(userUid: Int, date: String): List<DailyRecordEntity> {
        return dailyRecordDao.getTodayRecords(userUid, date)
    }

    suspend fun getCompletedCountToday(userUid: Int, date: String): Int {
        return dailyRecordDao.getCompletedCountToday(userUid, date)
    }

    suspend fun getAllRecordsByUser(userUid: Int): List<DailyRecordEntity> {
        return dailyRecordDao.getAllRecordsByUser(userUid)
    }

    suspend fun addRecord(record: DailyRecordEntity) {
        dailyRecordDao.insertRecord(record)
    }

    suspend fun updateRecord(record: DailyRecordEntity) {
        dailyRecordDao.updateRecord(record)
    }
}