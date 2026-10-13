package com.example.test.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.test.Routine.RoutineDate
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Native Android SQLite checks; do not mark these passed without a connected device. */
@RunWith(AndroidJUnit4::class)
class AppRepositoryTest {
    private lateinit var context: Context
    private lateinit var databaseName: String
    private lateinit var preferencesName: String
    private val adopted = RoutineDate(2026, 10, 12)

    @Before fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        val unique = UUID.randomUUID().toString()
        databaseName = "repository_$unique.db"
        preferencesName = "legacy_$unique"
    }

    @After fun cleanUp() {
        context.deleteDatabase(databaseName)
        context.deleteSharedPreferences(preferencesName)
    }

    private fun repository() = AppRepository(context, databaseName, preferencesName) { adopted }
    private fun add(repository: AppRepository, suffix: String = "question") = repository.addRoutine("franklin", suffix, "오늘의 선한 일", "07:00")
    private fun rawDatabase() = SQLiteDatabase.openDatabase(context.getDatabasePath(databaseName).path, null, SQLiteDatabase.OPEN_READWRITE)

    @Test fun migrationPreservesOldHistoryAndSelectionWithoutAssigningRewards() {
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        assertTrue(preferences.edit().putString("selected_hero", "einstein")
            .putStringSet("completed:sejong:2026-10-12", setOf("1", "2"))
            .putStringSet("completed:einstein:2026-10-11", setOf("1")).commit())
        repository().use {
            assertEquals("einstein", it.selectedHeroId)
            assertEquals(3, it.legacyHistoryCount())
            assertEquals(2, it.legacyHistoryCount("sejong"))
            assertEquals(1, it.legacyHistoryCount("einstein"))
            assertEquals(setOf("1", "2"), it.legacyHistory(heroId = "sejong").map { entry -> entry.sampleId }.toSet())
            assertTrue(it.legacyHistory(heroId = "einstein").all { entry -> entry.heroId == "einstein" })
            assertTrue(it.legacyHistory(heroId = "missing").isEmpty())
            val newRoutine = add(it)
            assertFalse(it.isCompleted(newRoutine, adopted))
            assertTrue(it.history().isEmpty())
            assertEquals(0L, it.balance())
        }
        repository().use {
            assertEquals(3, it.legacyHistoryCount()); assertEquals(2, it.legacyHistoryCount("sejong"))
            assertEquals(1, it.legacyHistory(limit = 1, heroId = "sejong").size); assertEquals(0L, it.balance())
        }
        assertEquals(setOf("1", "2"), preferences.getStringSet("completed:sejong:2026-10-12", emptySet()))
    }

    @Test fun completionSeparatesRoutineHeroDateAndPersistsExactlyOneReward() {
        val id = repository().use { add(it) }
        repository().use {
            val other = it.addRoutine("sejong", "reading", "읽기", "08:00")
            assertEquals(CompletionOutcome.COMPLETED, it.complete(id, adopted, adopted))
            assertEquals(CompletionOutcome.ALREADY_COMPLETED, it.complete(id, adopted, adopted))
            assertFalse(it.isCompleted(other, adopted))
            assertFalse(it.isCompleted(id, adopted.plusDays(1)))
            assertEquals(10L, it.balance())
        }
        repository().use { assertTrue(it.isCompleted(id, adopted)); assertEquals(1, it.history().size); assertEquals(10L, it.balance()) }
    }

    @Test fun concurrentRepositoriesCannotAwardDuplicateCompletion() {
        val id = repository().use { add(it) }
        val pool = Executors.newFixedThreadPool(4)
        try {
            val results = pool.invokeAll((1..12).map { Callable { repository().use { it.complete(id, adopted, adopted) } } })
                .map { it.get(15, TimeUnit.SECONDS) }
            assertEquals(1, results.count { it == CompletionOutcome.COMPLETED })
            assertEquals(11, results.count { it == CompletionOutcome.ALREADY_COMPLETED })
            repository().use { assertEquals(10L, it.balance()); assertEquals(1, it.history().size) }
        } finally { pool.shutdownNow() }
    }

    @Test fun invalidDatesAndUnscheduledDaysNeverCreateHistoryOrCoins() {
        repository().use {
            val id = add(it)
            assertEquals(CompletionOutcome.NOT_SCHEDULED, it.complete(id, adopted.plusDays(-1), adopted))
            assertEquals(CompletionOutcome.FUTURE_DATE, it.complete(id, adopted.plusDays(1), adopted))
            it.updateRoutine(it.getRoutine(id)!!.copy(weekdayMask = 2))
            assertEquals(CompletionOutcome.NOT_SCHEDULED, it.complete(id, adopted, adopted))
            assertTrue(it.listRoutines("franklin", adopted.plusDays(-1)).isEmpty())
            assertEquals(0L, it.balance()); assertTrue(it.history().isEmpty())
        }
    }

    @Test fun historyKeepsOriginalSnapshotAfterEditAndDeleteAndIdIsNeverReused() {
        repository().use {
            val id = add(it)
            it.complete(id, adopted, adopted)
            it.updateRoutine(it.getRoutine(id)!!.copy(title = "수정된 제목", time = "09:00"))
            it.deleteRoutine(id)
            assertNull(it.getRoutine(id))
            assertEquals("오늘의 선한 일", it.history().single().title)
            assertEquals(CompletionOutcome.ALREADY_COMPLETED, it.complete(id, adopted, adopted))
            assertEquals(CompletionOutcome.ROUTINE_UNAVAILABLE, it.complete(id, adopted.plusDays(1), adopted.plusDays(1)))
            assertTrue(add(it) > id)
        }
    }

    @Test fun duplicateAdoptionDoesNotResetUserEdits() {
        repository().use {
            val id = add(it)
            it.updateRoutine(it.getRoutine(id)!!.copy(time = "10:00", weekdayMask = 3, alarmEnabled = true))
            assertEquals(id, add(it))
            assertEquals("10:00", it.getRoutine(id)!!.time)
            assertEquals(3, it.getRoutine(id)!!.weekdayMask)
            assertTrue(it.getRoutine(id)!!.alarmEnabled)
            assertEquals(1, it.listRoutines().size)
        }
    }

    @Test fun deleteAndReadoptCannotRepeatTemplateRewardOnSameDay() {
        val oldId = repository().use { repo ->
            val id = add(repo)
            assertEquals(CompletionOutcome.COMPLETED, repo.complete(id, adopted, adopted))
            repo.deleteRoutine(id)
            id
        }
        repository().use { repo ->
            val newId = repo.addRoutine("franklin", "question", "새로운 도입 제목", "09:00")
            assertTrue(newId > oldId)
            assertTrue(repo.isCompleted(newId, adopted))
            assertEquals(setOf(newId), repo.completedRoutineIds(adopted, "franklin"))
            assertTrue(repo.completedRoutineIds(adopted, "sejong").isEmpty())
            assertEquals(CompletionOutcome.ALREADY_COMPLETED, repo.complete(newId, adopted, adopted))
            assertEquals(10L, repo.balance())
            assertEquals(oldId, repo.history().single().routineId)
            assertEquals("오늘의 선한 일", repo.history().single().title)
            assertEquals(1, repo.listRoutines("franklin", adopted).size)
            assertEquals(CompletionOutcome.COMPLETED, repo.complete(newId, adopted.plusDays(1), adopted.plusDays(1)))
            assertEquals(20L, repo.balance()); assertEquals(2, repo.history().size)
        }
    }

    @Test fun overflowingRewardLeavesWalletAndHistoryUntouched() {
        val id = repository().use { add(it) }
        rawDatabase().use { db -> db.compileStatement("UPDATE wallet SET balance=? WHERE singleton_id=1").use {
            it.bindLong(1, Long.MAX_VALUE - 9); it.executeUpdateDelete()
        } }
        repository().use {
            assertEquals(CompletionOutcome.BALANCE_OVERFLOW, it.complete(id, adopted, adopted))
            assertEquals(Long.MAX_VALUE - 9, it.balance()); assertTrue(it.history().isEmpty())
        }
    }

    @Test fun failedHistoryInsertRollsBackReward() {
        val id = repository().use { add(it) }
        rawDatabase().use { it.execSQL("CREATE TRIGGER test_reject_completion BEFORE INSERT ON completion_records BEGIN SELECT RAISE(ABORT,'test injection'); END") }
        repository().use {
            assertThrows(android.database.SQLException::class.java) { it.complete(id, adopted, adopted) }
            assertEquals(0L, it.balance()); assertTrue(it.history().isEmpty())
        }
    }

    @Test fun purchaseDebitOwnershipAndAppliedBadgePersistAtomically() {
        val item = ShopCatalog.items.first()
        repository().use {
            assertEquals(PurchaseOutcome.INSUFFICIENT_FUNDS, it.purchase(item.id))
            assertFalse(it.applyItem(item.id))
            repeat(3) { suffix -> it.complete(add(it, "habit_$suffix"), adopted, adopted) }
            assertEquals(PurchaseOutcome.PURCHASED, it.purchase(item.id))
            assertEquals(0L, it.balance())
            assertEquals(PurchaseOutcome.ALREADY_OWNED, it.purchase(item.id))
            assertTrue(it.applyItem(item.id))
            assertEquals(PurchaseOutcome.UNKNOWN_ITEM, it.purchase("missing"))
        }
        repository().use { assertEquals(setOf(item.id), it.ownedItemIds()); assertEquals(item.id, it.appliedItemId()); assertEquals(0L, it.balance()) }
    }

    @Test fun failedOwnershipInsertRollsBackDebit() {
        repository().use { repo -> repeat(3) { repo.complete(add(repo, "habit_$it"), adopted, adopted) } }
        rawDatabase().use { it.execSQL("CREATE TRIGGER test_reject_purchase BEFORE INSERT ON owned_items BEGIN SELECT RAISE(ABORT,'test injection'); END") }
        repository().use {
            assertThrows(android.database.SQLException::class.java) { it.purchase(ShopCatalog.items.first().id) }
            assertEquals(30L, it.balance()); assertTrue(it.ownedItemIds().isEmpty())
        }
    }

    @Test fun concurrentPurchasesChargeOnlyOnce() {
        repository().use { repo -> repeat(6) { repo.complete(add(repo, "habit_$it"), adopted, adopted) } }
        val pool = Executors.newFixedThreadPool(4)
        try {
            val item = ShopCatalog.items.first()
            val results = pool.invokeAll((1..12).map { Callable { repository().use { it.purchase(item.id) } } })
                .map { it.get(15, TimeUnit.SECONDS) }
            assertEquals(1, results.count { it == PurchaseOutcome.PURCHASED })
            assertEquals(11, results.count { it == PurchaseOutcome.ALREADY_OWNED })
            repository().use { assertEquals(30L, it.balance()); assertEquals(setOf(item.id), it.ownedItemIds()) }
        } finally { pool.shutdownNow() }
    }

    @Test fun historyReadsAreBoundedAndOffsetDoesNotDuplicateRows() {
        repository().use { repo ->
            repeat(4) { repo.complete(add(repo, "habit_$it"), adopted, adopted) }
            val first = repo.history(limit = 2)
            val second = repo.history(limit = 2, offset = 2)
            assertEquals(2, first.size); assertEquals(2, second.size)
            assertTrue(first.map { it.routineId }.toSet().intersect(second.map { it.routineId }.toSet()).isEmpty())
            assertThrows(IllegalArgumentException::class.java) { repo.history(limit = 501) }
        }
    }

    @Test fun malformedLegacyValuesRemainAvailableWithoutCrashingMigration() {
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        assertTrue(preferences.edit().putInt("selected_hero", 123)
            .putBoolean("completed:sejong:2026-10-12", true)
            .putStringSet("completed:sejong:2025-02-29", setOf("1")).commit())
        repository().use { assertEquals("sejong", it.selectedHeroId); assertEquals(0, it.legacyHistoryCount()); assertEquals(0L, it.balance()) }
        assertEquals(123, preferences.getInt("selected_hero", 0))
        assertTrue(preferences.getBoolean("completed:sejong:2026-10-12", false))
    }

    @Test fun identityCannotBeReassignedAndSettingsPreferenceFileRemainsUntouched() {
        val settings = context.getSharedPreferences("settings_$preferencesName", Context.MODE_PRIVATE)
        try {
            assertTrue(settings.edit().putBoolean("sound_enabled", false).putBoolean("alarm_enabled", false).commit())
            repository().use {
                val routine = it.getRoutine(add(it))!!
                assertThrows(IllegalArgumentException::class.java) { it.updateRoutine(routine.copy(heroId = "sejong")) }
                assertEquals("franklin", it.getRoutine(routine.id)!!.heroId)
            }
            assertFalse(settings.getBoolean("sound_enabled", true)); assertFalse(settings.getBoolean("alarm_enabled", true))
        } finally { context.deleteSharedPreferences("settings_$preferencesName") }
    }
}
