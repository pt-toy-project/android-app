package com.example.test

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test.Hero.HeroCatalog
import com.example.test.Routine.*
import java.util.Calendar
import java.util.GregorianCalendar

class RoutineActivity : AppCompatActivity() {
    private lateinit var repository: RoutineRepository
    private lateinit var routineAdapter: RoutineAdapter
    private lateinit var dateAdapter: DateAdapter
    private lateinit var datesView: RecyclerView
    private lateinit var selectedDate: RoutineDate
    private lateinit var heroId: String
    private var datePicker: DatePickerDialog? = null
    private val handler = Handler(Looper.getMainLooper())
    private val clockRefresh = object : Runnable {
        override fun run() {
            renderRoutines()
            handler.postDelayed(this, 60_000L - System.currentTimeMillis() % 60_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_routine)
        applySystemBarInsets()
        setupBottomNavigation(R.id.nav_routine)
        repository = RoutineRepository(this)
        heroId = HeroCatalog.getOrDefault(
            savedInstanceState?.getString(HERO_ID) ?: intent.getStringExtra(HERO_ID) ?: repository.selectedHeroId
        ).id
        repository.selectedHeroId = heroId
        selectedDate = RoutineDate.parse(savedInstanceState?.getString(SELECTED_DATE)) ?: RoutineDate.today()
        routineAdapter = RoutineAdapter(::showRoutine)
        findViewById<RecyclerView>(R.id.rv_routine_list).apply {
            layoutManager = LinearLayoutManager(this@RoutineActivity)
            adapter = routineAdapter
        }
        dateAdapter = DateAdapter { date ->
            selectedDate = date
            renderDates()
            renderRoutines()
        }
        datesView = findViewById<RecyclerView>(R.id.rv_date_selector).apply {
            layoutManager = LinearLayoutManager(this@RoutineActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = dateAdapter
        }
        findViewById<ImageButton>(R.id.iv_calendar).setOnClickListener { showDatePicker() }
        supportFragmentManager.setFragmentResultListener(RoutineDetailBottomSheet.RESULT_KEY, this) { _, result ->
            val date = RoutineDate.parse(result.getString(SELECTED_DATE)) ?: return@setFragmentResultListener
            val resultHero = result.getString(HERO_ID) ?: return@setFragmentResultListener
            if (HeroCatalog.getOrDefault(resultHero).id != resultHero) return@setFragmentResultListener
            val outcome = repository.complete(RoutineScope(resultHero, date), result.getInt(ROUTINE_ID, -1), RoutineDate.today())
            val message = when (outcome) {
                CompletionResult.RECORDED -> R.string.routine_completion_recorded
                CompletionResult.ALREADY_COMPLETED -> R.string.routine_already_completed
                CompletionResult.FUTURE_DATE -> R.string.routine_future_completion
                CompletionResult.UNKNOWN_ROUTINE -> R.string.routine_invalid
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            renderRoutines()
        }
        renderDates()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(HERO_ID)?.let {
            heroId = HeroCatalog.getOrDefault(it).id
            repository.selectedHeroId = heroId
            (supportFragmentManager.findFragmentByTag(DETAIL_TAG) as? RoutineDetailBottomSheet)?.dismiss()
            renderRoutines()
        }
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(clockRefresh)
        handler.post(clockRefresh)
    }

    override fun onPause() {
        handler.removeCallbacks(clockRefresh)
        super.onPause()
    }

    override fun onStop() {
        datePicker?.dismiss()
        datePicker = null
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(HERO_ID, heroId)
        outState.putString(SELECTED_DATE, selectedDate.iso)
        super.onSaveInstanceState(outState)
    }

    private fun renderDates() {
        val week = selectedDate.week()
        dateAdapter.submitDates(week, selectedDate)
        datesView.scrollToPosition(week.indexOf(selectedDate))
    }

    private fun renderRoutines() {
        val now = GregorianCalendar()
        val hero = HeroCatalog.getOrDefault(heroId)
        findViewById<TextView>(R.id.routine_title).text = getString(R.string.routine_title, hero.name)
        findViewById<TextView>(R.id.routine_date).text = selectedDate.label("yyyy년 M월 d일 E요일")
        routineAdapter.submitRoutines(
            repository.routines(RoutineScope(heroId, selectedDate)), selectedDate, RoutineDate.fromCalendar(now),
            RoutineTime(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
        )
        (supportFragmentManager.findFragmentByTag(DETAIL_TAG) as? RoutineDetailBottomSheet)?.refreshCompletionAvailability()
    }

    private fun showRoutine(routine: Routine) {
        if (supportFragmentManager.isStateSaved || supportFragmentManager.findFragmentByTag(DETAIL_TAG) != null) return
        val scope = RoutineScope(heroId, selectedDate)
        val currentRoutine = repository.routines(scope).firstOrNull { it.id == routine.id } ?: return
        RoutineDetailBottomSheet.newInstance(
            scope, currentRoutine, HeroCatalog.getOrDefault(heroId).name
        ).showNow(supportFragmentManager, DETAIL_TAG)
    }

    private fun showDatePicker() {
        if (datePicker?.isShowing == true) return
        datePicker = DatePickerDialog(this, { _, year, month, day ->
            selectedDate = RoutineDate(year, month + 1, day)
            renderDates()
            renderRoutines()
        }, selectedDate.year, selectedDate.month - 1, selectedDate.day).also { it.show() }
    }

    companion object {
        const val HERO_ID = "hero_id"
        const val SELECTED_DATE = "selected_date"
        const val ROUTINE_ID = "routine_id"
        private const val DETAIL_TAG = "routine_detail"
    }
}
