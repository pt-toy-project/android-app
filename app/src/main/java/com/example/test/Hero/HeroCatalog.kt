package com.example.test.Hero

import com.example.test.R

/** Small development catalog; it is not a source of verified historical routines. */
object HeroCatalog {
    const val ALL_FIELDS = "전체"

    val fields: List<String> = listOf(ALL_FIELDS, "정치", "과학", "예술", "문학", "음악")

    val all: List<Hero> = listOf(
        Hero("sejong", "세종대왕", "정치 · 과학 · 문화", "조선 · 15세기", R.drawable.sesong),
        Hero("yi_sun_sin", "이순신", "군사 · 정치", "조선 · 16세기", R.drawable.ic_launcher_foreground),
        Hero("einstein", "아인슈타인", "과학 · 물리학", "독일 · 20세기", R.drawable.ic_launcher_foreground),
        Hero("newton", "뉴턴", "과학 · 수학", "영국 · 17세기", R.drawable.ic_launcher_foreground),
        Hero("da_vinci", "레오나르도 다 빈치", "예술 · 과학", "이탈리아 · 15세기", R.drawable.ic_launcher_foreground),
        Hero("shakespeare", "셰익스피어", "문학 · 연극", "영국 · 16세기", R.drawable.ic_launcher_foreground)
    )

    val default: Hero = all.first()

    fun getOrDefault(id: String?): Hero = all.firstOrNull { it.id == id } ?: default

    fun filterByField(field: String?): List<Hero> = when (field) {
        null, ALL_FIELDS -> all
        else -> all.filter { hero -> hero.field.split('·').any { it.trim() == field } }
    }
}
