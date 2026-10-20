package com.example.test

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.test.alarm.ExperienceReminder
import com.example.test.alarm.ReminderResult
import com.example.test.data.AppRepository
import com.example.test.data.RoutineSource
import com.example.test.focus.FocusPhase
import com.example.test.focus.FocusTimerStore
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.util.Locale

/** One device-local timer. Completion remains an explicit, guarded repository operation. */
class FocusActivity : AppCompatActivity() {
    private lateinit var store: FocusTimerStore
    private val handler = Handler(Looper.getMainLooper())
    private var routineId = 0L
    private var title = ""
    private var minutes = 10
    private var decision: AlertDialog? = null
    private var routineMissing = false
    private var incomingHandled = false
    private var alertResult: ReminderResult? = null
    private var finishingTimer = false
    private val tick = object : Runnable {
        override fun run() { render(); handler.postDelayed(this, 1000) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_focus)
        applySystemBarInsets()
        ScreenToolbar.bind(this, R.string.polish_focus_title, back = true)
        store = FocusTimerStore(applicationContext)
        minutes = savedInstanceState?.getInt("minutes", 10) ?: 10
        routineId = savedInstanceState?.getLong("routineId") ?: 0L
        title = savedInstanceState?.getString("routineTitle").orEmpty()
        routineMissing = savedInstanceState?.getBoolean("routineMissing") ?: false
        incomingHandled = savedInstanceState?.getBoolean("incomingHandled") ?: false
        findViewById<View>(R.id.focus_notification_settings).setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }
        val group = findViewById<ChipGroup>(R.id.focus_durations)
        listOf(5, 10, 25).forEach { value ->
            group.addView(Chip(this).apply {
                id = View.generateViewId(); text = getString(R.string.polish_focus_minutes, value)
                isCheckable = true; isChecked = value == minutes
                minHeight = (48 * resources.displayMetrics.density).toInt()
                setOnClickListener { minutes = value; render() }
            })
        }
        findViewById<View>(R.id.focus_primary).setOnClickListener {
            if (store.read()?.remainingMillis == 0L) { openRoutine(); return@setOnClickListener }
            when (store.read()?.phase) {
                FocusPhase.RUNNING -> { store.pause(); cancelAlert(); render() }
                FocusPhase.PAUSED -> { store.resume(); scheduleAlert(); render() }
                FocusPhase.FINISHED -> openRoutine()
                null -> if (routineId > 0 && title.isNotBlank()) { store.start(routineId, title, minutes); scheduleAlert(); render() }
            }
        }
        findViewById<View>(R.id.focus_reset).setOnClickListener {
            decision?.dismiss()
            decision = AlertDialog.Builder(this).setMessage(R.string.polish_focus_reset_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.polish_focus_reset) { _, _ -> store.clear(); alertResult = null; cancelAlert(); render() }.show()
        }
        val current = store.read()
        if (current != null) { routineId = current.routineId; title = current.title }
        val incomingId = intent.getLongExtra(TimelineActivity.USER_ROUTINE_ID, 0)
        if (!incomingHandled && incomingId > 0 && (current == null || current.routineId != incomingId)) {
            AppIo.run(this, { AppRepository(applicationContext).use { it.getRoutine(incomingId) } }) { result ->
                val routine = result.getOrNull()
                if (routine == null) { if (current == null) { routineMissing = true; render() }; return@run }
                fun useIncoming() { routineMissing = false; incomingHandled = true; store.clear(); alertResult = null; cancelAlert(); routineId = routine.id; title = routine.title; render() }
                if (current == null || current.phase == FocusPhase.FINISHED) useIncoming()
                else {
                    decision?.dismiss()
                    decision = AlertDialog.Builder(this).setTitle(R.string.polish_focus_other).setMessage(current.title)
                        .setNegativeButton(R.string.polish_focus_keep) { _, _ -> incomingHandled = true }
                        .setPositiveButton(R.string.polish_focus_replace) { _, _ -> useIncoming() }.show()
                }
            }
        }
        render()
    }

    override fun onResume() {
        super.onResume(); handler.removeCallbacks(tick); handler.post(tick)
        if (store.read()?.phase == FocusPhase.RUNNING) scheduleAlert()
    }
    override fun onPause() { handler.removeCallbacks(tick); super.onPause() }
    override fun onDestroy() { decision?.dismiss(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("minutes", minutes)
        outState.putLong("routineId", routineId)
        outState.putString("routineTitle", title)
        outState.putBoolean("incomingHandled", incomingHandled)
        outState.putBoolean("routineMissing", routineMissing)
        super.onSaveInstanceState(outState)
    }

    private fun render() {
        val session = store.read()
        if (!finishingTimer && session?.phase == FocusPhase.RUNNING && session.remainingMillis == 0L) {
            finishingTimer = true
            // Let the receiver post the same-channel alert, also when the activity is visible.
            AppIo.run(this, { ExperienceReminder(applicationContext).finishFocusIfDue() }) { result ->
                finishingTimer = false
                if (result.isSuccess && store.read()?.phase == FocusPhase.FINISHED) render()
            }
        }
        val finished = session?.phase == FocusPhase.FINISHED || session?.remainingMillis == 0L
        val seconds = ((session?.remainingMillis ?: minutes * 60_000L) + 999) / 1000
        findViewById<TextView>(R.id.focus_title).text = session?.title ?: title
        findViewById<TextView>(R.id.focus_clock).apply {
            text = String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
            contentDescription = getString(R.string.polish_focus_time_label, seconds / 60, seconds % 60)
        }
        findViewById<TextView>(R.id.focus_phase).setText(when {
            finished -> R.string.polish_focus_finished
            session?.phase == FocusPhase.RUNNING -> R.string.polish_focus_running
            session?.phase == FocusPhase.PAUSED -> R.string.polish_focus_paused
            else -> R.string.polish_focus_ready
        })
        findViewById<LinearProgressIndicator>(R.id.focus_progress).progress = session?.let {
            ((it.totalMillis - it.remainingMillis) * 1000 / it.totalMillis).toInt()
        } ?: 0
        findViewById<View>(R.id.focus_durations).visibility = if (session == null) View.VISIBLE else View.GONE
        findViewById<MaterialButton>(R.id.focus_primary).apply {
            isEnabled = !routineMissing && routineId > 0 && title.isNotBlank()
            setText(when {
                finished -> R.string.polish_focus_back
                session?.phase == FocusPhase.RUNNING -> R.string.polish_focus_pause
                session?.phase == FocusPhase.PAUSED -> R.string.polish_focus_resume
                else -> R.string.polish_focus_start
            })
        }
        findViewById<View>(R.id.focus_reset).visibility = if (session == null) View.GONE else View.VISIBLE
        val alert = when {
            routineMissing -> getString(R.string.polish_focus_missing)
            finished -> getString(R.string.polish_focus_reward_notice)
            session?.phase != FocusPhase.RUNNING -> ""
            alertResult == ReminderResult.APPROXIMATE -> getString(R.string.polish_focus_alert_approximate)
            alertResult == ReminderResult.BLOCKED -> getString(R.string.upgrade_focus_blocked)
            alertResult == ReminderResult.DISABLED -> getString(R.string.polish_focus_alert_off)
            alertResult == ReminderResult.FAILED -> getString(R.string.polish_preview_error)
            else -> ""
        }
        findViewById<TextView>(R.id.focus_alert).text = alert
        findViewById<View>(R.id.focus_notification_settings).visibility =
            if (!routineMissing && !finished && session?.phase == FocusPhase.RUNNING && alert.isNotBlank()) View.VISIBLE else View.GONE
    }

    private fun scheduleAlert() {
        val token = store.read()?.token ?: return
        AppIo.run(this, { ExperienceReminder(applicationContext).scheduleFocus() }) { result ->
            // A response for an old timer must not replace the current completion message.
            if (store.read()?.token != token) return@run
            alertResult = result.getOrDefault(ReminderResult.FAILED)
            render()
        }
    }
    private fun cancelAlert() { AppIo.run(this, { ExperienceReminder(applicationContext).cancelFocus() }) { } }
    private fun openRoutine() {
        val id = store.read()?.routineId ?: routineId
        AppIo.run(this, { AppRepository(applicationContext).use { it.getRoutine(id) } }) { result ->
            val routine = result.getOrNull()
            if (routine == null) { routineMissing = true; render() }
            else {
                startActivity(Intent(this, if (routine.source == RoutineSource.HERO_DAY) MainActivity::class.java else RoutineActivity::class.java)
                    .putExtra(TimelineActivity.USER_ROUTINE_ID, routine.id).putExtra(TimelineActivity.SELECTED_DATE, com.example.test.Routine.RoutineDate.today().iso)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                finish()
            }
        }
    }
}
