package com.example.test.Hero

import org.junit.Assert.*
import org.junit.Test

class HeroQuestCatalogTest {
    @Test fun everyExistingHeroHasStableUniqueActionableModernTasks() {
        assertEquals(HeroCatalog.all.map { it.id }.toSet(), HeroQuestCatalog.profiles.keys)
        val ids = mutableSetOf<String>()
        HeroCatalog.all.forEach { hero ->
            val day = HeroDayCatalog.forHero(hero)
            day.blocks.forEach { block ->
                assertTrue(block.quests.size in 2..4)
                block.quests.forEach { quest ->
                    assertTrue(ids.add(quest.id))
                    assertTrue(quest.id.length <= 160)
                    assertTrue(quest.title.isNotBlank() && quest.instruction.isNotBlank() && quest.measure.isNotBlank())
                    assertEquals(EvidenceGrade.MODERN, quest.grade)
                    assertEquals(block.source, quest.source)
                }
            }
            assertEquals(day, HeroDayCatalog.forHero(hero))
        }
    }

    @Test fun coreExercisesAreSpecificToEachHeroAndAllGoalsHaveExamples() {
        assertEquals(30, HeroQuestCatalog.profiles.values.map { it.work }.toSet().size)
        assertEquals(HeroQuestCatalog.goals.drop(1).toSet(), HeroQuestCatalog.profiles.values.map { it.goal }.toSet())
        assertTrue(HeroQuestCatalog.profiles.getValue("ada_lovelace").work.any { it.contains("입력·처리·결과") })
        assertTrue(HeroQuestCatalog.profiles.getValue("shakespeare").work.any { it.contains("가상 인물") })
        assertTrue(HeroQuestCatalog.profiles.getValue("franklin").learn.any { it.contains("시간표") })
        assertFalse(HeroCatalog.all.any { it.id == "pele" }) // Pelé was an example, not an approved addition.
    }

    @Test fun threeMealsAreExplicitWithoutAddingOrRenamingExistingBlockIdentities() {
        HeroCatalog.all.forEach { hero ->
            val blocks = HeroDayCatalog.forHero(hero).blocks
            assertEquals(8, blocks.size)
            assertTrue(blocks.single { it.kind == DayKind.WAKE }.quests.any { it.title == "아침 식사" })
            assertTrue(blocks.single { it.kind == DayKind.MEAL }.quests.any { it.title == "점심 식사" })
            assertTrue(blocks.single { it.kind == DayKind.REST }.quests.any { it.title == "저녁 식사" })
            assertTrue(hero.routines.all { old -> blocks.any { it.id == old.id } })
        }
    }
}
