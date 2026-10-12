package com.example.test

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test.Hero.Hero
import com.example.test.Hero.HeroAdapter
import com.example.test.Hero.HeroCatalog

class CharacterSelectActivity : AppCompatActivity() {
    private lateinit var fieldSpinner: Spinner
    private var heroDialog: AlertDialog? = null
    private var openingRoutine = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_character_select)
        applySystemBarInsets()
        setupBottomNavigation(R.id.nav_hero)

        val recyclerView = findViewById<RecyclerView>(R.id.heroRecyclerView)
        val emptyView = findViewById<TextView>(R.id.heroEmptyState)
        val heroAdapter = HeroAdapter(::openRoutine, ::showHeroDetail)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = heroAdapter

        fieldSpinner = findViewById(R.id.fieldSpinner)
        fieldSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, HeroCatalog.fields
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        fieldSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val heroes = HeroCatalog.filterByField(HeroCatalog.fields[position])
                heroAdapter.submitList(heroes)
                emptyView.visibility = if (heroes.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (heroes.isEmpty()) View.GONE else View.VISIBLE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        val selectedField = savedInstanceState?.getString(STATE_FIELD) ?: HeroCatalog.ALL_FIELDS
        fieldSpinner.setSelection(HeroCatalog.fields.indexOf(selectedField).coerceAtLeast(0))
    }

    override fun onResume() {
        super.onResume()
        openingRoutine = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_FIELD, HeroCatalog.fields[fieldSpinner.selectedItemPosition.coerceAtLeast(0)])
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        heroDialog?.dismiss()
        heroDialog = null
        super.onStop()
    }

    private fun openRoutine(hero: Hero) {
        if (openingRoutine) return
        openingRoutine = true
        startActivity(
            Intent(this, RoutineActivity::class.java)
                .putExtra("hero_id", hero.id)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
    }

    private fun showHeroDetail(hero: Hero) {
        heroDialog?.dismiss()
        val dialogView = layoutInflater.inflate(R.layout.dialog_hero_detail, null)
        dialogView.findViewById<ImageView>(R.id.ivHero).setImageResource(hero.imageResId)
        dialogView.findViewById<TextView>(R.id.tvHeroName).text = hero.name
        dialogView.findViewById<TextView>(R.id.tvHeroField).text = hero.field
        dialogView.findViewById<TextView>(R.id.tvHeroInfo).text = hero.info

        heroDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(R.string.hero_detail_select) { _, _ -> openRoutine(hero) }
            .setNegativeButton(android.R.string.cancel, null)
            .create().apply {
                setOnDismissListener { heroDialog = null }
                show()
            }
    }

    private companion object {
        const val STATE_FIELD = "selected_hero_field"
    }
}
