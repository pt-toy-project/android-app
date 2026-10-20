package com.example.test.data

import com.example.test.Routine.RoutineDate

data class ProgressSummary(val completions: Int, val activeDays: Int, val earnedCoins: Long, val streak: Int) {
    fun earned(threshold: Int) = completions >= threshold
    companion object {
        /** Today's unfinished day does not break yesterday's ongoing streak. */
        fun streak(dates: Set<RoutineDate>, today: RoutineDate): Int {
            var day = if (today in dates) today else today.plusDays(-1)
            var count = 0
            while (day in dates) { count++; day = day.plusDays(-1) }
            return count
        }
    }
}
