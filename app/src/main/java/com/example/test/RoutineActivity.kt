package com.example.test

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RoutineActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_routine)

        // XML에 추가한 ImageView와 TextView 찾기
        val ivProfile = findViewById<ImageView>(R.id.ivCharacterProfile)
        val tvName = findViewById<TextView>(R.id.tvCharacterName)

        // 1. 이전 화면(Intent)에서 이름 받아오기
        val name = intent.getStringExtra("character_name")
        // 값이 잘 넘어왔다면 TextView에 설정
        if (name != null) {
            tvName.text = name
        }

        val imageId = intent.getIntExtra("character_image_id", 0)

        // 값이 0이 아니라면 (유효한 ID가 넘어왔다면) ImageView에 이미지 띄우기
        if (imageId != 0) {
            ivProfile.setImageResource(imageId)
        }
    }
}