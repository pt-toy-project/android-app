package com.example.test

import com.example.test.data.RoutineSource

/** Today uses the same independently editable timeline controls, scoped to HeroDay. */
class MainActivity : RoutineActivity() {
    override val routineSource = RoutineSource.HERO_DAY
}
