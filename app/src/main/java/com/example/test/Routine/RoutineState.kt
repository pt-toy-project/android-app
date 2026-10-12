package com.example.test.Routine

data class RoutineTime(val hour: Int, val minute: Int) {
    init {
        require(hour in 0..23 && minute in 0..59)
    }
    val minutes: Int get() = hour * 60 + minute

    companion object {
        fun parse(value: String): RoutineTime? {
            if (!value.matches(Regex("\\d{2}:\\d{2}"))) return null
            return runCatching { RoutineTime(value.substring(0, 2).toInt(), value.substring(3).toInt()) }.getOrNull()
        }
    }
}

data class RoutineScope(val heroId: String, val date: RoutineDate) {
    init { require(heroId.matches(Regex("[a-z0-9_-]+"))) }
    val storageKey: String get() = "$heroId:${date.iso}"
}

enum class RoutineStatus { COMPLETED, OVERDUE, UPCOMING }

fun Routine.statusOn(date: RoutineDate, today: RoutineDate, now: RoutineTime): RoutineStatus {
    if (isCompleted) return RoutineStatus.COMPLETED
    val scheduled = requireNotNull(RoutineTime.parse(time))
    val due = date < today || (date == today && now.minutes >= scheduled.minutes)
    return if (due) RoutineStatus.OVERDUE else RoutineStatus.UPCOMING
}

/** Demo schedule only; these entries are not verified historical routines. */
object RoutineCatalog {
    val all = listOf(
        Routine(1, "06:00", "기상"),
        Routine(2, "06:30", "명상 및 하루 계획"),
        Routine(3, "07:00", "독서"),
        Routine(4, "08:00", "업무 시작"),
        Routine(5, "18:00", "저녁 식사"),
        Routine(6, "22:00", "취침")
    )
}

interface CompletionStore {
    fun read(key: String): Set<Int>
    fun write(key: String, completedIds: Set<Int>)
}

enum class CompletionResult { RECORDED, ALREADY_COMPLETED, FUTURE_DATE, UNKNOWN_ROUTINE }

/** Contains no Android objects; all writes are scoped by hero, civil date and routine ID. */
class RoutineTracker(private val store: CompletionStore, private val catalog: List<Routine> = RoutineCatalog.all) {
    fun routines(scope: RoutineScope): List<Routine> {
        val completed = store.read(scope.storageKey)
        return catalog.map { it.copy(isCompleted = it.id in completed) }
    }

    @Synchronized
    fun complete(scope: RoutineScope, routineId: Int, today: RoutineDate): CompletionResult {
        if (catalog.none { it.id == routineId }) return CompletionResult.UNKNOWN_ROUTINE
        if (scope.date > today) return CompletionResult.FUTURE_DATE
        val completed = store.read(scope.storageKey)
        if (routineId in completed) return CompletionResult.ALREADY_COMPLETED
        store.write(scope.storageKey, completed + routineId)
        return CompletionResult.RECORDED
    }
}
