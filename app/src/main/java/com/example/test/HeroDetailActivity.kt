package com.example.test

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.test.Hero.Hero
import com.example.test.Hero.HeroCatalog
import com.example.test.Hero.HeroDayCatalog
import com.example.test.alarm.AlarmCoordinator
import com.example.test.data.AppRepository
import com.example.test.data.RoutineSource

/** One person's whole day is previewed here; user settings are only created on explicit adoption. */
class HeroDetailActivity : AppCompatActivity() {
    private var hero: Hero? = null
    private var saving = false
    private var aboutDialog: AlertDialog? = null
    private val personalMode get() = intent.getBooleanExtra("personal_mode", false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hero_detail)
        applySystemBarInsets()
        ScreenToolbar.bind(this, R.string.redesign_preview, back = true)
        hero = HeroCatalog.all.firstOrNull { it.id == intent.getStringExtra(EXTRA_HERO_ID) }
        val person = hero ?: run {
            findViewById<View>(R.id.heroDetailError).visibility = View.VISIBLE
            findViewById<View>(R.id.heroDetailContent).visibility = View.GONE
            findViewById<View>(R.id.hero_footer).visibility = View.GONE
            return
        }
        bindPerson(person)
        bindBlocks(person)
        findViewById<Button>(R.id.heroPersonalRoutines).apply {
            setText(if (personalMode) R.string.day_personal else R.string.day_start)
            setOnClickListener {
                if (personalMode) startActivity(Intent(this@HeroDetailActivity, RoutineActivity::class.java))
                else adoptDay(person)
            }
        }
        findViewById<Button>(R.id.heroStorageRetry).setOnClickListener { loadAdoption() }
    }

    private fun bindPerson(person: Hero) {
        findViewById<TextView>(R.id.heroDetailName).text = person.name
        findViewById<TextView>(R.id.heroDetailField).text = getString(R.string.redesign_hero_intro, person.field, person.info)
        findViewById<TextView>(R.id.heroDetailBio).text = HeroDayCatalog.NOTICE
        findViewById<TextView>(R.id.heroPortraitCredit).text = person.portraitCredit
        findViewById<ImageView>(R.id.heroDetailPortrait).apply {
            AvatarView.bind(this, person.imageResId)
            contentDescription = getString(R.string.hero_portrait_description, person.name)
        }
        findViewById<Button>(R.id.hero_about).setOnClickListener {
            aboutDialog?.dismiss()
            aboutDialog = AlertDialog.Builder(this).setTitle(person.name).setMessage(person.bio)
                .setPositiveButton(android.R.string.ok, null).show()
        }
        findViewById<Button>(R.id.hero_credit).setOnClickListener {
            aboutDialog?.dismiss()
            aboutDialog = AlertDialog.Builder(this).setTitle(R.string.redesign_credit)
                .setMessage(person.portraitCredit).setPositiveButton(android.R.string.ok, null).show()
        }
    }

    private fun bindBlocks(person: Hero) {
        val container = findViewById<LinearLayout>(R.id.historicalRoutineContainer)
        HeroDayCatalog.forHero(person).blocks.sortedBy { it.suggestedTime }.forEach { block ->
            val card = layoutInflater.inflate(R.layout.item_day_preview, container, false)
            card.findViewById<TextView>(R.id.day_preview_title).apply { text = block.title; CompactText.apply(this) }
            card.findViewById<TextView>(R.id.day_preview_subtitle).text = getString(R.string.redesign_suggested, block.suggestedTime, block.kind.label)
            card.findViewById<TextView>(R.id.day_preview_description).apply { text = block.description; CompactText.apply(this) }
            card.findViewById<TextView>(R.id.day_grade).text = block.grade.label
            fun showEvidence() {
                if (!supportFragmentManager.isStateSaved && supportFragmentManager.findFragmentByTag("hero_evidence") == null) {
                    HeroEvidenceSheet.forBlock(person.id, block.id).show(supportFragmentManager, "hero_evidence")
                }
            }
            card.setOnClickListener { showEvidence() }
            card.findViewById<Button>(R.id.day_preview_details).setOnClickListener { showEvidence() }
            card.findViewById<Button>(R.id.day_preview_add).apply {
                visibility = if (personalMode) View.VISIBLE else View.GONE
                setOnClickListener { adoptPersonalBlock(person, block.id, block.title, block.suggestedTime) }
            }
            container.addView(card)
        }
    }

    private fun loadAdoption() {
        val person = hero ?: return
        AppIo.run(this, {
            AppRepository(applicationContext).use { it.listRoutines(person.id, source = RoutineSource.HERO_DAY).size }
        }) { result ->
            findViewById<View>(R.id.heroStorageError).visibility = if (result.isFailure) View.VISIBLE else View.GONE
            findViewById<View>(R.id.heroStorageRetry).visibility = if (result.isFailure) View.VISIBLE else View.GONE
            findViewById<TextView>(R.id.hero_day_summary).text = if (result.getOrNull() == 8) getString(R.string.redesign_started) else getString(R.string.redesign_eight_blocks)
        }
    }

    private fun adoptDay(person: Hero) {
        if (saving) return
        saving = true
        findViewById<Button>(R.id.heroPersonalRoutines).isEnabled = false
        AppIo.run(this, {
            AppRepository(applicationContext).use { it.startHeroDay(person) }
            runCatching { AlarmCoordinator(applicationContext).reconcile() }
        }) { result ->
            saving = false
            findViewById<Button>(R.id.heroPersonalRoutines).isEnabled = true
            result.onSuccess {
                Toast.makeText(this, R.string.day_started, Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivity::class.java).putExtra(RoutineActivity.HERO_ID, person.id).putExtra(RoutineActivity.SELECTED_DATE, com.example.test.Routine.RoutineDate.today().iso)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            }.onFailure { Toast.makeText(this, R.string.hero_save_error, Toast.LENGTH_LONG).show() }
        }
    }

    private fun adoptPersonalBlock(person: Hero, id: String, title: String, time: String) {
        if (saving) return
        saving = true
        AppIo.run(this, { AppRepository(applicationContext).use { it.addRoutine(person.id, id, title, time) } }) { result ->
            saving = false
            Toast.makeText(this, if (result.isSuccess) R.string.hero_added_message else R.string.hero_save_error, Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() { super.onResume(); loadAdoption() }
    override fun onDestroy() { aboutDialog?.dismiss(); super.onDestroy() }
    companion object { const val EXTRA_HERO_ID = "hero_id" }
}
