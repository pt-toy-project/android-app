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
    private var selectedGoal = com.example.test.Hero.HeroQuestCatalog.goals.first()
    private var openingDetail = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_character_select)
        findViewById<View>(R.id.hero_ad_slot).visibility = if (BuildConfig.DEBUG) View.VISIBLE else View.GONE
        applySystemBarInsets()
        ScreenToolbar.bind(this, R.string.nav_hero)
        selectedField = savedInstanceState?.getString(FIELD) ?: HeroCatalog.ALL_FIELDS
        selectedGoal = savedInstanceState?.getString(GOAL)?.takeIf { it in com.example.test.Hero.HeroQuestCatalog.goals }
            ?: com.example.test.Hero.HeroQuestCatalog.goals.first()
        search = findViewById(R.id.heroSearch)
        heroAdapter = HeroAdapter(::openHero, ::openHero)
        findViewById<RecyclerView>(R.id.heroRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@CharacterSelectActivity)
            adapter = heroAdapter
        }
        bindFields()
        findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.hero_goal).apply {
            setSimpleItems(com.example.test.Hero.HeroQuestCatalog.goals.toTypedArray())
            setText(selectedGoal, false)
            setOnItemClickListener { parent, _, position, _ ->
                // Filtered and accessibility popup adapters can have different item positions.
                selectedGoal = (parent?.getItemAtPosition(position) as? String ?: text.toString())
                    .takeIf { it in com.example.test.Hero.HeroQuestCatalog.goals } ?: selectedGoal
                renderResults()
            }
        }
        findViewById<View>(R.id.hero_reset_filters).setOnClickListener {
            selectedField = HeroCatalog.ALL_FIELDS
            selectedGoal = com.example.test.Hero.HeroQuestCatalog.goals.first()
            findViewById<ChipGroup>(R.id.hero_fields).apply { removeAllViews() }
            bindFields()
            findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.hero_goal)
                .setText(selectedGoal, false)
            search.setText("")
            search.clearFocus()
            renderResults()
        }
        val suggestions = this@CharacterSelectActivity.findViewById<ChipGroup>(R.id.hero_goal_suggestions)
        listOf("공부·연구", "글쓰기·창작", "탐험·관찰").forEach { goal ->
            suggestions.addView(Chip(this).apply {
                id = View.generateViewId(); text = goal; tag = goal; isCheckable = true
                minHeight = (48 * resources.displayMetrics.density).toInt()
                setOnClickListener {
                    selectedGoal = goal
                    selectedField = HeroCatalog.ALL_FIELDS
                    this@CharacterSelectActivity.findViewById<ChipGroup>(R.id.hero_fields).removeAllViews(); bindFields()
                    search.setText("")
                    this@CharacterSelectActivity.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.hero_goal).setText(goal, false)
                    renderResults()
                }
            })
        }
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
        val suggestions = findViewById<ChipGroup>(R.id.hero_goal_suggestions)
        for (index in 0 until suggestions.childCount) {
            (suggestions.getChildAt(index) as Chip).isChecked = suggestions.getChildAt(index).tag == selectedGoal
        }
        val query = search.text.toString().trim()
        val rows = HeroCatalog.filterByField(selectedField).filter { hero ->
            val goal = com.example.test.Hero.HeroQuestCatalog.goalFor(hero.id)
            (selectedGoal == com.example.test.Hero.HeroQuestCatalog.goals.first() || goal == selectedGoal) &&
                (query.isEmpty() || listOf(hero.name, hero.field, hero.info, hero.bio, goal)
                    .any { it.contains(query, ignoreCase = true) })
        }
        heroAdapter.submitList(rows)
        findViewById<View>(R.id.hero_empty_actions).visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
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
        outState.putString(GOAL, selectedGoal)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val FIELD = "selected_hero_field"
        private const val QUERY = "hero_search"
        private const val GOAL = "hero_learning_goal"
    }
}
