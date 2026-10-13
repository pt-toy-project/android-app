package com.example.test

import android.content.Intent
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
import com.example.test.alarm.AlarmCoordinator
import com.example.test.data.AppRepository
import com.example.test.data.ShopCatalog

class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var loading = false
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
        findViewById<Button>(R.id.btnCheckRoutine).setOnClickListener { navigateTo(RoutineActivity::class.java) }
        findViewById<Button>(R.id.btnViewHero).setOnClickListener { navigateTo(CharacterSelectActivity::class.java) }
        findViewById<Button>(R.id.btnHistory).setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        findViewById<Button>(R.id.btnShop).setOnClickListener { navigateTo(ShopActivity::class.java) }
    }

    override fun onResume() {
        super.onResume()
        setupBottomNavigation(R.id.nav_home)
        handler.removeCallbacks(refresh)
        handler.post(refresh)
        AppIo.run(this, { AlarmCoordinator(applicationContext).reconcile() }) { result ->
            findViewById<TextView>(R.id.home_alarm_status).text = result.fold(
                { if (it.notificationsBlocked) getString(R.string.personal_permission_needed) else if (it.errors.isNotEmpty()) getString(R.string.personal_alarm_failed) else "" },
                { getString(R.string.personal_alarm_failed) }
            )
        }
    }

    override fun onPause() {
        handler.removeCallbacks(refresh)
        super.onPause()
    }

    private fun renderToday() {
        if (loading) return
        loading = true
        val today = RoutineDate.today()
        AppIo.run(this, {
            AppRepository(applicationContext).use { repository ->
                val routines = repository.listRoutines(date = today)
                val recorded = repository.completedRoutineIds(today)
                val complete = routines.map { it.id }.filter { it in recorded }.toSet()
                HomeSnapshot(repository.selectedHeroId, complete.size, routines.size,
                    routines.firstOrNull { it.id !in complete }?.let { getString(R.string.home_next_routine, it.time, it.title) },
                    repository.balance(), repository.appliedItemId())
            }
        }) { result ->
            loading = false
            result.fold(onSuccess = { state ->
                val hero = HeroCatalog.getOrDefault(state.heroId)
                val progressLabel = getString(R.string.home_progress, state.completed, state.total)
                findViewById<TextView>(R.id.tvGreeting2).text = today.label("yyyy년 M월 d일 E요일")
                findViewById<TextView>(R.id.tvRoutineProgressLabel).text = progressLabel
                findViewById<ProgressBar>(R.id.progressBar).apply {
                    max = state.total.coerceAtLeast(1); progress = state.completed; contentDescription = progressLabel
                }
                findViewById<TextView>(R.id.tvNextRoutine).text = state.next ?: getString(
                    if (state.total == 0) R.string.personal_start_hint else R.string.home_all_completed
                )
                findViewById<TextView>(R.id.home_coins).text = getString(R.string.personal_wallet, state.balance)
                findViewById<TextView>(R.id.home_badge).text = ShopCatalog.find(state.badge ?: "")?.let {
                    getString(R.string.personal_badge, it.title)
                } ?: getString(R.string.personal_badge_default)
                findViewById<TextView>(R.id.tvHeroName).text = hero.name
                findViewById<TextView>(R.id.tvHeroQuote).text = hero.bio
                findViewById<ImageView>(R.id.ivHeroImage).apply {
                    setImageResource(hero.imageResId); contentDescription = getString(R.string.home_hero_image, hero.name)
                }
            }, onFailure = {
                findViewById<TextView>(R.id.tvNextRoutine).setText(R.string.personal_storage_error)
            })
        }
    }

    private data class HomeSnapshot(val heroId: String, val completed: Int, val total: Int, val next: String?, val balance: Long, val badge: String?)
}
