package com.example.test

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test.Hero.Hero
import com.example.test.Hero.HeroAdapter
import com.example.test.Hero.HeroCatalog
import com.example.test.alarm.AlarmCoordinator

class CharacterSelectActivity : AppCompatActivity() {
    private lateinit var fieldSpinner: Spinner
    private lateinit var heroSearch: EditText
    private var openingDetail = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_character_select)
        applySystemBarInsets()
        setupBottomNavigation(R.id.nav_hero)

        val recyclerView = findViewById<RecyclerView>(R.id.heroRecyclerView)
        val emptyView = findViewById<TextView>(R.id.heroEmptyState)
        val heroAdapter = HeroAdapter(::showHeroDetail, ::showHeroDetail)
        recyclerView.layoutManager = GridLayoutManager(this, 1)
        recyclerView.adapter = heroAdapter

        fieldSpinner = findViewById(R.id.fieldSpinner)
        fieldSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, HeroCatalog.fields
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        heroSearch = findViewById(R.id.heroSearch)
        fun updateResults() {
            val selectedField = HeroCatalog.fields[fieldSpinner.selectedItemPosition.coerceAtLeast(0)]
            val query = heroSearch.text.toString().trim()
            val heroes = HeroCatalog.filterByField(selectedField).filter { hero ->
                query.isEmpty() || listOf(hero.name, hero.field, hero.info, hero.bio).any {
                    it.contains(query, ignoreCase = true)
                }
            }
            heroAdapter.submitList(heroes)
            emptyView.visibility = if (heroes.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (heroes.isEmpty()) View.GONE else View.VISIBLE
            findViewById<TextView>(R.id.heroResultCount).text = getString(
                R.string.hero_result_count, heroes.size, HeroCatalog.all.size
            )
        }
        fieldSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateResults()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        val selectedField = savedInstanceState?.getString(STATE_FIELD) ?: HeroCatalog.ALL_FIELDS
        fieldSpinner.setSelection(HeroCatalog.fields.indexOf(selectedField).coerceAtLeast(0))
        heroSearch.setText(savedInstanceState?.getString(STATE_SEARCH).orEmpty())
        heroSearch.doAfterTextChanged { updateResults() }
        updateResults()
    }

    override fun onResume() {
        super.onResume()
        openingDetail = false
        val appContext = applicationContext
        AppIo.run(this, { AlarmCoordinator(appContext).reconcile() }) { result ->
            val restoreFailed = result.isFailure || result.getOrNull()?.errors?.isNotEmpty() == true
            findViewById<TextView>(R.id.heroAlarmStatus).visibility = if (restoreFailed) View.VISIBLE else View.GONE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_FIELD, HeroCatalog.fields[fieldSpinner.selectedItemPosition.coerceAtLeast(0)])
        outState.putString(STATE_SEARCH, heroSearch.text.toString())
        super.onSaveInstanceState(outState)
    }

    private fun showHeroDetail(hero: Hero) {
        if (openingDetail) return
        openingDetail = true
        startActivity(Intent(this, HeroDetailActivity::class.java).putExtra(HeroDetailActivity.EXTRA_HERO_ID, hero.id).putExtra("personal_mode", intent.getBooleanExtra("personal_mode", false)))
    }

    private companion object {
        const val STATE_FIELD = "selected_hero_field"
        const val STATE_SEARCH = "hero_search"
    }
}
