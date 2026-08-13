package com.example.test.Routine

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.test.R

// 월(month) 데이터 포함
data class DateItem(val month: String, val dayNumber: String, val dayWeek: String, val fullDate: String)

class DateAdapter(
    private val dateList: List<DateItem>,
    private var selectedPosition: Int = 2,
    private val onDateClick: (DateItem) -> Unit
) : RecyclerView.Adapter<DateAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutBg: LinearLayout = view.findViewById(R.id.layout_date_bg)
        val tvMonth: TextView = view.findViewById(R.id.tv_month)
        val tvNumber: TextView = view.findViewById(R.id.tv_day_number)
        val tvWeek: TextView = view.findViewById(R.id.tv_day_week)

        fun bind(dateItem: DateItem, isSelected: Boolean) {
            tvMonth.text = dateItem.month
            tvNumber.text = dateItem.dayNumber
            tvWeek.text = dateItem.dayWeek

            // 🌟 확실하게 작동하는 배경 교체 방식 🌟
            if (isSelected) {
                // 선택됨: 파란색 배경 파일 장착 + 흰색 글씨
                layoutBg.setBackgroundResource(R.drawable.bg_date_selected)
                tvMonth.setTextColor(Color.WHITE)
                tvNumber.setTextColor(Color.WHITE)
                tvWeek.setTextColor(Color.WHITE)
            } else {
                // 선택 안 됨: 회색 배경 파일 장착 + 어두운 글씨
                layoutBg.setBackgroundResource(R.drawable.bg_date_unselected)
                tvMonth.setTextColor(Color.parseColor("#888888"))
                tvNumber.setTextColor(Color.parseColor("#333333"))
                tvWeek.setTextColor(Color.parseColor("#888888"))
            }

            itemView.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = absoluteAdapterPosition

                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)

                onDateClick(dateItem)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dateList[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = dateList.size
}