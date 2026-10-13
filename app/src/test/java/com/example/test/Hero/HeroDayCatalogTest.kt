package com.example.test.Hero

import org.junit.Assert.*
import org.junit.Test

class HeroDayCatalogTest {
    @Test fun completeDaysHaveStableUniqueBlocksAndSeparateNotificationTimes() {
        assertEquals(30, HeroCatalog.all.size)
        val ids = mutableSetOf<String>()
        HeroCatalog.all.forEach { hero ->
            val day = HeroDayCatalog.forHero(hero)
            assertTrue(day.blocks.size in 5..8)
            assertEquals(DayKind.entries.toSet(), day.blocks.map { it.kind }.toSet())
            assertEquals(hero.id, day.heroId)
            day.blocks.forEach { block ->
                assertTrue(ids.add(block.id))
                java.time.LocalTime.parse(block.suggestedTime)
                assertTrue(block.description.isNotBlank())
                assertTrue(block.source.title.isNotBlank() && block.source.url.startsWith("https://") && block.source.locator.isNotBlank())
                if (block.grade == EvidenceGrade.RECORDED) {
                    assertTrue(block.recordedActionVerified && block.recordedTimeVerified)
                    assertFalse(block.historicalTime.isNullOrBlank())
                }
            }
            assertEquals(hero.routines.map { it.id }.toSet(), day.blocks.filter { b -> hero.routines.any { it.id == b.id } }.map { it.id }.toSet())
            assertEquals(day, HeroDayCatalog.forHero(hero))
        }
    }
    @Test fun recordedRequiresBothActionAndTimeVerified() {
        val block = HeroDayCatalog.forHero(HeroCatalog.default).blocks.first()
        assertThrows(IllegalArgumentException::class.java) { block.copy(grade = EvidenceGrade.RECORDED) }
        assertThrows(IllegalArgumentException::class.java) { block.copy(grade = EvidenceGrade.RECORDED, recordedActionVerified = true, historicalTime = "아침") }
        assertEquals(EvidenceGrade.RECORDED, block.copy(grade = EvidenceGrade.RECORDED,
            recordedActionVerified = true, recordedTimeVerified = true, historicalTime = "아침").grade)
    }
    @Test fun unverifiedSourcesNeverBecomeHistoricalDailyActions() {
        assertTrue(HeroCatalog.all.flatMap { HeroDayCatalog.forHero(it).blocks }.all { it.grade == EvidenceGrade.MODERN })
        listOf("newton", "da_vinci", "nightingale", "shakespeare").forEach { id ->
            assertTrue(HeroDayCatalog.forHero(HeroCatalog.getOrDefault(id)).blocks.all { it.grade == EvidenceGrade.MODERN })
        }
        assertTrue(HeroCatalog.getOrDefault("franklin").bio.contains("계획"))
        assertFalse(HeroCatalog.all.any { it.id in setOf("jang_yeong_sil", "hokusai") })
        assertTrue(HeroCatalog.all.drop(15).all { it.imageResId == com.example.test.R.drawable.avatar_default })
    }
}
