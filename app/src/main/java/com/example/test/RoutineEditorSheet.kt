package com.example.test

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.example.test.data.RoutineSource
import com.example.test.data.UserRoutine
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale

/** Stateful editing form. Only a repository write changes saved routines. */
class RoutineEditorSheet : BottomSheetDialogFragment(R.layout.sheet_routine_editor) {
    private var time = "08:00"
    private var days = 127
    private var picker: TimePickerDialog? = null
    private var submitted = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val args = requireArguments()
        time = savedInstanceState?.getString("time") ?: args.getString("time") ?: "08:00"
        days = savedInstanceState?.getInt("days") ?: args.getInt("days", 127)
        val personal = args.getString("source") != RoutineSource.HERO_DAY.name
        view.findViewById<TextInputLayout>(R.id.editor_title_layout).visibility = if (personal) View.VISIBLE else View.GONE
        view.findViewById<EditText>(R.id.editor_title).setText(savedInstanceState?.getString("title") ?: args.getString("title"))
        view.findViewById<TextView>(R.id.editor_heading).text = if (personal) getString(R.string.redesign_editor_title) else args.getString("title")
        view.findViewById<MaterialSwitch>(R.id.editor_enabled).isChecked = savedInstanceState?.getBoolean("enabled") ?: args.getBoolean("enabled", true)
        view.findViewById<MaterialSwitch>(R.id.editor_alarm).isChecked = savedInstanceState?.getBoolean("alarm") ?: args.getBoolean("alarm")
        bindTime(view)
        view.findViewById<Button>(R.id.editor_time).setOnClickListener {
            picker?.dismiss()
            picker = TimePickerDialog(requireContext(), { _, hour, minute ->
                time = String.format(Locale.ROOT, "%02d:%02d", hour, minute)
                this.view?.let(::bindTime)
            }, time.substring(0, 2).toInt(), time.substring(3, 5).toInt(), true).also { it.show() }
        }
        val group = view.findViewById<ChipGroup>(R.id.editor_days)
        resources.getStringArray(R.array.personal_weekdays).forEachIndexed { index, label ->
            group.addView(Chip(requireContext()).apply {
                id = View.generateViewId()
                text = label
                isCheckable = true
                isChecked = days and (1 shl index) != 0
                minHeight = (48 * resources.displayMetrics.density).toInt()
                setOnCheckedChangeListener { _, checked ->
                    days = if (checked) days or (1 shl index) else days and (1 shl index).inv()
                    view.findViewById<TextView>(R.id.editor_error).visibility = View.GONE
                }
            })
        }
        view.findViewById<Button>(R.id.editor_save).setOnClickListener { save(view, personal) }
    }

    private fun bindTime(view: View) {
        view.findViewById<Button>(R.id.editor_time).text = getString(R.string.redesign_time, time)
    }

    private fun save(view: View, personal: Boolean) {
        if (submitted) return
        val title = if (personal) view.findViewById<EditText>(R.id.editor_title).text.toString().trim() else requireArguments().getString("title").orEmpty()
        val titleField = view.findViewById<TextInputLayout>(R.id.editor_title_layout)
        if (title.length !in 1..160) {
            titleField.error = getString(R.string.redesign_title_required)
            return
        }
        titleField.error = null
        if (days == 0) {
            view.findViewById<TextView>(R.id.editor_error).apply {
                setText(R.string.redesign_days_required)
                visibility = View.VISIBLE
            }
            return
        }
        submitted = true
        parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle().apply {
            putLong("id", requireArguments().getLong("id"))
            putString("title", title)
            putString("time", time)
            putInt("days", days)
            putBoolean("enabled", view.findViewById<MaterialSwitch>(R.id.editor_enabled).isChecked)
            putBoolean("alarm", view.findViewById<MaterialSwitch>(R.id.editor_alarm).isChecked)
        })
        dismiss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("time", time)
        outState.putInt("days", days)
        view?.let {
            outState.putString("title", it.findViewById<EditText>(R.id.editor_title).text.toString())
            outState.putBoolean("enabled", it.findViewById<MaterialSwitch>(R.id.editor_enabled).isChecked)
            outState.putBoolean("alarm", it.findViewById<MaterialSwitch>(R.id.editor_alarm).isChecked)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        picker?.dismiss()
        picker = null
        super.onDestroyView()
    }

    companion object {
        const val RESULT_KEY = "routine_editor_saved"
        fun forNewPlan() = RoutineEditorSheet().apply {
            arguments = Bundle().apply { putString("source", RoutineSource.PERSONAL.name) }
        }
        fun forRoutine(routine: UserRoutine) = RoutineEditorSheet().apply {
            arguments = Bundle().apply {
                putLong("id", routine.id)
                putString("title", routine.title)
                putString("time", routine.time)
                putInt("days", routine.weekdayMask)
                putBoolean("enabled", routine.enabled)
                putBoolean("alarm", routine.alarmEnabled)
                putString("source", routine.source.name)
            }
        }
    }
}
