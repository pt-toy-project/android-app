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
        val btnYiSunSin = findViewById<Button>(R.id.btnYiSunSin)
        val btnEinstein = findViewById<Button>(R.id.btnEinstein)
        btnSejong.setOnClickListener {

            val intent = Intent(this, RoutineActivity::class.java)

            intent.putExtra("character", "세종대왕")
            intent.putExtra("character_image_id",R.drawable.sesong)


            startActivity(intent)
        }
        btnYiSunSin.setOnClickListener {

            val intent = Intent(this, RoutineActivity::class.java)

            intent.putExtra("character", "이순신")

            startActivity(intent)
        }
        btnEinstein.setOnClickListener {

            val intent = Intent(this, RoutineActivity::class.java)

            intent.putExtra("character", "아인슈타인")

            startActivity(intent)
        }

    }
}