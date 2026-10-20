package com.example.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.test.timetable.EverytimeShareImporter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the XML parser on Android, where desktop DocumentBuilder features differed. */
@RunWith(AndroidJUnit4::class)
class EverytimeShareParserDeviceTest {
    @Test fun publicShareXmlParsesOnDevice() {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
            <response><table status="1"><subject><name value="예시 수업"/>
            <professor value="예시 교수"/><time><data day="0" starttime="108"
            endtime="132" place="예시 강의실"/></time></subject></table></response>"""
        val preview = EverytimeShareImporter.parse(xml.byteInputStream())
        assertEquals(1, preview.entries.size)
        assertEquals(1, preview.entries.single().weekday)
        assertEquals(540, preview.entries.single().startMinute)
    }
}
