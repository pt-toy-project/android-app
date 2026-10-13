package com.example.test.data

import com.example.test.Routine.RoutineDate
import org.junit.Assert.*
import org.junit.Test

class RoutineRulesTest {
    private val monday = RoutineDate(2026, 10, 12)
    private val routine = UserRoutine(1, "franklin", "morning_question", "오늘의 선한 일", "07:00", 127, true, false, monday.iso)

    @Test fun validLocalTimeAndUnicodeTitleAreAccepted() {
        RoutineRules.validateRoutine("franklin", "morning_question", "한글과 이모지 🌿", "23:59", 127)
    }

    @Test fun invalidClockTimesAndBlankTitlesAreRejected() {
        listOf("24:00", "12:60", "7:00", "-1:00", "00:00:00").forEach {
            assertThrows(IllegalArgumentException::class.java) { RoutineRules.validateRoutine("hero", "habit", "습관", it, 1) }
        }
        assertThrows(IllegalArgumentException::class.java) { RoutineRules.validateRoutine("hero", "habit", "  ", "07:00", 1) }
    }

    @Test fun invalidWeekdayMasksAndIdsAreRejected() {
        listOf(0, -1, 128).forEach {
            assertThrows(IllegalArgumentException::class.java) { RoutineRules.validateRoutine("hero", "habit", "습관", "07:00", it) }
        }
        assertThrows(IllegalArgumentException::class.java) { RoutineRules.validateRoutine("hero:other", "habit", "습관", "07:00", 1) }
    }

    @Test fun weekdaysUseMondayBitZeroAndSundayBitSix() {
        (0..6).forEach { day ->
            (0..6).forEach { bit ->
                assertEquals(day == bit, RoutineRules.scheduledOn(1 shl bit, monday.plusDays(day)))
            }
        }
    }

    @Test fun repeatingScheduleKeepsLeapAndYearBoundaries() {
        assertTrue(RoutineRules.scheduledOn(8, RoutineDate(2024, 2, 29))) // Thursday
        assertTrue(RoutineRules.scheduledOn(8, RoutineDate(2026, 1, 1)))
        assertFalse(RoutineRules.scheduledOn(0, monday))
    }

    @Test fun firstCompletionOnAdoptionDateIsAllowed() {
        assertNull(RoutineRules.completionFailure(routine, monday, monday, false, 0))
    }

    @Test fun noRewardCanBeFarmedBeforeAdoption() {
        assertEquals(CompletionOutcome.NOT_SCHEDULED, RoutineRules.completionFailure(routine, monday.plusDays(-1), monday, false, 0))
    }

    @Test fun pastScheduledDateSinceAdoptionCanBeRecorded() {
        assertNull(RoutineRules.completionFailure(routine, monday, monday.plusDays(3), false, 0))
    }

    @Test fun futureDateAndOffDayCannotComplete() {
        assertEquals(CompletionOutcome.FUTURE_DATE, RoutineRules.completionFailure(routine, monday.plusDays(1), monday, false, 0))
        assertEquals(CompletionOutcome.NOT_SCHEDULED, RoutineRules.completionFailure(routine.copy(weekdayMask = 2), monday, monday, false, 0))
    }

    @Test fun disabledAndDeletedRoutinesCannotGainNewCompletions() {
        assertEquals(CompletionOutcome.ROUTINE_UNAVAILABLE, RoutineRules.completionFailure(routine.copy(enabled = false), monday, monday, false, 0))
        assertEquals(CompletionOutcome.ROUTINE_UNAVAILABLE, RoutineRules.completionFailure(routine.copy(deleted = true), monday, monday, false, 0))
        assertEquals(CompletionOutcome.ROUTINE_UNAVAILABLE, RoutineRules.completionFailure(null, monday, monday, false, 0))
    }

    @Test fun previouslyCompletedDeletedRoutineStillReturnsDuplicate() {
        assertEquals(CompletionOutcome.ALREADY_COMPLETED, RoutineRules.completionFailure(routine.copy(deleted = true), monday, monday, true, 0))
    }

    @Test fun rewardUsesSignedLongBoundaryWithoutWrapping() {
        assertNull(RoutineRules.completionFailure(routine, monday, monday, false, Long.MAX_VALUE - 10))
        assertEquals(CompletionOutcome.BALANCE_OVERFLOW, RoutineRules.completionFailure(routine, monday, monday, false, Long.MAX_VALUE - 9))
        assertEquals(CompletionOutcome.BALANCE_OVERFLOW, RoutineRules.completionFailure(routine, monday, monday, false, -1))
    }

    @Test fun exactPriceCanBePurchasedButInsufficientBalanceCannot() {
        val item = ShopCatalog.items.first()
        assertNull(RoutineRules.purchaseFailure(item, false, item.price))
        assertEquals(PurchaseOutcome.INSUFFICIENT_FUNDS, RoutineRules.purchaseFailure(item, false, item.price - 1))
    }

    @Test fun ownedAndUnknownItemsNeverChargeAgain() {
        assertEquals(PurchaseOutcome.ALREADY_OWNED, RoutineRules.purchaseFailure(ShopCatalog.items.first(), true, 0))
        assertEquals(PurchaseOutcome.UNKNOWN_ITEM, RoutineRules.purchaseFailure(null, false, Long.MAX_VALUE))
    }

    @Test fun shopIdsAreUniqueAndPricesPositive() {
        assertEquals(ShopCatalog.items.size, ShopCatalog.items.map { it.id }.toSet().size)
        assertTrue(ShopCatalog.items.all { it.price > 0 })
        assertNull(ShopCatalog.find("missing"))
    }
}
