package com.example.test.focus

import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FocusTimerStoreTest {
    private lateinit var context: Context
    private var elapsed = 10_000L
    private var boot = 1
    private fun store() = FocusTimerStore(context, { elapsed }, { boot })
    @Before fun before() { context = RuntimeEnvironment.getApplication(); context.deleteSharedPreferences("focus_timer") }

    @Test fun reopeningUsesElapsedTimeAndPauseDoesNotConsumeRestTime() {
        store().start(42, "연구", 10)
        elapsed += 90_000
        assertEquals(510_000L, store().read()!!.remainingMillis)
        assertEquals(FocusPhase.PAUSED, store().pause()!!.phase)
        elapsed += 300_000
        assertEquals(510_000L, store().read()!!.remainingMillis)
        assertEquals(FocusPhase.RUNNING, store().resume()!!.phase)
        elapsed += 1000
        assertEquals(509_000L, store().read()!!.remainingMillis)
    }
    @Test fun onlyCurrentExpiredRunningTokenCanFinishOnce() {
        val old = store().start(1, "읽기", 1)
        assertNull(store().finish(old.token))
        val current = store().start(2, "쓰기", 1)
        elapsed += 60_000
        assertNull(store().finish(old.token))
        assertNotNull(store().finish(current.token))
        assertNull(store().finish(current.token))
        assertEquals(FocusPhase.FINISHED, store().read()!!.phase)
    }
    @Test fun pauseAndResetInvalidateQueuedEndEvents() {
        val session = store().start(1, "읽기", 1)
        elapsed += 20_000
        store().pause(); elapsed += 60_000
        assertNull(store().finish(session.token))
        store().clear()
        assertNull(store().read()); assertNull(store().finish(session.token))
    }
    @Test fun rebootPausesInsteadOfUsingDeadlineFromPreviousBoot() {
        val session = store().start(1, "관찰", 5)
        boot++; elapsed = 500
        assertEquals(FocusPhase.PAUSED, store().read()!!.phase)
        assertNull(store().finish(session.token))
        assertEquals(300_000L, store().resume()!!.remainingMillis)
    }
    @Test fun invalidDurationAndMissingRoutineCannotStartATimer() {
        listOf(0, 61).forEach { minutes ->
            assertThrows(IllegalArgumentException::class.java) { store().start(1, "읽기", minutes) }
        }
        assertThrows(IllegalArgumentException::class.java) { store().start(0, "읽기", 10) }
        assertNull(store().read())
    }
}
