package com.example.test

import android.content.ActivityNotFoundException
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.content.res.Configuration
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.test.Routine.RoutineDate
import com.example.test.data.TimetableStore
import com.example.test.data.AppRepository
import com.example.test.data.RoutineSource
import com.example.test.data.UserRoutine
import com.example.test.timetable.EverytimeLink
import com.example.test.timetable.EverytimeShareImporter
import com.example.test.timetable.TimetableCsv
import com.example.test.timetable.TimetableEditor
import com.example.test.timetable.TimetableEntry
import com.example.test.timetable.TimetableAgenda
import com.example.test.timetable.TimetableSurfaces
import com.example.test.timetable.TimetableWidgetProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Calendar

/** Weekly classes are local data; public share links can be imported without an account. */
class TimetableActivity : AppCompatActivity() {
    private var listMode = false
    private var entries: List<TimetableEntry> = emptyList()
    private var sharedLink: String? = null
    private var busy = false
    private var generation = 0
    private var transient: AlertDialog? = null
    private var shareHandled = false
    private val importCsv = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            setBusy(true)
            AppIo.run(this, { requireNotNull(contentResolver.openInputStream(uri)).use(TimetableCsv::read) }) { result ->
                setBusy(false)
                result.onSuccess { previewImport(it) }.onFailure { status(R.string.timetable_import_error) }
            }
        }
    }
    private val exportCsv = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            setBusy(true)
            AppIo.run(this, {
                val bytes = TimetableStore(applicationContext).use { TimetableCsv.export(it.entries()) }
                requireNotNull(contentResolver.openOutputStream(uri, "wt")).use { it.write(bytes) }
            }) { result -> setBusy(false); status(if (result.isSuccess) R.string.timetable_exported else R.string.timetable_import_error) }
        }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_timetable)
        applySystemBarInsets()
        ScreenToolbar.bind(this, R.string.timetable_title)
        findViewById<MaterialToolbar>(R.id.screen_toolbar).menu.add(0, R.id.timetable_toolbar_add, 0, R.string.timetable_add).apply {
            setIcon(R.drawable.ic_add_timetable)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            setOnMenuItemClickListener { openEditor(null); true }
        }
        listMode = state?.getBoolean("listMode") ?: (resources.configuration.fontScale >= 1.5f)
        findViewById<Chip>(R.id.timetable_week_mode).isChecked = !listMode
        findViewById<Chip>(R.id.timetable_list_mode).isChecked = listMode
        findViewById<View>(R.id.timetable_week_mode).setOnClickListener { listMode = false; render() }
        findViewById<View>(R.id.timetable_list_mode).setOnClickListener { listMode = true; render() }
        findViewById<View>(R.id.timetable_add).setOnClickListener { openEditor(null) }
        findViewById<View>(R.id.timetable_personal_add).setOnClickListener {
            startActivity(Intent(this, RoutineActivity::class.java).putExtra(RoutineActivity.OPEN_CREATE, true))
        }
        findViewById<View>(R.id.timetable_personal_open).setOnClickListener {
            startActivity(Intent(this, RoutineActivity::class.java))
        }
        findViewById<View>(R.id.timetable_import).setOnClickListener {
            if (!busy) importCsv.launch(arrayOf("text/*", "application/vnd.ms-excel", "application/octet-stream"))
        }
        findViewById<View>(R.id.timetable_export).setOnClickListener { if (!busy) exportCsv.launch("위인의하루-시간표.csv") }
        findViewById<View>(R.id.timetable_everytime).setOnClickListener { showSharedLink() }
        findViewById<View>(R.id.timetable_widget_add).setOnClickListener { addHomeWidget() }
        findViewById<View>(R.id.timetable_notice_toggle).setOnClickListener { toggleNotice() }
        findViewById<View>(R.id.timetable_notice_settings).setOnClickListener {
            try {
                startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName))
            } catch (_: ActivityNotFoundException) {
                startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")))
            }
        }
        supportFragmentManager.setFragmentResultListener(TimetableEditor.CHANGED, this) { _, _ -> load() }
        render()
        if (state == null && intent.getBooleanExtra("open_add", false)) {
            window.decorView.post { openEditor(null) }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent); shareHandled = false; handleShare()
    }
    override fun onResume() {
        super.onResume(); setupBottomNavigation(R.id.nav_timetable); load(); handleShare()
    }
    override fun onSaveInstanceState(out: Bundle) {
        out.putBoolean("listMode", listMode)
        super.onSaveInstanceState(out)
    }
    override fun onDestroy() { transient?.dismiss(); super.onDestroy() }
    private fun load() {
        val ticket = ++generation
        AppIo.run(this, {
            val timetable = TimetableStore(applicationContext).use { Triple(it.entries(), it.sharedLink(), it.notificationEnabled()) }
            val plans = AppRepository(applicationContext).use { it.listRoutines(source = RoutineSource.PERSONAL) }
            timetable to plans
        }) { result ->
            if (ticket != generation) return@run
            result.onSuccess { (timetable, plans) ->
                val (rows, link, notices) = timetable
                entries = rows; sharedLink = link
                renderPersonalPlans(plans)
                findViewById<MaterialButton>(R.id.timetable_notice_toggle).setText(if (notices) R.string.timetable_notice_off else R.string.timetable_notice_on)
                findViewById<View>(R.id.timetable_notice_settings).visibility =
                    if (notices && !TimetableSurfaces.notificationsAllowed(this)) View.VISIBLE else View.GONE
                if (notices && !TimetableSurfaces.notificationsAllowed(this)) status(R.string.timetable_notice_permission)
                render()
            }.onFailure { status(R.string.timetable_load_error) }
        }
    }
    private fun renderPersonalPlans(plans: List<UserRoutine>) {
        val container = findViewById<LinearLayout>(R.id.timetable_personal_list)
        container.removeAllViews()
        if (plans.isEmpty()) {
            container.addView(TextView(this).apply {
                setText(R.string.timetable_personal_empty)
                setPadding(0, dp(8), 0, dp(8))
            })
            return
        }
        plans.sortedWith(compareByDescending<UserRoutine> { it.enabled }.thenBy { it.time }).take(3).forEach { plan ->
            val days = if (plan.weekdayMask == 127) getString(R.string.timetable_personal_every_day)
                else TimetableEntry.weekdays.filterIndexed { index, _ -> plan.weekdayMask and (1 shl index) != 0 }.joinToString("·")
            val detail = if (plan.enabled) days else getString(R.string.timetable_personal_paused)
            container.addView(TextView(this).apply {
                text = "${plan.time}  ${plan.title}\n$detail"
                tag = "personal-plan:${plan.id}"
                minHeight = dp(56)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                setTextColor(ContextCompat.getColor(context, R.color.ui_text))
                setBackgroundColor(ContextCompat.getColor(context, R.color.ui_surface))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    startActivity(Intent(this@TimetableActivity, RoutineActivity::class.java)
                        .putExtra(TimelineActivity.USER_ROUTINE_ID, plan.id))
                }
            })
        }
        if (plans.size > 3) container.addView(TextView(this).apply {
            text = getString(R.string.timetable_personal_more, plans.size - 3)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        })
    }
    private fun handleShare() {
        if (shareHandled || intent.action != Intent.ACTION_SEND || intent.type != "text/plain") return
        shareHandled = true
        val link = EverytimeLink.fromSharedText(intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty())
        if (link == null) status(R.string.timetable_link_invalid)
        else showSharedLink(link)
    }
    private fun showSharedLink(initial: String? = sharedLink) {
        val input = TextInputEditText(this).apply {
            id = R.id.timetable_link_input
            setSingleLine(); inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setText(initial.orEmpty())
        }
        val field = TextInputLayout(this).apply { hint = getString(R.string.timetable_link_hint); addView(input) }
        val view = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(12), dp(20), 0)
            addView(TextView(this@TimetableActivity).apply { setText(R.string.timetable_everytime_note) })
            addView(field, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }) }
        transient?.dismiss()
        transient = com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle(R.string.timetable_everytime)
            .setView(view).setNegativeButton(R.string.timetable_cancel, null)
            .setNeutralButton(R.string.timetable_link_open, null)
            .setPositiveButton(R.string.timetable_link_import, null).create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val link = EverytimeLink.parse(input.text.toString())
                        if (link == null) field.error = getString(R.string.timetable_link_invalid)
                        else {
                            field.error = null
                            dialog.setCancelable(false)
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = false
                            setBusy(true)
                            AppIo.run(this, { EverytimeShareImporter.fetch(link) }) { result ->
                                setBusy(false)
                                dialog.setCancelable(true)
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = true
                                result.onSuccess { preview ->
                                    dialog.dismiss()
                                    previewImport(preview.entries, link, preview.unscheduled)
                                }.onFailure { error ->
                                    field.error = getString(when ((error as? EverytimeShareImporter.ImportError)?.problem) {
                                        EverytimeShareImporter.Problem.PRIVATE -> R.string.timetable_link_private
                                        EverytimeShareImporter.Problem.EMPTY -> R.string.timetable_link_empty
                                        EverytimeShareImporter.Problem.UNAVAILABLE -> R.string.timetable_link_unavailable
                                        EverytimeShareImporter.Problem.TOO_MANY -> R.string.timetable_link_too_many
                                        EverytimeShareImporter.Problem.FORMAT -> R.string.timetable_link_format
                                        else -> R.string.timetable_link_connection
                                    })
                                }
                            }
                        }
                    }
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                        val link = EverytimeLink.parse(input.text.toString())
                        if (link == null) field.error = getString(R.string.timetable_link_invalid)
                        else {
                            field.error = null
                            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = false
                            AppIo.run(this, { TimetableStore(applicationContext).use { it.saveSharedLink(link) } }) { result ->
                                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = true
                                if (result.isSuccess) {
                                    sharedLink = link; dialog.dismiss(); status(R.string.timetable_link_saved)
                                    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
                                    catch (_: ActivityNotFoundException) { status(R.string.timetable_browser_missing) }
                                } else field.error = getString(R.string.timetable_save_error)
                            }
                        }
                    }
                }
                dialog.show()
            }
    }
    private fun previewImport(rows: List<TimetableEntry>, sourceLink: String? = null, unscheduled: Int = 0) {
        transient?.dismiss()
        val summary = if (sourceLink == null) getString(R.string.timetable_import_note, rows.size)
            else getString(R.string.timetable_everytime_review, rows.size, unscheduled)
        val previewView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
            addView(TextView(this@TimetableActivity).apply {
                text = summary; textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.ui_secondary))
                setPadding(dp(4), 0, dp(4), dp(12))
            })
        }
        val viewport = ((resources.displayMetrics.heightPixels / resources.displayMetrics.density).toInt() - 350)
            .coerceIn(220, 420)
        renderGrid(previewView, rows, viewport) { entry ->
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(entry.title)
                .setMessage("${TimetableEntry.weekdays[entry.weekday - 1]}요일 ${entry.timeLabel}\n${entry.location}" +
                    entry.instructor.takeIf(String::isNotBlank)?.let { "\n$it" }.orEmpty())
                .setPositiveButton(android.R.string.ok, null).show()
        }
        transient = com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle(R.string.timetable_import_review)
            .setView(previewView)
            .setNegativeButton(R.string.timetable_cancel, null)
            .setPositiveButton(R.string.timetable_import_confirm, null).create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.window?.setLayout(resources.displayMetrics.widthPixels - dp(24), -2)
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (busy) return@setOnClickListener
                        setBusy(true); dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                        AppIo.run(this, { TimetableStore(applicationContext).use { it.import(rows, sourceLink) } }) { result ->
                            setBusy(false)
                            result.onSuccess { count ->
                                dialog.dismiss(); if (sourceLink != null) sharedLink = sourceLink
                                findViewById<TextView>(R.id.timetable_status).text = getString(R.string.timetable_imported, count); load()
                            }
                                .onFailure { dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true; status(R.string.timetable_import_error) }
                        }
                    }
                }
                dialog.show()
            }
    }
    private fun toggleNotice() {
        if (busy) return
        setBusy(true)
        AppIo.run(this, { TimetableStore(applicationContext).use { store ->
            val next = !store.notificationEnabled(); store.setNotificationEnabled(next); next
        } }) { result ->
            setBusy(false)
            if (result.isSuccess) {
                if (result.getOrNull() == true && !TimetableSurfaces.notificationsAllowed(this)) status(R.string.timetable_notice_permission)
                load()
            } else status(R.string.timetable_save_error)
        }
    }
    private fun addHomeWidget() {
        val manager = getSystemService(AppWidgetManager::class.java)
        val requested = android.os.Build.VERSION.SDK_INT >= 26 && manager.isRequestPinAppWidgetSupported &&
            manager.requestPinAppWidget(ComponentName(this, TimetableWidgetProvider::class.java), null, null)
        if (!requested) transient = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setMessage(R.string.timetable_widget_manual).setPositiveButton(android.R.string.ok, null).show()
    }
    private fun openEditor(entry: TimetableEntry?) {
        if (supportFragmentManager.findFragmentByTag("timetable_editor") != null || supportFragmentManager.isStateSaved) return
        TimetableEditor.create(entry, TimetableEntry.weekday(RoutineDate.today())).show(supportFragmentManager, "timetable_editor")
    }
    private fun render() {
        val minutes = entries.sumOf { it.endMinute - it.startMinute }
        findViewById<TextView>(R.id.timetable_summary).text = getString(R.string.timetable_summary, entries.size, minutes / 60, minutes % 60)
        val now = Calendar.getInstance(); val minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val current = TimetableAgenda.at(entries, TimetableEntry.weekday(RoutineDate.today()), minute).next
        findViewById<TextView>(R.id.timetable_next_class).text = current?.let {
            getString(if (it.startMinute <= minute) R.string.timetable_ongoing else R.string.timetable_upcoming,
                it.title, it.timeLabel, it.location.ifBlank { getString(R.string.timetable_location_empty) })
        } ?: getString(R.string.timetable_no_today)
        val content = findViewById<LinearLayout>(R.id.timetable_content); content.removeAllViews()
        findViewById<TextView>(R.id.timetable_view_hint).visibility = if (entries.isNotEmpty() && !listMode) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.timetable_view_hint).setText(R.string.timetable_scroll_hint)
        if (entries.isEmpty()) content.addView(TextView(this).apply { setText(R.string.timetable_empty); textSize = 18f; setPadding(dp(20),dp(24),dp(20),dp(24)) })
        else if (listMode) renderList(content)
        else renderGrid(content)
        findViewById<Chip>(R.id.timetable_week_mode).isChecked = !listMode
        findViewById<Chip>(R.id.timetable_list_mode).isChecked = listMode
    }
    private fun renderList(container: LinearLayout) {
        TimetableEntry.weekdays.forEachIndexed { index, weekday ->
            val rows = entries.filter { it.weekday == index + 1 }.sortedBy { it.startMinute }
            if (rows.isEmpty()) return@forEachIndexed
            container.addView(TextView(this).apply { text = "${weekday}요일"; tag = "list-day:$index"; textSize = 18f; setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0,dp(16),0,dp(8)) })
            rows.forEach { entry ->
                val card = MaterialCardView(this).apply { radius = dp(16).toFloat(); setCardBackgroundColor(ContextCompat.getColor(context, R.color.ui_surface)); cardElevation = 0f; tag = "class:${entry.id}" }
                card.addView(LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(12), dp(12), dp(12), dp(12)); minimumHeight = dp(64)
                    addView(TextView(this@TimetableActivity).apply {
                        text = "${TimetableEntry.time(entry.startMinute)}\n${TimetableEntry.time(entry.endMinute)}"
                        textSize = if (resources.configuration.fontScale >= 1.3f) 12f else 14f
                        setTextColor(ContextCompat.getColor(context, R.color.ui_primary))
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    }, LinearLayout.LayoutParams(dp(if (resources.configuration.fontScale >= 1.3f) 90 else 72), -2))
                    addView(LinearLayout(this@TimetableActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(TextView(this@TimetableActivity).apply {
                            text = entry.title; textSize = 16f
                            setTypeface(null, android.graphics.Typeface.BOLD)
                            setTextColor(ContextCompat.getColor(context, R.color.ui_text))
                        })
                        addView(TextView(this@TimetableActivity).apply {
                            text = entry.location.ifBlank { getString(R.string.timetable_location_empty) } +
                                entry.instructor.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()
                            textSize = 13f; setTextColor(ContextCompat.getColor(context, R.color.ui_secondary))
                        })
                    }, LinearLayout.LayoutParams(0, -2, 1f))
                })
                card.setOnClickListener { openEditor(entry) }
                container.addView(card, LinearLayout.LayoutParams(-1,-2).apply { bottomMargin = dp(8) })
            }
        }
    }
    private fun renderGrid(container: LinearLayout, sourceRows: List<TimetableEntry> = entries,
        maxViewportDp: Int = 640, onOpen: (TimetableEntry) -> Unit = ::openEditor) {
        val largeText = resources.configuration.fontScale >= 1.3f
        val scale = if (largeText) 80 else 52
        val railWidth = if (largeText) 56 else 36
        val usableWidth = (resources.displayMetrics.widthPixels / resources.displayMetrics.density).toInt() - 24 - railWidth
        val cell = if (largeText) maxOf(88, usableWidth / 3) else maxOf(55, usableWidth / 6)
        val header = if (resources.configuration.fontScale >= 1.5f) 48 else 32
        val startHour = minOf(9, sourceRows.minOf { it.startMinute } / 60)
        val endHour = maxOf(18, (sourceRows.maxOf { it.endMinute } + 59) / 60)
        val board = FrameLayout(this).apply { setBackgroundColor(ContextCompat.getColor(context, R.color.ui_surface)) }
        val rail = FrameLayout(this).apply { setBackgroundColor(ContextCompat.getColor(context, R.color.ui_surface)) }
        val headerBoard = FrameLayout(this).apply { setBackgroundColor(ContextCompat.getColor(context, R.color.ui_surface)) }
        val width = dp(7 * cell); val height = dp((endHour - startHour) * scale)
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val swatches = (if (night) listOf("#294461", "#28514D", "#493E62", "#5A452F", "#3C503F", "#584051")
            else listOf("#DDEBFF", "#DDF4EA", "#EFE6FA", "#FFF0D8", "#E7F2DE", "#FAE5EC"))
            .map(android.graphics.Color::parseColor)
        val classNames = sourceRows.map { it.title }.distinct().sorted()
        val cardText = android.graphics.Color.parseColor(if (night) "#F4F7FB" else "#233247")
        val cardSecondary = android.graphics.Color.parseColor(if (night) "#D6DFEA" else "#4B5D70")
        board.layoutParams = LinearLayout.LayoutParams(width, height)
        rail.layoutParams = LinearLayout.LayoutParams(dp(railWidth), height)
        headerBoard.layoutParams = LinearLayout.LayoutParams(width, dp(header))
        for (hour in startHour..endHour) {
            val top = dp((hour - startHour) * scale)
            board.addView(View(this).apply { setBackgroundColor(ContextCompat.getColor(context, R.color.ui_outline)) }, FrameLayout.LayoutParams(width, dp(1)).apply { topMargin = top })
            if (hour < endHour) {
                board.addView(View(this).apply { setBackgroundColor(ContextCompat.getColor(context, R.color.ui_outline)); alpha = .38f },
                    FrameLayout.LayoutParams(width, dp(1)).apply { topMargin = top + dp(scale / 2) })
                rail.addView(TextView(this).apply { text = TimetableEntry.time(hour * 60); textSize = if (largeText) 11f else 12f
                    setTextColor(ContextCompat.getColor(context, R.color.ui_secondary)) },
                    FrameLayout.LayoutParams(dp(railWidth - 2), dp(36)).apply { topMargin = top + dp(3); leftMargin = dp(3) })
            }
        }
        TimetableEntry.weekdays.forEachIndexed { index, weekday ->
            val left = dp(index*cell)
            val isToday = index + 1 == TimetableEntry.weekday(RoutineDate.today())
            if (isToday) board.addView(View(this).apply { setBackgroundColor(ContextCompat.getColor(context,R.color.ui_primary_container)); alpha=.35f },
                FrameLayout.LayoutParams(dp(cell),height).apply { leftMargin=left })
            headerBoard.addView(TextView(this).apply { text = weekday; gravity=Gravity.CENTER; textSize=if (largeText) 16f else 14f
                setTypeface(null,android.graphics.Typeface.BOLD)
                tag = "day-header:$index"
                setTextColor(ContextCompat.getColor(context, if (isToday) R.color.ui_primary else R.color.ui_secondary)) },
                FrameLayout.LayoutParams(dp(cell),dp(header)).apply { leftMargin=left })
            board.addView(View(this).apply { setBackgroundColor(ContextCompat.getColor(context,R.color.ui_outline)); alpha = .6f },
                FrameLayout.LayoutParams(dp(1),height).apply { leftMargin=left })
            val rows=sourceRows.filter { it.weekday==index+1 }.sortedWith(compareBy<TimetableEntry> { it.startMinute }.thenBy { it.endMinute })
            val placed=mutableListOf<Pair<TimetableEntry,Int>>()
            rows.forEach { entry ->
                val free=(0..rows.size).first { lane -> placed.none { (other,occupied) -> occupied==lane && entry.startMinute < maxOf(other.endMinute,other.startMinute+36) && other.startMinute < maxOf(entry.endMinute,entry.startMinute+36) } }
                placed+=entry to free
            }
            rows.forEachIndexed { position, entry ->
                val lane=placed[position].second
                val lanes=placed.filter { (other,_) -> other.startMinute < maxOf(entry.endMinute,entry.startMinute+36) && entry.startMinute < maxOf(other.endMinute,other.startMinute+36) }.maxOfOrNull { it.second }?.plus(1) ?: 1
                val cardWidth=(cell-4)/lanes
                val top=(entry.startMinute-startHour*60)*dp(scale)/60
                val cardHeight=maxOf(dp(48),(entry.endMinute-entry.startMinute)*dp(scale)/60-dp(3))
                val card=LinearLayout(this).apply {
                    orientation=LinearLayout.VERTICAL
                    setPadding(dp(5),dp(6),dp(4),dp(4))
                    background=android.graphics.drawable.GradientDrawable().apply { cornerRadius=dp(10).toFloat()
                        setColor(swatches[classNames.indexOf(entry.title) % swatches.size]) }
                    tag="class:${entry.id}"; minimumHeight=dp(48)
                    contentDescription="${TimetableEntry.weekdays[index]}요일 ${entry.timeLabel} ${entry.title} ${entry.location} ${entry.instructor}"
                    setOnClickListener { onOpen(entry) }
                }
                card.addView(TextView(this).apply {
                    text=entry.title; textSize=if (largeText) 14f else if (lanes>1) 10f else 12f
                    setTypeface(null,android.graphics.Typeface.BOLD); setTextColor(cardText)
                    maxLines=if (cardHeight >= dp(86)) 4 else 3
                    ellipsize=android.text.TextUtils.TruncateAt.END
                    setLineSpacing(0f, 1.05f)
                })
                if (cardHeight >= dp(72) && lanes == 1 && entry.location.isNotBlank()) card.addView(TextView(this).apply {
                    text=entry.location; textSize=if (largeText) 12f else 10f; setTextColor(cardSecondary)
                    maxLines=2; ellipsize=android.text.TextUtils.TruncateAt.END
                    setPadding(0,dp(4),0,0)
                })
                board.addView(card,FrameLayout.LayoutParams(dp(cardWidth),cardHeight).apply { leftMargin=left+dp(2+lane*cardWidth); topMargin=top })
            }
        }
        val headerScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(headerBoard) }
        val bodyScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(board) }
        headerScroll.setOnScrollChangeListener { _, x, _, _, _ -> if (bodyScroll.scrollX != x) bodyScroll.scrollTo(x, 0) }
        bodyScroll.setOnScrollChangeListener { _, x, _, _, _ -> if (headerScroll.scrollX != x) headerScroll.scrollTo(x, 0) }
        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            tag = "timetable-grid"
            addView(LinearLayout(this@TimetableActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(ContextCompat.getColor(context, R.color.ui_surface))
                addView(View(this@TimetableActivity), LinearLayout.LayoutParams(dp(railWidth), dp(header)))
                addView(headerScroll, LinearLayout.LayoutParams(0, dp(header), 1f))
            }, LinearLayout.LayoutParams(-1, dp(header)))
            val bodyRow = LinearLayout(this@TimetableActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(rail)
                addView(bodyScroll, LinearLayout.LayoutParams(0, height, 1f))
            }
            val screenHeight = (resources.displayMetrics.heightPixels / resources.displayMetrics.density).toInt()
            val viewport = dp(minOf((screenHeight - 170).coerceAtLeast(240), maxViewportDp, (endHour - startHour) * scale))
            addView(NestedScrollView(this@TimetableActivity).apply {
                tag = "timetable-grid-body"
                isFillViewport = false
                isNestedScrollingEnabled = true
                addView(bodyRow)
            }, LinearLayout.LayoutParams(-1, viewport))
        }
        container.addView(MaterialCardView(this).apply {
            radius = dp(16).toFloat(); cardElevation = 0f
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.ui_surface))
            addView(grid)
        }, LinearLayout.LayoutParams(-1, -2))
    }
    private fun setBusy(value: Boolean) {
        busy=value
        listOf(R.id.timetable_import,R.id.timetable_export,R.id.timetable_everytime,R.id.timetable_notice_toggle).forEach { findViewById<View>(it).isEnabled=!value }
        if(value) status(R.string.timetable_working)
    }
    private fun status(message: Int) { findViewById<TextView>(R.id.timetable_status).setText(message) }
    private fun dp(value: Int)=(resources.displayMetrics.density*value).toInt()
}
