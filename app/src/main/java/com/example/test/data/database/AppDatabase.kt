package com.example.test.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.test.data.dao.DailyRecordDao
import com.example.test.data.dao.HeroDao
import com.example.test.data.dao.RoutineDao
import com.example.test.data.dao.UserDao
import com.example.test.data.entity.DailyRecordEntity
import com.example.test.data.entity.HeroEntity
import com.example.test.data.entity.RoutineEntity
import com.example.test.data.entity.UserEntity

@Database(
    entities = [
        HeroEntity::class,
        UserEntity::class,
        RoutineEntity::class,
        DailyRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun heroDao(): HeroDao
    abstract fun userDao(): UserDao
    abstract fun routineDao(): RoutineDao
    abstract fun dailyRecordDao(): DailyRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}