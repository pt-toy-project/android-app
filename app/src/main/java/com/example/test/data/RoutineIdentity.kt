package com.example.test.data

import com.example.test.Hero.HeroCatalog

/** Personal plans never acquire another person's identity through a catalog fallback. */
object RoutineIdentity {
    const val PERSONAL_HERO_ID = "personal"
    fun displayName(heroId: String): String =
        HeroCatalog.all.firstOrNull { it.id == heroId }?.name ?: "나만의 계획"
}
