package com.example.test.Routine

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.test.R
import java.util.Calendar

class RoutineAdapter(
    private val routineList: List<Routine>,
    private val onItemClick: (Routine) -> Unit
) : RecyclerView.Adapter<RoutineAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tv_time)
        val tvTitle: TextView = view.findViewById(R.id.tv_routine_title)
        val viewTimeline: View = view.findViewById(R.id.view_timeline_line)
        val ivActionBtn: ImageView = view.findViewById(R.id.iv_action_btn)
        val ivTimelineDot: ImageView = view.findViewById(R.id.iv_timeline_dot)

        fun bind(routine: Routine) {
            tvTime.text = routine.time
            tvTitle.text = routine.title

            // 1. 휴대폰의 현재 시간 가져오기
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            // 2. 루틴의 시간 데이터를 시간(Hour)과 분(Minute)으로 분리 ("06:30" -> 6과 30)
            val timeParts = routine.time.split(":")
            val routineHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
            val routineMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

            // 3. 현재 시간이 루틴 시간을 지났는지 판별
            val isTimePassed = (currentHour > routineHour) ||
                    (currentHour == routineHour && currentMinute > routineMinute)

            // 4. 상태에 따른 UI 색상 업데이트
            if (routine.isCompleted) {
                // [완료] 파란색
                tvTime.setTextColor(Color.parseColor("#5B67C9"))
                viewTimeline.setBackgroundColor(Color.parseColor("#5B67C9"))

                // ivActionBtn.setImageResource(R.drawable.ic_check_circle)
            } else if (isTimePassed) {
                // [미완료 & 시간 지남] 빨간색
                tvTime.setTextColor(Color.parseColor("#FF3B30"))
                viewTimeline.setBackgroundColor(Color.parseColor("#FF3B30"))


            } else {
                // [미완료 & 아직 시간 안 지남] 기본 회색
                tvTime.setTextColor(Color.parseColor("#999999"))
                viewTimeline.setBackgroundColor(Color.parseColor("#EEEEEE"))


            }

            // 아이템 클릭 이벤트
            itemView.setOnClickListener {
                onItemClick(routine)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_routine, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(routineList[position])
    }

    override fun getItemCount(): Int = routineList.size
}