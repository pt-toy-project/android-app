package com.example.test

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class SettingActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        setupBottomNavigation(R.id.nav_option)

        // 1. 뷰 가져오기
        val switchSound = findViewById<SwitchCompat>(R.id.switch_sound)
        val switchAlarm = findViewById<SwitchCompat>(R.id.switch_alarm)
        val layoutSound = findViewById<View>(R.id.layout_sound)
        val layoutAlarm = findViewById<View>(R.id.layout_alarm)
        val layoutEtc = findViewById<View>(R.id.layout_etc)

        // 2. 사운드 설정 동작
        switchSound.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "사운드가 활성화되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "사운드가 비활성화되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 사운드 영역 전체를 터치해도 스위치가 토글되도록 처리
        layoutSound.setOnClickListener {
            switchSound.isChecked = !switchSound.isChecked
        }

        // 3. 알람 설정 동작
        switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "알람이 켜졌습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "알람이 꺼졌습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 알람 영역 전체를 터치해도 스위치가 토글되도록 처리
        layoutAlarm.setOnClickListener {
            switchAlarm.isChecked = !switchAlarm.isChecked
        }

        // 4. 기타 항목 클릭 동작
        layoutEtc.setOnClickListener {
            Toast.makeText(this, "기타 설정 화면으로 이동합니다.", Toast.LENGTH_SHORT).show()
            // TODO: 필요한 추가 이동 로직(Intent 등)을 작성하세요.
        }
    }
}