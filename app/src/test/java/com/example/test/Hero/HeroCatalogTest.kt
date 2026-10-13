package com.example.test.Hero

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HeroCatalogTest {
    @Test
    fun idsAreUniqueAndIndependentOfDisplayNames() {
        val ids = HeroCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it.matches(Regex("[a-z_]+")) })
        assertTrue(HeroCatalog.all.all { it.name.isNotBlank() && it.id != it.name })
    }

    @Test
    fun selectedHeroIdResolvesToItsOwnCard() {
        val selected = HeroCatalog.all.first { it.id == "newton" }
        assertSame(selected, HeroCatalog.getOrDefault("newton"))
        assertEquals("아이작 뉴턴", HeroCatalog.getOrDefault("newton").name)
        assertFalse(HeroCatalog.getOrDefault("newton") == HeroCatalog.default)
    }

    @Test
    fun missingOrUnknownSelectionUsesDefault() {
        assertSame(HeroCatalog.default, HeroCatalog.getOrDefault(null))
        assertSame(HeroCatalog.default, HeroCatalog.getOrDefault(""))
        assertSame(HeroCatalog.default, HeroCatalog.getOrDefault("unknown_hero"))
    }

    @Test
    fun allFieldsPreservesEveryHero() {
        assertEquals(HeroCatalog.all, HeroCatalog.filterByField(HeroCatalog.ALL_FIELDS))
        assertEquals(HeroCatalog.all, HeroCatalog.filterByField(null))
    }

    @Test
    fun scienceFilterIncludesHeroesWithMultipleFields() {
        assertEquals(
            setOf("sejong", "einstein", "newton", "da_vinci", "franklin", "darwin", "curie"),
            HeroCatalog.filterByField("과학").map { it.id }.toSet()
        )
        assertEquals(
            setOf("sejong", "yi_sun_sin", "franklin", "marcus_aurelius"),
            HeroCatalog.filterByField("정치").map { it.id }.toSet()
        )
    }

    @Test
    fun categoriesUseWholeNamesAndEmptyCategoriesReturnEmptyLists() {
        assertTrue(HeroCatalog.fields.contains("환경"))
        assertEquals(listOf("thoreau", "john_muir"), HeroCatalog.filterByField("환경").map { it.id })
        assertTrue(HeroCatalog.filterByField("음악").isEmpty())
        assertTrue(HeroCatalog.filterByField("과").isEmpty())
        assertTrue(HeroCatalog.filterByField("unknown_field").isEmpty())
    }

    @Test
    fun changingFilterDoesNotMutateCatalogOrSelection() {
        val selected = HeroCatalog.getOrDefault("shakespeare")
        val original = HeroCatalog.all.toList()
        HeroCatalog.filterByField("과학")
        HeroCatalog.filterByField("음악")
        assertEquals(original, HeroCatalog.all)
        assertSame(selected, HeroCatalog.getOrDefault("shakespeare"))
    }
    @Test
    fun expandedCatalogRetainsAllPreviouslyPersistedHeroIds() {
        assertTrue(HeroCatalog.all.size >= 15)
        val previousIds = setOf("sejong", "yi_sun_sin", "einstein", "newton", "da_vinci", "shakespeare")
        previousIds.forEach { id -> assertEquals(id, HeroCatalog.getOrDefault(id).id) }
        assertTrue(HeroCatalog.all.all { it.bio.isNotBlank() && it.routines.isNotEmpty() })
    }

    @Test
    fun templateIdentityRemainsOwnedByItsHero() {
        val routines = HeroCatalog.all.flatMap { it.routines }
        assertEquals(29, routines.size)
        assertEquals(routines.size, routines.map { it.id }.toSet().size)
        HeroCatalog.all.forEach { hero ->
            assertTrue(hero.routines.all { it.id.startsWith(hero.id + "_") })
        }
        assertEquals(routines.size, routines.map { it.historicalFact }.toSet().size)
    }

    @Test
    fun everySuggestionHasAValidLocalTimeAndSeparatePracticeAndEvidence() {
        HeroCatalog.all.flatMap { it.routines }.forEach { routine ->
            java.time.LocalTime.parse(routine.suggestedTime)
            assertTrue(routine.title.isNotBlank())
            assertTrue(routine.historicalFact.isNotBlank())
            assertTrue(routine.modernPractice.isNotBlank())
            assertFalse(routine.historicalFact == routine.modernPractice)
            assertTrue(routine.source.title.isNotBlank())
            assertTrue(routine.source.locator.isNotBlank())
            val uri = java.net.URI(routine.source.url)
            assertEquals("https", uri.scheme)
            assertTrue(uri.host.isNotBlank())
        }
    }

    @Test
    fun historicalTimesAreNotFilledFromAppSuggestedTimes() {
        val shakespeare = HeroCatalog.getOrDefault("shakespeare")
        assertTrue(shakespeare.bio.contains("개인 생활 시간표를 확인하지 못"))
        assertTrue(shakespeare.routines.all { it.historicalTime == null })
        val franklin = HeroCatalog.getOrDefault("franklin")
        assertEquals("계획표의 오전 5~7시 구간", franklin.routines.first().historicalTime)
        assertEquals("07:00", franklin.routines.first().suggestedTime)
        assertTrue(HeroCatalog.getOrDefault("darwin").routines.all { it.historicalTime == null })
    }

    @Test
    fun everyPortraitIsDistinctAndIncludesOriginalSourceAndRights() {
        val heroes = HeroCatalog.all
        assertEquals(heroes.size, heroes.map { it.imageResId }.toSet().size)
        assertTrue(heroes.all { it.imageResId != 0 })
        assertTrue(heroes.all { it.portraitCredit.contains("https://commons.wikimedia.org/wiki/File:") })
        assertTrue(heroes.all { it.portraitCredit.contains("Public domain") ||
                it.portraitCredit.contains("CC BY-SA") || it.portraitCredit.contains("KOGL Type 1") })
        assertTrue(HeroCatalog.getOrDefault("sejong").portraitCredit.contains("생전 초상화가 아닙니다"))
        assertTrue(HeroCatalog.getOrDefault("yi_sun_sin").portraitCredit.contains("생전 초상화가 아닙니다"))
    }
}
