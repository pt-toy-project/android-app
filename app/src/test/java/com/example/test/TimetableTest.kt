package com.example.test

import android.database.sqlite.SQLiteDatabase
import com.example.test.data.TimetableStore
import com.example.test.timetable.EverytimeLink
import com.example.test.timetable.TimetableAgenda
import com.example.test.timetable.TimetableCsv
import com.example.test.timetable.TimetableEntry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TimetableTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private fun row() = TimetableEntry(title = "데이터 통신, 심화", weekday = 2, startMinute = 570,
        endMinute = 720, location = "공학관 \"501\"", instructor = "김 교수")

    @Test fun csvRoundTripAndInvalidBatchDoesNotMutateClasses() {
        val expected = row()
        assertEquals(listOf(expected), TimetableCsv.parse(TimetableCsv.export(listOf(expected)).toString(Charsets.UTF_8)))
        assertThrows(IllegalArgumentException::class.java) {
            TimetableCsv.parse("title,weekday,start,end,location,instructor\n잘못된 수업,2,10:00,09:00,,")
        }
        val name = "timetable_${UUID.randomUUID()}.db"
        try {
            TimetableStore(context, name).use { store ->
                assertEquals(1, store.import(listOf(expected)))
                assertEquals(0, store.import(listOf(expected)))
                assertEquals(1, store.entries().size)
                assertThrows(IllegalArgumentException::class.java) {
                    store.import((1..201).map { expected.copy(title = "수업 $it") })
                }
                assertEquals(1, store.entries().size)
                val sameId = store.save(expected)
                assertEquals(store.entries().single().id, sameId)
                store.saveSharedLink("https://everytime.kr/@Example123")
                assertEquals("https://everytime.kr/@Example123", store.sharedLink())
            }
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                assertEquals(0L, db.rawQuery("SELECT balance FROM wallet", null).use { it.moveToFirst(); it.getLong(0) })
                assertEquals(0, db.rawQuery("SELECT count(*) FROM completion_records", null).use { it.moveToFirst(); it.getInt(0) })
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun nextClassChangesAtStartAndEndWithoutChangingRewards() {
        val first = TimetableEntry(title = "첫 수업", weekday = 2, startMinute = 540, endMinute = 600)
        val second = TimetableEntry(title = "둘째 수업", weekday = 2, startMinute = 660, endMinute = 720)
        val rows = listOf(second, first)
        assertEquals(first, TimetableAgenda.at(rows, 2, 539).next)
        assertEquals(first, TimetableAgenda.at(rows, 2, 540).current)
        assertEquals(second, TimetableAgenda.at(rows, 2, 600).next)
        assertNull(TimetableAgenda.at(rows, 2, 720).next)
        assertNull(TimetableAgenda.at(rows, 3, 540).next)
    }

    @Test fun linkAcceptsOnlyPublicEverytimeTimetableUrl() {
        assertEquals("https://everytime.kr/@Example123", EverytimeLink.fromSharedText("내 시간표 https://everytime.kr/@Example123 확인"))
        listOf("http://everytime.kr/@Example123", "https://everytime.kr.evil.test/@Example123",
            "https://everytime.kr/@Example123?token=abc", "https://user@everytime.kr/@Example123")
            .forEach { assertNull(EverytimeLink.parse(it)) }
    }
}
