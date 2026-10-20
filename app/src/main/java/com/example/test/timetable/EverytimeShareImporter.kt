package com.example.test.timetable

import com.example.test.data.TimetableStore
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

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
        val document = try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
                isXIncludeAware = false
                isExpandEntityReferences = false
            }
            factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
        } catch (_: Exception) { throw ImportError(Problem.FORMAT) }
        val root = document.documentElement ?: throw ImportError(Problem.FORMAT)
        if (root.tagName != "response") throw ImportError(Problem.FORMAT)
        if (root.textContent.trim() == "-1") throw ImportError(Problem.UNAVAILABLE)
        val table = root.getElementsByTagName("table").item(0) as? Element ?: throw ImportError(Problem.UNAVAILABLE)
        when (table.getAttribute("status")) {
            "-2" -> throw ImportError(Problem.PRIVATE)
            "-1" -> throw ImportError(Problem.UNAVAILABLE)
        }
        val subjects = table.getElementsByTagName("subject")
        val rows = ArrayList<TimetableEntry>()
        var unscheduled = 0
        for (index in 0 until subjects.length) {
            val subject = subjects.item(index) as? Element ?: throw ImportError(Problem.FORMAT)
            val name = subject.childValue("name")
            val professor = subject.childValue("professor")
            val fallbackPlace = subject.childValue("place")
            val times = subject.getElementsByTagName("data")
            if (times.length == 0) { unscheduled++; continue }
            for (position in 0 until times.length) {
                val slot = times.item(position) as? Element ?: throw ImportError(Problem.FORMAT)
                val weekday = (slot.getAttribute("day").toIntOrNull() ?: throw ImportError(Problem.FORMAT)) + 1
                val start = (slot.getAttribute("starttime").toIntOrNull() ?: throw ImportError(Problem.FORMAT)) * 5
                val end = (slot.getAttribute("endtime").toIntOrNull() ?: throw ImportError(Problem.FORMAT)) * 5
                val place = slot.getAttribute("place").ifBlank { fallbackPlace }
                val row = try { TimetableEntry(title = name, weekday = weekday, startMinute = start,
                    endMinute = end, location = place, instructor = professor).normalized() }
                catch (_: IllegalArgumentException) { throw ImportError(Problem.FORMAT) }
                rows.add(row)
                if (rows.size > TimetableStore.MAX_ENTRIES) throw ImportError(Problem.TOO_MANY)
            }
        }
        if (rows.isEmpty()) throw ImportError(Problem.EMPTY)
        return Preview(rows.distinctBy { it.identity }, unscheduled)
    }

    private fun Element.childValue(tag: String): String {
        val child = childNodes.asSequence().filterIsInstance<Element>().firstOrNull { it.tagName == tag }
        return child?.getAttribute("value").orEmpty().trim()
    }
    private fun org.w3c.dom.NodeList.asSequence(): Sequence<Node> = sequence {
        for (index in 0 until length) yield(item(index))
    }
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
