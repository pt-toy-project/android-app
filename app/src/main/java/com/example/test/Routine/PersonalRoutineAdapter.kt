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

class PersonalRoutineAdapter(private val onClick: (PersonalRoutineRow) -> Unit) : ListAdapter<PersonalRoutineRow, PersonalRoutineAdapter.Holder>(DIFF) {
    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val time = view.findViewById<TextView>(R.id.tv_time)
        private val title = view.findViewById<TextView>(R.id.tv_routine_title)
        private val status = view.findViewById<TextView>(R.id.tv_status)
        init { view.setOnClickListener { if (bindingAdapterPosition != RecyclerView.NO_POSITION) onClick(getItem(bindingAdapterPosition)) } }
        fun bind(row: PersonalRoutineRow) {
            time.text = row.routine.time; title.text = row.routine.title
            val due = RoutineTime.parse(row.routine.time)?.minutes ?: Int.MAX_VALUE
            val (label, color) = when {
                row.completed -> R.string.routine_status_completed to R.color.routine_completed
                !row.routine.enabled -> R.string.personal_paused to R.color.routine_upcoming
                row.date < row.today || (row.date == row.today && row.currentMinute >= due) -> R.string.routine_status_overdue to R.color.routine_overdue
                else -> R.string.routine_status_upcoming to R.color.routine_upcoming
            }
            status.setText(label); status.setTextColor(ContextCompat.getColor(itemView.context, color))
            time.setTextColor(ContextCompat.getColor(itemView.context, color))
            itemView.contentDescription = itemView.context.getString(R.string.routine_accessibility, row.routine.time, row.routine.title, itemView.context.getString(label))
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
