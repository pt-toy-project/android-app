package com.example.test.data

import android.content.ContentValues
import android.content.Context
import com.example.test.timetable.EverytimeLink
import com.example.test.timetable.TimetableEntry
import com.example.test.timetable.TimetableSurfaces
import java.io.Closeable

class TimetableStore(context: Context, name: String = "historical_routines.db") : Closeable {
    private val context = context.applicationContext
    private val helper = AppDatabase(this.context, name)
    private val refreshSurfaces = name == "historical_routines.db"
    private val db get() = helper.writableDatabase
    fun entries(): List<TimetableEntry> = db.query("timetable_entries", null, null, null, null, null, "weekday,start_minute,end_minute,id").use { c ->
        buildList { while (c.moveToNext()) add(TimetableEntry(c.getLong(0), c.getString(1), c.getInt(2), c.getInt(3), c.getInt(4), c.getString(5), c.getString(6))) }
    }
    fun save(entry: TimetableEntry): Long {
        val value = entry.normalized()
        val saved = transaction {
            if (value.id == 0L) {
                entries().firstOrNull { it.identity == value.identity }?.let { return@transaction it.id }
                require(entries().size < MAX_ENTRIES)
                db.insertOrThrow("timetable_entries", null, values(value))
            } else {
                check(db.update("timetable_entries", values(value), "id=?", arrayOf(value.id.toString())) == 1)
                value.id
            }
        }
        if (refreshSurfaces) TimetableSurfaces.refresh(context)
        return saved
    }
    /** Duplicate import is a no-op. Any invalid batch/limit failure rolls back the entire operation. */
    fun import(entries: List<TimetableEntry>): Int {
        val added = transaction {
            require(entries.isNotEmpty() && entries.size <= MAX_ENTRIES)
            val existing = this.entries().map { it.identity }.toSet()
            val additions = entries.map { it.normalized().copy(id = 0) }.distinctBy { it.identity }.filter { it.identity !in existing }
            require(existing.size + additions.size <= MAX_ENTRIES)
            additions.forEach { db.insertOrThrow("timetable_entries", null, values(it)) }
            additions.size
        }
        if (refreshSurfaces) TimetableSurfaces.refresh(context)
        return added
    }
    fun delete(id: Long) {
        db.delete("timetable_entries", "id=?", arrayOf(id.toString()))
        if (refreshSurfaces) TimetableSurfaces.refresh(context)
    }
    fun notificationEnabled(): Boolean = db.rawQuery("SELECT value FROM app_metadata WHERE key='timetable_notification'", null)
        .use { it.moveToFirst() && it.getString(0) == "1" }
    fun setNotificationEnabled(enabled: Boolean) {
        db.insertWithOnConflict("app_metadata", null, ContentValues().apply {
            put("key", "timetable_notification"); put("value", if (enabled) "1" else "0")
        }, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        if (refreshSurfaces) TimetableSurfaces.refresh(context)
    }
    fun sharedLink(): String? = db.rawQuery("SELECT value FROM app_metadata WHERE key='timetable_shared_link'", null)
        .use { if (it.moveToFirst()) EverytimeLink.parse(it.getString(0)) else null }
    fun saveSharedLink(link: String) {
        val valid = requireNotNull(EverytimeLink.parse(link))
        db.insertWithOnConflict("app_metadata", null, ContentValues().apply { put("key", "timetable_shared_link"); put("value", valid) }, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }
    private fun values(e: TimetableEntry) = ContentValues().apply {
        put("title", e.title); put("weekday", e.weekday); put("start_minute", e.startMinute); put("end_minute", e.endMinute)
        put("location", e.location); put("instructor", e.instructor)
    }
    private fun <T> transaction(action: () -> T): T {
        db.beginTransaction()
        try { val result = action(); db.setTransactionSuccessful(); return result } finally { db.endTransaction() }
    }
    override fun close() = helper.close()
    companion object { const val MAX_ENTRIES = 200 }
}
