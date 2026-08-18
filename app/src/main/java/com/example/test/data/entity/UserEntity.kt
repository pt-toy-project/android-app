package com.example.test.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "user",
    foreignKeys = [
        ForeignKey(
            entity = HeroEntity::class,
            parentColumns = ["hero_id"],
            childColumns = ["current_hero_id"]
        )
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val user_uid: Int = 0,
    val user_id: String,
    val password: String,
    val current_hero_id: Int?,
    val user_name: String,
    val coin_bal: Int = 0
)