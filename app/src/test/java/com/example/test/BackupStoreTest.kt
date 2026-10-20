package com.example.test

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.test.Hero.HeroCatalog
import com.example.test.Hero.HeroDayCatalog
import com.example.test.Routine.RoutineDate
import com.example.test.data.*
import com.example.test.timetable.TimetableEntry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.security.MessageDigest
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupStoreTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private fun databaseRows(name: String): Map<String, List<List<String?>>> =
        SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            BackupStore.COLUMNS.mapValues { (table, columns) ->
                db.query(table, columns.toTypedArray(), null, null, null, null, null).use { cursor ->
                    buildList { while (cursor.moveToNext()) add(columns.indices.map { cursor.getString(it) }) }
                }
            }
        }

    @Test fun roundTripPreservesAllTablesQuestsRewardsSettingsAndPurchaseState() {
        val name = "backup_${UUID.randomUUID()}.db"
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        javaClass.getResourceAsStream("/historical_routines_v2.db")!!.use { input -> path.outputStream().use { input.copyTo(it) } }
        try {
            val today = RoutineDate.today()
            AppRepository(context, name).use { repo ->
                val id = repo.startHeroDay(HeroCatalog.default).first()
                repo.setQuestChecked(id, HeroDayCatalog.forHero(HeroCatalog.default).blocks.first().quests.first().id, today, true)
            }
            context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit()
                .putBoolean("sound_enabled", false).putBoolean("alarm_enabled", false).commit()
            TimetableStore(context, name).use { it.save(TimetableEntry(title = "컴퓨터 그래픽스", weekday = 2, startMinute = 840, endMinute = 960)) }
            val store = BackupStore(context, name)
            val before = databaseRows(name)
            val bytes = store.export()
            assertEquals(80L, store.preview(bytes).balance)
            assertEquals(1, store.preview(bytes).classes)
            AppRepository(context, name).use { it.addPersonalPlan("새로 쓴 계획 🌿", "08:15") }
            context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit()
                .putBoolean("sound_enabled", true).putBoolean("alarm_enabled", true).commit()
            store.restore(bytes)
            assertEquals(before, databaseRows(name))
            assertFalse(SettingsPreferences(context).alarmEnabled)
            assertFalse(SettingsPreferences(context).soundEnabled)
            assertTrue(store.hasSafetyBackup())
            assertTrue(store.preview(store.safetyBackup()).routines > store.preview(bytes).routines)
        } finally { context.deleteDatabase(name) }
    }


    @Test fun oldV3BackupKeepsCurrentTimetableAndSharedLink() {
        val name = "legacy_${UUID.randomUUID()}.db"
        try {
            TimetableStore(context, name).use {
                it.save(TimetableEntry(title = "강의", weekday = 1, startMinute = 540, endMinute = 600))
                it.saveSharedLink("https://everytime.kr/@Example123")
                it.setNotificationEnabled(true)
            }
            val store = BackupStore(context, name)
            val legacy = editPayload(store.export()) { it.getJSONObject("tables").remove("timetable_entries") }
            val legacyEnvelope = JSONObject(legacy.toString(Charsets.UTF_8)).put("databaseVersion", 3)
            assertNull(store.preview(legacyEnvelope.toString().toByteArray()).classes)
            store.restore(legacyEnvelope.toString().toByteArray())
            TimetableStore(context, name).use {
                assertEquals(1, it.entries().size)
                assertEquals("https://everytime.kr/@Example123", it.sharedLink())
                assertTrue(it.notificationEnabled())
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun damagedAndFutureFormatBackupsLeaveCurrentRecordsUntouched() {
        val name = "damaged_${UUID.randomUUID()}.db"
        try {
            AppRepository(context, name).use { it.addPersonalPlan("원래 기록", "07:00") }
            val store = BackupStore(context, name)
            val bytes = store.export()
            val before = databaseRows(name)
            val damaged = JSONObject(bytes.toString(Charsets.UTF_8)).put("sha256", "0".repeat(64)).toString().toByteArray()
            assertThrows(IllegalArgumentException::class.java) { store.restore(damaged) }
            val future = JSONObject(bytes.toString(Charsets.UTF_8)).put("databaseVersion", 999).toString().toByteArray()
            assertThrows(IllegalArgumentException::class.java) { store.restore(future) }
            assertEquals(before, databaseRows(name))
        } finally { context.deleteDatabase(name) }
    }

    private fun editPayload(bytes: ByteArray, edit: (JSONObject) -> Unit): ByteArray {
        val envelope = JSONObject(bytes.toString(Charsets.UTF_8))
        val data = JSONObject(envelope.getString("payload"))
        edit(data)
        val text = data.toString()
        val hash = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
        return envelope.put("payload", text).put("sha256", hash).toString().toByteArray()
    }

    @Test fun validChecksumWithBadForeignKeyRollsBackDeletedRowsAndSettings() {
        val name = "foreign_${UUID.randomUUID()}.db"
        try {
            AppRepository(context, name).use { it.startHeroDay(HeroCatalog.default) }
            val store = BackupStore(context, name)
            val before = databaseRows(name)
            val corrupt = editPayload(store.export()) {
                it.getJSONObject("tables").getJSONArray("quest_checks")
                    .put(org.json.JSONArray().put(999999).put("missing_q1").put(RoutineDate.today().iso).put(1))
                it.getJSONObject("settings").put("alarm_enabled", false)
            }
            assertThrows(Exception::class.java) { store.restore(corrupt) }
            assertEquals(before, databaseRows(name))
            assertTrue(SettingsPreferences(context).alarmEnabled)
            assertTrue(store.hasSafetyBackup())
        } finally { context.deleteDatabase(name) }
    }

    @Test fun invalidRoutineTimeCannotReplaceExistingData() {
        val name = "constraint_${UUID.randomUUID()}.db"
        try {
            AppRepository(context, name).use { it.addPersonalPlan("보존", "07:00") }
            val store = BackupStore(context, name)
            val before = databaseRows(name)
            val corrupt = editPayload(store.export()) {
                it.getJSONObject("tables").getJSONArray("user_routines").getJSONArray(0).put(4, "25:00")
            }
            assertThrows(Exception::class.java) { store.restore(corrupt) }
            assertEquals(before, databaseRows(name))
        } finally { context.deleteDatabase(name) }
    }

    @Test fun recordIdentityMismatchAndFractionalVersionCannotBeRestored() {
        val name = "identity_${UUID.randomUUID()}.db"
        try {
            AppRepository(context, name).use { repo ->
                val id = repo.startHeroDay(HeroCatalog.default).first()
                repo.complete(id, RoutineDate.today())
            }
            val store = BackupStore(context, name)
            val bytes = store.export()
            val before = databaseRows(name)
            val corrupt = editPayload(bytes) {
                it.getJSONObject("tables").getJSONArray("completion_records").getJSONArray(0).put(3, "different_block")
            }
            assertThrows(Exception::class.java) { store.restore(corrupt) }
            val fractional = JSONObject(bytes.toString(Charsets.UTF_8)).put("databaseVersion", 3.7).toString().toByteArray()
            assertThrows(IllegalArgumentException::class.java) { store.preview(fractional) }
            assertEquals(before, databaseRows(name))
        } finally { context.deleteDatabase(name) }
    }

    @Test fun backupExcludesAccountTokensAndDeviceAlarmMetadata() {
        val name = "privacy_${UUID.randomUUID()}.db"
        try {
            context.getSharedPreferences("google_account", Context.MODE_PRIVATE).edit()
                .putString("email", "test@example.invalid").putString("test_token", "FAKE_TEST_TOKEN").commit()
            val bytes = BackupStore(context, name).export().toString(Charsets.UTF_8)
            assertFalse(bytes.contains("FAKE_TEST_TOKEN"))
            assertFalse(bytes.contains("test@example.invalid"))
            assertFalse(bytes.contains("google_account"))
        } finally { context.deleteDatabase(name) }
    }

    @Test fun oversizedInputIsRejectedInsteadOfTruncated() {
        val stream = object : java.io.InputStream() {
            var remaining = BackupStore.MAX_BYTES + 1
            override fun read(): Int = if (remaining-- > 0) 0 else -1
            override fun read(buffer: ByteArray, off: Int, len: Int): Int {
                if (remaining <= 0) return -1
                val size = minOf(len, remaining)
                buffer.fill(0, off, off + size); remaining -= size
                return size
            }
        }
        assertThrows(IllegalArgumentException::class.java) { BackupStore.readLimited(stream) }
    }
}
