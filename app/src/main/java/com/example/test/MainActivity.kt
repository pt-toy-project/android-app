package com.example.test

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnSelectCharacter = findViewById<Button>(R.id.btnSelectCharacter)

        btnSelectCharacter.setOnClickListener {
            val intent = Intent(this, CharacterSelectActivity::class.java)
            startActivity(intent)
        }

    }
}