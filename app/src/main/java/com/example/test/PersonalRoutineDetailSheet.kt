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
        val hero = HeroCatalog.all.firstOrNull { it.id == args.getString(RoutineActivity.HERO_ID) }
        val block = hero?.let { com.example.test.Hero.HeroDayCatalog.find(it.id, args.getString("template").orEmpty()) }
        val template = hero?.routines?.firstOrNull { it.id == args.getString("template") }
        view.findViewById<TextView>(R.id.personal_detail_title).text = args.getString("title")
        view.findViewById<TextView>(R.id.personal_detail_hero).text = com.example.test.data.RoutineIdentity.displayName(args.getString(RoutineActivity.HERO_ID).orEmpty())
        view.findViewById<TextView>(R.id.personal_detail_date).text = getString(R.string.routine_detail_schedule,
            args.getString(RoutineActivity.SELECTED_DATE).orEmpty(), args.getString("time").orEmpty())
        view.findViewById<TextView>(R.id.personal_detail_fact).text = block?.let { "${it.grade.label} · 자료 재확인 미완료\n${it.historicalTime?.let { time -> "v2 출처 시간대: $time\n" }.orEmpty()}${com.example.test.Hero.HeroDayCatalog.NOTICE}" } ?: template?.historicalFact ?: getString(R.string.personal_snapshot)
        view.findViewById<TextView>(R.id.personal_detail_practice).text = block?.description ?: template?.modernPractice.orEmpty()
        // Evidence is available from the source button; keep the action sheet focused on doing.
        view.findViewById<View>(R.id.personal_detail_fact).visibility = View.GONE
        view.findViewById<View>(R.id.personal_detail_practice).visibility = if (block == null) View.VISIBLE else View.GONE
        bindQuests(block?.quests.orEmpty())
        view.findViewById<Button>(R.id.personal_detail_source).apply {
            visibility = if (block == null && template == null) View.GONE else View.VISIBLE
            setText(R.string.day_details)
            setOnClickListener {
                if (block != null && hero != null && !parentFragmentManager.isStateSaved) {
                    HeroEvidenceSheet.forBlock(hero.id, block.id).show(parentFragmentManager, "hero_evidence")
                } else (block?.source ?: template?.source)?.url?.let(::openSource)
            }
        }
        view.findViewById<Button>(R.id.personal_detail_complete).setOnClickListener { dispatch("complete") }
        view.findViewById<Button>(R.id.personal_detail_edit).setOnClickListener { dispatch("edit") }
        view.findViewById<Button>(R.id.personal_detail_delete).setOnClickListener { dispatch("delete") }
        view.findViewById<Button>(R.id.personal_detail_focus).setOnClickListener {
            startActivity(Intent(requireContext(), FocusActivity::class.java)
                .putExtra(TimelineActivity.USER_ROUTINE_ID, args.getLong(RoutineActivity.USER_ROUTINE_ID)))
        }
        refreshDateBoundary()
    }

    private fun bindQuests(quests: List<com.example.test.Hero.DayQuest>) {
        val root = view ?: return
        val args = requireArguments()
        val date = RoutineDate.parse(args.getString(RoutineActivity.SELECTED_DATE)) ?: return
        val id = args.getLong(RoutineActivity.USER_ROUTINE_ID)
        val progress = root.findViewById<TextView>(R.id.quest_detail_progress)
        val container = root.findViewById<android.widget.LinearLayout>(R.id.quest_detail_container)
        progress.visibility = if (quests.isEmpty()) View.GONE else View.VISIBLE
        root.findViewById<View>(R.id.quest_detail_notice).visibility = progress.visibility
        val context = requireContext().applicationContext
        AppIo.run(viewLifecycleOwner, {
            com.example.test.data.AppRepository(context).use { repo ->
                val routine = repo.getRoutine(id)
                val editable = routine != null && routine.enabled && date <= RoutineDate.today() &&
                    date.iso >= routine.createdOn && RoutineRules.scheduledOn(routine.weekdayMask, date)
                repo.questChecks(id, date) to editable
            }
        }) { result ->
            result.onSuccess { (checks, editable) ->
                val checked = checks.filter { key -> quests.any { it.id == key } }.toMutableSet()
                fun updateProgress() { progress.text = getString(R.string.quest_progress, checked.size, quests.size) }
                updateProgress()
                QuestViews.bind(container, quests, checked, editable) { quest, requested, finish ->
                    AppIo.run(viewLifecycleOwner, {
                        com.example.test.data.AppRepository(context).use { it.setQuestChecked(id, quest.id, date, requested) }
                    }) { saved ->
                        val success = saved.getOrNull() == true
                        finish(success)
                        if (success) {
                            if (requested) checked.add(quest.id) else checked.remove(quest.id)
                            updateProgress()
                            parentFragmentManager.setFragmentResult(CHECKS_CHANGED, Bundle.EMPTY)
                        } else android.widget.Toast.makeText(context,
                            if (saved.isFailure) R.string.quest_save_error else R.string.quest_unavailable,
                            android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }.onFailure {
                progress.setText(R.string.quest_save_error)
                QuestViews.bind(container, quests)
            }
        }
    }

    fun refreshDateBoundary(today: RoutineDate = RoutineDate.today()) {
        val view = view ?: return
        val args = requireArguments()
        val date = RoutineDate.parse(args.getString(RoutineActivity.SELECTED_DATE))
        val adoption = RoutineDate.parse(args.getString("created"))
        val completed = args.getBoolean("completed")
        view.findViewById<Button>(R.id.personal_detail_focus).visibility = if (!completed && args.getBoolean("enabled") &&
            date == today && adoption != null && date >= adoption && RoutineRules.scheduledOn(args.getInt("days"), date)) View.VISIBLE else View.GONE
        view.findViewById<Button>(R.id.personal_detail_complete).apply {
            setText(if (completed) R.string.routine_status_completed else R.string.personal_complete_reward)
            isEnabled = !submitted && !completed && args.getBoolean("enabled") && date != null && date <= today &&
                (adoption == null || date >= adoption) && RoutineRules.scheduledOn(args.getInt("days"), date)
        }
    }

    fun updateCompletion(completedIds: Set<Long>, heroId: String, date: RoutineDate) {
        val args = requireArguments()
        if (args.getString(RoutineActivity.SELECTED_DATE) == date.iso) {
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
        const val CHECKS_CHANGED = "quest_checks_changed"
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
