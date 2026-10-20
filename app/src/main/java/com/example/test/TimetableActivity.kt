package com.example.test

import android.content.ActivityNotFoundException
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.test.Routine.RoutineDate
import com.example.test.data.TimetableStore
import com.example.test.timetable.EverytimeLink
import com.example.test.timetable.TimetableCsv
import com.example.test.timetable.TimetableEditor
import com.example.test.timetable.TimetableEntry
import com.example.test.timetable.TimetableAgenda
import com.example.test.timetable.TimetableSurfaces
import com.example.test.timetable.TimetableWidgetProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Calendar

/** Weekly classes are local data. A shared Everytime URL is only opened in the user's browser. */
class TimetableActivity : AppCompatActivity() {
    private var week = RoutineDate.today().week().first()
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
                result.onSuccess(::previewImport).onFailure { status(R.string.timetable_import_error) }
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
        week = RoutineDate.parse(state?.getString("week")) ?: week
        listMode = state?.getBoolean("listMode") ?: (resources.configuration.fontScale >= 1.5f)
        findViewById<Chip>(R.id.timetable_week_mode).isChecked = !listMode
        findViewById<Chip>(R.id.timetable_list_mode).isChecked = listMode
        findViewById<View>(R.id.timetable_week_mode).setOnClickListener { listMode = false; render() }
        findViewById<View>(R.id.timetable_list_mode).setOnClickListener { listMode = true; render() }
        findViewById<View>(R.id.timetable_previous).setOnClickListener { week = week.plusDays(-7); render() }
        findViewById<View>(R.id.timetable_next).setOnClickListener { week = week.plusDays(7); render() }
        findViewById<View>(R.id.timetable_today).setOnClickListener { week = RoutineDate.today().week().first(); render() }
        findViewById<View>(R.id.timetable_add).setOnClickListener { openEditor(null) }
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
        out.putString("week", week.iso); out.putBoolean("listMode", listMode)
        super.onSaveInstanceState(out)
    }
    override fun onDestroy() { transient?.dismiss(); super.onDestroy() }
    private fun load() {
        val ticket = ++generation
        AppIo.run(this, { TimetableStore(applicationContext).use { Triple(it.entries(), it.sharedLink(), it.notificationEnabled()) } }) { result ->
            if (ticket != generation) return@run
            result.onSuccess { (rows, link, notices) ->
                entries = rows; sharedLink = link
                findViewById<MaterialButton>(R.id.timetable_notice_toggle).setText(if (notices) R.string.timetable_notice_off else R.string.timetable_notice_on)
                findViewById<View>(R.id.timetable_notice_settings).visibility =
                    if (notices && !TimetableSurfaces.notificationsAllowed(this)) View.VISIBLE else View.GONE
                if (notices && !TimetableSurfaces.notificationsAllowed(this)) status(R.string.timetable_notice_permission)
                render()
            }.onFailure { status(R.string.timetable_load_error) }
        }
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
            .setView(view).setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.timetable_link_open, null).create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val link = EverytimeLink.parse(input.text.toString())
                        if (link == null) field.error = getString(R.string.timetable_link_invalid)
                        else {
                            field.error = null
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                            AppIo.run(this, { TimetableStore(applicationContext).use { it.saveSharedLink(link) } }) { result ->
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                                if (result.isSuccess) { sharedLink = link; dialog.dismiss(); status(R.string.timetable_link_saved)
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
    private fun previewImport(rows: List<TimetableEntry>) {
        transient?.dismiss()
        transient = com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle(R.string.timetable_import_review)
            .setMessage(getString(R.string.timetable_import_note, rows.size))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.timetable_import_confirm, null).create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (busy) return@setOnClickListener
                        setBusy(true); dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                        AppIo.run(this, { TimetableStore(applicationContext).use { it.import(rows) } }) { result ->
                            setBusy(false)
                            result.onSuccess { count -> dialog.dismiss(); findViewById<TextView>(R.id.timetable_status).text = getString(R.string.timetable_imported, count); load() }
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
        val days = (0..6).map(week::plusDays)
        findViewById<TextView>(R.id.timetable_week_label).text = "${days.first().label("M월 d일")} – ${days.last().label("M월 d일")}"
        findViewById<View>(R.id.timetable_today).visibility = if (week == RoutineDate.today().week().first()) View.GONE else View.VISIBLE
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
        else if (listMode) renderList(content, days)
        else renderGrid(content, days)
        findViewById<Chip>(R.id.timetable_week_mode).isChecked = !listMode
        findViewById<Chip>(R.id.timetable_list_mode).isChecked = listMode
    }
    private fun renderList(container: LinearLayout, days: List<RoutineDate>) {
        days.forEachIndexed { index, date ->
            val rows = entries.filter { it.weekday == index + 1 }.sortedBy { it.startMinute }
            if (rows.isEmpty()) return@forEachIndexed
            container.addView(TextView(this).apply { text = "${date.label("M월 d일 E요일")}"; textSize = 18f; setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0,dp(16),0,dp(8)) })
            rows.forEach { entry ->
                val card = MaterialCardView(this).apply { radius = dp(20).toFloat(); setCardBackgroundColor(ContextCompat.getColor(context, R.color.ui_surface)); cardElevation = 0f; tag = "class:${entry.id}" }
                card.addView(TextView(this).apply { text = "${entry.timeLabel} · ${entry.title}\n${entry.location.ifBlank { getString(R.string.timetable_location_empty) }}${entry.instructor.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()}"; textSize = 16f; setTextColor(ContextCompat.getColor(context, R.color.ui_text)); setPadding(dp(16),dp(14),dp(16),dp(14)) })
                card.setOnClickListener { openEditor(entry) }
                container.addView(card, LinearLayout.LayoutParams(-1,-2).apply { bottomMargin = dp(8) })
            }
        }
    }
    private fun renderGrid(container: LinearLayout, days: List<RoutineDate>) {
        val scale = if (resources.configuration.fontScale >= 1.3f) 104 else 80
        val cell = if (resources.configuration.fontScale >= 1.3f) 136 else 112
        val header = when {
            resources.configuration.fontScale >= 1.5f -> 80
            resources.configuration.fontScale >= 1.3f -> 60
            else -> 44
        }
        val railWidth = if (resources.configuration.fontScale >= 1.5f) 68 else 46
        val startHour = minOf(8, entries.minOf { it.startMinute } / 60)
        val endHour = maxOf(20, (entries.maxOf { it.endMinute } + 59) / 60)
        val board = FrameLayout(this).apply { setBackgroundColor(ContextCompat.getColor(context, R.color.ui_surface)) }
        val rail = FrameLayout(this).apply { setBackgroundColor(ContextCompat.getColor(context, R.color.ui_surface)) }
        val width = dp(7 * cell); val height = dp(header + (endHour - startHour) * scale)
        board.layoutParams = LinearLayout.LayoutParams(width, height)
        rail.layoutParams = LinearLayout.LayoutParams(dp(railWidth), height)
        for (hour in startHour..endHour) {
            val top = dp(header + (hour - startHour) * scale)
            board.addView(View(this).apply { setBackgroundColor(ContextCompat.getColor(context, R.color.ui_outline)) }, FrameLayout.LayoutParams(width, dp(1)).apply { topMargin = top })
            if (hour < endHour) rail.addView(TextView(this).apply { text = TimetableEntry.time(hour*60); textSize = if (railWidth > 46) 10f else 12f; setTextColor(ContextCompat.getColor(context,R.color.ui_secondary)) },
                FrameLayout.LayoutParams(dp(railWidth-2),dp(36)).apply { topMargin = top + dp(2); leftMargin=dp(2) })
        }
        days.forEachIndexed { index, date ->
            val left = dp(index*cell)
            if (date == RoutineDate.today()) board.addView(View(this).apply { setBackgroundColor(ContextCompat.getColor(context,R.color.ui_primary_container)); alpha=.35f },
                FrameLayout.LayoutParams(dp(cell),height-dp(header)).apply { leftMargin=left; topMargin=dp(header) })
            board.addView(TextView(this).apply { text = date.label("E\nd"); gravity=Gravity.CENTER; textSize=14f; setTypeface(null,android.graphics.Typeface.BOLD)
                tag = "day-header:$index"
                setTextColor(ContextCompat.getColor(context,R.color.ui_text)) }, FrameLayout.LayoutParams(dp(cell),dp(header)).apply { leftMargin=left })
            board.addView(View(this).apply { setBackgroundColor(ContextCompat.getColor(context,R.color.ui_outline)) }, FrameLayout.LayoutParams(dp(1),height).apply { leftMargin=left })
            val rows=entries.filter { it.weekday==index+1 }.sortedWith(compareBy<TimetableEntry> { it.startMinute }.thenBy { it.endMinute })
            val placed=mutableListOf<Pair<TimetableEntry,Int>>()
            rows.forEach { entry ->
                val free=(0..rows.size).first { lane -> placed.none { (other,occupied) -> occupied==lane && entry.startMinute < maxOf(other.endMinute,other.startMinute+36) && other.startMinute < maxOf(entry.endMinute,entry.startMinute+36) } }
                placed+=entry to free
            }
            rows.forEach { entry ->
                val lane=placed.first { it.first.id==entry.id }.second
                val lanes=placed.filter { (other,_) -> other.startMinute < maxOf(entry.endMinute,entry.startMinute+36) && entry.startMinute < maxOf(other.endMinute,other.startMinute+36) }.maxOfOrNull { it.second }?.plus(1) ?: 1
                val cardWidth=(cell-8)/lanes
                val top=dp(header)+(entry.startMinute-startHour*60)*dp(scale)/60
                val cardHeight=maxOf(dp(48),(entry.endMinute-entry.startMinute)*dp(scale)/60-dp(3))
                val card=TextView(this).apply {
                    text=if(cardHeight>=dp(84)) "${entry.title}\n${entry.timeLabel}\n${entry.location}" else entry.title
                    textSize=if(lanes>1) 11f else 13f; maxLines=4; ellipsize=android.text.TextUtils.TruncateAt.END
                    setTextColor(ContextCompat.getColor(context,R.color.ui_on_container))
                    setPadding(dp(7),dp(6),dp(5),dp(4)); gravity=Gravity.TOP
                    background=android.graphics.drawable.GradientDrawable().apply { cornerRadius=dp(12).toFloat(); setColor(ContextCompat.getColor(context,R.color.ui_primary_container)) }
                    tag="class:${entry.id}"; minHeight=dp(48)
                    contentDescription="${TimetableEntry.weekdays[index]}요일 ${entry.timeLabel} ${entry.title} ${entry.location} ${entry.instructor}"
                    setOnClickListener { openEditor(entry) }
                }
                board.addView(card,FrameLayout.LayoutParams(dp(cardWidth),cardHeight).apply { leftMargin=left+dp(4+lane*cardWidth); topMargin=top })
            }
        }
        val horizontal=HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled=false; addView(board) }
        container.addView(LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            addView(rail)
            addView(horizontal,LinearLayout.LayoutParams(0,height,1f))
        },LinearLayout.LayoutParams(-1,height))
        if (week == RoutineDate.today().week().first()) horizontal.post { horizontal.scrollTo(dp((TimetableEntry.weekday(RoutineDate.today())-1)*cell),0) }
    }
    private fun setBusy(value: Boolean) {
        busy=value
        listOf(R.id.timetable_import,R.id.timetable_export,R.id.timetable_everytime,R.id.timetable_notice_toggle).forEach { findViewById<View>(it).isEnabled=!value }
        if(value) status(R.string.timetable_working)
    }
    private fun status(message: Int) { findViewById<TextView>(R.id.timetable_status).setText(message) }
    private fun dp(value: Int)=(resources.displayMetrics.density*value).toInt()
}
