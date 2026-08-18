package com.example.test.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "routine",
    foreignKeys = [
        ForeignKey(
            entity = HeroEntity::class,
            parentColumns = ["hero_id"],
            childColumns = ["hero_id"]
        )
    ]
)
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val routine_id: Int = 0,
    val hero_id: Int?,      // 타입 뒤에 ?가 붙으면 "값이 NULL일 수도 있다" (위인 선택 안한 경우)
    val routine_name: String,
    val target_time: String,
    val is_active: Boolean = true
)