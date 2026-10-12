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

data class DateRow(val date: RoutineDate, val selected: Boolean)

class DateAdapter(private val onDateClick: (RoutineDate) -> Unit) : ListAdapter<DateRow, DateAdapter.ViewHolder>(DIFF) {
    fun submitDates(dates: List<RoutineDate>, selectedDate: RoutineDate) {
        submitList(dates.map { DateRow(it, it == selectedDate) })
    }
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val month = view.findViewById<TextView>(R.id.tv_month)
        private val number = view.findViewById<TextView>(R.id.tv_day_number)
        private val weekday = view.findViewById<TextView>(R.id.tv_day_week)
        init {
            view.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onDateClick(getItem(position).date)
            }
        }
        fun bind(row: DateRow) {
            month.text = row.date.label("M월")
            number.text = row.date.label("dd")
            weekday.text = row.date.label("E")
            itemView.isSelected = row.selected
            itemView.setBackgroundResource(if (row.selected) R.drawable.bg_date_selected else R.drawable.bg_date_unselected)
            val color = ContextCompat.getColor(itemView.context, if (row.selected) android.R.color.white else R.color.date_unselected_text)
            listOf(month, number, weekday).forEach { it.setTextColor(color) }
            itemView.contentDescription = row.date.label("yyyy년 M월 d일 E요일")
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_date, parent, false)
    )
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))
    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<DateRow>() {
            override fun areItemsTheSame(old: DateRow, new: DateRow) = old.date == new.date
            override fun areContentsTheSame(old: DateRow, new: DateRow) = old == new
        }
    }
}
