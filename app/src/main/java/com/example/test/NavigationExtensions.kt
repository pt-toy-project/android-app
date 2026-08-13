package com.example.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

fun AppCompatActivity.setupBottomNavigation(selectedItemId: Int) {

    val bottomNav = findViewById<BottomNavigationView>(
        R.id.bottomNavigationView
    ) ?: return

    // 현재 페이지의 메뉴를 선택 상태로 표시
    bottomNav.selectedItemId = selectedItemId

    bottomNav.setOnItemSelectedListener { item ->

        when (item.itemId) {

            R.id.nav_home -> {

                if (this !is MainActivity) {
                    startActivity(
                        Intent(this, MainActivity::class.java)
                    )

                    overridePendingTransition(0, 0)
                    finish()
                }

                true
            }

            R.id.nav_routine -> {

                // 나중에 RoutineActivity가 만들어지면 연결
                true
            }

            R.id.nav_hero -> {

                if (this !is CharacterSelectActivity) {
                    startActivity(
                        Intent(
                            this,
                            CharacterSelectActivity::class.java
                        )
                    )

                    overridePendingTransition(0, 0)
                    finish()
                }

                true
            }

            R.id.nav_myroom -> {

                // 나중에 MyRoomActivity가 만들어지면 연결
                true
            }

            R.id.nav_option -> {

                // 나중에 OptionActivity가 만들어지면 연결
                true
            }

            else -> false
        }
    }
}