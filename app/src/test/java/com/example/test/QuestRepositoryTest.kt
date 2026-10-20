package com.example.test

import android.database.sqlite.SQLiteDatabase
import com.example.test.Hero.HeroCatalog
import com.example.test.Hero.HeroDayCatalog
import com.example.test.Routine.RoutineDate
import com.example.test.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class QuestRepositoryTest {
    private val today = RoutineDate(2026, 10, 13)
    private val context get() = RuntimeEnvironment.getApplication()

    @Test fun checklistSurvivesReopenAndRestartWithoutEditingSettingsOrPayingCoins() {
        val name = "quests_${UUID.randomUUID()}.db"
        try {
            var routineId = 0L
            val hero = HeroCatalog.default
            val quest = HeroDayCatalog.forHero(hero).blocks.first().quests.first().id
            AppRepository(context, name, "prefs_$name") { today }.use { repo ->
                routineId = repo.startHeroDay(hero).first()
                repo.updateRoutine(repo.getRoutine(routineId)!!.copy(time = "05:42", alarmEnabled = true))
                assertTrue(repo.setQuestChecked(routineId, quest, today, true))
                assertTrue(repo.setQuestChecked(routineId, quest, today, true))
                assertEquals(0L, repo.balance())
                assertTrue(repo.history().isEmpty())
            }
            AppRepository(context, name, "prefs_$name") { today }.use { repo ->
                assertEquals(routineId, repo.startHeroDay(hero).first())
                assertEquals(setOf(quest), repo.questChecks(routineId, today))
                assertEquals("05:42", repo.getRoutine(routineId)!!.time)
                assertTrue(repo.getRoutine(routineId)!!.alarmEnabled)
                assertTrue(repo.questChecks(routineId, today.plusDays(1)).isEmpty())
                assertTrue(repo.setQuestChecked(routineId, quest, today, false))
                assertTrue(repo.questChecks(routineId, today).isEmpty())
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun sourcesKeepSeparateChecksAndParentRewardStillPaysOnlyOnce() {
        val name = "sources_${UUID.randomUUID()}.db"
        try {
            AppRepository(context, name, "prefs_$name") { today }.use { repo ->
                val hero = HeroCatalog.default
                val block = HeroDayCatalog.forHero(hero).blocks.first()
                val day = repo.startHeroDay(hero).first()
                val personal = repo.addRoutine(hero.id, block.id, "내 아침", "09:00")
                block.quests.forEach { assertTrue(repo.setQuestChecked(day, it.id, today, true)) }
                assertTrue(repo.questChecks(personal, today).isEmpty())
                assertEquals(block.quests.map { it.id }.toSet(), repo.questChecksForDate(today)[day])
                assertEquals(0L, repo.balance())
                assertTrue(repo.setQuestChecked(personal, block.quests.first().id, today, true))
                assertEquals(CompletionOutcome.COMPLETED, repo.complete(day, today))
                assertEquals(CompletionOutcome.ALREADY_COMPLETED, repo.complete(personal, today))
                assertEquals(10L, repo.balance())
                assertEquals(1, repo.history().size)
                assertEquals(1, repo.questChecks(personal, today).size)
                assertEquals(block.quests.size, repo.questChecks(day, today).size)
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun beforeAdoptionFutureUnscheduledDisabledDeletedAndForgedChecksAreRejected() {
        val name = "bounds_${UUID.randomUUID()}.db"
        try {
            AppRepository(context, name, "prefs_$name") { today }.use { repo ->
                val hero = HeroCatalog.default
                val block = HeroDayCatalog.forHero(hero).blocks.first()
                val id = repo.startHeroDay(hero).first()
                val quest = block.quests.first().id
                assertFalse(repo.setQuestChecked(id, quest, today.plusDays(-1), true))
                assertFalse(repo.setQuestChecked(id, quest, today.plusDays(1), true))
                assertFalse(repo.setQuestChecked(id, "forged_quest", today, true))
                assertFalse(repo.setQuestChecked(99999, quest, today, true))
                val original = repo.getRoutine(id)!!
                // 2026-10-13 is Tuesday; Monday-only must not accept a check.
                repo.updateRoutine(original.copy(weekdayMask = 1))
                assertFalse(repo.setQuestChecked(id, quest, today, true))
                repo.updateRoutine(original.copy(enabled = false))
                assertFalse(repo.setQuestChecked(id, quest, today, true))
                repo.updateRoutine(original)
                assertTrue(repo.setQuestChecked(id, quest, today, true))
                repo.deleteRoutine(id)
                assertFalse(repo.setQuestChecked(id, quest, today, false))
                assertEquals(setOf(quest), repo.questChecks(id, today))
                assertEquals(0L, repo.balance())
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun insertFailureDoesNotCreatePartialChecksHistoryOrReward() {
        val name = "failure_${UUID.randomUUID()}.db"
        try {
            AppRepository(context, name, "prefs_$name") { today }.use { repo ->
                val id = repo.startHeroDay(HeroCatalog.default).first()
                val quest = HeroDayCatalog.forHero(HeroCatalog.default).blocks.first().quests.first().id
                SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use {
                    it.execSQL("CREATE TRIGGER reject_quest BEFORE INSERT ON quest_checks BEGIN SELECT RAISE(ABORT,'injected'); END")
                }
                assertThrows(Exception::class.java) { repo.setQuestChecked(id, quest, today, true) }
                assertTrue(repo.questChecks(id, today).isEmpty())
                assertEquals(0L, repo.balance())
                assertTrue(repo.history().isEmpty())
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun actualV2FileUpgradePreservesEveryExistingTableAndMatchesFreshV3Structure() {
        val name = "upgrade2_${UUID.randomUUID()}.db"
        val fresh = "fresh_${UUID.randomUUID()}.db"
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        javaClass.getResourceAsStream("/historical_routines_v2.db")!!.use { input ->
            path.outputStream().use { input.copyTo(it) }
        }
        fun rows(database: SQLiteDatabase, sql: String): List<List<String?>> = database.rawQuery(sql, null).use { cursor ->
            buildList { while (cursor.moveToNext()) add((0 until cursor.columnCount).map { cursor.getString(it) }) }
        }
        val tables = listOf("user_routines", "completion_records", "wallet", "owned_items", "applied_item", "legacy_completions", "app_metadata")
        val before = SQLiteDatabase.openDatabase(path.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            assertEquals(2, db.version)
            tables.associateWith { rows(db, "SELECT * FROM $it ORDER BY 1") }
        }
        try {
            AppRepository(context, name, "prefs_$name") { today }.use { repo ->
                assertEquals(80L, repo.balance())
                assertEquals("05:42", repo.getRoutine(42)!!.time)
                assertTrue(repo.getRoutine(42)!!.alarmEnabled)
                assertEquals(RoutineSource.HERO_DAY, repo.getRoutine(42)!!.source)
                assertTrue(repo.questChecks(42, today).isEmpty())
            }
            AppRepository(context, fresh, "prefs_$fresh").close()
            SQLiteDatabase.openDatabase(path.path, null, SQLiteDatabase.OPEN_READONLY).use { upgraded ->
                assertEquals(4, upgraded.version)
                tables.forEach { assertEquals(it, before[it], rows(upgraded, "SELECT * FROM $it ORDER BY 1")) }
                val structure = "SELECT type,name,tbl_name,sql FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' ORDER BY type,name"
                SQLiteDatabase.openDatabase(context.getDatabasePath(fresh).path, null, SQLiteDatabase.OPEN_READONLY).use {
                    assertEquals(rows(it, structure), rows(upgraded, structure))
                }
                assertEquals(listOf(listOf("ok")), rows(upgraded, "PRAGMA integrity_check"))
                assertTrue(rows(upgraded, "PRAGMA foreign_key_check").isEmpty())
            }
        } finally { context.deleteDatabase(name); context.deleteDatabase(fresh) }
    }
}
