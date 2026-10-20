package com.example.test.timetable

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/** Bounded UTF-8 CSV import. Parse all rows before changing the database. */
object TimetableCsv {
    private const val LIMIT = 512 * 1024
    private val header = listOf("title", "weekday", "start", "end", "location", "instructor")
    fun read(input: InputStream): List<TimetableEntry> {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            require(out.size() + read <= LIMIT)
            out.write(buffer, 0, read)
        }
        val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(out.toByteArray())).toString().removePrefix("\uFEFF")
        return parse(text)
    }
    fun parse(text: String): List<TimetableEntry> {
        require(text.toByteArray(Charsets.UTF_8).size <= LIMIT)
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>(); val field = StringBuilder()
        var quoted = false; var closed = false; var i = 0
        fun cell() { row.add(field.toString()); field.setLength(0); closed = false }
        fun line() { cell(); if (row.any { it.isNotEmpty() }) rows.add(row); row = mutableListOf(); require(rows.size <= 201) }
        while (i < text.length) {
            val ch = text[i++]
            if (quoted) {
                if (ch == '"') {
                    if (i < text.length && text[i] == '"') { field.append('"'); i++ }
                    else { quoted = false; closed = true }
                } else field.append(ch)
            } else when (ch) {
                '"' -> { require(field.isEmpty() && !closed); quoted = true }
                ',' -> cell()
                '\r', '\n' -> { if (ch == '\r' && i < text.length && text[i] == '\n') i++; line() }
                else -> { require(!closed); field.append(ch) }
            }
            require(field.length <= 1000 && row.size <= 6)
        }
        require(!quoted)
        if (field.isNotEmpty() || row.isNotEmpty() || closed) line()
        require(rows.isNotEmpty() && rows.first().map { it.trim() } == header)
        return rows.drop(1).map { values ->
            require(values.size == 6)
            val day = values[1].trim().let { it.toIntOrNull() ?: (TimetableEntry.weekdays.indexOf(it.removeSuffix("요일")) + 1) }
            TimetableEntry(title = values[0], weekday = day, startMinute = TimetableEntry.minute(values[2].trim()),
                endMinute = TimetableEntry.minute(values[3].trim()), location = values[4], instructor = values[5]).normalized()
        }.also { require(it.isNotEmpty()) }
    }
    fun export(entries: List<TimetableEntry>): ByteArray {
        fun encode(values: List<String>) = values.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
        return (encode(header) + "\r\n" + entries.joinToString("\r\n", postfix = if (entries.isEmpty()) "" else "\r\n") {
            encode(listOf(it.title, it.weekday.toString(), TimetableEntry.time(it.startMinute), TimetableEntry.time(it.endMinute), it.location, it.instructor))
        }).toByteArray(Charsets.UTF_8)
    }
}
