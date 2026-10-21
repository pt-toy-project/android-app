package com.example.test

import android.database.sqlite.SQLiteDatabase
import com.example.test.data.TimetableStore
import com.example.test.timetable.EverytimeLink
import com.example.test.timetable.EverytimeShareImporter
import com.example.test.timetable.TimetableAgenda
import com.example.test.timetable.TimetableCsv
import com.example.test.timetable.TimetableEntry
import com.example.test.timetable.TodayTimetablePreview
import com.example.test.Routine.RoutineDate
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

    @Test fun homePreviewUsesSelectedWeekdayAndShowsRemainingCount() {
        val monday = RoutineDate(2026, 10, 19)
        val tuesday = monday.plusDays(1)
        val morning = TimetableEntry(title = "아침 수업", weekday = 1, startMinute = 540, endMinute = 600)
        val midday = TimetableEntry(title = "점심 수업", weekday = 1, startMinute = 720, endMinute = 780)
        val afternoon = TimetableEntry(title = "오후 수업", weekday = 1, startMinute = 900, endMinute = 960)
        val differentDay = TimetableEntry(title = "화요일 수업", weekday = 2, startMinute = 480, endMinute = 540)
        val entries = listOf(afternoon, differentDay, midday, morning)
        assertEquals(TodayTimetablePreview(listOf(morning, midday), 1), TodayTimetablePreview.forDate(entries, monday))
        assertEquals(TodayTimetablePreview(listOf(morning), 2), TodayTimetablePreview.forDate(entries, monday, limit = 1))
        assertEquals(TodayTimetablePreview(listOf(differentDay), 0), TodayTimetablePreview.forDate(entries, tuesday))
        assertEquals(TodayTimetablePreview(emptyList(), 0), TodayTimetablePreview.forDate(entries, monday.plusDays(2)))
    }

    @Test fun csvRoundTripAndInvalidBatchDoesNotMutateClasses() {
        val expected = row()
        assertEquals(listOf(expected), TimetableCsv.parse(TimetableCsv.export(listOf(expected)).toString(Charsets.UTF_8)))
        assertThrows(IllegalArgumentException::class.java) {
            TimetableCsv.parse("title,weekday,start,end,location,instructor\n잘못된 수업,2,10:00,09:00,,")
        }
        val name = "timetable_${UUID.randomUUID()}.db"
        try {
            TimetableStore(context, name).use { store ->
                assertEquals(1, store.import(listOf(expected), "https://everytime.kr/@Example123"))
                assertEquals("https://everytime.kr/@Example123", store.sharedLink())
                assertEquals(0, store.import(listOf(expected)))
                assertEquals(1, store.entries().size)
                assertThrows(IllegalArgumentException::class.java) {
                    store.import((1..201).map { expected.copy(title = "수업 $it") }, "https://everytime.kr/@Other123")
                }
                assertEquals(1, store.entries().size)
                assertEquals("https://everytime.kr/@Example123", store.sharedLink())
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

    @Test fun publicShareResponseMapsFiveMinuteSlotsAndSkipsUnscheduledClasses() {
        val xml = """<response><table status="1">
            <subject id="1"><name value="자료구조"/><professor value="김 교수"/><place value="기본 강의실"/>
                <time><data day="0" starttime="108" endtime="132" place="501호"/>
                <data day="2" starttime="114" endtime="138" place=""/></time></subject>
            <subject id="2"><name value="시간 미정"/><time/></subject>
        </table></response>"""
        val preview = EverytimeShareImporter.parse(xml.byteInputStream())
        assertEquals(2, preview.entries.size)
        assertEquals(1, preview.unscheduled)
        assertEquals(TimetableEntry(title = "자료구조", weekday = 1, startMinute = 540,
            endMinute = 660, location = "501호", instructor = "김 교수"), preview.entries[0])
        assertEquals("기본 강의실", preview.entries[1].location)
        assertEquals(3, preview.entries[1].weekday)
    }

    @Test fun anonymizedRealShareStructureIncludesSiblingAccountDataWithoutImportingIt() {
        val fixture = javaClass.classLoader!!.getResourceAsStream("everytime-share-anonymized.xml")!!
        val preview = fixture.use(EverytimeShareImporter::parse)
        assertEquals(10, preview.entries.size)
        assertEquals(1, preview.unscheduled)
        assertTrue(preview.entries.all { it.title.startsWith("예시 수업") })
        assertTrue(preview.entries.all { it.location == "예시 강의실" })
    }

    @Test fun privateInvalidAndUnsafeShareResponsesAreRejected() {
        val responses = listOf("<response><table status=\"-2\"/></response>" to EverytimeShareImporter.Problem.PRIVATE,
            "<response>-1</response>" to EverytimeShareImporter.Problem.UNAVAILABLE,
            "<!DOCTYPE response [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><response>&x;</response>" to EverytimeShareImporter.Problem.FORMAT)
        responses.forEach { (xml, reason) ->
            val error = assertThrows(EverytimeShareImporter.ImportError::class.java) {
                EverytimeShareImporter.parse(xml.byteInputStream())
            }
            assertEquals(reason, error.problem)
        }
    }
}
