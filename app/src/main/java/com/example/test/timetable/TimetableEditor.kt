package com.example.test.timetable

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.test.AppIo
import com.example.test.R
import com.example.test.data.TimetableStore
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class TimetableEditor : DialogFragment() {
    private lateinit var form: LinearLayout
    private var transient: Dialog? = null
    private var busy = false
    private val entryId get() = requireArguments().getLong("id")
    override fun onCreateDialog(state: Bundle?): Dialog {
        val context = requireContext()
        val data = state ?: requireArguments()
        form = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(12)) }
        fun input(id: Int, label: Int, key: String, limit: Int) {
            val box = TextInputLayout(context).apply { hint = getString(label) }
            box.addView(TextInputEditText(context).apply {
                this.id = id; setText(data.getString(key).orEmpty()); maxLines = 3
                filters = arrayOf(android.text.InputFilter.LengthFilter(limit))
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            }, LinearLayout.LayoutParams(-1, -2))
            form.addView(box, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        }
        input(R.id.timetable_editor_name, R.string.timetable_name, "title", 120)
        form.addView(TextView(context).apply { setText(R.string.timetable_weekday); setPadding(0, dp(12), 0, 0) })
        form.addView(Spinner(context).apply {
            id = R.id.timetable_editor_day
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, TimetableEntry.weekdays.map { "${it}요일" })
            setSelection(data.getInt("weekday", 1) - 1)
            minimumHeight = dp(48)
        }, LinearLayout.LayoutParams(-1, -2))
        listOf(Triple(R.id.timetable_editor_start, "start", R.string.timetable_start), Triple(R.id.timetable_editor_end, "end", R.string.timetable_end)).forEach { (id, key, label) ->
            form.addView(TextView(context).apply { setText(label) })
            form.addView(MaterialButton(context).apply {
                this.id = id; minHeight = dp(48)
                val minute = data.getInt(key, if (key == "start") 540 else 600)
                tag = minute; text = TimetableEntry.time(minute)
                contentDescription = getString(label) + ", " + text
                setOnClickListener {
                    val current = tag as Int
                    transient?.dismiss()
                    transient = TimePickerDialog(context, { _, hour, minutes ->
                        val next = if (key == "end" && hour == 0 && minutes == 0) 1440 else hour * 60 + minutes
                        tag = next; text = TimetableEntry.time(next); contentDescription = getString(label) + ", " + text
                    }, current / 60 % 24, current % 60, true).also { it.show() }
                }
            }, LinearLayout.LayoutParams(-1, -2))
        }
        input(R.id.timetable_editor_location, R.string.timetable_location, "location", 160)
        input(R.id.timetable_editor_instructor, R.string.timetable_instructor, "instructor", 80)
        form.addView(TextView(context).apply { id = R.id.timetable_editor_error; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE })
        val scroll = ScrollView(context).apply { addView(form) }
        return MaterialAlertDialogBuilder(context).setTitle(if (entryId > 0) R.string.timetable_edit else R.string.timetable_add)
            .setView(scroll).setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.timetable_save, null)
            .apply { if (entryId > 0) setNeutralButton(R.string.timetable_delete, null) }.create()
    }
    override fun onStart() {
        super.onStart()
        val alert = requireDialog() as AlertDialog
        alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (busy) return@setOnClickListener
            val entry = runCatching { TimetableEntry(entryId, text(R.id.timetable_editor_name), form.findViewById<Spinner>(R.id.timetable_editor_day).selectedItemPosition + 1,
                form.findViewById<View>(R.id.timetable_editor_start).tag as Int, form.findViewById<View>(R.id.timetable_editor_end).tag as Int,
                text(R.id.timetable_editor_location), text(R.id.timetable_editor_instructor)) }.getOrNull()
            if (entry == null) form.findViewById<TextView>(R.id.timetable_editor_error).setText(R.string.timetable_invalid)
            else {
                val appContext = requireContext().applicationContext
                work { TimetableStore(appContext).use { it.save(entry) } }
            }
        }
        alert.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
            transient?.dismiss()
            transient = MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.timetable_delete_confirm)
                .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.timetable_delete) { _, _ ->
                    val appContext = requireContext().applicationContext
                    work { TimetableStore(appContext).use { it.delete(entryId) } }
                }.show()
        }
    }
    private fun work(action: () -> Any) {
        busy = true
        val alert = requireDialog() as AlertDialog
        listOf(-1, -2, -3).forEach { alert.getButton(it)?.isEnabled = false }
        AppIo.run(this, action) { result ->
            busy = false
            if (result.isSuccess) { parentFragmentManager.setFragmentResult(CHANGED, Bundle()); dismiss() }
            else {
                listOf(-1, -2, -3).forEach { alert.getButton(it)?.isEnabled = true }
                form.findViewById<TextView>(R.id.timetable_editor_error).setText(R.string.timetable_save_error)
            }
        }
    }
    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putString("title", text(R.id.timetable_editor_name)); out.putString("location", text(R.id.timetable_editor_location)); out.putString("instructor", text(R.id.timetable_editor_instructor))
        out.putInt("weekday", form.findViewById<Spinner>(R.id.timetable_editor_day).selectedItemPosition + 1)
        out.putInt("start", form.findViewById<View>(R.id.timetable_editor_start).tag as Int); out.putInt("end", form.findViewById<View>(R.id.timetable_editor_end).tag as Int)
    }
    override fun onDestroyView() { transient?.dismiss(); transient = null; super.onDestroyView() }
    private fun text(id: Int) = form.findViewById<EditText>(id).text.toString()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    companion object {
        const val CHANGED = "timetable_changed"
        fun create(entry: TimetableEntry? = null, weekday: Int = 1) = TimetableEditor().apply {
            arguments = Bundle().apply {
                putLong("id", entry?.id ?: 0); putString("title", entry?.title.orEmpty()); putInt("weekday", entry?.weekday ?: weekday)
                putInt("start", entry?.startMinute ?: 540); putInt("end", entry?.endMinute ?: 600)
                putString("location", entry?.location.orEmpty()); putString("instructor", entry?.instructor.orEmpty())
            }
        }
    }
}
