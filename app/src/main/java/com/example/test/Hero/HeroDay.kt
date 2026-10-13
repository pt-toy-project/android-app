package com.example.test.Hero

/** Evidence describes the block, never its editable notification time. */
enum class EvidenceGrade(val label: String) { RECORDED("기록"), RECONSTRUCTED("재구성"), MODERN("현대 적용") }
enum class DayKind(val label: String) { WAKE("기상"), WORK("일·연구"), MEAL("식사"), LEARN("학습"), WALK("운동·산책"), REST("휴식"), REFLECT("성찰·기록"), SLEEP("취침") }
data class HeroDay(val heroId: String, val blocks: List<HeroDayBlock>)
data class HeroDayBlock(
    val id: String,
    val kind: DayKind,
    val title: String,
    val suggestedTime: String,
    val description: String,
    val source: HistoricalSource,
    val grade: EvidenceGrade,
    val historicalTime: String? = null,
    val recordedActionVerified: Boolean = false,
    val recordedTimeVerified: Boolean = false
) {
    init {
        require(id.isNotBlank() && title.isNotBlank() && description.isNotBlank())
        require(grade != EvidenceGrade.RECORDED ||
            (recordedActionVerified && recordedTimeVerified && !historicalTime.isNullOrBlank()))
    }
}

object HeroDayCatalog {
    const val NOTICE = "기록에 없는 활동과 시각은 앱 제안입니다. 알림 시각은 직접 바꿀 수 있습니다."
    fun forHero(hero: Hero): HeroDay {
        val first = hero.routines.first()
        val last = hero.routines.getOrNull(1)
        val learning = hero.routines.getOrNull(2)
        val theme = first.title
        val profile = DayEditorialProfiles.forHero(hero.id)
        val titles = listOf("기상", first.title, "식사", learning?.title ?: "학습", "운동·산책", "휴식", last?.title ?: "성찰·기록", "취침")
        val times = listOf("07:00", first.suggestedTime, "12:00", learning?.suggestedTime ?: "14:00", "17:00", "18:30", last?.suggestedTime ?: "20:00", "22:30")
        val descriptions = listOf(
            "오늘의 $theme 실천을 떠올리며 몸과 공간을 정돈합니다.", first.modernPractice,
            "${hero.name}의 주제에서 잠시 벗어나 식사를 천천히 하고 몸 상태를 살핍니다.",
            learning?.modernPractice ?: profile.learning,
            profile.walking,
            profile.rest, last?.modernPractice ?: "오늘 배운 점을 한 줄 적습니다.",
            "오늘의 $theme 실천을 마무리하고 수면을 준비합니다."
        )
        return HeroDay(hero.id, DayKind.entries.mapIndexed { index, kind ->
            val template = when (kind) { DayKind.WORK -> first; DayKind.REFLECT -> last; DayKind.LEARN -> learning; else -> null }
            HeroDayBlock(template?.id ?: "${hero.id}_day_${kind.name.lowercase(java.util.Locale.ROOT)}",
                kind, titles[index], times[index], descriptions[index], template?.source ?: first.source,
                // Source sites were actually requested again; proxy CONNECT 403 prevented reading.
                // Retain v2 source locations separately, but do not upgrade them to verified daily actions.
                EvidenceGrade.MODERN, template?.historicalTime)
        })
    }
    fun find(heroId: String, blockId: String): HeroDayBlock? =
        HeroCatalog.all.firstOrNull { it.id == heroId }?.let { forHero(it).blocks.firstOrNull { b -> b.id == blockId } }
}
