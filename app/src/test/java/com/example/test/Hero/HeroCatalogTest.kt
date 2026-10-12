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
        assertEquals("뉴턴", HeroCatalog.getOrDefault("newton").name)
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
            setOf("sejong", "einstein", "newton", "da_vinci"),
            HeroCatalog.filterByField("과학").map { it.id }.toSet()
        )
        assertEquals(
            setOf("sejong", "yi_sun_sin"),
            HeroCatalog.filterByField("정치").map { it.id }.toSet()
        )
    }

    @Test
    fun categoriesUseWholeNamesAndEmptyCategoriesReturnEmptyLists() {
        assertTrue(HeroCatalog.fields.contains("음악"))
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
}
