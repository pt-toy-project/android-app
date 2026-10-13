package com.example.test.Hero

/** Historical templates stay separate from editable user routines and completion records. */
data class Hero(
    val id: String,
    val name: String,
    val field: String,
    val info: String,
    val imageResId: Int,
    val bio: String = "",
    val routines: List<HistoricalRoutine> = emptyList(),
    val portraitCredit: String = ""
)

data class HistoricalSource(
    val title: String,
    val url: String,
    val locator: String
)

/** suggestedTime is an editable app suggestion; historicalTime is present only in the source. */
data class HistoricalRoutine(
    val id: String,
    val title: String,
    val suggestedTime: String,
    val historicalFact: String,
    val modernPractice: String,
    val source: HistoricalSource,
    val historicalTime: String? = null
)
