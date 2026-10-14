package com.example.test.focus

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import java.util.UUID

enum class FocusPhase { RUNNING, PAUSED, FINISHED }
data class FocusSession(val token: String, val routineId: Long, val title: String,
    val totalMillis: Long, val remainingMillis: Long, val phase: FocusPhase)

/** Device-local elapsed time survives recreation and wall-clock changes. Never awards points. */
class FocusTimerStore(context: Context,
    private val elapsed: () -> Long = SystemClock::elapsedRealtime,
    private val boot: () -> Int = { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0) }) {
    private val prefs = context.applicationContext.getSharedPreferences("focus_timer", Context.MODE_PRIVATE)

    fun read(): FocusSession? = synchronized(LOCK) {
        val token = prefs.getString("token", null) ?: return@synchronized null
        val total = prefs.getLong("total", 0)
        if (total !in 60_000L..3_600_000L) return@synchronized null
        var phase = runCatching { FocusPhase.valueOf(prefs.getString("phase", "PAUSED")!!) }.getOrDefault(FocusPhase.PAUSED)
        val saved = prefs.getLong("remaining", total).coerceIn(0, total)
        val rebooted = prefs.getInt("boot", -1) != boot()
        val remaining = if (phase == FocusPhase.RUNNING && !rebooted)
            (prefs.getLong("deadline", 0) - elapsed()).coerceIn(0, total) else saved
        if (phase == FocusPhase.RUNNING && rebooted) {
            phase = FocusPhase.PAUSED
            check(prefs.edit().putString("phase", phase.name).putInt("boot", boot()).commit())
        }
        FocusSession(token, prefs.getLong("routine", 0), prefs.getString("title", "").orEmpty(), total, remaining, phase)
    }

    fun start(routineId: Long, title: String, minutes: Int): FocusSession = synchronized(LOCK) {
        require(routineId > 0 && minutes in 1..60 && title.isNotBlank())
        val total = minutes * 60_000L
        val session = FocusSession(UUID.randomUUID().toString(), routineId, title.take(200), total, total, FocusPhase.RUNNING)
        save(session)
        session
    }

    fun pause(): FocusSession? = synchronized(LOCK) {
        read()?.let { current ->
            if (current.phase != FocusPhase.RUNNING) current
            else current.copy(phase = if (current.remainingMillis == 0L) FocusPhase.FINISHED else FocusPhase.PAUSED).also(::save)
        }
    }

    fun resume(): FocusSession? = synchronized(LOCK) {
        read()?.takeIf { it.phase == FocusPhase.PAUSED && it.remainingMillis > 0 }
            ?.copy(phase = FocusPhase.RUNNING)?.also(::save)
    }

    /** Token + monotonic deadline prevents stale, duplicate and early broadcasts. */
    fun finish(token: String): FocusSession? = synchronized(LOCK) {
        read()?.takeIf { it.token == token && it.phase == FocusPhase.RUNNING && it.remainingMillis == 0L }
            ?.copy(phase = FocusPhase.FINISHED)?.also(::save)
    }

    fun clear() = synchronized(LOCK) { check(prefs.edit().clear().commit()) }

    private fun save(session: FocusSession) {
        check(prefs.edit().putString("token", session.token).putLong("routine", session.routineId)
            .putString("title", session.title).putLong("total", session.totalMillis)
            .putLong("remaining", session.remainingMillis).putString("phase", session.phase.name)
            .putLong("deadline", elapsed() + session.remainingMillis).putInt("boot", boot()).commit())
    }

    companion object { private val LOCK = Any() }
}
