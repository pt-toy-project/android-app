package com.example.test.Routine

data class Routine(
    val id: Int,
    val time: String,
    val title: String,
    val isCompleted: Boolean = false
) {
    init {
        require(id > 0) { "Routine ID must be positive" }
        require(RoutineTime.parse(time) != null) { "Routine time must be HH:mm" }
        require(title.isNotBlank()) { "Routine title must not be blank" }
    }
}
