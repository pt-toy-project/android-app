package com.example.test

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.test.data.AppRepository
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator

class ProgressActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)
        applySystemBarInsets()
        ScreenToolbar.bind(this, R.string.progress_title, back = true)
        findViewById<View>(R.id.progress_month_open).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).putExtra("calendar_expanded", true)
                .putExtra(TimelineActivity.SELECTED_DATE, com.example.test.Routine.RoutineDate.today().iso)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
    }
    override fun onResume() {
        super.onResume()
        AppIo.run(this, { AppRepository(applicationContext).use { it.progressSummary() } }) { result ->
            result.fold(onSuccess = { summary ->
                findViewById<TextView>(R.id.progress_totals).text = getString(R.string.progress_totals, summary.activeDays, summary.streak, summary.completions, summary.earnedCoins)
                val badges = findViewById<LinearLayout>(R.id.progress_badges)
                badges.removeAllViews()
                listOf(1 to R.string.progress_badge_first, 10 to R.string.progress_badge_ten, 30 to R.string.progress_badge_thirty, 100 to R.string.progress_badge_hundred).forEach { (threshold, title) ->
                    val achieved = summary.earned(threshold)
                    val card = MaterialCardView(this).apply {
                        radius = dp(24).toFloat(); cardElevation = 0f; strokeWidth = 0
                        setCardBackgroundColor(ContextCompat.getColor(context, if (achieved) R.color.ui_primary_container else R.color.ui_surface))
                        tag = "badge:$threshold"
                    }
                    val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(20)) }
                    val heading = TextView(this).apply { text = getString(title); textSize = 18f; setTypeface(typeface, android.graphics.Typeface.BOLD); setTextColor(ContextCompat.getColor(context, R.color.ui_text)) }
                    val caption = TextView(this).apply {
                        text = if (achieved) getString(R.string.progress_badge_earned, threshold) else getString(R.string.progress_badge_remaining, summary.completions, threshold, threshold - summary.completions)
                        textSize = 14f; setTextColor(ContextCompat.getColor(context, R.color.ui_secondary)); setPadding(0, dp(8), 0, dp(12))
                    }
                    content.addView(heading); content.addView(caption)
                    content.addView(LinearProgressIndicator(this).apply { max = threshold; progress = summary.completions.coerceAtMost(threshold); contentDescription = caption.text })
                    card.addView(content)
                    badges.addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12) })
                }
            }, onFailure = { findViewById<TextView>(R.id.progress_totals).setText(R.string.calendar_error) })
        }
    }
    private fun dp(value: Int) = (resources.displayMetrics.density * value).toInt()
}
