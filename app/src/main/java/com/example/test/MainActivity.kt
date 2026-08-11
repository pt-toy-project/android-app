package com.example.test

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. [ 루틴 확인 ] 버튼 클릭 이벤트
        val btnCheckRoutine = findViewById<Button>(R.id.btnCheckRoutine)
        btnCheckRoutine.setOnClickListener {
            Toast.makeText(this, "루틴 확인 페이지로 이동", Toast.LENGTH_SHORT).show()
        }

        // 2. [ 위인 보기 ] 버튼 클릭 이벤트
        val btnViewHero = findViewById<Button>(R.id.btnViewHero)
        btnViewHero.setOnClickListener {
            val intent = Intent(this, CharacterSelectActivity::class.java)
            startActivity(intent)
        }

        // 3. 하단 네비게이션 버튼 이벤트 (함수 이름 변경됨!)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.setOnNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // 홈 화면 처리
                    true
                }
                R.id.nav_routine -> {
                    // 루틴 목록 화면 처리
                    Toast.makeText(this, "루틴 탭 클릭", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_hero -> {
                    // 위인 목록 화면 처리
                    Toast.makeText(this, "위인 탭 클릭", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_myroom -> {
                    // 마이룸 화면 처리
                    Toast.makeText(this, "마이룸 탭 클릭", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_option -> {
                    // 마이룸 화면 처리
                    Toast.makeText(this, "없어 이자식아", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }
}