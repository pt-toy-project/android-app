package com.example.test

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.example.test.Routine.Routine
import com.example.test.Routine.RoutineDate
import com.example.test.Routine.RoutineScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** Arguments survive recreation; the result keeps the original hero/date/routine identity. */
class RoutineDetailBottomSheet : BottomSheetDialogFragment(R.layout.dialog_routine_detail) {
    private var completionRequested = false
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        val date = RoutineDate.parse(args.getString(RoutineActivity.SELECTED_DATE))
        view.findViewById<TextView>(R.id.detail_hero).text = args.getString(HERO_NAME)
        view.findViewById<TextView>(R.id.detail_title).text = args.getString(TITLE)
        view.findViewById<TextView>(R.id.detail_schedule).text = getString(
            R.string.routine_detail_schedule, date?.iso.orEmpty(), args.getString(TIME).orEmpty()
        )
        view.findViewById<Button>(R.id.btn_start).apply {
            setOnClickListener {
                if (completionRequested) return@setOnClickListener
                completionRequested = true
                isEnabled = false
                parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle(args))
                dismiss()
            }
        }
        refreshCompletionAvailability()
        view.findViewById<Button>(R.id.btn_remind_later).setOnClickListener { dismiss() }
    }

    /** Re-evaluate an open sheet at midnight as well as after view recreation. */
    fun refreshCompletionAvailability(today: RoutineDate = RoutineDate.today()) {
        val view = view ?: return
        val args = requireArguments()
        val date = RoutineDate.parse(args.getString(RoutineActivity.SELECTED_DATE))
        val completed = args.getBoolean(COMPLETED)
        view.findViewById<Button>(R.id.btn_start).apply {
            setText(if (completed) R.string.routine_status_completed else R.string.routine_complete)
            isEnabled = date != null && date <= today && !completed && !completionRequested
        }
        view.findViewById<TextView>(R.id.detail_notice).setText(
            if (date != null && date > today) R.string.routine_future_completion else R.string.routine_sample_notice
        )
    }
    companion object {
        const val RESULT_KEY = "routine_completion"
        private const val HERO_NAME = "hero_name"
        private const val TITLE = "routine_title"
        private const val TIME = "routine_time"
        private const val COMPLETED = "completed"
        fun newInstance(scope: RoutineScope, routine: Routine, heroName: String) = RoutineDetailBottomSheet().apply {
            arguments = Bundle().apply {
                putString(RoutineActivity.HERO_ID, scope.heroId)
                putString(RoutineActivity.SELECTED_DATE, scope.date.iso)
                putInt(RoutineActivity.ROUTINE_ID, routine.id)
                putString(HERO_NAME, heroName)
                putString(TITLE, routine.title)
                putString(TIME, routine.time)
                putBoolean(COMPLETED, routine.isCompleted)
            }
        }
    }
}
