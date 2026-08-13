package com.example.test

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test.Routine.CharacterSelectActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. 공통 하단 네비게이션 설정 (현재 선택된 메뉴 ID 전달)
        setupBottomNavigation(R.id.nav_home)

        // 2. 개별 버튼 이벤트 설정
        findViewById<Button>(R.id.btnCheckRoutine).setOnClickListener {
            Toast.makeText(this, "루틴 확인 페이지로 이동", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnViewHero).setOnClickListener {
            val intent = Intent(this, CharacterSelectActivity::class.java)
            startActivity(intent)
        }
    }
}