package com.example.test

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

abstract class TimelineActivity : AppCompatActivity() {
    protected abstract val timelineLayout: Int
    protected abstract val routineSource: com.example.test.data.RoutineSource
    protected lateinit var timelineHeader: View
    private lateinit var routineAdapter: PersonalRoutineAdapter
    private var heroId = HeroCatalog.default.id
    private var selectedDate = RoutineDate.today()
    private var managementMode = false
    private var dayFilter: String? = null
    private var resolveStoredHero = false
    private var requestedDetailId = -1L
    private var generation = 0
    private var scopeVersion = 0
    private var busy = false
    private var alarmRequested = false
    private var transientDialog: AlertDialog? = null
    private lateinit var activityCalendar: com.example.test.calendar.ActivityCalendarView
    private var calendarExpanded = false
    private var calendarMonth: RoutineDate? = null
    private var calendarData: List<DayActivity>? = null
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
        calendarExpanded = savedInstanceState?.getBoolean("calendar_expanded") ?: intent.getBooleanExtra("calendar_expanded", false)
        managementMode = savedInstanceState?.getBoolean(MANAGE) ?: false
        dayFilter = savedInstanceState?.getString("day_filter") ?: incomingHero?.takeIf { routineSource == com.example.test.data.RoutineSource.HERO_DAY }
        requestedDetailId = savedInstanceState?.getLong(USER_ROUTINE_ID, -1) ?: intent.getLongExtra(USER_ROUTINE_ID, -1)
        val timelineList = findViewById<RecyclerView>(R.id.rv_routine_list)
        timelineList.layoutManager = LinearLayoutManager(this)
        // Inflating against RecyclerView preserves MATCH_PARENT in its layout params.
        // A detached header measured as WRAP_CONTENT can squeeze a seven-day month
        // into a narrow card on real devices, even when Robolectric looks correct.
        timelineHeader = layoutInflater.inflate(R.layout.item_timeline_header, timelineList, false)
        ScreenToolbar.bind(this, if (routineSource == com.example.test.data.RoutineSource.HERO_DAY) R.string.nav_home else R.string.nav_routine)
        routineAdapter = PersonalRoutineAdapter(onComplete = { complete(it.routine.id, it.date) }) { showDetail(it.routine.id) }
        timelineList.apply {
            adapter = androidx.recyclerview.widget.ConcatAdapter(TimelineHeaderAdapter(timelineHeader), routineAdapter)
        }
        activityCalendar = timelineHeader.findViewById(R.id.activity_calendar)
        activityCalendar.onDateSelected = ::selectDate
        activityCalendar.onOpenDay = { calendarExpanded = false; renderDates(); scrollToCalendar() }
        timelineHeader.findViewById<View>(R.id.iv_calendar).setOnClickListener {
            calendarExpanded = !calendarExpanded
            renderDates()
            scrollToCalendar()
        }
        timelineHeader.findViewById<Button>(R.id.personal_add).setOnClickListener {
            startActivity(Intent(this, CharacterSelectActivity::class.java).putExtra("personal_mode", routineSource == com.example.test.data.RoutineSource.PERSONAL))
        }
        timelineHeader.findViewById<Button>(R.id.personal_manage).setOnClickListener { managementMode = !managementMode; load() }
        timelineHeader.findViewById<Button>(R.id.personal_history).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java).putExtra(HERO_ID, heroId))
        }
        timelineHeader.findViewById<View>(R.id.timeline_finished_history).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java).putExtra(HERO_ID, dayFilter ?: heroId))
        }
        timelineHeader.findViewById<View>(R.id.timeline_focus_resume).setOnClickListener {
            startActivity(Intent(this, FocusActivity::class.java))
        }
        timelineHeader.findViewById<View>(R.id.timeline_timetable_open).setOnClickListener {
            startActivity(Intent(this, TimetableActivity::class.java))
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
        supportFragmentManager.setFragmentResultListener(PersonalRoutineDetailSheet.CHECKS_CHANGED, this) { _, _ -> load() }
        timelineHeader.findViewById<Button>(R.id.day_settings).setOnClickListener { startActivity(Intent(this, SettingActivity::class.java)) }
        timelineHeader.findViewById<Button>(R.id.empty_start).setOnClickListener {
            if (routineSource == com.example.test.data.RoutineSource.PERSONAL) openPersonalEditor()
            else startActivity(Intent(this, CharacterSelectActivity::class.java))
        }
        supportFragmentManager.setFragmentResultListener(RoutineEditorSheet.RESULT_KEY, this) { _, result ->
            saveEditor(result)
        }
        renderDates()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent)
        if (intent.hasExtra("calendar_expanded")) calendarExpanded = intent.getBooleanExtra("calendar_expanded", false)
        scopeVersion++; dayFilter = null
        intent.getStringExtra(HERO_ID)?.let { heroId = HeroCatalog.getOrDefault(it).id; resolveStoredHero = false; if (routineSource == com.example.test.data.RoutineSource.HERO_DAY) dayFilter = heroId }
        RoutineDate.parse(intent.getStringExtra(SELECTED_DATE))?.let { selectedDate = it; renderDates() }
        requestedDetailId = intent.getLongExtra(USER_ROUTINE_ID, -1)
        (supportFragmentManager.findFragmentByTag(DETAIL_TAG) as? PersonalRoutineDetailSheet)?.dismissAllowingStateLoss()
        load()
        if (intent.hasExtra("calendar_expanded")) scrollToCalendar()
    }

    private fun scrollToCalendar() {
        (findViewById<RecyclerView>(R.id.rv_routine_list).layoutManager as? LinearLayoutManager)
            ?.scrollToPositionWithOffset(0, 0)
    }

    override fun onResume() {
        super.onResume(); setupBottomNavigation(if (routineSource == com.example.test.data.RoutineSource.HERO_DAY) R.id.nav_home else R.id.nav_routine); handler.removeCallbacks(refresh); handler.post(refresh)
        AppIo.run(this, { AlarmCoordinator(applicationContext).reconcile() }) { result ->
            timelineHeader.findViewById<TextView>(R.id.personal_alarm_status).text = result.fold(
                { if (it.notificationsBlocked) getString(R.string.personal_permission_needed)
                    else if (it.errors.isNotEmpty()) getString(R.string.personal_alarm_failed)
                    else if (it.exactPermissionMissing) getString(R.string.personal_saved_inexact) else "" },
                { getString(R.string.personal_alarm_failed) }
            )
            val status = timelineHeader.findViewById<TextView>(R.id.personal_alarm_status)
            status.visibility = if (alarmRequested && status.text.isNotBlank()) View.VISIBLE else View.GONE
        }
    }
    override fun onPause() { handler.removeCallbacks(refresh); super.onPause() }
    override fun onStop() { transientDialog?.dismiss(); transientDialog = null; super.onStop() }
    override fun onSaveInstanceState(out: Bundle) {
        out.putString(HERO_ID, heroId); out.putString(SELECTED_DATE, selectedDate.iso); out.putBoolean(MANAGE, managementMode)
        out.putLong(USER_ROUTINE_ID, requestedDetailId)
        out.putString("day_filter", dayFilter)
        out.putBoolean("calendar_expanded", calendarExpanded)
        super.onSaveInstanceState(out)
    }

    private fun renderDates() {
        timelineHeader.findViewById<View>(R.id.routine_date).visibility = if (calendarExpanded) View.GONE else View.VISIBLE
        timelineHeader.findViewById<Button>(R.id.iv_calendar).apply {
            setText(if (calendarExpanded) R.string.calendar_day_view else R.string.calendar_month_view)
            contentDescription = text
        }
        activityCalendar.bind(selectedDate, calendarExpanded,
            calendarData.takeIf { calendarMonth == ActivityCalendar.monthStart(selectedDate) })
    }

    private fun selectDate(date: RoutineDate) {
        scopeVersion++; requestedDetailId = -1; selectedDate = date
        (supportFragmentManager.findFragmentByTag(DETAIL_TAG) as? PersonalRoutineDetailSheet)?.dismissAllowingStateLoss()
        renderDates(); load()
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
                val questChecks = repo.questChecksForDate(date)
                val adopted = repo.listRoutines(source = routineSource)
                val now = java.util.GregorianCalendar()
                val rows = repo.listRoutines(date = if (manage) null else date, source = routineSource).map { routine ->
                    PersonalRoutineRow(routine, routine.id in completed, date, RoutineDate.today(), now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE), questChecks[routine.id].orEmpty())
                }
                val classes = com.example.test.data.TimetableStore(applicationContext).use { it.entries() }
                PersonalSnapshot(hero.id, rows, repo.balance(), completed, repo.appliedItemId(),
                    adopted.map { it.heroId }.distinct(), adopted.any { it.enabled && it.alarmEnabled },
                    repo.calendarActivity(ActivityCalendar.monthStart(date).week().first(), ActivityCalendar.days(date).last().week().last()), classes)
            }
        }) { result ->
            if (ticket != generation) return@run
            result.fold(onSuccess = { snapshot ->
                calendarData = snapshot.activity; calendarMonth = ActivityCalendar.monthStart(date)
                renderDates()
                timelineHeader.findViewById<View>(R.id.timeline_timetable_card).visibility =
                    if (routineSource == com.example.test.data.RoutineSource.HERO_DAY) View.VISIBLE else View.GONE
                val classRows = snapshot.classes.filter { it.weekday == com.example.test.timetable.TimetableEntry.weekday(RoutineDate.today()) }
                timelineHeader.findViewById<TextView>(R.id.timeline_timetable_summary).text =
                    if (classRows.isEmpty()) getString(R.string.timetable_widget_no_class)
                    else classRows.sortedBy { it.startMinute }.joinToString(" · ", limit = 2) { "${it.timeLabel} ${it.title}" }
                heroId = snapshot.heroId; resolveStoredHero = false
                timelineHeader.findViewById<TextView>(R.id.routine_title).text = getString(if (routineSource == com.example.test.data.RoutineSource.HERO_DAY) R.string.redesign_day_title else R.string.redesign_personal_title)
                timelineHeader.findViewById<TextView>(R.id.day_notice).visibility = if (routineSource == com.example.test.data.RoutineSource.HERO_DAY) View.VISIBLE else View.GONE
                timelineHeader.findViewById<Button>(R.id.personal_add).setText(if (routineSource == com.example.test.data.RoutineSource.HERO_DAY) R.string.redesign_choose_day else R.string.redesign_from_hero)
                timelineHeader.findViewById<TextView>(R.id.routine_date).text = selectedDate.label("yyyy년 M월 d일 E요일")
                if (dayFilter !in snapshot.startedHeroes) dayFilter = null
                val visibleRows = TimelinePresentation.forHero(snapshot.rows, dayFilter)
                renderHeroFilters(snapshot.startedHeroes)
                alarmRequested = snapshot.requestedAlarm
                val alarmStatus = timelineHeader.findViewById<TextView>(R.id.personal_alarm_status)
                alarmStatus.visibility = if (alarmRequested && alarmStatus.text.isNotBlank()) View.VISIBLE else View.GONE
                timelineHeader.findViewById<View>(R.id.timeline_section).visibility = if (visibleRows.isEmpty()) View.GONE else View.VISIBLE
                timelineHeader.findViewById<View>(R.id.personal_add).visibility = if (visibleRows.isEmpty() || routineSource == com.example.test.data.RoutineSource.HERO_DAY) View.GONE else View.VISIBLE
                timelineHeader.findViewById<View>(R.id.personal_manage).visibility = if (snapshot.startedHeroes.isEmpty()) View.GONE else View.VISIBLE
                timelineHeader.findViewById<View>(R.id.personal_create).visibility = if (routineSource == com.example.test.data.RoutineSource.PERSONAL && visibleRows.isNotEmpty()) View.VISIBLE else View.GONE
                val finished = visibleRows.count { it.completed }
                timelineHeader.findViewById<View>(R.id.timeline_finished_card).visibility = if (!managementMode &&
                    selectedDate == RoutineDate.today() && visibleRows.isNotEmpty() && finished == visibleRows.size) View.VISIBLE else View.GONE
                val focus = com.example.test.focus.FocusTimerStore(applicationContext).read()
                timelineHeader.findViewById<Button>(R.id.timeline_focus_resume).apply {
                    visibility = if (focus != null && focus.routineId !in snapshot.completedIds && selectedDate == RoutineDate.today()) View.VISIBLE else View.GONE
                    text = focus?.let {
                        when {
                            it.phase == com.example.test.focus.FocusPhase.FINISHED || it.remainingMillis == 0L -> getString(R.string.upgrade_focus_finished, it.title)
                            it.phase == com.example.test.focus.FocusPhase.PAUSED -> getString(R.string.upgrade_focus_paused, it.title)
                            else -> getString(R.string.upgrade_focus_running, it.title, (it.remainingMillis + 59_999) / 60_000)
                        }
                    }.orEmpty()
                    CompactText.apply(this)
                }
                timelineHeader.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.timeline_progress).apply {
                    max = visibleRows.size.coerceAtLeast(1)
                    progress = finished
                    contentDescription = getString(R.string.home_progress, finished, visibleRows.size)
                }
                val next = TimelinePresentation.next(visibleRows)
                timelineHeader.findViewById<View>(R.id.timeline_next_card).visibility = if (next == null) View.GONE else View.VISIBLE
                timelineHeader.findViewById<TextView>(R.id.timeline_next).text = next?.routine?.title.orEmpty()
                timelineHeader.findViewById<TextView>(R.id.timeline_next_subtitle).text = next?.let {
                    "${it.routine.time} · ${com.example.test.data.RoutineIdentity.displayName(it.routine.heroId)}"
                }.orEmpty()
                timelineHeader.findViewById<Button>(R.id.timeline_next_complete).apply {
                    contentDescription = next?.let { getString(R.string.timeline_complete_accessibility, com.example.test.data.RoutineIdentity.displayName(it.routine.heroId), it.routine.title) }.orEmpty()
                    isEnabled = next?.let(TimelinePresentation::canComplete) == true
                    setOnClickListener { next?.let { complete(it.routine.id, it.date) } }
                }
                timelineHeader.findViewById<Button>(R.id.timeline_next_open).apply {
                    contentDescription = next?.let { getString(R.string.timeline_detail_accessibility, com.example.test.data.RoutineIdentity.displayName(it.routine.heroId), it.routine.title) }.orEmpty()
                    setOnClickListener { next?.let { showDetail(it.routine.id) } }
                }
                val nextActions = timelineHeader.findViewById<LinearLayout>(R.id.timeline_next_actions)
                val largeText = resources.configuration.fontScale >= 1.3f
                nextActions.orientation = if (largeText) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
                for (index in 0 until nextActions.childCount) nextActions.getChildAt(index).layoutParams = LinearLayout.LayoutParams(
                    if (largeText) android.view.ViewGroup.LayoutParams.MATCH_PARENT else 0,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT, if (largeText) 0f else if (index == 0) 2f else 1f)
                CompactText.apply(timelineHeader.findViewById(R.id.timeline_next))
                CompactText.apply(timelineHeader.findViewById(R.id.timeline_next_subtitle))
                val personal = routineSource == com.example.test.data.RoutineSource.PERSONAL
                timelineHeader.findViewById<TextView>(R.id.empty_heading).setText(if (snapshot.startedHeroes.isNotEmpty()) R.string.redesign_empty_schedule else if (personal) R.string.redesign_personal_empty else R.string.experience_empty)
                timelineHeader.findViewById<TextView>(R.id.personal_empty).setText(if (snapshot.startedHeroes.isNotEmpty()) R.string.redesign_check_schedule else if (personal) R.string.redesign_personal_hint else R.string.experience_empty_body)
                timelineHeader.findViewById<Button>(R.id.empty_start).setText(if (personal) R.string.redesign_add_plan else R.string.redesign_choose_day)
                timelineHeader.findViewById<View>(R.id.timeline_empty_card).visibility = if (visibleRows.isEmpty()) View.VISIBLE else View.GONE
                timelineHeader.findViewById<TextView>(R.id.timeline_section).setText(if (personal) R.string.redesign_my_flow else R.string.redesign_timeline)
                timelineHeader.findViewById<android.widget.ImageView>(R.id.today_hero_portrait).apply {
                    setImageResource(R.drawable.avatar_default)
                    CompactText.circle(this)
                }
                (timelineHeader.findViewById<TextView>(R.id.personal_summary).parent.parent as View).visibility = if (snapshot.startedHeroes.isEmpty()) View.GONE else View.VISIBLE
                timelineHeader.findViewById<TextView>(R.id.personal_summary).text = getString(R.string.redesign_progress, finished, visibleRows.size)
                timelineHeader.findViewById<TextView>(R.id.timeline_wallet).text = getString(R.string.personal_wallet, snapshot.balance)
                timelineHeader.findViewById<TextView>(R.id.day_badge).text = com.example.test.data.ShopCatalog.find(snapshot.badge.orEmpty())?.let { getString(R.string.personal_badge, it.title) }.orEmpty()
                timelineHeader.findViewById<View>(R.id.day_badge).visibility = if (snapshot.badge.isNullOrBlank()) View.GONE else View.VISIBLE
                routineAdapter.submitList(if (managementMode) visibleRows else visibleRows.filterNot { it.routine.id == next?.routine?.id })
                timelineHeader.findViewById<Button>(R.id.personal_manage).setText(if (managementMode) R.string.personal_schedule else if (personal) R.string.redesign_manage_plans else R.string.redesign_manage)
                (supportFragmentManager.findFragmentByTag(DETAIL_TAG) as? PersonalRoutineDetailSheet)?.apply {
                    updateCompletion(snapshot.completedIds, heroId, selectedDate)
                }
                if (requestedDetailId > 0) {
                    showDetail(requestedDetailId)
                }
            }, onFailure = { activityCalendar.showError(); timelineHeader.findViewById<TextView>(R.id.personal_summary).setText(R.string.personal_storage_error) })
        }
    }

    private fun renderHeroFilters(startedHeroes: List<String>) {
        val group = timelineHeader.findViewById<com.google.android.material.chip.ChipGroup>(R.id.timeline_heroes)
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

    protected fun complete(id: Long, date: RoutineDate) {
        if (busy || date != selectedDate) return
        busy = true
        val requestedHero = dayFilter
        AppIo.run(this, {
            val outcome = AppRepository(applicationContext).use { if (it.getRoutine(id)?.let { row -> row.source == routineSource && (requestedHero == null || row.heroId == requestedHero) } == true) it.complete(id, date) else CompletionOutcome.ROUTINE_UNAVAILABLE }
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
        if (!supportFragmentManager.isStateSaved) {
            RoutineEditorSheet.forRoutine(routine).show(supportFragmentManager, "routine_editor")
        }
    }

    protected fun openPersonalEditor() {
        if (routineSource != com.example.test.data.RoutineSource.PERSONAL || busy || supportFragmentManager.isStateSaved) return
        if (supportFragmentManager.findFragmentByTag("routine_editor") == null) {
            RoutineEditorSheet.forNewPlan().show(supportFragmentManager, "routine_editor")
        }
    }

    private fun saveEditor(values: Bundle) {
        if (busy) return
        busy = true
        val id = values.getLong("id")
        AppIo.run(this, {
            AppRepository(applicationContext).use { repo ->
                if (id == 0L) {
                    check(routineSource == com.example.test.data.RoutineSource.PERSONAL)
                    repo.addPersonalPlan(values.getString("title").orEmpty(), values.getString("time").orEmpty(),
                        values.getInt("days"), values.getBoolean("enabled"), values.getBoolean("alarm"))
                } else {
                    val original = requireNotNull(repo.getRoutine(id))
                    check(original.source == routineSource)
                    repo.updateRoutine(original.copy(
                        title = if (original.source == com.example.test.data.RoutineSource.PERSONAL) values.getString("title").orEmpty() else original.title,
                        time = values.getString("time").orEmpty(), weekdayMask = values.getInt("days"),
                        enabled = values.getBoolean("enabled"), alarmEnabled = values.getBoolean("alarm")))
                }
            }
            AlarmCoordinator(applicationContext).reconcile()
        }) { result ->
            busy = false
            result.fold(onSuccess = { state ->
                showMessage(if (state.notificationsBlocked && values.getBoolean("alarm")) R.string.personal_permission_needed
                    else if (state.errors.isNotEmpty()) R.string.personal_alarm_failed
                    else if (state.exactPermissionMissing && values.getBoolean("alarm")) R.string.personal_saved_inexact else R.string.personal_saved)
            }, onFailure = { showMessage(R.string.personal_storage_error) })
            load()
        }
    }

    private fun confirmDelete(id: Long) {
        transientDialog = AlertDialog.Builder(this).setMessage(R.string.personal_remove_confirm)
            .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.personal_remove) { _, _ ->
                if (busy) return@setPositiveButton
                busy = true
                AppIo.run(this, {
                    val removed = AppRepository(applicationContext).use {
                        if (it.getRoutine(id)?.source == routineSource) { it.deleteRoutine(id); true } else false
                    }
                    if (removed) AlarmCoordinator(applicationContext).cancel(id)
                }) { result -> busy = false; if (result.isFailure) showMessage(R.string.personal_storage_error); load() }
            }.create().also { it.show() }
    }

    private fun showMessage(message: Int) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    private data class PersonalSnapshot(val heroId: String, val rows: List<PersonalRoutineRow>, val balance: Long, val completedIds: Set<Long>, val badge: String?, val startedHeroes: List<String>, val requestedAlarm: Boolean, val activity: List<DayActivity>, val classes: List<com.example.test.timetable.TimetableEntry>)
    companion object {
        const val HERO_ID = "hero_id"
        const val SELECTED_DATE = "selected_date"
        const val ROUTINE_ID = "routine_id" // Kept for legacy fragment restoration.
        const val USER_ROUTINE_ID = "user_routine_id"
        private const val MANAGE = "management_mode"
        private const val DETAIL_TAG = "personal_detail"
    }
}
