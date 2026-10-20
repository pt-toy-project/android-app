package com.example.test.timetable

import java.net.URI

/** Only an explicit public timetable URL can be opened; never accepts credentials or executable schemes. */
object EverytimeLink {
    fun parse(text: String): String? = runCatching {
        val uri = URI(text.trim())
        require(uri.scheme == "https" && uri.host in setOf("everytime.kr", "www.everytime.kr"))
        require(uri.rawUserInfo == null && uri.port == -1 && uri.rawQuery == null && uri.rawFragment == null)
        require(uri.rawPath.matches(Regex("/@[A-Za-z0-9]{1,100}")))
        "https://everytime.kr${uri.rawPath}"
    }.getOrNull()
    fun fromSharedText(text: String) = Regex("https://[^\\s<>]+")
        .findAll(text.take(4000)).mapNotNull { parse(it.value) }.firstOrNull()
}
