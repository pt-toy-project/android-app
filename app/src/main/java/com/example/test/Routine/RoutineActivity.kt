package com.example.test

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test.Hero.HeroCatalog
import com.example.test.Routine.*
import com.example.test.alarm.AlarmCoordinator
import com.example.test.data.AppRepository
import com.example.test.data.CompletionOutcome
import com.example.test.data.UserRoutine

open class RoutineActivity : AppCompatActivity() {
    protected open val timelineLayout = R.layout.activity_routine
    protected open val routineSource = com.example.test.data.RoutineSource.PERSONAL
    private lateinit var dateAdapter: DateAdapter
    private lateinit var routineAdapter: PersonalRoutineAdapter
    private lateinit var datesView: RecyclerView
    private var heroId = HeroCatalog.default.id
    private var selectedDate = RoutineDate.today()
    private var managementMode = false
    private var dayFilter: String? = null
    private var resolveStoredHero = false
    private var requestedDetailId = -1L
    private var generation = 0
    private var scopeVersion = 0
    private var busy = false
    private var transientDialog: AlertDialog? = null
    private var datePicker: DatePickerDialog? = null
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            load()
            handler.postDelayed(this, 60_000L - System.currentTimeMillis() % 60_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(timelineLayout)
        // Keep the old fragment class loadable for upgrade restoration, but do not show obsolete sample UI.
        supportFragmentManager.fragments.filterIsInstance<RoutineDetailBottomSheet>().forEach { it.dismissAllowingStateLoss() }
        applySystemBarInsets()
        setupBottomNavigation(if (routineSource == com.example.test.data.RoutineSource.HERO_DAY) R.id.nav_home else R.id.nav_routine)
        val incomingHero = savedInstanceState?.getString(HERO_ID) ?: intent.getStringExtra(HERO_ID)
        resolveStoredHero = incomingHero == null
        heroId = HeroCatalog.getOrDefault(incomingHero).id
        selectedDate = RoutineDate.parse(savedInstanceState?.getString(SELECTED_DATE) ?: intent.getStringExtra(SELECTED_DATE)) ?: RoutineDate.today()
        managementMode = savedInstanceState?.getBoolean(MANAGE) ?: false
        dayFilter = savedInstanceState?.getString("day_filter")
        requestedDetailId = savedInstanceState?.getLong(USER_ROUTINE_ID, -1) ?: intent.getLongExtra(USER_ROUTINE_ID, -1)
        routineAdapter = PersonalRoutineAdapter(onComplete = { complete(it.routine.id, it.date) }) { showDetail(it.routine.id) }
        findViewById<RecyclerView>(R.id.rv_routine_list).apply {
            layoutManager = LinearLayoutManager(this@RoutineActivity); adapter = routineAdapter
        }
        dateAdapter = DateAdapter { date -> scopeVersion++; requestedDetailId = -1; selectedDate = date; renderDates(); load() }
        datesView = findViewById<RecyclerView>(R.id.rv_date_selector).apply {
            layoutManager = LinearLayoutManager(this@RoutineActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = dateAdapter
        }
        findViewById<ImageButton>(R.id.iv_calendar).setOnClickListener { showDatePicker() }
        findViewById<Button>(R.id.personal_add).setOnClickListener {
            startActivity(Intent(this, CharacterSelectActivity::class.java).putExtra("personal_mode", routineSource == com.example.test.data.RoutineSource.PERSONAL))
        }
        findViewById<Button>(R.id.personal_manage).setOnClickListener { managementMode = !managementMode; load() }
        findViewById<Button>(R.id.personal_history).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java).putExtra(HERO_ID, heroId))
        }
        supportFragmentManager.setFragmentResultListener(PersonalRoutineDetailSheet.RESULT_KEY, this) { _, result ->
            val id = result.getLong(USER_ROUTINE_ID, -1)
            val date = RoutineDate.parse(result.getString(SELECTED_DATE)) ?: return@setFragmentResultListener
            when (result.getString("operation")) {
                "complete" -> complete(id, date)
                "edit" -> edit(id)
                "delete" -> confirmDelete(id)
            }
        }
        findViewById<Button>(R.id.day_settings).setOnClickListener { startActivity(Intent(this, SettingActivity::class.java)) }
        renderDates()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent)
        scopeVersion++; dayFilter = null
        intent.getStringExtra(HERO_ID)?.let { heroId = HeroCatalog.getOrDefault(it).id; resolveStoredHero = false }
        RoutineDate.parse(intent.getStringExtra(SELECTED_DATE))?.let { selectedDate = it; renderDates() }
        requestedDetailId = intent.getLongExtra(USER_ROUTINE_ID, -1)
        (supportFragmentManager.findFragmentByTag(DETAIL_TAG) as? PersonalRoutineDetailSheet)?.dismissAllowingStateLoss()
        load()
    }

    override fun onResume() {
        super.onResume(); setupBottomNavigation(if (routineSource == com.example.test.data.RoutineSource.HERO_DAY) R.id.nav_home else R.id.nav_routine); handler.removeCallbacks(refresh); handler.post(refresh)
        AppIo.run(this, { AlarmCoordinator(applicationContext).reconcile() }) { result ->
            findViewById<TextView>(R.id.personal_alarm_status).text = result.fold(
                { if (it.notificationsBlocked) getString(R.string.personal_permission_needed)
                    else if (it.errors.isNotEmpty()) getString(R.string.personal_alarm_failed)
                    else if (it.exactPermissionMissing) getString(R.string.personal_saved_inexact) else "" },
                { getString(R.string.personal_alarm_failed) }
            )
        }
    }
    override fun onPause() { handler.removeCallbacks(refresh); super.onPause() }
    override fun onStop() { datePicker?.dismiss(); datePicker = null; transientDialog?.dismiss(); transientDialog = null; super.onStop() }
    override fun onSaveInstanceState(out: Bundle) {
        out.putString(HERO_ID, heroId); out.putString(SELECTED_DATE, selectedDate.iso); out.putBoolean(MANAGE, managementMode)
        out.putLong(USER_ROUTINE_ID, requestedDetailId)
        out.putString("day_filter", dayFilter)
        super.onSaveInstanceState(out)
    }

    private fun renderDates() {
        val week = selectedDate.week(); dateAdapter.submitDates(week, selectedDate); datesView.scrollToPosition(week.indexOf(selectedDate))
    }

    protected fun reloadTimeline(selectedHero: String? = null) {
        if (selectedHero != null) {
            heroId = selectedHero
            dayFilter = selectedHero
            resolveStoredHero = false
            scopeVersion++
        }
        load()
    }

    private fun load() {
        val ticket = ++generation
        val targetHero = heroId
        val date = selectedDate
        val manage = managementMode
        val resolve = resolveStoredHero
        AppIo.run(this, {
            AppRepository(applicationContext).use { repo ->
                val hero = HeroCatalog.getOrDefault(if (resolve) repo.selectedHeroId else targetHero)
                repo.selectedHeroId = hero.id
                val completed = repo.completedRoutineIds(date)
                val now = java.util.GregorianCalendar()
                val rows = repo.listRoutines(date = if (manage) null else date, source = routineSource).map { routine ->
                    PersonalRoutineRow(routine, routine.id in completed, date, RoutineDate.today(), now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE))
                }
                PersonalSnapshot(hero.id, rows, repo.balance(), completed, repo.appliedItemId(),
                    repo.listRoutines(source = routineSource).map { it.heroId }.distinct())
            }
        }) { result ->
            if (ticket != generation) return@run
            result.fold(onSuccess = { snapshot ->
                heroId = snapshot.heroId; resolveStoredHero = false
                findViewById<TextView>(R.id.routine_title).text = getString(if (routineSource == com.example.test.data.RoutineSource.HERO_DAY) R.string.day_today else R.string.day_personal)
                findViewById<TextView>(R.id.day_notice).visibility = if (routineSource == com.example.test.data.RoutineSource.HERO_DAY) View.VISIBLE else View.GONE
                findViewById<Button>(R.id.personal_add).setText(if (routineSource == com.example.test.data.RoutineSource.HERO_DAY) R.string.day_choose else R.string.day_personal_add)
                findViewById<TextView>(R.id.routine_date).text = selectedDate.label("yyyy년 M월 d일 E요일")
                if (dayFilter !in snapshot.startedHeroes) dayFilter = null
                val visibleRows = TimelinePresentation.forHero(snapshot.rows, dayFilter)
                renderHeroFilters(snapshot.startedHeroes)
                val finished = visibleRows.count { it.completed }
                findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.timeline_progress).apply {
                    max = visibleRows.size.coerceAtLeast(1)
                    progress = finished
                    contentDescription = getString(R.string.home_progress, finished, visibleRows.size)
                }
                findViewById<TextView>(R.id.timeline_next).text = TimelinePresentation.next(visibleRows)?.let {
                    getString(R.string.timeline_next, it.routine.time, it.routine.title)
                } ?: getString(when {
                    visibleRows.isEmpty() -> R.string.timeline_start_hint
                    finished == visibleRows.size -> R.string.home_all_completed
                    else -> R.string.timeline_paused
                })
                findViewById<TextView>(R.id.personal_empty).setText(
                    if (routineSource == com.example.test.data.RoutineSource.HERO_DAY) R.string.timeline_empty else R.string.personal_empty)
                findViewById<TextView>(R.id.personal_summary).text = getString(R.string.personal_progress,
                    finished, visibleRows.size, snapshot.balance)
                findViewById<TextView>(R.id.day_badge).text = com.example.test.data.ShopCatalog.find(snapshot.badge.orEmpty())?.let { getString(R.string.personal_badge, it.title) }.orEmpty()
                routineAdapter.submitList(visibleRows)
                findViewById<TextView>(R.id.personal_empty).visibility = if (visibleRows.isEmpty()) View.VISIBLE else View.GONE
                findViewById<Button>(R.id.personal_manage).setText(if (managementMode) R.string.personal_schedule else R.string.personal_manage)
                (supportFragmentManager.findFragmentByTag(DETAIL_TAG) as? PersonalRoutineDetailSheet)?.apply {
                    updateCompletion(snapshot.completedIds, heroId, selectedDate)
                }
                if (requestedDetailId > 0) {
                    showDetail(requestedDetailId)
                }
            }, onFailure = { findViewById<TextView>(R.id.personal_summary).setText(R.string.personal_storage_error) })
        }
    }

    private fun renderHeroFilters(startedHeroes: List<String>) {
        val group = findViewById<com.google.android.material.chip.ChipGroup>(R.id.timeline_heroes)
        group.visibility = if (routineSource == com.example.test.data.RoutineSource.HERO_DAY && startedHeroes.isNotEmpty()) View.VISIBLE else View.GONE
        val key = startedHeroes to dayFilter
        if (group.tag == key) return
        group.tag = key
        group.removeAllViews()
        val choices = listOf<String?>(null) + startedHeroes
        choices.forEach { id ->
            group.addView(com.google.android.material.chip.Chip(this).apply {
                text = id?.let { HeroCatalog.getOrDefault(it).name } ?: getString(R.string.timeline_all_heroes)
                isCheckable = true; isChecked = id == dayFilter
                minHeight = (48 * resources.displayMetrics.density).toInt()
                setOnClickListener {
                    dayFilter = id; scopeVersion++; requestedDetailId = -1
                    (supportFragmentManager.findFragmentByTag(DETAIL_TAG) as? PersonalRoutineDetailSheet)?.dismissAllowingStateLoss()
                    load()
                }
            })
        }
    }

    private fun showDetail(id: Long): Boolean {
        if (busy || !lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) ||
            supportFragmentManager.isStateSaved || supportFragmentManager.findFragmentByTag(DETAIL_TAG) != null) return false
        busy = true
        val date = selectedDate
        val expectedHero = heroId
        val version = scopeVersion
        AppIo.run(this, { AppRepository(applicationContext).use { repo -> repo.getRoutine(id)?.let { it to repo.isCompleted(id, date) } } }) { result ->
            busy = false
            if (version != scopeVersion || date != selectedDate || expectedHero != heroId ||
                !lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                if (requestedDetailId > 0) load()
                return@run
            }
            result.fold(onSuccess = { value ->
                if (value == null) {
                    if (requestedDetailId == id) requestedDetailId = -1
                    showMessage(R.string.routine_invalid); return@fold
                }
                if (value.first.source != routineSource) {
                    startActivity(Intent(this, if (value.first.source == com.example.test.data.RoutineSource.HERO_DAY) MainActivity::class.java else RoutineActivity::class.java)
                        .putExtra(USER_ROUTINE_ID, id).putExtra(SELECTED_DATE, date.iso))
                    requestedDetailId = -1
                    return@fold
                }
                if (!supportFragmentManager.isStateSaved && supportFragmentManager.findFragmentByTag(DETAIL_TAG) == null) {
                    PersonalRoutineDetailSheet.newInstance(value.first, date, value.second).showNow(supportFragmentManager, DETAIL_TAG)
                    if (requestedDetailId == id) requestedDetailId = -1
                }
            }, onFailure = { showMessage(R.string.personal_storage_error) })
        }
        return true
    }

    private fun complete(id: Long, date: RoutineDate) {
        if (busy) return
        busy = true
        AppIo.run(this, {
            val outcome = AppRepository(applicationContext).use { if (it.getRoutine(id)?.source == routineSource) it.complete(id, date) else CompletionOutcome.ROUTINE_UNAVAILABLE }
            runCatching { AlarmCoordinator(applicationContext).reconcile() }
            outcome
        }) { result ->
            busy = false
            result.fold(onSuccess = { outcome ->
                showMessage(when (outcome) {
                    CompletionOutcome.COMPLETED -> R.string.personal_reward_received
                    CompletionOutcome.ALREADY_COMPLETED -> R.string.routine_already_completed
                    CompletionOutcome.FUTURE_DATE -> R.string.routine_future_completion
                    CompletionOutcome.NOT_SCHEDULED -> R.string.personal_not_scheduled
                    CompletionOutcome.BALANCE_OVERFLOW -> R.string.personal_balance_overflow
                    CompletionOutcome.ROUTINE_UNAVAILABLE -> R.string.personal_unavailable
                })
            }, onFailure = { showMessage(R.string.personal_storage_error) })
            load()
        }
    }

    private fun edit(id: Long) {
        if (busy) return
        busy = true
        val expectedScope = scopeVersion
        AppIo.run(this, { AppRepository(applicationContext).use { it.getRoutine(id)?.takeIf { row -> row.source == routineSource } } }) { result ->
            busy = false
            if (expectedScope != scopeVersion || !lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) return@run
            result.fold(onSuccess = { it?.let(::showEditor) ?: showMessage(R.string.routine_invalid) }, onFailure = { showMessage(R.string.personal_storage_error) })
        }
    }

    private fun showEditor(routine: UserRoutine) {
        val view = layoutInflater.inflate(R.layout.dialog_personal_routine_edit, null)
        val clock = view.findViewById<TimePicker>(R.id.personal_time).apply {
            setIs24HourView(true); hour = routine.time.substring(0, 2).toInt(); minute = routine.time.substring(3, 5).toInt()
        }
        val days = view.findViewById<LinearLayout>(R.id.personal_weekdays)
        val checks = resources.getStringArray(R.array.personal_weekdays).mapIndexed { index, label ->
            CheckBox(this).apply { text = label; isChecked = routine.weekdayMask and (1 shl index) != 0; days.addView(this) }
        }
        val active = view.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.personal_enabled).apply { isChecked = routine.enabled }
        val alarm = view.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.personal_alarm).apply { isChecked = routine.alarmEnabled }
        val dialog = AlertDialog.Builder(this).setTitle(routine.title).setView(view)
            .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.personal_save, null).create()
        transientDialog?.dismiss(); transientDialog = dialog
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val mask = checks.mapIndexed { index, check -> if (check.isChecked) 1 shl index else 0 }.fold(0, Int::or)
                if (mask == 0) { showMessage(R.string.personal_choose_day); return@setOnClickListener }
                if (busy) return@setOnClickListener
                busy = true; dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                val updated = routine.copy(time = String.format(java.util.Locale.ROOT, "%02d:%02d", clock.hour, clock.minute),
                    weekdayMask = mask, enabled = active.isChecked, alarmEnabled = alarm.isChecked)
                AppIo.run(this, {
                    AppRepository(applicationContext).use { it.updateRoutine(updated) }
                    AlarmCoordinator(applicationContext).reconcile()
                }) { result ->
                    busy = false; dialog.dismiss()
                    result.fold(onSuccess = { state ->
                        showMessage(if (state.notificationsBlocked && updated.alarmEnabled) R.string.personal_permission_needed
                            else if (state.errors.isNotEmpty()) R.string.personal_alarm_failed
                            else if (state.exactPermissionMissing && updated.alarmEnabled) R.string.personal_saved_inexact else R.string.personal_saved)
                    }, onFailure = { showMessage(R.string.personal_storage_error) })
                    load()
                }
            }
        }
        dialog.show()
    }

    private fun confirmDelete(id: Long) {
        transientDialog = AlertDialog.Builder(this).setMessage(R.string.personal_remove_confirm)
            .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.personal_remove) { _, _ ->
                if (busy) return@setPositiveButton
                busy = true
                AppIo.run(this, {
                    AppRepository(applicationContext).use { if (it.getRoutine(id)?.source == routineSource) it.deleteRoutine(id) }
                    AlarmCoordinator(applicationContext).cancel(id)
                }) { result -> busy = false; if (result.isFailure) showMessage(R.string.personal_storage_error); load() }
            }.create().also { it.show() }
    }

    private fun showDatePicker() {
        if (datePicker?.isShowing == true) return
        datePicker = DatePickerDialog(this, { _, year, month, day -> scopeVersion++; requestedDetailId = -1; selectedDate = RoutineDate(year, month + 1, day); renderDates(); load() },
            selectedDate.year, selectedDate.month - 1, selectedDate.day).also { it.show() }
    }
    private fun showMessage(message: Int) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    private data class PersonalSnapshot(val heroId: String, val rows: List<PersonalRoutineRow>, val balance: Long, val completedIds: Set<Long>, val badge: String?, val startedHeroes: List<String>)
    companion object {
        const val HERO_ID = "hero_id"
        const val SELECTED_DATE = "selected_date"
        const val ROUTINE_ID = "routine_id" // Kept for legacy fragment restoration.
        const val USER_ROUTINE_ID = "user_routine_id"
        private const val MANAGE = "management_mode"
        private const val DETAIL_TAG = "personal_detail"
    }
}
