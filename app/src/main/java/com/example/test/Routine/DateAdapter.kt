package com.example.test.Routine

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.test.R


// 데이터 아이템
data class DateItem(val month: String, val dayNumber: String, val dayWeek: String, val fullDate: String)

class DateAdapter(
    private val dateList: List<DateItem>,
    private var selectedPosition: Int = 2,
    private val onDateClick: (DateItem) -> Unit // Unit void 와 같은 리턴이 필요없는것
) : RecyclerView.Adapter<DateAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {


        val layoutBg: LinearLayout = view.findViewById(R.id.layout_date_bg) //날짜 백그라운드
        val tvMonth: TextView = view.findViewById(R.id.tv_month) // 날짜 월
        val tvNumber: TextView = view.findViewById(R.id.tv_day_number) // 날짜 일
        val tvWeek: TextView = view.findViewById(R.id.tv_day_week) // 날자 몇요일인지

        fun bind(dateItem: DateItem, isSelected: Boolean) {
            tvMonth.text = dateItem.month
            tvNumber.text = dateItem.dayNumber
            tvWeek.text = dateItem.dayWeek

            //  확실하게 작동하는 배경 교체 방식
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
                selectedPosition = absoluteAdapterPosition // 이 아이템이 지금 어느 인덱스에있는지 확인

                notifyItemChanged(previousPosition) // 확인후 색깔변경
                notifyItemChanged(selectedPosition) //
                onDateClick(dateItem)
            }
        }
    }

    // 그림 그려주는 역할
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date, parent, false)
        return ViewHolder(view)
    }
    // 데이터 채우는 역할 : 리사이클러뷰가 변경되었을때 onCreateViewHolder와 연결
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dateList[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = dateList.size
}