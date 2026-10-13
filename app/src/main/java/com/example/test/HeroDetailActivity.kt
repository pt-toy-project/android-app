package com.example.test

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import com.example.test.Hero.HeroDayBlock
import com.example.test.Hero.HeroDayCatalog
import com.example.test.data.AppRepository
import com.example.test.data.RoutineSource

/** Preview is editorial; adopting a day atomically creates independent user snapshots. */
class HeroDetailActivity : AppCompatActivity() {
    private var hero: Hero? = null
    private var saving = false
    private var detailDialog: AlertDialog? = null
    private val personalMode get() = intent.getBooleanExtra("personal_mode", false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hero_detail)
        applySystemBarInsets()
        findViewById<Button>(R.id.heroDetailBack).setOnClickListener { finish() }
        hero = HeroCatalog.all.firstOrNull { it.id == intent.getStringExtra(EXTRA_HERO_ID) }
        val person = hero ?: run {
            findViewById<TextView>(R.id.heroDetailError).visibility = View.VISIBLE
            findViewById<View>(R.id.heroDetailContent).visibility = View.GONE
            return
        }
        findViewById<TextView>(R.id.heroDetailName).text = person.name
        findViewById<TextView>(R.id.heroDetailField).text = person.field
        findViewById<TextView>(R.id.heroDetailBio).text = HeroDayCatalog.NOTICE
        findViewById<ImageView>(R.id.heroDetailPortrait).apply {
            setImageResource(person.imageResId)
            CompactText.circle(this)
            contentDescription = getString(R.string.hero_portrait_description, person.name)
        }
        findViewById<TextView>(R.id.heroPortraitCredit).text = person.portraitCredit
        findViewById<Button>(R.id.heroPortraitSource).visibility = View.GONE
        findViewById<Button>(R.id.heroStorageRetry).visibility = View.GONE
        findViewById<Button>(R.id.heroPersonalRoutines).apply {
            setText(if (personalMode) R.string.day_personal else R.string.day_start)
            setOnClickListener {
                if (personalMode) startActivity(Intent(this@HeroDetailActivity, RoutineActivity::class.java))
                else startDay(person)
            }
        }
        val container = findViewById<LinearLayout>(R.id.historicalRoutineContainer)
        HeroDayCatalog.forHero(person).blocks.sortedBy { it.suggestedTime }.forEach { block ->
            val card = layoutInflater.inflate(R.layout.item_day_preview, container, false)
            card.findViewById<TextView>(R.id.day_preview_title).apply { text = block.title; CompactText.apply(this) }
            card.findViewById<TextView>(R.id.day_preview_subtitle).apply {
                text = getString(R.string.day_preview_summary, block.suggestedTime, block.kind.label)
                CompactText.apply(this)
            }
            card.findViewById<com.google.android.material.chip.Chip>(R.id.day_grade).text = block.grade.label
            card.findViewById<Button>(R.id.day_preview_details).setOnClickListener { showEvidence(person, block) }
            card.findViewById<Button>(R.id.day_preview_add).apply {
                visibility = if (personalMode) View.VISIBLE else View.GONE
                setOnClickListener { addPersonal(person, block) }
            }
            container.addView(card)
        }
    }
    private fun showEvidence(person: Hero, block: HeroDayBlock) {
        val original = person.routines.firstOrNull { it.id == block.id }
        val evidence = original?.historicalFact?.let { "\n\nv2 설명(이번 환경에서 원문 재확인 불가): $it" }.orEmpty()
        detailDialog?.dismiss()
        detailDialog = AlertDialog.Builder(this).setTitle(block.title)
            .setMessage("${block.grade.label}\n${block.description}\n\n알림 제안: ${block.suggestedTime}\n" +
                "v2 출처 시간대: ${block.historicalTime ?: "미기록"}\n개인 고정 일과로 확인되지 않았습니다.$evidence\n\n" +
                "${block.source.title}\n${block.source.locator}\n${block.source.url}\n미확인: 프록시 CONNECT 403\n\n${person.bio}")
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(R.string.day_source_open) { _, _ ->
                val sourceUri = Uri.parse(block.source.url)
                try { startActivity(Intent(Intent.ACTION_VIEW, sourceUri)) }
                catch (_: ActivityNotFoundException) { Toast.makeText(this, R.string.hero_browser_unavailable, Toast.LENGTH_LONG).show() }
            }.show()
    }
    private fun startDay(person: Hero) {
        if (saving) return
        saving = true
        findViewById<Button>(R.id.heroPersonalRoutines).isEnabled = false
        AppIo.run(this, { AppRepository(applicationContext).use { it.startHeroDay(person) } }) { result ->
            saving = false
            findViewById<Button>(R.id.heroPersonalRoutines).isEnabled = true
            result.onSuccess {
                Toast.makeText(this, R.string.day_started, Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            }.onFailure { Toast.makeText(this, R.string.hero_save_error, Toast.LENGTH_LONG).show() }
        }
    }
    private fun addPersonal(person: Hero, block: HeroDayBlock) {
        if (saving) return
        saving = true
        AppIo.run(this, {
            AppRepository(applicationContext).use {
                it.addRoutine(person.id, block.id, block.title, block.suggestedTime, source = RoutineSource.PERSONAL)
            }
        }) { result ->
            saving = false
            Toast.makeText(this, if (result.isSuccess) R.string.hero_added_message else R.string.hero_save_error, Toast.LENGTH_LONG).show()
        }
    }
    override fun onDestroy() { detailDialog?.dismiss(); super.onDestroy() }
    companion object { const val EXTRA_HERO_ID = "hero_id" }
}
