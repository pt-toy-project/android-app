package com.example.test.Routine

import android.content.Context
import androidx.core.content.edit
import com.example.test.Hero.HeroCatalog

/** Small local preferences, not a relational DB or a backend/reward ledger. */
class RoutineRepository(context: Context, fileName: String = "routine_state_v1") {
    private val preferences = context.applicationContext.getSharedPreferences(fileName, Context.MODE_PRIVATE)
    private val tracker = RoutineTracker(object : CompletionStore {
        override fun read(key: String): Set<Int> =
            preferences.getStringSet("completed:$key", emptySet()).orEmpty().mapNotNull(String::toIntOrNull).toSet()

        override fun write(key: String, completedIds: Set<Int>) {
            preferences.edit { putStringSet("completed:$key", completedIds.map(Int::toString).toSet()) }
        }
    })

    var selectedHeroId: String
        get() = HeroCatalog.getOrDefault(preferences.getString("selected_hero", null)).id
        set(value) { preferences.edit { putString("selected_hero", HeroCatalog.getOrDefault(value).id) } }

    fun routines(scope: RoutineScope): List<Routine> = tracker.routines(scope)
    fun complete(scope: RoutineScope, id: Int, today: RoutineDate): CompletionResult = synchronized(COMPLETION_LOCK) {
        tracker.complete(scope, id, today)
    }

    companion object {
        // SharedPreferences exposes no transaction; serialize read/modify/write across repository instances in this process.
        private val COMPLETION_LOCK = Any()
    }
}
