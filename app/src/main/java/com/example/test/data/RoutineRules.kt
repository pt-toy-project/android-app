package com.example.test.data

import com.example.test.Routine.RoutineDate
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import java.util.Date

/** Pure validation shared by SQLite writes and executable JVM regression tests. */
object RoutineRules {
    const val COMPLETION_REWARD: Long = 10

    fun validateRoutine(heroId: String, templateId: String, title: String, time: String, weekdayMask: Int) {
        require(heroId.matches(Regex("[a-zA-Z0-9_-]{1,80}"))) { "Invalid hero ID" }
        require(templateId.matches(Regex("[a-zA-Z0-9_-]{1,120}"))) { "Invalid template ID" }
        require(title.trim().length in 1..160) { "Title must contain 1 to 160 characters" }
        require(time.matches(Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]"))) { "Use HH:mm local time" }
        require(weekdayMask in 1..127) { "Select at least one weekday" }
    }

    fun scheduledOn(mask: Int, date: RoutineDate): Boolean {
        if (mask !in 1..127) return false
        val calendar = GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT).apply {
            gregorianChange = Date(Long.MIN_VALUE)
            clear()
            set(date.year, date.month - 1, date.day)
        }
        val mondayIndex = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
        return mask and (1 shl mondayIndex) != 0
    }

    fun completionFailure(routine: UserRoutine?, date: RoutineDate, today: RoutineDate, completed: Boolean, balance: Long): CompletionOutcome? = when {
        date > today -> CompletionOutcome.FUTURE_DATE
        completed -> CompletionOutcome.ALREADY_COMPLETED
        routine == null || routine.deleted || !routine.enabled -> CompletionOutcome.ROUTINE_UNAVAILABLE
        RoutineDate.parse(routine.createdOn)?.let { date < it } == true -> CompletionOutcome.NOT_SCHEDULED
        !scheduledOn(routine.weekdayMask, date) -> CompletionOutcome.NOT_SCHEDULED
        balance < 0 || balance > Long.MAX_VALUE - COMPLETION_REWARD -> CompletionOutcome.BALANCE_OVERFLOW
        else -> null
    }

    fun purchaseFailure(item: ShopItem?, owned: Boolean, balance: Long): PurchaseOutcome? = when {
        item == null -> PurchaseOutcome.UNKNOWN_ITEM
        owned -> PurchaseOutcome.ALREADY_OWNED
        balance < item.price -> PurchaseOutcome.INSUFFICIENT_FUNDS
        else -> null
    }
}
