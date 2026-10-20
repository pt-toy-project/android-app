package com.example.test.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.example.test.R
import com.example.test.Routine.ActivityCalendar
import com.example.test.Routine.DayActivity
import com.example.test.Routine.RoutineDate
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/** Inline month/day calendar. All figures come from saved records; navigation never writes data. */
class ActivityCalendarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    var onDateSelected: (RoutineDate) -> Unit = {}
    var onOpenDay: () -> Unit = {}
    private var selected = RoutineDate.today()
    private var monthly = false
    private val monthLabel = label(18f, true)
    private val previous = button("‹", R.id.calendar_previous) { move(-1) }
    private val next = button("›", R.id.calendar_next) { move(1) }
    private val today = button(context.getString(R.string.calendar_today), R.id.calendar_today) { onDateSelected(RoutineDate.today()) }
    private val summary = label(14f)
    private val scope = label(12f).apply { text = context.getString(R.string.calendar_scope); setPadding(0, dp(8), 0, dp(8)) }
    private val divider = View(context).apply { setBackgroundColor(color(R.color.ui_outline)) }
    private val scheduleNote = label(11f).apply {
        text = context.getString(R.string.calendar_schedule_note); setTextColor(color(R.color.ui_secondary)); setPadding(dp(4), dp(8), dp(4), 0)
    }
    private val grid = LinearLayout(context).apply { orientation = VERTICAL; id = R.id.calendar_grid }
    private val selectedTitle = label(18f, true).apply { id = R.id.calendar_selected_title }
    private val rings = ActivityRingsView(context).apply { id = R.id.calendar_day_rings }
    private val metrics = LinearLayout(context)
    private val completed = label(15f, true).apply { id = R.id.calendar_completed; setTextColor(color(R.color.calendar_routine)) }
    private val checked = label(15f, true).apply { id = R.id.calendar_checked; setTextColor(color(R.color.calendar_check)) }
    private val coins = label(15f, true).apply { id = R.id.calendar_coins }
    private val notice = label(13f).apply { id = R.id.calendar_notice }
    private val openDay = button(context.getString(R.string.calendar_open_day), R.id.calendar_open_day) { onOpenDay() }

    init {
        orientation = VERTICAL
        setPadding(0, dp(8), 0, dp(12))
        val card = MaterialCardView(context).apply {
            radius = dp(24).toFloat(); cardElevation = 0f; strokeWidth = 0
            setCardBackgroundColor(color(R.color.ui_surface))
        }
        addView(card, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        val content = LinearLayout(context).apply { orientation = VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(16)) }
        card.addView(content)
        val nav = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        nav.addView(previous, LayoutParams(dp(48), dp(48)))
        nav.addView(monthLabel, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        nav.addView(next, LayoutParams(dp(48), dp(48)))
        content.addView(nav)
        content.addView(today, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER_HORIZONTAL })
        content.addView(scope)
        summary.id = R.id.calendar_month_summary
        content.addView(summary)
        val weekdays = LinearLayout(context)
        listOf("월", "화", "수", "목", "금", "토", "일").forEach { day ->
            weekdays.addView(label(12f).apply { text = day; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO },
                LayoutParams(0, dp(32), 1f))
        }
        content.addView(weekdays)
        content.addView(grid, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        content.addView(divider,
            LayoutParams(LayoutParams.MATCH_PARENT, dp(1)).apply { topMargin = dp(12); bottomMargin = dp(16) })
        content.addView(selectedTitle)
        content.addView(rings, LayoutParams(dp(144), dp(144)).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(8); bottomMargin = dp(8) })
        listOf(completed, checked, coins).forEach { metrics.addView(it) }
        content.addView(metrics)
        notice.setPadding(dp(4), dp(12), dp(4), dp(4))
        content.addView(notice)
        content.addView(openDay)
        content.addView(scheduleNote)
    }

    fun bind(date: RoutineDate, expanded: Boolean, activity: List<DayActivity>?) {
        selected = date; monthly = expanded
        val current = RoutineDate.today()
        val entries = activity?.associateBy { it.date }.orEmpty()
        monthLabel.text = date.label(if (monthly) "yyyy년 M월" else "M월 d일 E요일")
        previous.contentDescription = context.getString(if (monthly) R.string.calendar_previous_month else R.string.calendar_previous_day)
        next.contentDescription = context.getString(if (monthly) R.string.calendar_next_month else R.string.calendar_next_day)
        previous.isEnabled = adjacent(-1) != null; next.isEnabled = adjacent(1) != null
        today.visibility = if (date == current) GONE else VISIBLE
        summary.visibility = if (monthly) VISIBLE else GONE
        val monthEntries = activity.orEmpty().filter { it.date.year == date.year && it.date.month == date.month }
        summary.text = if (activity == null) context.getString(R.string.calendar_loading) else context.getString(
            R.string.calendar_month_summary, monthEntries.count { it.active }, monthEntries.sumOf { it.completed }, monthEntries.sumOf { it.coins })
        grid.removeAllViews()
        val dates = if (monthly) List<RoutineDate?>(ActivityCalendar.leadingBlanks(date)) { null } + ActivityCalendar.days(date) else date.week()
        dates.chunked(7).forEach { week ->
            val row = LinearLayout(context).apply { orientation = HORIZONTAL }
            (week + List<RoutineDate?>(7 - week.size) { null }).forEach { day ->
                val cell = if (day == null) View(context) else dayCell(day, entries[day], current)
                // Empty calendar slots have no intrinsic height; WRAP_CONTENT can fill an
                // unbounded RecyclerView header and stretch the entire week.
                row.addView(cell, LayoutParams(0, if (day == null) 0 else LayoutParams.WRAP_CONTENT, 1f))
            }
            grid.addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
        selectedTitle.text = date.label("M월 d일") + if (date == current) " · 오늘" else ""
        val day = entries[date] ?: DayActivity(date)
        rings.bind(day)
        rings.contentDescription = context.getString(R.string.calendar_rings, day.completed, day.routineGoal, day.checked, day.checkGoal)
        completed.text = context.getString(R.string.calendar_complete, day.completed, day.routineGoal)
        checked.text = context.getString(R.string.calendar_checks, day.checked, day.checkGoal)
        coins.text = context.getString(R.string.calendar_coins, day.coins)
        val large = resources.configuration.fontScale >= 1.3f
        metrics.orientation = if (large) VERTICAL else HORIZONTAL
        listOf(completed, checked, coins).forEach {
            it.layoutParams = LayoutParams(if (large) LayoutParams.MATCH_PARENT else 0, LayoutParams.WRAP_CONTENT, if (large) 0f else 1f)
            it.setPadding(dp(2), dp(6), dp(2), dp(6))
        }
        listOf(scope, divider, selectedTitle, scheduleNote).forEach { it.visibility = if (monthly) VISIBLE else GONE }
        rings.visibility = if (monthly && activity != null) VISIBLE else GONE
        metrics.visibility = if (monthly && activity != null) VISIBLE else GONE
        notice.visibility = if (monthly || activity == null || date > current) VISIBLE else GONE
        notice.setText(when { activity == null -> R.string.calendar_loading; date > current -> R.string.calendar_future; !day.active -> R.string.calendar_empty; else -> R.string.calendar_recorded })
        openDay.visibility = if (monthly) VISIBLE else GONE
    }

    fun showError() { notice.visibility = VISIBLE; notice.setText(R.string.calendar_error); summary.setText(R.string.calendar_error) }

    private fun dayCell(day: RoutineDate, data: DayActivity?, current: RoutineDate): View = LinearLayout(context).apply {
        orientation = VERTICAL; gravity = Gravity.CENTER; minimumHeight = dp(68)
        setPadding(dp(1), dp(5), dp(1), dp(5))
        tag = "calendar:${day.iso}"; isSelected = day == selected; isFocusable = true; isClickable = true
        background = GradientDrawable().apply {
            cornerRadius = dp(22).toFloat()
            setColor(if (day == selected) color(R.color.ui_primary_container) else android.graphics.Color.TRANSPARENT)
            if (day == selected || day == current) setStroke(dp(1), color(R.color.ui_primary))
        }
        val stat = data ?: DayActivity(day)
        contentDescription = context.getString(R.string.calendar_day_accessibility, day.label("yyyy년 M월 d일 E요일"), stat.completed, stat.checked,
            (if (day == selected) context.getString(R.string.calendar_selected) else "") + (if (day == current) context.getString(R.string.calendar_current) else ""))
        addView(ActivityRingsView(context).apply {
            bind(stat); importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO; alpha = if (day > current) 0.35f else 1f
        }, LayoutParams(dp(30), dp(30)))
        addView(AppCompatTextView(context).apply {
            text = day.day.toString(); textSize = 14f; gravity = Gravity.CENTER; maxLines = 1
            setTextColor(color(if (day == selected) R.color.ui_on_container else R.color.ui_text))
            if (day == selected) setTypeface(typeface, Typeface.BOLD)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this, 10, 14, 1, android.util.TypedValue.COMPLEX_UNIT_SP)
        }, LayoutParams(LayoutParams.MATCH_PARENT, dp(if (resources.configuration.fontScale >= 1.3f) 36 else 24)))
        setOnClickListener { onDateSelected(day) }
    }

    private fun adjacent(amount: Int) = runCatching { if (monthly) ActivityCalendar.moveMonth(selected, amount) else selected.plusDays(amount) }.getOrNull()
    private fun move(amount: Int) { adjacent(amount)?.let(onDateSelected) }
    private fun label(size: Float, bold: Boolean = false) = TextView(context).apply {
        textSize = size; gravity = Gravity.CENTER; setTextColor(color(R.color.ui_text))
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun button(text: String, viewId: Int, action: () -> Unit) = MaterialButton(context).apply {
        backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
        setTextColor(color(R.color.ui_primary))
        id = viewId; this.text = text; minWidth = dp(48); minimumWidth = dp(48); minHeight = dp(48)
        setPadding(dp(4), 0, dp(4), 0); insetTop = 0; insetBottom = 0
        setOnClickListener { action() }
    }
    private fun color(id: Int) = ContextCompat.getColor(context, id)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

/** Two independently measured rings: outer = completed routines, inner = checked subtasks. */
class ActivityRingsView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private var activity = DayActivity(RoutineDate.today())
    fun bind(day: DayActivity) { activity = day; invalidate() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height).toFloat()
        val stroke = size * .085f
        val cx = width / 2f; val cy = height / 2f
        paint.strokeWidth = stroke
        listOf(activity.routineProgress to R.color.calendar_routine, activity.checkProgress to R.color.calendar_check).forEachIndexed { index, (fraction, color) ->
            val radius = size * (.43f - index * .14f)
            val bounds = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            paint.color = ContextCompat.getColor(context, color); paint.alpha = 45
            canvas.drawOval(bounds, paint)
            paint.alpha = 255
            if (fraction > 0) canvas.drawArc(bounds, -90f, 360f * fraction.coerceIn(0f, 1f), false, paint)
        }
    }
}
