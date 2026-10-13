package com.example.test

import android.os.Bundle
import com.example.test.data.RoutineSource

/** Dedicated launcher: the day's progress, next action and adopted HeroDay timeline. */
class MainActivity : TimelineActivity() {
    override val timelineLayout = R.layout.activity_main
    override val routineSource = RoutineSource.HERO_DAY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        timelineHeader.findViewById<android.widget.TextView>(R.id.day_notice).text = com.example.test.Hero.HeroDayCatalog.NOTICE
    }
}
