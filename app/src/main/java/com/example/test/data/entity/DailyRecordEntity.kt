package com.example.test.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "daily_record",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_uid"],
            childColumns = ["user_uid"]
        ),
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["routine_id"],
            childColumns = ["routine_id"]
        )
    ]
)
data class DailyRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val record_id: Int = 0,
    val user_uid: Int,
    val routine_id: Int,
    val date: String,
    val is_completed: Boolean = false
)