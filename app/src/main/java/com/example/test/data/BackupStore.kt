package com.example.test.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.security.MessageDigest

data class BackupPreview(val createdAt: Long, val routines: Int, val records: Int, val balance: Long, val classes: Int?)

/** Portable typed rows. Import is a transaction, with all foreign keys and native CHECKs enabled. */
class BackupStore(context: Context, private val databaseName: String = "historical_routines.db") {
    private val context = context.applicationContext
    private val settings get() = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun export(): ByteArray {
        AppRepository(context, databaseName).close() // Finish any v1 import/migration before the snapshot.
        val helper = AppDatabase(context, databaseName)
        try {
            val db = helper.writableDatabase
            val tables = JSONObject()
            db.beginTransaction()
            try {
                COLUMNS.forEach { (table, columns) ->
                    val rows = JSONArray()
                    db.query(table, columns.toTypedArray(), null, null, null, null, null).use { cursor ->
                        while (cursor.moveToNext()) {
                            val row = JSONArray()
                            columns.indices.forEach { index -> row.put(when (cursor.getType(index)) {
                                Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                                Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index)
                                Cursor.FIELD_TYPE_STRING -> cursor.getString(index)
                                else -> error("Unsupported backup value")
                            }) }
                            rows.put(row)
                        }
                    }
                    tables.put(table, rows)
                }
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
            val prefs = settings.all
            val data = JSONObject().put("tables", tables).put("settings", JSONObject()
                .put("sound_enabled", prefs["sound_enabled"] as? Boolean ?: true)
                .put("alarm_enabled", prefs["alarm_enabled"] as? Boolean ?: true)).toString()
            val bytes = JSONObject().put("format", FORMAT).put("formatVersion", 1).put("databaseVersion", 4)
                .put("createdAt", System.currentTimeMillis()).put("payload", data).put("sha256", hash(data)).toString().toByteArray(Charsets.UTF_8)
            require(bytes.size <= MAX_BYTES) { "Backup is too large" }
            return bytes
        } finally { helper.close() }
    }

    fun preview(bytes: ByteArray): BackupPreview {
        val parsed = parse(bytes)
        val rows = parsed.second.getJSONObject("tables")
        return BackupPreview(parsed.first.getLong("createdAt"), rows.getJSONArray("user_routines").length(),
            rows.getJSONArray("completion_records").length(), rows.getJSONArray("wallet").getJSONArray(0).getLong(1), rows.optJSONArray("timetable_entries")?.length())
    }

