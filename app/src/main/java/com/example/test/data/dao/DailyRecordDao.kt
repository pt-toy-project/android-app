package com.example.test.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.test.data.entity.DailyRecordEntity

@Dao
interface DailyRecordDao {

    @Insert
    suspend fun insertRecord(record: DailyRecordEntity)

    @Update
    suspend fun updateRecord(record: DailyRecordEntity)

    @Query("SELECT * FROM daily_record WHERE user_uid = :userUid AND date = :date")
    suspend fun getTodayRecords(userUid: Int, date: String): List<DailyRecordEntity>

    @Query("SELECT COUNT(*) FROM daily_record WHERE user_uid = :userUid AND date = :date AND is_completed = 1")
    suspend fun getCompletedCountToday(userUid: Int, date: String): Int

    @Query("SELECT * FROM daily_record WHERE user_uid = :userUid ORDER BY date DESC")
    suspend fun getAllRecordsByUser(userUid: Int): List<DailyRecordEntity>
}