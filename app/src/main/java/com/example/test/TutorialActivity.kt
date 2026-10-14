package com.example.test

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class TutorialActivity : AppCompatActivity() {
    private var page = 0
    private var signing = false
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_tutorial)
        applySystemBarInsets()
        page = state?.getInt("page") ?: if (intent.getBooleanExtra("replay", false)) 1 else 0
        findViewById<View>(R.id.intro_skip).setOnClickListener { finishTutorial(false) }
        findViewById<View>(R.id.intro_google).setOnClickListener {
            if (signing) return@setOnClickListener
            signing = true
            findViewById<View>(R.id.intro_google).isEnabled = false
            GoogleAccountSession.signIn(this) { success ->
                signing = false
                findViewById<View>(R.id.intro_google).isEnabled = true
                if (success) { page = 1; render() }
                else findViewById<TextView>(R.id.intro_account_status).setText(R.string.account_unavailable)
            }
        }
        findViewById<View>(R.id.intro_next).setOnClickListener {
            if (page == 3) finishTutorial(true) else { page++; render() }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { if (page > 0) { page--; render() } else finishTutorial(false) }
        })
        AvatarView.bind(findViewById(R.id.intro_portrait), com.example.test.Hero.HeroCatalog.default.imageResId)
        render()
    }
    private fun render() {
        findViewById<TextView>(R.id.intro_step).text = if (page == 0) getString(R.string.intro_welcome) else "$page / 3"
        val titles = intArrayOf(R.string.intro_title_welcome, R.string.intro_title_hero, R.string.intro_title_quest, R.string.intro_title_backup)
        val subtitles = intArrayOf(R.string.intro_body_welcome, R.string.intro_body_hero, R.string.intro_body_quest, R.string.intro_body_backup)
        findViewById<TextView>(R.id.intro_title).setText(titles[page])
        findViewById<TextView>(R.id.intro_body).setText(subtitles[page])
        findViewById<View>(R.id.intro_google).visibility = if (page == 0) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.intro_account_status).apply {
            visibility = if (page == 0) View.VISIBLE else View.GONE
            setText(if (GoogleAccountSession.available()) R.string.account_optional else R.string.account_not_configured)
        }
        findViewById<TextView>(R.id.intro_demo_title).setText(when (page) {
            2 -> R.string.intro_demo_quest
            3 -> R.string.intro_demo_backup
            else -> R.string.intro_demo_hero
        })
        findViewById<TextView>(R.id.intro_demo_subtitle).setText(when (page) {
            2 -> R.string.intro_demo_quest_detail
            3 -> R.string.intro_demo_backup_detail
            else -> R.string.intro_demo_hero_detail
        })
        findViewById<Button>(R.id.intro_next).setText(when (page) {
            0 -> R.string.intro_guest
            3 -> R.string.intro_start
            else -> R.string.intro_next
        })
    }
    private fun finishTutorial(browse: Boolean) {
        check(getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("completed", true).commit())
        if (browse) startActivity(Intent(this, CharacterSelectActivity::class.java))
        finish()
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putInt("page", page); super.onSaveInstanceState(outState) }
    companion object {
        const val PREFS = "first_run_guide"
        fun needed(context: Context) = !context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("completed", false)
    }
}