    /** Caller must show the preview and receive the user's explicit restore click first. */
    fun restore(bytes: ByteArray) {
        val (_, data) = parse(bytes)
        // Keep a recoverable local snapshot; a failure writing it must not start destructive work.
        val safety = android.util.AtomicFile(java.io.File(context.noBackupFilesDir, "before-restore.heroday.json"))
        val previous = export()
        val stream = safety.startWrite()
        try { stream.write(previous); safety.finishWrite(stream) }
        catch (e: Exception) { safety.failWrite(stream); throw e }
        val helper = AppDatabase(context, databaseName)
        val oldPrefs = settings.all
        var settingsChanged = false
        try {
            val db = helper.writableDatabase
            db.beginTransaction()
            var failure: Throwable? = null
            try {
                val tables = data.getJSONObject("tables")
                // A v3 backup predates classes; preserve the user's current timetable.
                val oldTimetableSettings = if (!tables.has("timetable_entries")) {
                    db.rawQuery("SELECT key,value FROM app_metadata WHERE key IN ('timetable_shared_link','timetable_notification')", null).use { cursor ->
                        buildList { while (cursor.moveToNext()) add(cursor.getString(0) to cursor.getString(1)) }
                    }
                } else emptyList()
                DELETE_ORDER.filter { tables.has(it) }.forEach { db.delete(it, null, null) }
                COLUMNS.filterKeys { tables.has(it) }.forEach { (table, columns) ->
                    val rows = tables.getJSONArray(table)
                    repeat(rows.length()) { position ->
                        val row = rows.getJSONArray(position)
                        db.insertOrThrow(table, null, ContentValues().apply {
                            columns.forEachIndexed { index, column -> when (val value = row.get(index)) {
                                JSONObject.NULL -> putNull(column)
                                is String -> put(column, value)
                                is Int -> put(column, value)
                                is Long -> put(column, value)
                                else -> error("Unsupported backup type")
                            } }
                        })
                    }
                }
                oldTimetableSettings.forEach { (key, value) ->
                    db.insertWithOnConflict("app_metadata", null, ContentValues().apply { put("key", key); put("value", value) }, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
                }
                require(db.rawQuery("PRAGMA foreign_key_check", null).use { !it.moveToFirst() })
                require(db.rawQuery("SELECT 1 FROM completion_records c JOIN user_routines u ON u.id=c.routine_id WHERE c.hero_id!=u.hero_id OR c.template_id!=u.template_id LIMIT 1", null).use { !it.moveToFirst() })
                require(db.rawQuery("PRAGMA integrity_check", null).use { it.moveToFirst() && it.getString(0) == "ok" })
                val choices = data.getJSONObject("settings")
                settingsChanged = true
                check(settings.edit().putBoolean("sound_enabled", choices.getBoolean("sound_enabled"))
                    .putBoolean("alarm_enabled", choices.getBoolean("alarm_enabled")).commit())
                db.setTransactionSuccessful()
            } catch (e: Throwable) { failure = e }
            try { db.endTransaction() } catch (e: Throwable) { if (failure == null) failure = e else failure.addSuppressed(e) }
            failure?.let { throw it }
        } catch (e: Throwable) {
            if (settingsChanged) {
                val editor = settings.edit()
                listOf("sound_enabled", "alarm_enabled").forEach { key ->
                    val value = oldPrefs[key]
                    if (value is Boolean) editor.putBoolean(key, value) else editor.remove(key)
                }
                if (!editor.commit()) e.addSuppressed(IllegalStateException("Could not roll back preferences"))
            }
            throw e
        } finally { helper.close() }
    }

    fun safetyBackup(): ByteArray = java.io.File(context.noBackupFilesDir, "before-restore.heroday.json").readBytes()
    fun hasSafetyBackup() = java.io.File(context.noBackupFilesDir, "before-restore.heroday.json").isFile

    private fun parse(bytes: ByteArray): Pair<JSONObject, JSONObject> {
        require(bytes.size in 1..MAX_BYTES)
        val envelope = JSONObject(bytes.toString(Charsets.UTF_8))
        require(envelope.getString("format") == FORMAT)
        listOf("formatVersion", "databaseVersion", "createdAt").forEach { key ->
            require(envelope.get(key) is Int || envelope.get(key) is Long)
        }
        require(envelope.getLong("formatVersion") == 1L && envelope.getLong("databaseVersion") in 3L..4L)
        require(envelope.getLong("createdAt") >= 0)
        val payload = envelope.getString("payload")
        require(MessageDigest.isEqual(hash(payload).toByteArray(Charsets.US_ASCII), envelope.getString("sha256").toByteArray(Charsets.US_ASCII)))
        val data = JSONObject(payload)
        val tables = data.getJSONObject("tables")
        val expected = if (envelope.getLong("databaseVersion") == 3L) COLUMNS.keys - "timetable_entries" else COLUMNS.keys
        require(tables.keys().asSequence().toSet() == expected)
        var total = 0
        COLUMNS.filterKeys { it in expected }.forEach { (table, columns) ->
            val rows = tables.getJSONArray(table)
            total += rows.length()
            require(total <= 100_000)
            repeat(rows.length()) { i ->
                val row = rows.getJSONArray(i)
                require(row.length() == columns.size)
                columns.forEachIndexed { index, column ->
                    val value = row.get(index)
                    if (column in INTEGERS) require(value is Int || value is Long)
                    else require(value is String && value.length <= 20_000)
                }
            }
        }
        tables.optJSONArray("timetable_entries")?.let { classes ->
            require(classes.length() <= TimetableStore.MAX_ENTRIES)
            repeat(classes.length()) { index ->
                val row = classes.getJSONArray(index)
                require(row.getLong(0) > 0)
                com.example.test.timetable.TimetableEntry(row.getLong(0), row.getString(1), row.getInt(2), row.getInt(3), row.getInt(4), row.getString(5), row.getString(6))
            }
        }
        val routines = tables.getJSONArray("user_routines")
        repeat(routines.length()) { index ->
            val row = routines.getJSONArray(index)
            require(row.getLong(0) > 0 && com.example.test.Routine.RoutineDate.parse(row.getString(9)) != null)
            RoutineRules.validateRoutine(row.getString(1), row.getString(2), row.getString(3), row.getString(4), row.getInt(5))
            RoutineSource.valueOf(row.getString(10))
        }
        listOf("completion_records" to 1, "legacy_completions" to 1, "quest_checks" to 2).forEach { (table, column) ->
            val rows = tables.getJSONArray(table)
            repeat(rows.length()) { require(com.example.test.Routine.RoutineDate.parse(rows.getJSONArray(it).getString(column)) != null) }
        }
        val wallet = tables.getJSONArray("wallet")
        require(wallet.length() == 1 && wallet.getJSONArray(0).getLong(0) == 1L && wallet.getJSONArray(0).getLong(1) >= 0)
        val choices = data.getJSONObject("settings")
        require(choices.get("sound_enabled") is Boolean && choices.get("alarm_enabled") is Boolean)
        return envelope to data
    }

    companion object {
        const val MAX_BYTES = 16 * 1024 * 1024
        private const val FORMAT = "hero-day-backup"
        private fun hash(text: String) = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        val COLUMNS = linkedMapOf(
            "timetable_entries" to listOf("id", "title", "weekday", "start_minute", "end_minute", "location", "instructor"),
            "app_metadata" to listOf("key", "value"),
            "user_routines" to listOf("id", "hero_id", "template_id", "title", "local_time", "weekday_mask", "enabled", "alarm_enabled", "deleted", "created_on", "source"),
            "wallet" to listOf("singleton_id", "balance"),
            "owned_items" to listOf("item_id", "price_paid", "purchased_at"),
            "applied_item" to listOf("singleton_id", "item_id"),
            "completion_records" to listOf("routine_id", "local_date", "hero_id", "template_id", "title", "coins", "completed_at"),
            "legacy_completions" to listOf("hero_id", "local_date", "sample_id"),
            "quest_checks" to listOf("routine_id", "quest_id", "local_date", "checked_at")
        )
        private val INTEGERS = setOf("weekday", "start_minute", "end_minute", "id", "routine_id", "weekday_mask", "enabled", "alarm_enabled", "deleted", "singleton_id", "balance", "price_paid", "purchased_at", "coins", "completed_at", "checked_at")
        private val DELETE_ORDER = listOf("timetable_entries", "quest_checks", "completion_records", "applied_item", "owned_items", "user_routines", "wallet", "legacy_completions", "app_metadata")
        fun readLimited(input: InputStream): ByteArray {
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                require(out.size() + read <= MAX_BYTES) { "Backup is too large" }
                out.write(buffer, 0, read)
            }
            return out.toByteArray()
        }
    }
}
