package com.example.test

import android.database.sqlite.SQLiteDatabase
import com.example.test.Hero.HeroCatalog
import com.example.test.Routine.RoutineDate
import com.example.test.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

/** Execute AppRepository and SQLiteOpenHelper, complementing host SQL and real-device tests. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RepositoryUpgradeBehaviorTest {
    private val today = RoutineDate(2026, 10, 13)

    @Test fun actualV1FileUpgradesAndRetainsIdentityHistoryWalletAndItems() {
        val context = RuntimeEnvironment.getApplication()
        val name = "upgrade_${UUID.randomUUID()}.db"
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        javaClass.getResourceAsStream("/historical_routines_v1.db")!!.use { input ->
            path.outputStream().use { input.copyTo(it) }
        }
        try {
            AppRepository(context, name, "preferences_$name") { today }.use { repo ->
                val existing = repo.getRoutine(41)!!
                assertEquals(RoutineSource.PERSONAL, existing.source)
                assertEquals("사용자 제목 🌿", existing.title)
                assertEquals("06:15", existing.time)
                assertEquals(31, existing.weekdayMask)
                assertFalse(existing.enabled)
                assertTrue(existing.alarmEnabled)
                assertEquals(80L, repo.balance())
                assertEquals("이전 제목", repo.history().single().title)
                assertEquals(41L, repo.history().single().routineId)
                assertEquals("apprentice_scholar", repo.appliedItemId())
                assertEquals(setOf("apprentice_scholar"), repo.ownedItemIds())
                assertEquals(1, repo.legacyHistoryCount())
                assertEquals("franklin", repo.selectedHeroId)
            }
            SQLiteDatabase.openDatabase(path.path, null, SQLiteDatabase.OPEN_READONLY).use { assertEquals(3, it.version) }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun wholeDayRestartRetainsSettingsAndCrossSourceCompletionPaysOnce() {
        val context = RuntimeEnvironment.getApplication()
        val name = "day_${UUID.randomUUID()}.db"
        try {
            AppRepository(context, name, "preferences_$name") { today }.use { repo ->
                val hero = HeroCatalog.default
                val ids = repo.startHeroDay(hero)
                val original = repo.getRoutine(ids[1])!!.copy(time = "05:42", weekdayMask = 31, alarmEnabled = true)
                repo.updateRoutine(original)
                assertEquals(ids, repo.startHeroDay(hero))
                assertEquals(original, repo.getRoutine(ids[1]))
                val block = repo.getRoutine(ids.first())!!
                val personal = repo.addRoutine(block.heroId, block.templateId, "내 실천", "19:30")
                assertEquals(CompletionOutcome.COMPLETED, repo.complete(block.id, today))
                assertEquals(CompletionOutcome.ALREADY_COMPLETED, repo.complete(personal, today))
                assertEquals(10L, repo.balance())
                val own = repo.addPersonalPlan("개인 계획 🌿", "08:15", 127)
                assertEquals(RoutineIdentity.PERSONAL_HERO_ID, repo.getRoutine(own)!!.heroId)
                assertEquals(RoutineSource.PERSONAL, repo.getRoutine(own)!!.source)
                assertEquals(8, repo.listRoutines(source = RoutineSource.HERO_DAY).size)
                assertEquals(2, repo.listRoutines(source = RoutineSource.PERSONAL).size)
                assertEquals(CompletionOutcome.NOT_SCHEDULED, repo.complete(own, today.plusDays(-1)))
                assertEquals(CompletionOutcome.FUTURE_DATE, repo.complete(own, today.plusDays(1)))
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun injectedBlockFailureRollsBackEntireAdoptionAndSelection() {
        val context = RuntimeEnvironment.getApplication()
        val name = "rollback_${UUID.randomUUID()}.db"
        try {
            AppRepository(context, name, "preferences_$name") { today }.use { repo ->
                repo.selectedHeroId = "franklin"
                SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use {
                    it.execSQL("CREATE TRIGGER fail_day BEFORE INSERT ON user_routines WHEN NEW.template_id='sejong_day_meal' BEGIN SELECT RAISE(ABORT,'injected'); END")
                }
                assertThrows(Exception::class.java) { repo.startHeroDay(HeroCatalog.default) }
                assertTrue(repo.listRoutines().isEmpty())
                assertEquals(0L, repo.balance())
                assertEquals("franklin", repo.selectedHeroId)
            }
        } finally { context.deleteDatabase(name) }
    }
}
