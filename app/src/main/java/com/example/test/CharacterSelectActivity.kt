package com.example.test

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CharacterSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_character_select)

        setupBottomNavigation(R.id.nav_home)

        val recyclerView = findViewById<RecyclerView>(
            R.id.heroRecyclerView
        )

        recyclerView.layoutManager = GridLayoutManager(
            this,
            2
        )

        val spinner = findViewById<Spinner>(
            R.id.fieldSpinner
        )

        val fields = arrayOf(
            "전체",
            "정치",
            "과학",
            "예술",
            "문학",
            "음악"
        )

        val spinnerAdapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            fields
        )

        spinner.adapter = spinnerAdapter

        val heroList = listOf(

            Hero(
                "세종대왕",
                "정치 · 과학 · 문화",
                "조선 · 15세기",
                R.drawable.sesong
            ),

            Hero(
                "이순신",
                "군사 · 정치",
                "조선 · 16세기",
                R.drawable.ic_launcher_foreground
            ),

            Hero(
                "아인슈타인",
                "과학 · 물리학",
                "독일 · 20세기",
                R.drawable.ic_launcher_foreground
            ),

            Hero(
                "뉴턴",
                "과학 · 수학",
                "영국 · 17세기",
                R.drawable.ic_launcher_foreground
            ),

            Hero(
                "레오나르도 다 빈치",
                "예술 · 과학",
                "이탈리아 · 15세기",
                R.drawable.ic_launcher_foreground
            ),

            Hero(
                "셰익스피어",
                "문학 · 연극",
                "영국 · 16세기",
                R.drawable.ic_launcher_foreground
            )
        )

        // 처음에는 전체 영웅을 보여줌
        var heroAdapter = HeroAdapter(heroList)
        recyclerView.adapter = heroAdapter

        // Spinner에서 분야를 선택했을 때
        spinner.setOnItemSelectedListener(
            object : android.widget.AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {

                    val selectedField = fields[position]

                    val filteredList = if (selectedField == "전체") {
                        heroList
                    } else {
                        heroList.filter { hero ->
                            hero.field.contains(selectedField)
                        }
                    }

                    heroAdapter = HeroAdapter(filteredList)
                    recyclerView.adapter = heroAdapter
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) {
                }
            }
        )
    }
}