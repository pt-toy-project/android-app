package com.example.test.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.test.Hero.HeroCatalog
import com.example.test.Hero.HeroDayCatalog
import com.example.test.Routine.RoutineDate
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class HeroDayRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "day_${UUID.randomUUID()}.db"
    private val preferences = "day_${UUID.randomUUID()}"
    private val today = RoutineDate(2026, 10, 13)
    private fun repo() = AppRepository(context, name, preferences) { today }
    @After fun clean() { context.deleteDatabase(name); context.deleteSharedPreferences(preferences) }

    @Test fun restartKeepsAllUserSettingsAndSourcesEditIndependently() {
        repo().use { r ->
            val hero = HeroCatalog.default
            val ids = r.startHeroDay(hero)
            assertEquals(8, ids.size)
            val before = r.getRoutine(ids.first())!!.copy(time = "05:23", weekdayMask = 31, enabled = false, alarmEnabled = true)
            r.updateRoutine(before)
            val block = HeroDayCatalog.forHero(hero).blocks.first()
            val personal = r.addRoutine(hero.id, block.id, "개인 제목", "19:30")
            assertEquals(ids, r.startHeroDay(hero))
            assertEquals(before, r.getRoutine(ids.first()))
            assertEquals("19:30", r.getRoutine(personal)!!.time)
            assertEquals(8, r.listRoutines(source = RoutineSource.HERO_DAY).size)
            assertEquals(listOf(personal), r.listRoutines(source = RoutineSource.PERSONAL).map { it.id })
            assertThrows(IllegalArgumentException::class.java) { r.updateRoutine(before.copy(source = RoutineSource.PERSONAL)) }
        }
    }
    @Test fun crossSourceCompletionPaysOnceAndKeepsDateGuards() {
        repo().use { r ->
            val hero = HeroCatalog.default
            val day = r.startHeroDay(hero).first()
            val block = HeroDayCatalog.forHero(hero).blocks.first()
            val personal = r.addRoutine(hero.id, block.id, block.title, "07:00")
            assertEquals(CompletionOutcome.NOT_SCHEDULED, r.complete(day, today.plusDays(-1)))
            assertEquals(CompletionOutcome.FUTURE_DATE, r.complete(day, today.plusDays(1)))
            assertEquals(CompletionOutcome.COMPLETED, r.complete(day, today))
            assertEquals(CompletionOutcome.ALREADY_COMPLETED, r.complete(personal, today))
            assertEquals(CompletionOutcome.ALREADY_COMPLETED, r.complete(day, today))
            assertEquals(setOf(day, personal), r.completedRoutineIds(today))
            assertEquals(10L, r.balance()); assertEquals(1, r.history().size)
        }
    }
    @Test fun failedBlockInsertRollsBackWholeDayAndSelection() {
        repo().use { r ->
            r.selectedHeroId = "franklin"
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use {
                it.execSQL("CREATE TRIGGER fail_day BEFORE INSERT ON user_routines WHEN NEW.template_id='sejong_day_meal' BEGIN SELECT RAISE(ABORT,'injected'); END")
            }
            assertThrows(Exception::class.java) { r.startHeroDay(HeroCatalog.default) }
            assertTrue(r.listRoutines().isEmpty()); assertEquals("franklin", r.selectedHeroId); assertEquals(0L, r.balance())
        }
    }
    @Test fun actualV1FileUpgradesWithoutChangingRecordsWalletOrPurchases() {
        val target = context.getDatabasePath(name)
        target.parentFile!!.mkdirs()
        InstrumentationRegistry.getInstrumentation().context.assets.open("historical_routines_v1.db").use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        val settings = context.getSharedPreferences("app_settings_fixture_$preferences", Context.MODE_PRIVATE)
        try {
            assertTrue(settings.edit().putBoolean("alarm_enabled", false).commit())
            repo().use { r ->
                val routine = r.getRoutine(41)!!
                assertEquals(RoutineSource.PERSONAL, routine.source)
                assertEquals("사용자 제목 🌿", routine.title); assertEquals("06:15", routine.time)
                assertEquals(31, routine.weekdayMask); assertFalse(routine.enabled); assertTrue(routine.alarmEnabled)
                assertEquals(80L, r.balance()); assertEquals("이전 제목", r.history().single().title)
                assertEquals(41L, r.history().single().routineId)
                assertEquals("apprentice_scholar", r.appliedItemId()); assertTrue(r.ownedItemIds().contains("apprentice_scholar"))
                assertEquals(1, r.legacyHistoryCount()); assertEquals("franklin", r.selectedHeroId)
                assertTrue(r.startHeroDay(HeroCatalog.default).all { it > 41 })
            }
            assertFalse(settings.getBoolean("alarm_enabled", true))
            SQLiteDatabase.openDatabase(target.path, null, SQLiteDatabase.OPEN_READONLY).use { assertEquals(4, it.version) }
        } finally { context.deleteSharedPreferences("app_settings_fixture_$preferences") }
    }
}
