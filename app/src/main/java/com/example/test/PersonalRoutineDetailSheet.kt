package com.example.test

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.example.test.Hero.HeroCatalog
import com.example.test.Routine.RoutineDate
import com.example.test.data.RoutineRules
import com.example.test.data.UserRoutine
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** The sheet is a display snapshot; repository transactions validate every action again. */
class PersonalRoutineDetailSheet : BottomSheetDialogFragment(R.layout.dialog_personal_routine_detail) {
    private var submitted = false
    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        val args = requireArguments()
        val hero = HeroCatalog.getOrDefault(args.getString(RoutineActivity.HERO_ID))
        val template = hero.routines.firstOrNull { it.id == args.getString("template") }
        view.findViewById<TextView>(R.id.personal_detail_title).text = args.getString("title")
        view.findViewById<TextView>(R.id.personal_detail_hero).text = hero.name
        view.findViewById<TextView>(R.id.personal_detail_date).text = getString(R.string.routine_detail_schedule,
            args.getString(RoutineActivity.SELECTED_DATE).orEmpty(), args.getString("time").orEmpty())
        view.findViewById<TextView>(R.id.personal_detail_fact).text = template?.historicalFact ?: getString(R.string.personal_snapshot)
        view.findViewById<TextView>(R.id.personal_detail_practice).text = template?.modernPractice.orEmpty()
        view.findViewById<Button>(R.id.personal_detail_source).apply {
            visibility = if (template == null) View.GONE else View.VISIBLE
            text = template?.source?.let { getString(R.string.personal_source, it.title, it.locator) }
            setOnClickListener { template?.source?.url?.let(::openSource) }
        }
        view.findViewById<Button>(R.id.personal_detail_complete).setOnClickListener { dispatch("complete") }
        view.findViewById<Button>(R.id.personal_detail_edit).setOnClickListener { dispatch("edit") }
        view.findViewById<Button>(R.id.personal_detail_delete).setOnClickListener { dispatch("delete") }
        refreshDateBoundary()
    }

    fun refreshDateBoundary(today: RoutineDate = RoutineDate.today()) {
        val view = view ?: return
        val args = requireArguments()
        val date = RoutineDate.parse(args.getString(RoutineActivity.SELECTED_DATE))
        val adoption = RoutineDate.parse(args.getString("created"))
        val completed = args.getBoolean("completed")
        view.findViewById<Button>(R.id.personal_detail_complete).apply {
            setText(if (completed) R.string.routine_status_completed else R.string.personal_complete_reward)
            isEnabled = !submitted && !completed && args.getBoolean("enabled") && date != null && date <= today &&
                (adoption == null || date >= adoption) && RoutineRules.scheduledOn(args.getInt("days"), date)
        }
    }

    fun updateCompletion(completedIds: Set<Long>, heroId: String, date: RoutineDate) {
        val args = requireArguments()
        if (args.getString(RoutineActivity.HERO_ID) == heroId && args.getString(RoutineActivity.SELECTED_DATE) == date.iso) {
            args.putBoolean("completed", args.getLong(RoutineActivity.USER_ROUTINE_ID) in completedIds)
        }
        refreshDateBoundary()
    }

    private fun dispatch(operation: String) {
        if (submitted) return
        submitted = true
        refreshDateBoundary()
        parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle(requireArguments()).apply { putString("operation", operation) })
        dismiss()
    }

    private fun openSource(url: String) {
        if (!url.startsWith("https://")) return
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: ActivityNotFoundException) { android.widget.Toast.makeText(requireContext(), R.string.personal_browser_missing, android.widget.Toast.LENGTH_LONG).show() }
    }

    companion object {
        const val RESULT_KEY = "personal_routine_action"
        fun newInstance(routine: UserRoutine, date: RoutineDate, completed: Boolean) = PersonalRoutineDetailSheet().apply {
            arguments = Bundle().apply {
                putLong(RoutineActivity.USER_ROUTINE_ID, routine.id); putString(RoutineActivity.HERO_ID, routine.heroId)
                putString(RoutineActivity.SELECTED_DATE, date.iso); putString("template", routine.templateId)
                putString("title", routine.title); putString("time", routine.time); putString("created", routine.createdOn)
                putInt("days", routine.weekdayMask); putBoolean("enabled", routine.enabled); putBoolean("completed", completed)
            }
        }
    }
}
