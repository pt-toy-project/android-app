package com.example.test.Routine

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.test.R
import com.example.test.data.UserRoutine

data class PersonalRoutineRow(val routine: UserRoutine, val completed: Boolean, val date: RoutineDate, val today: RoutineDate, val currentMinute: Int)

class PersonalRoutineAdapter(
    private val onComplete: (PersonalRoutineRow) -> Unit = {},
    private val onClick: (PersonalRoutineRow) -> Unit
) : ListAdapter<PersonalRoutineRow, PersonalRoutineAdapter.Holder>(DIFF) {
    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val time = view.findViewById<TextView>(R.id.tv_time)
        private val title = view.findViewById<TextView>(R.id.tv_routine_title)
        private val status = view.findViewById<TextView>(R.id.tv_status)
        init { view.setOnClickListener { if (bindingAdapterPosition != RecyclerView.NO_POSITION) onClick(getItem(bindingAdapterPosition)) } }
        fun bind(row: PersonalRoutineRow) {
            time.text = "${row.routine.time} · ${com.example.test.data.RoutineIdentity.displayName(row.routine.heroId)}"
            title.text = row.routine.title
            com.example.test.CompactText.apply(time)
            com.example.test.CompactText.apply(title)
            val heroName = com.example.test.data.RoutineIdentity.displayName(row.routine.heroId)
            val block = com.example.test.Hero.HeroDayCatalog.find(row.routine.heroId, row.routine.templateId)
            itemView.findViewById<TextView>(R.id.day_row_subtitle).visibility = View.GONE
            itemView.findViewById<TextView>(R.id.day_row_subtitle).text = if (row.routine.source == com.example.test.data.RoutineSource.HERO_DAY)
                block?.description.orEmpty() else {
                    val schedule = if (row.routine.weekdayMask == 127) itemView.context.getString(R.string.redesign_everyday)
                        else itemView.resources.getStringArray(R.array.personal_weekdays).filterIndexed { index, _ -> row.routine.weekdayMask and (1 shl index) != 0 }.joinToString(" · ")
                    itemView.context.getString(R.string.redesign_plan_schedule, schedule,
                        itemView.context.getString(if (row.routine.alarmEnabled) R.string.redesign_alarm_on else R.string.redesign_alarm_off))
                }
            itemView.findViewById<com.google.android.material.chip.Chip>(R.id.day_grade).apply {
                visibility = if (row.routine.source == com.example.test.data.RoutineSource.HERO_DAY) View.VISIBLE else View.GONE
                text = block?.grade?.label ?: "현대 적용"
            }
            com.example.test.CompactText.apply(itemView.findViewById(R.id.day_row_subtitle))
            itemView.findViewById<android.widget.Button>(R.id.timeline_complete).apply {
                isEnabled = TimelinePresentation.canComplete(row)
                setText(if (row.completed) R.string.routine_status_completed else R.string.timeline_complete)
                contentDescription = itemView.context.getString(R.string.timeline_complete_accessibility, heroName, row.routine.title)
                setOnClickListener { if (bindingAdapterPosition != RecyclerView.NO_POSITION) onComplete(getItem(bindingAdapterPosition)) }
            }
            itemView.findViewById<android.widget.Button>(R.id.timeline_detail).apply {
                contentDescription = itemView.context.getString(R.string.timeline_detail_accessibility, heroName, row.routine.title)
                setOnClickListener { if (bindingAdapterPosition != RecyclerView.NO_POSITION) onClick(getItem(bindingAdapterPosition)) }
            }
            val actions = itemView.findViewById<android.widget.LinearLayout>(R.id.timeline_actions)
            val expanded = itemView.resources.configuration.fontScale >= 1.3f
            actions.orientation = if (expanded) android.widget.LinearLayout.VERTICAL else android.widget.LinearLayout.HORIZONTAL
            for (index in 0 until actions.childCount) {
                actions.getChildAt(index).layoutParams = android.widget.LinearLayout.LayoutParams(
                    if (expanded) android.view.ViewGroup.LayoutParams.MATCH_PARENT else 0,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT, if (expanded) 0f else 1f)
            }
            val due = RoutineTime.parse(row.routine.time)?.minutes ?: Int.MAX_VALUE
            val (label, color) = when {
                row.completed -> R.string.routine_status_completed to R.color.routine_completed
                !row.routine.enabled -> R.string.personal_paused to R.color.routine_upcoming
                row.date < row.today || (row.date == row.today && row.currentMinute >= due) -> R.string.routine_status_overdue to R.color.routine_overdue
                else -> R.string.routine_status_upcoming to R.color.routine_upcoming
            }
            status.setText(label); status.setTextColor(ContextCompat.getColor(itemView.context, color))
            time.setTextColor(ContextCompat.getColor(itemView.context, color))
            itemView.contentDescription = heroName + ", " + itemView.context.getString(R.string.routine_accessibility, row.routine.time, row.routine.title, itemView.context.getString(label)) + ", " + itemView.findViewById<TextView>(R.id.day_row_subtitle).text + ", " + (block?.grade?.label.orEmpty())
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_routine, parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))
    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PersonalRoutineRow>() {
            override fun areItemsTheSame(old: PersonalRoutineRow, new: PersonalRoutineRow) = old.routine.id == new.routine.id
            override fun areContentsTheSame(old: PersonalRoutineRow, new: PersonalRoutineRow) = old == new
        }
    }
}
