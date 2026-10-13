package com.example.test

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.test.Hero.HeroCatalog
import com.example.test.data.AppRepository
import com.google.android.material.button.MaterialButton

/** Bounded recent history includes archived pre-upgrade sample records without awarding coins. */
class HistoryActivity : AppCompatActivity() {
    private var offset = 0
    private var busy = false
    private val records = mutableListOf<com.example.test.data.CompletionRecord>()
    private lateinit var adapter: RecordAdapter
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_history)
        applySystemBarInsets()
        adapter = RecordAdapter()
        findViewById<RecyclerView>(R.id.history_items).apply { layoutManager = LinearLayoutManager(this@HistoryActivity); adapter = this@HistoryActivity.adapter }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        findViewById<MaterialButton>(R.id.history_back).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.history_more).setOnClickListener { loadPage() }
        loadPage()
    }
    private fun loadPage() {
        if (busy) return
        busy = true
        val start = offset
        val hero = intent.getStringExtra(RoutineActivity.HERO_ID)
        AppIo.run(this, {
            AppRepository(applicationContext).use { repo -> Triple(repo.history(hero, limit = 30, offset = start),
                if (start == 0) repo.legacyHistory(limit = 100, heroId = hero) else emptyList(), repo.legacyHistoryCount(hero)) }
        }) { result ->
            busy = false
            result.fold(onSuccess = { (page, legacy, legacyCount) ->
                val previousSize = records.size
                records.addAll(page)
                adapter.notifyItemRangeInserted(previousSize, page.size)
                if (start == 0) {
                    findViewById<TextView>(R.id.history_legacy).text = getString(R.string.personal_legacy_history, legacyCount,
                        legacy.joinToString("\n") { "${it.date} · ${HeroCatalog.getOrDefault(it.heroId).name} · #${it.sampleId}" })
                    if (page.isEmpty()) findViewById<TextView>(R.id.history_status).setText(R.string.personal_history_empty)
                }
                offset += page.size
                findViewById<MaterialButton>(R.id.history_more).isEnabled = page.size == 30
            }, onFailure = { findViewById<TextView>(R.id.history_status).setText(R.string.personal_storage_error) })
        }
    }

    private inner class RecordAdapter : RecyclerView.Adapter<RecordHolder>() {
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecordHolder = RecordHolder(TextView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(-1, -2)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
            val padding = (16 * resources.displayMetrics.density).toInt(); setPadding(padding, padding, padding, padding)
        })
        override fun onBindViewHolder(holder: RecordHolder, position: Int) {
            val record = records[position]
            holder.text.text = getString(R.string.personal_history_record, record.date.iso,
                HeroCatalog.getOrDefault(record.heroId).name, record.title, record.coins)
        }
        override fun getItemCount() = records.size
    }
    private class RecordHolder(val text: TextView) : RecyclerView.ViewHolder(text)
}
