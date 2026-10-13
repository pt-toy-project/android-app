package com.example.test

import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.example.test.Hero.Hero
import com.example.test.Hero.HeroCatalog
import com.example.test.Hero.HistoricalRoutine
import com.example.test.data.AppRepository
import com.example.test.alarm.AlarmCoordinator
import java.util.Locale

/** The editorial catalog is read-only; adopting a habit creates a personal database snapshot. */
class HeroDetailActivity : AppCompatActivity() {
    private var hero: Hero? = null
    private val addButtons = mutableMapOf<String, Button>()
    private var adoptedTemplateIds = emptySet<String>()
    private var storageReady = false
    private var openingRoutines = false
    private var addDialog: AlertDialog? = null
    private var timeDialog: TimePickerDialog? = null
    private var savingTemplateId: String? = null
    private var pendingTemplateId: String? = null
    private var pendingTime = "08:00"
    private var pendingWeekdayMask = 127

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hero_detail)
        applySystemBarInsets()
        findViewById<Button>(R.id.heroDetailBack).setOnClickListener { finish() }
        // An invalid deep link must not silently show a different person's content.
        hero = HeroCatalog.all.firstOrNull { it.id == intent.getStringExtra(EXTRA_HERO_ID) }
        val person = hero
        if (person == null) {
            findViewById<TextView>(R.id.heroDetailError).visibility = View.VISIBLE
            findViewById<View>(R.id.heroDetailContent).visibility = View.GONE
            return
        }
        bindHero(person)
        pendingTemplateId = savedInstanceState?.getString(STATE_TEMPLATE)
        pendingTime = savedInstanceState?.getString(STATE_TIME) ?: "08:00"
        pendingWeekdayMask = savedInstanceState?.getInt(STATE_WEEKDAYS, 127) ?: 127
        findViewById<Button>(R.id.heroStorageRetry).setOnClickListener { loadAdoptedRoutines() }
        findViewById<Button>(R.id.heroPersonalRoutines).setOnClickListener { openPersonalRoutines() }
    }

    override fun onResume() {
        super.onResume()
        openingRoutines = false
        findViewById<Button>(R.id.heroPersonalRoutines).isEnabled = hero != null
        if (hero != null) loadAdoptedRoutines()
        val appContext = applicationContext
        AppIo.run(this, { AlarmCoordinator(appContext).reconcile() }) { result ->
            val restoreFailed = result.isFailure || result.getOrNull()?.errors?.isNotEmpty() == true
            findViewById<TextView>(R.id.heroDetailAlarmStatus).visibility = if (restoreFailed) View.VISIBLE else View.GONE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_TEMPLATE, pendingTemplateId)
        outState.putString(STATE_TIME, pendingTime)
        outState.putInt(STATE_WEEKDAYS, pendingWeekdayMask)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        timeDialog?.dismiss()
        addDialog?.dismiss()
        super.onDestroy()
    }

    private fun bindHero(person: Hero) {
        findViewById<TextView>(R.id.heroDetailName).text = person.name
        findViewById<TextView>(R.id.heroDetailField).text = getString(R.string.hero_detail_metadata, person.field, person.info)
        findViewById<TextView>(R.id.heroDetailBio).text = person.bio
        findViewById<ImageView>(R.id.heroDetailPortrait).apply {
            setImageResource(person.imageResId)
            contentDescription = getString(R.string.hero_portrait_description, person.name)
        }
        findViewById<TextView>(R.id.heroPortraitCredit).text = person.portraitCredit
        val portraitUrl = Regex("https?://[^\\s]+").find(person.portraitCredit)?.value
        findViewById<Button>(R.id.heroPortraitSource).apply {
            visibility = if (portraitUrl == null) View.GONE else View.VISIBLE
            setOnClickListener { portraitUrl?.let(::openWebSource) }
        }
        val routines = findViewById<LinearLayout>(R.id.historicalRoutineContainer)
        person.routines.forEach { routine ->
            val card = layoutInflater.inflate(R.layout.item_historical_routine, routines, false)
            card.findViewById<TextView>(R.id.historicalRoutineTitle).text = routine.title
            card.findViewById<TextView>(R.id.historicalFactText).text = routine.historicalFact
            card.findViewById<TextView>(R.id.modernPracticeText).text = routine.modernPractice
            card.findViewById<TextView>(R.id.historicalSuggestedTime).text = getString(R.string.hero_time_heading, routine.suggestedTime)
            card.findViewById<TextView>(R.id.historicalRecordedTime).apply {
                visibility = if (routine.historicalTime.isNullOrBlank()) View.GONE else View.VISIBLE
                text = getString(R.string.hero_historical_time, routine.historicalTime.orEmpty())
            }
            card.findViewById<TextView>(R.id.historicalSourceText).text = getString(
                R.string.hero_source_citation, routine.source.title, routine.source.locator, routine.source.url
            )
            card.findViewById<Button>(R.id.historicalSourceButton).setOnClickListener { openWebSource(routine.source.url) }
            val addButton = card.findViewById<Button>(R.id.historicalAddButton)
            addButtons[routine.id] = addButton
            addButton.isEnabled = false
            addButton.setOnClickListener { showAddDialog(routine) }
            routines.addView(card)
        }
    }

    private fun loadAdoptedRoutines() {
        val person = hero ?: return
        val appContext = applicationContext
        AppIo.run(this, {
            AppRepository(appContext).use { repository ->
                repository.listRoutines(person.id).map { it.templateId }.toSet()
            }
        }) { result ->
            storageReady = result.isSuccess
            result.onSuccess { adoptedTemplateIds = it }
            findViewById<TextView>(R.id.heroStorageError).visibility = if (result.isFailure) View.VISIBLE else View.GONE
            findViewById<Button>(R.id.heroStorageRetry).visibility = if (result.isFailure) View.VISIBLE else View.GONE
            updateAddButtons()
            if (storageReady && addDialog == null && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                pendingTemplateId?.let { templateId ->
                    person.routines.firstOrNull { it.id == templateId && it.id !in adoptedTemplateIds }?.let {
                        showAddDialog(it, restore = true)
                    } ?: run { pendingTemplateId = null }
                }
            }
        }
    }

    private fun updateAddButtons() {
        addButtons.forEach { (templateId, button) ->
            val adopted = templateId in adoptedTemplateIds
            button.setText(if (adopted) R.string.hero_already_added else R.string.hero_add_routine)
            button.isEnabled = storageReady && !adopted && savingTemplateId == null
        }
    }

    private fun showAddDialog(routine: HistoricalRoutine, restore: Boolean = false) {
        if (!storageReady || savingTemplateId != null || routine.id in adoptedTemplateIds ||
            !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        addDialog?.dismiss()
        pendingTemplateId = routine.id
        if (!restore) {
            pendingTime = routine.suggestedTime
            pendingWeekdayMask = 127
        }
        val view = layoutInflater.inflate(R.layout.dialog_add_historical_routine, null)
        val timeButton = view.findViewById<Button>(R.id.historicalTimePicker)
        fun updateTimeLabel() { timeButton.text = getString(R.string.hero_set_time, pendingTime) }
        updateTimeLabel()
        timeButton.setOnClickListener {
            val parts = pendingTime.split(':')
            timeDialog = TimePickerDialog(this, { _, hour, minute ->
                pendingTime = String.format(Locale.ROOT, "%02d:%02d", hour, minute)
                updateTimeLabel()
            }, parts[0].toInt(), parts[1].toInt(), DateFormat.is24HourFormat(this)).apply {
                setOnDismissListener { timeDialog = null }
                show()
            }
        }
        val weekdays = view.findViewById<LinearLayout>(R.id.historicalWeekdays)
        resources.getStringArray(R.array.hero_weekdays).forEachIndexed { day, name ->
            weekdays.addView(CheckBox(this).apply {
                text = name
                isChecked = pendingWeekdayMask and (1 shl day) != 0
                minHeight = (48 * resources.displayMetrics.density).toInt()
                setOnCheckedChangeListener { _, checked ->
                    pendingWeekdayMask = if (checked) pendingWeekdayMask or (1 shl day)
                        else pendingWeekdayMask and (1 shl day).inv()
                    view.findViewById<TextView>(R.id.historicalWeekdayError).visibility = View.GONE
                }
            })
        }
        addDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.hero_add_dialog_title, routine.title))
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.hero_add_routine, null)
            .create().apply {
                setOnDismissListener {
                    addDialog = null
                    if (!isChangingConfigurations) pendingTemplateId = null
                }
                show()
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    if (pendingWeekdayMask == 0) {
                        view.findViewById<TextView>(R.id.historicalWeekdayError).visibility = View.VISIBLE
                    } else {
                        adoptRoutine(routine, pendingTime, pendingWeekdayMask)
                    }
                }
            }
    }

    private fun adoptRoutine(routine: HistoricalRoutine, time: String, weekdayMask: Int) {
        val person = hero ?: return
        if (savingTemplateId != null) return
        savingTemplateId = routine.id
        updateAddButtons()
        addDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
        val appContext = applicationContext
        AppIo.run(this, {
            AppRepository(appContext).use { repository ->
                repository.addRoutine(person.id, routine.id, routine.title, time, weekdayMask)
                repository.selectedHeroId = person.id
                repository.listRoutines(person.id).map { it.templateId }.toSet()
            }
        }) { result ->
            savingTemplateId = null
            result.onSuccess { templates ->
                adoptedTemplateIds = templates
                addDialog?.dismiss()
                Toast.makeText(this, R.string.hero_added_message, Toast.LENGTH_LONG).show()
            }.onFailure {
                addDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
                Toast.makeText(this, R.string.hero_save_error, Toast.LENGTH_LONG).show()
            }
            updateAddButtons()
        }
    }

    private fun openPersonalRoutines() {
        val person = hero ?: return
        if (openingRoutines) return
        openingRoutines = true
        findViewById<Button>(R.id.heroPersonalRoutines).isEnabled = false
        val appContext = applicationContext
        AppIo.run(this, { AppRepository(appContext).use { it.selectedHeroId = person.id } }) { result ->
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                openingRoutines = false
                findViewById<Button>(R.id.heroPersonalRoutines).isEnabled = true
                return@run
            }
            result.onSuccess {
                startActivity(Intent(this, RoutineActivity::class.java)
                    .putExtra(EXTRA_HERO_ID, person.id)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            }.onFailure {
                openingRoutines = false
                findViewById<Button>(R.id.heroPersonalRoutines).isEnabled = true
                Toast.makeText(this, R.string.hero_save_error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openWebSource(url: String) {
        val uri = Uri.parse(url)
        if (uri.scheme !in listOf("https", "http") || uri.host.isNullOrBlank()) {
            Toast.makeText(this, R.string.hero_invalid_source, Toast.LENGTH_LONG).show()
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.hero_browser_unavailable, Toast.LENGTH_LONG).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, R.string.hero_browser_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val EXTRA_HERO_ID = "hero_id"
        private const val STATE_TEMPLATE = "pending_template"
        private const val STATE_TIME = "pending_time"
        private const val STATE_WEEKDAYS = "pending_weekdays"
    }
}
