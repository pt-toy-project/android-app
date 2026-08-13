package com.example.test

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test.Routine.DateAdapter
import com.example.test.Routine.DateItem
import com.example.test.Routine.Routine
import com.example.test.Routine.RoutineAdapter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RoutineActivity : AppCompatActivity() {

    private var currentSelectedCalendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_routine)

        setupBottomNavigation(R.id.nav_routine)

        updateWeekCalendar(currentSelectedCalendar)

        val ivCalendar: ImageView = findViewById(R.id.iv_calendar)
        ivCalendar.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    currentSelectedCalendar.set(year, month, dayOfMonth)
                    updateWeekCalendar(currentSelectedCalendar)
                },
                currentSelectedCalendar.get(Calendar.YEAR),
                currentSelectedCalendar.get(Calendar.MONTH),
                currentSelectedCalendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        val rvRoutineList: RecyclerView = findViewById(R.id.rv_routine_list)

        // 현재 시간에 따른 색상 변화를 테스트하려면 여기 시간을 직접 바꿔가며 확인해보세요!
        val dummyData = listOf(
            Routine(1, "06:00", "기상", isCompleted = true),
            Routine(2, "06:30", "명상 및 하루 계획", isCompleted = true),
            Routine(3, "07:00", "독서", isCompleted = true),
            Routine(4, "08:00", "업무 시작", isCompleted = false),
            Routine(5, "18:00", "저녁 식사", isCompleted = false),
            Routine(6, "22:00", "취침", isCompleted = false)
        )

        val routineAdapter = RoutineAdapter(dummyData) { clickedRoutine ->
            val bottomSheet = RoutineDetailBottomSheet()
            bottomSheet.show(supportFragmentManager, "RoutineDetail")
        }

        rvRoutineList.layoutManager = LinearLayoutManager(this)
        rvRoutineList.adapter = routineAdapter
    }

    private fun updateWeekCalendar(targetCalendar: Calendar) {
        val rvDateSelector: RecyclerView = findViewById(R.id.rv_date_selector)
        val dateData = mutableListOf<DateItem>()
        val weekCalendar = targetCalendar.clone() as Calendar
        var targetPosition = 0

        val currentDayOfWeek = weekCalendar.get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - Calendar.MONDAY
        weekCalendar.add(Calendar.DAY_OF_MONTH, -daysToSubtract)

        // 월, 일, 요일, 전체날짜 포맷
        val monthFormat = SimpleDateFormat("M월", Locale.getDefault())
        val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
        val weekFormat = SimpleDateFormat("E", Locale.KOREAN)
        val fullDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (i in 0 until 7) {
            val monthStr = monthFormat.format(weekCalendar.time)
            val dayNumber = dayFormat.format(weekCalendar.time)
            val dayWeek = weekFormat.format(weekCalendar.time).replace("요일", "")
            val fullDate = fullDateFormat.format(weekCalendar.time)

            dateData.add(DateItem(monthStr, dayNumber, dayWeek, fullDate))

            if (weekCalendar.get(Calendar.YEAR) == targetCalendar.get(Calendar.YEAR) &&
                weekCalendar.get(Calendar.DAY_OF_YEAR) == targetCalendar.get(Calendar.DAY_OF_YEAR)
            ) {
                targetPosition = i
            }
            weekCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        rvDateSelector.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvDateSelector.adapter = DateAdapter(dateData, selectedPosition = targetPosition) { clickedDate ->
            Toast.makeText(this, "선택한 날짜: ${clickedDate.fullDate}", Toast.LENGTH_SHORT).show()
            Log.d("MY_DATE_TEST", "내부 데이터 값 -> ${clickedDate.fullDate}")
        }
    }
}