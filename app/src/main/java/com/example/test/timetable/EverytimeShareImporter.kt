package com.example.test.timetable

import android.util.Xml
import com.example.test.data.TimetableStore
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL

/** Reads only the public timetable behind a validated share URL, without cookies or a login. */
object EverytimeShareImporter {
    enum class Problem { INVALID_LINK, CONNECTION, UNAVAILABLE, PRIVATE, EMPTY, FORMAT, TOO_MANY }
    class ImportError(val problem: Problem) : Exception(problem.name)
    data class Preview(val entries: List<TimetableEntry>, val unscheduled: Int)
    private const val RESPONSE_LIMIT = 1024 * 1024
    private const val ENDPOINT = "https://api.everytime.kr/find/timetable/table/friend"

    fun fetch(link: String): Preview {
        val valid = EverytimeLink.parse(link) ?: throw ImportError(Problem.INVALID_LINK)
        val identifier = valid.substringAfter("/@")
        val body = "identifier=$identifier&friendInfo=true".toByteArray(Charsets.US_ASCII)
        val connection = try { URL(ENDPOINT).openConnection() as HttpURLConnection }
        catch (_: Exception) { throw ImportError(Problem.CONNECTION) }
        try {
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            connection.setRequestProperty("Accept", "application/xml, text/xml, */*")
            connection.setRequestProperty("Origin", "https://everytime.kr")
            connection.setRequestProperty("Referer", valid)
            connection.setRequestProperty("Cookie", "")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; HeroDayTimetable/1.0)")
            connection.outputStream.use { it.write(body) }
            if (connection.responseCode != 200) throw ImportError(Problem.CONNECTION)
            val bytes = connection.inputStream.use { bounded(it) }
            return parse(ByteArrayInputStream(bytes))
        } catch (error: ImportError) {
            throw error
        } catch (_: Exception) {
            throw ImportError(Problem.CONNECTION)
        } finally {
            connection.disconnect()
        }
    }

    /** Five-minute units and Monday=0 follow the public share page's own timetable renderer. */
    fun parse(input: InputStream): Preview {
        val bytes = try { bounded(input) } catch (_: Exception) { throw ImportError(Problem.FORMAT) }
        val xml = bytes.toString(Charsets.UTF_8)
        // Reject declarations before Android's pull parser can resolve an external entity.
        if (Regex("<!\\s*(DOCTYPE|ENTITY)", RegexOption.IGNORE_CASE).containsMatchIn(xml))
            throw ImportError(Problem.FORMAT)
        try {
            val parser = Xml.newPullParser().apply { setInput(StringReader(xml)) }
            val rows = ArrayList<TimetableEntry>()
            var tableFound = false
            var inTable = false
            var subject: Subject? = null
            var unscheduled = 0
            var responseText = ""
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> when {
                        parser.depth == 1 && parser.name != "response" -> throw ImportError(Problem.FORMAT)
                        parser.depth == 2 && parser.name == "table" && !tableFound -> {
                            tableFound = true
                            inTable = true
                            when (parser.getAttributeValue(null, "status")) {
                                "-2" -> throw ImportError(Problem.PRIVATE)
                                "-1" -> throw ImportError(Problem.UNAVAILABLE)
                            }
                        }
                        inTable && parser.depth == 3 && parser.name == "subject" -> subject = Subject()
                        subject != null && parser.depth == 4 -> when (parser.name) {
                            "name" -> subject.name = parser.value("value")
                            "professor" -> subject.professor = parser.value("value")
                            "place" -> subject.place = parser.value("value")
                        }
                        subject != null && parser.depth == 5 && parser.name == "data" -> {
                            val day = parser.value("day").toIntOrNull() ?: throw ImportError(Problem.FORMAT)
                            val start = parser.value("starttime").toIntOrNull() ?: throw ImportError(Problem.FORMAT)
                            val end = parser.value("endtime").toIntOrNull() ?: throw ImportError(Problem.FORMAT)
                            subject.slots.add(Slot(day + 1, start * 5, end * 5, parser.value("place")))
                        }
                    }
                    XmlPullParser.TEXT -> if (!tableFound && parser.depth == 1) responseText += parser.text
                    XmlPullParser.END_TAG -> when {
                        inTable && parser.depth == 3 && parser.name == "subject" && subject != null -> {
                            val finished = subject
                            if (finished.slots.isEmpty()) unscheduled++
                            for (slot in finished.slots) {
                                val row = TimetableEntry(title = finished.name, weekday = slot.day,
                                    startMinute = slot.start, endMinute = slot.end,
                                    location = slot.place.ifBlank { finished.place }, instructor = finished.professor).normalized()
                                rows.add(row)
                                if (rows.size > TimetableStore.MAX_ENTRIES) throw ImportError(Problem.TOO_MANY)
                            }
                            subject = null
                        }
                        inTable && parser.depth == 2 && parser.name == "table" -> inTable = false
                    }
                }
            }
            if (responseText.trim() == "-1") throw ImportError(Problem.UNAVAILABLE)
            if (responseText.trim() == "-2") throw ImportError(Problem.PRIVATE)
            if (!tableFound) throw ImportError(Problem.UNAVAILABLE)
            if (rows.isEmpty()) throw ImportError(Problem.EMPTY)
            return Preview(rows.distinctBy { it.identity }, unscheduled)
        } catch (error: ImportError) {
            throw error
        } catch (_: Exception) {
            throw ImportError(Problem.FORMAT)
        }
    }

    private class Subject(var name: String = "", var professor: String = "", var place: String = "",
        val slots: MutableList<Slot> = ArrayList())
    private data class Slot(val day: Int, val start: Int, val end: Int, val place: String)
    private fun XmlPullParser.value(name: String) = getAttributeValue(null, name).orEmpty().trim()
    private fun bounded(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (out.size() + count > RESPONSE_LIMIT) throw ImportError(Problem.FORMAT)
            out.write(buffer, 0, count)
        }
        return out.toByteArray()
    }
}
