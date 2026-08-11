package com.example.test

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

fun AppCompatActivity.setupBottomNavigation(selectedItemId: Int) {
    val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView) ?: return

    // 현재 선택된 탭 활성화 (이벤트 재발생 방지를 위해 리스너 등록 전 설정)
    bottomNav.selectedItemId = selectedItemId

    bottomNav.setOnItemSelectedListener { item ->
        if (item.itemId == selectedItemId) return@setOnItemSelectedListener true

        when (item.itemId) {
            R.id.nav_home -> {
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(0, 0)
                finish()
                true
            }
            R.id.nav_routine -> {
                Toast.makeText(this, "루틴 탭 클릭", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.nav_hero -> {
                startActivity(Intent(this, CharacterSelectActivity::class.java))
                overridePendingTransition(0, 0)
                finish()
                true
            }
            R.id.nav_myroom -> {
                Toast.makeText(this, "마이룸 탭 클릭", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.nav_option -> {
                Toast.makeText(this, "설정 탭 클릭", Toast.LENGTH_SHORT).show()
                true
            }
            else -> false
        }
    }
}