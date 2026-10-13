package com.example.test

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.example.test.Hero.Hero
import com.example.test.Hero.HeroCatalog
import com.example.test.Hero.HeroDayCatalog
import com.example.test.data.AppRepository
import com.example.test.data.RoutineSource

/** Launcher owns the Today layout, editorial preview and whole-day start action. */
class MainActivity : RoutineActivity() {
    override val timelineLayout = R.layout.activity_main
    override val routineSource = RoutineSource.HERO_DAY
    private var previewHero = HeroCatalog.default
    private var startingDay = false
    private var previewExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        previewExpanded = savedInstanceState?.getBoolean("preview_expanded") ?: false
        val restored = savedInstanceState?.getString("preview_hero")
        previewHero = HeroCatalog.getOrDefault(restored)
        val picker = findViewById<Spinner>(R.id.today_hero_picker)
        picker.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,
            HeroCatalog.all.map { it.name }).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        picker.setSelection(HeroCatalog.all.indexOf(previewHero))
        picker.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                bindPreview(HeroCatalog.all[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        findViewById<Button>(R.id.today_start_day).setOnClickListener { startSelectedDay() }
        findViewById<Button>(R.id.today_preview_toggle).setOnClickListener {
            previewExpanded = !previewExpanded
            renderPreviewVisibility()
        }
        findViewById<Button>(R.id.today_hero_detail).setOnClickListener {
            startActivity(Intent(this, HeroDetailActivity::class.java)
                .putExtra(HeroDetailActivity.EXTRA_HERO_ID, previewHero.id))
        }
        bindPreview(previewHero)
    }

    private fun bindPreview(hero: Hero) {
        previewHero = hero
        findViewById<TextView>(R.id.today_hero_field).text = hero.field
        findViewById<ImageView>(R.id.today_hero_portrait).apply {
            setImageResource(hero.imageResId)
            CompactText.circle(this)
            contentDescription = getString(R.string.hero_portrait_description, hero.name)
        }
        val container = findViewById<LinearLayout>(R.id.today_day_preview)
        container.removeAllViews()
        HeroDayCatalog.forHero(hero).blocks.sortedBy { it.suggestedTime }.forEach { block ->
            val row = layoutInflater.inflate(R.layout.item_today_preview, container, false)
            row.findViewById<TextView>(R.id.today_preview_title).apply {
                text = block.title
                CompactText.apply(this)
            }
            row.findViewById<TextView>(R.id.today_preview_time).text = block.suggestedTime
            row.findViewById<TextView>(R.id.today_preview_kind).text = block.kind.label
            row.findViewById<TextView>(R.id.today_preview_grade).text = block.grade.label
            row.contentDescription = "${block.suggestedTime}, ${block.title}, ${block.grade.label}"
            row.setOnClickListener { findViewById<Button>(R.id.today_hero_detail).performClick() }
            container.addView(row)
        }
        renderPreviewVisibility()
    }

    private fun renderPreviewVisibility() {
        findViewById<View>(R.id.today_day_preview).visibility = if (previewExpanded) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.today_preview_toggle).setText(
            if (previewExpanded) R.string.today_hide_preview else R.string.today_show_preview)
    }

    private fun startSelectedDay() {
        if (startingDay) return
        startingDay = true
        val hero = previewHero
        findViewById<Button>(R.id.today_start_day).isEnabled = false
        findViewById<Spinner>(R.id.today_hero_picker).isEnabled = false
        AppIo.run(this, {
            AppRepository(applicationContext).use { it.startHeroDay(hero) }
        }) { result ->
            startingDay = false
            findViewById<Button>(R.id.today_start_day).isEnabled = true
            findViewById<Spinner>(R.id.today_hero_picker).isEnabled = true
            result.onSuccess {
                previewExpanded = false
                renderPreviewVisibility()
                reloadTimeline(hero.id)
                Toast.makeText(this, R.string.day_started, Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(this, R.string.hero_save_error, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("preview_hero", previewHero.id)
        outState.putBoolean("preview_expanded", previewExpanded)
        super.onSaveInstanceState(outState)
    }
}
