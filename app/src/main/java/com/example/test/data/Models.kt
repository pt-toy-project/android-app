package com.example.test.data

import com.example.test.Routine.RoutineDate

/** A user-owned snapshot: editorial template changes never rename an existing routine. */
data class UserRoutine(
    val id: Long,
    val heroId: String,
    val templateId: String,
    val title: String,
    val time: String,
    /** Monday = bit 0, Sunday = bit 6. */
    val weekdayMask: Int,
    val enabled: Boolean,
    val alarmEnabled: Boolean,
    val createdOn: String = "",
    val deleted: Boolean = false
)

data class CompletionRecord(
    val routineId: Long,
    val date: RoutineDate,
    val title: String,
    val heroId: String,
    val coins: Long
)

data class LegacyCompletion(val heroId: String, val date: String, val sampleId: String)

enum class CompletionOutcome {
    COMPLETED, ALREADY_COMPLETED, FUTURE_DATE, ROUTINE_UNAVAILABLE, NOT_SCHEDULED, BALANCE_OVERFLOW
}

enum class PurchaseOutcome { PURCHASED, ALREADY_OWNED, INSUFFICIENT_FUNDS, UNKNOWN_ITEM }

data class ShopItem(val id: String, val title: String, val description: String, val price: Long)

object ShopCatalog {
    val items: List<ShopItem> = listOf(
        ShopItem("apprentice_scholar", "견습 학자", "홈에 표시되는 견습 학자 칭호", 30),
        ShopItem("steady_explorer", "꾸준한 탐구자", "홈에 표시되는 꾸준한 탐구자 칭호", 60),
        ShopItem("daily_historian", "일상의 역사가", "홈에 표시되는 일상의 역사가 칭호", 100)
    )
    fun find(id: String): ShopItem? = items.firstOrNull { it.id == id }
}
