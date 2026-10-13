package com.example.test

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test.Hero.Hero
import com.example.test.Hero.HeroAdapter
import com.example.test.Hero.HeroCatalog
import com.example.test.alarm.AlarmCoordinator
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/** Catalog browsing stays independent of adoption and user's saved block settings. */
class CharacterSelectActivity : AppCompatActivity() {
    private lateinit var heroAdapter: HeroAdapter
    private lateinit var search: EditText
    private var selectedField = HeroCatalog.ALL_FIELDS
    private var openingDetail = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_character_select)
        applySystemBarInsets()
        ScreenToolbar.bind(this, R.string.nav_hero)
        selectedField = savedInstanceState?.getString(FIELD) ?: HeroCatalog.ALL_FIELDS
        search = findViewById(R.id.heroSearch)
        heroAdapter = HeroAdapter(::openHero, ::openHero)
        findViewById<RecyclerView>(R.id.heroRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@CharacterSelectActivity)
            adapter = heroAdapter
        }
        bindFields()
        search.setText(savedInstanceState?.getString(QUERY).orEmpty())
        search.doAfterTextChanged { renderResults() }
        renderResults()
    }

    private fun bindFields() {
        val group = findViewById<ChipGroup>(R.id.hero_fields)
        HeroCatalog.fields.forEach { field ->
            group.addView(Chip(this).apply {
                id = View.generateViewId()
                text = field
                isCheckable = true
                isChecked = field == selectedField
                minHeight = (48 * resources.displayMetrics.density).toInt()
                contentDescription = getString(R.string.redesign_filter_label) + ", " + field
                setOnClickListener {
                    selectedField = field
                    renderResults()
                }
            })
        }
    }

    private fun renderResults() {
        val query = search.text.toString().trim()
        val rows = HeroCatalog.filterByField(selectedField).filter { hero ->
            query.isEmpty() || listOf(hero.name, hero.field, hero.info, hero.bio)
                .any { it.contains(query, ignoreCase = true) }
        }
        heroAdapter.submitList(rows)
        findViewById<View>(R.id.heroEmptyState).visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.heroRecyclerView).visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE
        findViewById<TextView>(R.id.heroResultCount).text = getString(R.string.hero_result_count, rows.size, HeroCatalog.all.size)
    }

    private fun openHero(hero: Hero) {
        if (openingDetail) return
        openingDetail = true
        startActivity(Intent(this, HeroDetailActivity::class.java)
            .putExtra(HeroDetailActivity.EXTRA_HERO_ID, hero.id)
            .putExtra("personal_mode", intent.getBooleanExtra("personal_mode", false)))
    }

    override fun onResume() {
        super.onResume()
        openingDetail = false
        setupBottomNavigation(R.id.nav_hero)
        AppIo.run(this, { AlarmCoordinator(applicationContext).reconcile() }) { result ->
            val failed = result.isFailure || result.getOrNull()?.errors?.isNotEmpty() == true
            findViewById<View>(R.id.heroAlarmStatus).visibility = if (failed) View.VISIBLE else View.GONE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(FIELD, selectedField)
        outState.putString(QUERY, search.text.toString())
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val FIELD = "selected_hero_field"
        private const val QUERY = "hero_search"
    }
}
