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

data class RoutineRow(val routine: Routine, val status: RoutineStatus)

class RoutineAdapter(private val onItemClick: (Routine) -> Unit) : ListAdapter<RoutineRow, RoutineAdapter.ViewHolder>(DIFF) {
    fun submitRoutines(routines: List<Routine>, date: RoutineDate, today: RoutineDate, now: RoutineTime) {
        submitList(routines.map { RoutineRow(it, it.statusOn(date, today, now)) })
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val time = view.findViewById<TextView>(R.id.tv_time)
        private val title = view.findViewById<TextView>(R.id.tv_routine_title)
        private val status = view.findViewById<TextView>(R.id.tv_status)
        init {
            view.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onItemClick(getItem(position).routine)
            }
        }
        fun bind(row: RoutineRow) {
            time.text = row.routine.time
            title.text = row.routine.title
            val (label, color) = when (row.status) {
                RoutineStatus.COMPLETED -> R.string.routine_status_completed to R.color.routine_completed
                RoutineStatus.OVERDUE -> R.string.routine_status_overdue to R.color.routine_overdue
                RoutineStatus.UPCOMING -> R.string.routine_status_upcoming to R.color.routine_upcoming
            }
            status.setText(label)
            status.setTextColor(ContextCompat.getColor(itemView.context, color))
            time.setTextColor(ContextCompat.getColor(itemView.context, color))
            itemView.contentDescription = itemView.context.getString(
                R.string.routine_accessibility, row.routine.time, row.routine.title, itemView.context.getString(label)
            )
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_routine, parent, false)
    )
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))
    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<RoutineRow>() {
            override fun areItemsTheSame(old: RoutineRow, new: RoutineRow) = old.routine.id == new.routine.id
            override fun areContentsTheSame(old: RoutineRow, new: RoutineRow) = old == new
        }
    }
}
