package com.example.test

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RoutineActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_routine)

        val tvCharacterName = findViewById<TextView>(R.id.tvCharacterName)

        val characterName = intent.getStringExtra("character")

        tvCharacterName.text = characterName
    }
}