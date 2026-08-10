package com.example.test

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class CharacterSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_character_select)

        val btnSejong = findViewById<Button>(R.id.btnSejong)

        btnSejong.setOnClickListener {

            val intent = Intent(this, RoutineActivity::class.java)

            intent.putExtra("character", "세종대왕")

            startActivity(intent)
        }
    }
}