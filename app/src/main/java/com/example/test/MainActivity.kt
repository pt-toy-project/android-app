package com.example.test

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.test.Hero.HeroCatalog
import com.example.test.Routine.RoutineDate
import com.example.test.Routine.RoutineRepository
import com.example.test.Routine.RoutineScope

class MainActivity : AppCompatActivity() {
    private lateinit var repository: RoutineRepository
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            renderToday()
            handler.postDelayed(this, 60_000L - System.currentTimeMillis() % 60_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applySystemBarInsets()
        repository = RoutineRepository(this)

        findViewById<Button>(R.id.btnCheckRoutine).setOnClickListener {
            navigateTo(RoutineActivity::class.java)
        }
        findViewById<Button>(R.id.btnViewHero).setOnClickListener {
            navigateTo(CharacterSelectActivity::class.java)
        }
    }

    override fun onResume() {
        super.onResume()
        setupBottomNavigation(R.id.nav_home)
        handler.removeCallbacks(refresh)
        handler.post(refresh)
    }

    override fun onPause() {
        handler.removeCallbacks(refresh)
        super.onPause()
    }

    private fun renderToday() {
        val today = RoutineDate.today()
        val hero = HeroCatalog.getOrDefault(repository.selectedHeroId)
        val routines = repository.routines(RoutineScope(hero.id, today))
        val completed = routines.count { it.isCompleted }
        val progressLabel = getString(R.string.home_progress, completed, routines.size)

        findViewById<TextView>(R.id.tvGreeting2).text = today.label("yyyy년 M월 d일 E요일")
        findViewById<TextView>(R.id.tvRoutineProgressLabel).text = progressLabel
        findViewById<ProgressBar>(R.id.progressBar).apply {
            max = routines.size.coerceAtLeast(1)
            progress = completed
            contentDescription = progressLabel
        }
        val next = routines.firstOrNull { !it.isCompleted }
        findViewById<TextView>(R.id.tvNextRoutine).text = when {
            routines.isEmpty() -> getString(R.string.home_no_routines)
            next == null -> getString(R.string.home_all_completed)
            else -> getString(R.string.home_next_routine, next.time, next.title)
        }
        findViewById<TextView>(R.id.tvHeroName).text = hero.name
        findViewById<TextView>(R.id.tvHeroQuote).text = getString(R.string.home_hero_info, hero.field, hero.info)
        findViewById<ImageView>(R.id.ivHeroImage).apply {
            setImageResource(hero.imageResId)
            contentDescription = getString(R.string.home_hero_image, hero.name)
        }
    }
}
