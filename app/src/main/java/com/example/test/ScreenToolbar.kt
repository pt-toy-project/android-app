package com.example.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

/** Common secondary routes keep every primary page focused on its own task. */
object ScreenToolbar {
    fun bind(activity: AppCompatActivity, title: Int, back: Boolean = false) {
        val toolbar = activity.findViewById<MaterialToolbar>(R.id.screen_toolbar) ?: return
        toolbar.title = if (activity is CharacterSelectActivity)
            activity.getString(R.string.compact_catalog_title, com.example.test.Hero.HeroCatalog.all.size)
        else activity.getString(title)
        if (activity is MainActivity) { toolbar.setTitle(R.string.app_name); toolbar.subtitle = null }
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.screen_menu)
        if (back) {
            toolbar.setNavigationIcon(R.drawable.ic_back_v3)
            toolbar.setNavigationContentDescription(R.string.personal_back)
            toolbar.setNavigationOnClickListener { activity.finish() }
        }
        toolbar.setOnMenuItemClickListener { item ->
            val destination = when (item.itemId) {
                R.id.screen_progress -> ProgressActivity::class.java
                R.id.screen_history -> HistoryActivity::class.java
                R.id.screen_settings -> SettingActivity::class.java
                R.id.screen_credits -> CreditsActivity::class.java
                else -> null
            }
            if (destination == null) false else {
                if (!destination.isInstance(activity)) activity.startActivity(Intent(activity, destination))
                true
            }
        }
    }
}
