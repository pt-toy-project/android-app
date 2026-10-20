package com.example.test

import com.example.test.Hero.HeroCatalog
import com.example.test.Hero.HeroDayCatalog
import com.example.test.Routine.RoutineDate
import com.example.test.data.AppRepository
import com.example.test.data.CompletionOutcome
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CalendarRepositoryTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val date = RoutineDate(2024, 2, 29)
    @Test fun sourceAdoptionsCannotDoubleCountRewardAndDeletedHistoryRemains() {
        AppRepository(context, todayProvider = { date }).use { repo ->
            val hero = HeroCatalog.default
            val block = HeroDayCatalog.forHero(hero).blocks.first()
            val id = repo.startHeroDay(hero).first()
            val personal = repo.addRoutine(hero.id, block.id, "내 실천", "08:00")
            assertTrue(repo.setQuestChecked(id, block.quests.first().id, date, true))
            assertEquals(CompletionOutcome.COMPLETED, repo.complete(id, date))
            assertEquals(CompletionOutcome.ALREADY_COMPLETED, repo.complete(personal, date))
            val before = repo.calendarActivity(date, date).single()
            assertEquals(8, before.planned)
            assertEquals(1, before.completed); assertEquals(1, before.checked); assertEquals(10L, before.coins)
            repo.deleteRoutine(id); repo.deleteRoutine(personal)
            val after = repo.calendarActivity(date, date).single()
            assertEquals(before.completed, after.completed); assertEquals(before.checked, after.checked)
            assertEquals(before.coins, after.coins); assertEquals(10L, repo.balance())
        }
    }
    @Test fun monthlySummaryHasNoHistoryPaginationLimitAndDoesNotChangeWallet() {
        val start = RoutineDate(2024, 1, 1); val end = RoutineDate(2024, 1, 31)
        AppRepository(context, todayProvider = { start }).use { repo ->
            val ids = (1..4).map { repo.addPersonalPlan("실천 $it", "08:00") }
            repeat(31) { day -> ids.forEach { assertEquals(CompletionOutcome.COMPLETED, repo.complete(it, start.plusDays(day), end)) } }
            assertEquals(100, repo.history().size)
            repeat(2) {
                val month = repo.calendarActivity(start, end)
                assertEquals(124, month.sumOf { it.completed })
                assertEquals(1240L, month.sumOf { it.coins })
                assertEquals(31, month.count { it.active })
                assertEquals(1240L, repo.balance())
            }
        }
    }
    @Test fun scheduleRespectsAdoptionDateWeekdayAndDisabledPlans() {
        AppRepository(context, todayProvider = { date }).use { repo ->
            val id = repo.addPersonalPlan("목요일", "08:00")
            val routine = repo.getRoutine(id)!!
            repo.updateRoutine(routine.copy(weekdayMask = 8))
            val rows = repo.calendarActivity(date.plusDays(-1), date.plusDays(1))
            assertEquals(listOf(0, 1, 0), rows.map { it.planned })
            assertTrue(rows.all { !it.active && it.coins == 0L })
            repo.updateRoutine(routine.copy(enabled = false))
            assertEquals(0, repo.calendarActivity(date, date).single().planned)
        }
    }
    @Test fun checklistOnlyDayIsActiveWithoutCompletionOrCoins() {
        AppRepository(context, todayProvider = { date }).use { repo ->
            val hero = HeroCatalog.default; val id = repo.startHeroDay(hero).first()
            val quest = HeroDayCatalog.forHero(hero).blocks.first().quests.first()
            assertTrue(repo.setQuestChecked(id, quest.id, date, true))
            val stat = repo.calendarActivity(date, date).single()
            assertTrue(stat.active); assertEquals(0, stat.completed); assertEquals(0L, stat.coins)
            assertEquals(1, stat.checked)
        }
    }
}
