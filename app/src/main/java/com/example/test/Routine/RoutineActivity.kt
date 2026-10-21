package com.example.test

import android.os.Bundle
import android.widget.Button
import com.example.test.data.RoutineSource

/** Personal plan management keeps its own source, while Today displays scheduled plans. */
class RoutineActivity : TimelineActivity() {
    override val timelineLayout = R.layout.activity_routine
    override val routineSource = RoutineSource.PERSONAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        timelineHeader.findViewById<Button>(R.id.personal_create).apply {
            visibility = android.view.View.VISIBLE
            setOnClickListener { openPersonalEditor() }
        }
        if (savedInstanceState == null && intent.getBooleanExtra(OPEN_CREATE, false)) {
            window.decorView.post { openPersonalEditor() }
        }
    }

    companion object {
        const val HERO_ID = TimelineActivity.HERO_ID
        const val SELECTED_DATE = TimelineActivity.SELECTED_DATE
        const val ROUTINE_ID = TimelineActivity.ROUTINE_ID
        const val USER_ROUTINE_ID = TimelineActivity.USER_ROUTINE_ID
        const val OPEN_CREATE = "open_create"
    }
}
