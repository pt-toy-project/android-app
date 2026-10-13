package com.example.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

/** Keep home as the task root and reuse an existing destination when it is on the stack. */
fun AppCompatActivity.navigateTo(destination: Class<out AppCompatActivity>) {
    if (destination.isInstance(this)) return
    startActivity(
        Intent(this, destination)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    )
    if (this !is MainActivity) finish()
}

fun AppCompatActivity.setupBottomNavigation(selectedItemId: Int) {
    val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView) ?: return
    // Clear the old listener before restoring the selection after returning to a retained screen.
    bottomNav.setOnItemSelectedListener(null)
    bottomNav.selectedItemId = selectedItemId
    bottomNav.setOnItemSelectedListener { item ->
        when (item.itemId) {
            R.id.nav_home -> {
                navigateTo(MainActivity::class.java)
                true
            }
            R.id.nav_routine -> {
                navigateTo(RoutineActivity::class.java)
                true
            }
            R.id.nav_hero -> {
                navigateTo(CharacterSelectActivity::class.java)
                true
            }
            R.id.nav_option -> {
                navigateTo(SettingActivity::class.java)
                true
            }
            R.id.nav_myroom -> {
                navigateTo(ShopActivity::class.java)
                true
            }
            else -> false
        }
    }
}
