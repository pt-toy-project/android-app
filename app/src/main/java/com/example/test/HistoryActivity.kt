package com.example.test

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test.Routine.TimelineHeaderAdapter
import com.example.test.data.AppRepository
import com.example.test.data.CompletionRecord
import com.example.test.data.RoutineIdentity

/** Bounded history keeps original titles and IDs, including archived pre-upgrade records. */
class HistoryActivity : AppCompatActivity() {
    private val records = mutableListOf<CompletionRecord>()
    private val adapter = RecordAdapter()
    private lateinit var header: View
    private var offset = 0
    private var loading = false
    private var legacyExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        applySystemBarInsets()
        ScreenToolbar.bind(this, R.string.personal_history, back = true)
        header = layoutInflater.inflate(R.layout.item_history_header, null)
        legacyExpanded = savedInstanceState?.getBoolean("legacy_expanded") ?: false
        findViewById<RecyclerView>(R.id.history_items).apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = ConcatAdapter(TimelineHeaderAdapter(header), this@HistoryActivity.adapter)
        }
        header.findViewById<Button>(R.id.history_legacy_toggle).setOnClickListener {
            legacyExpanded = !legacyExpanded
            header.findViewById<View>(R.id.history_legacy).visibility = if (legacyExpanded) View.VISIBLE else View.GONE
        }
        findViewById<Button>(R.id.history_more).setOnClickListener { loadPage() }
        loadPage()
    }

    private fun loadPage() {
        if (loading) return
        loading = true
        findViewById<Button>(R.id.history_more).isEnabled = false
        val start = offset
        val hero = intent.getStringExtra(RoutineActivity.HERO_ID)
        AppIo.run(this, {
            AppRepository(applicationContext).use { repo ->
                Triple(repo.history(hero, limit = 30, offset = start),
                    if (start == 0) repo.legacyHistory(limit = 100, heroId = hero) else emptyList(),
                    repo.legacyHistoryCount(hero))
            }
        }) { result ->
            loading = false
            result.fold(onSuccess = { (page, legacy, legacyCount) ->
                val previousSize = records.size
                records.addAll(page)
                adapter.notifyItemRangeInserted(previousSize, page.size)
                offset += page.size
                header.findViewById<TextView>(R.id.history_status).text = if (records.isEmpty())
                    getString(R.string.personal_history_empty) else getString(R.string.redesign_record_count, records.size)
                if (start == 0) {
                    header.findViewById<Button>(R.id.history_legacy_toggle).apply {
                        text = getString(R.string.redesign_legacy_count, legacyCount)
                        visibility = if (legacyCount == 0) View.GONE else View.VISIBLE
                    }
                    header.findViewById<TextView>(R.id.history_legacy).apply {
                        text = getString(R.string.personal_legacy_history, legacyCount,
                            legacy.joinToString("\n") { "${it.date} · ${RoutineIdentity.displayName(it.heroId)} · #${it.sampleId}" })
                        visibility = if (legacyExpanded && legacyCount > 0) View.VISIBLE else View.GONE
                    }
                }
                findViewById<Button>(R.id.history_more).isEnabled = page.size == 30
            }, onFailure = {
                header.findViewById<TextView>(R.id.history_status).setText(R.string.personal_storage_error)
                findViewById<Button>(R.id.history_more).isEnabled = true
            })
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("legacy_expanded", legacyExpanded)
        super.onSaveInstanceState(outState)
    }

    private inner class RecordAdapter : RecyclerView.Adapter<RecordHolder>() {
        override fun getItemCount() = records.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = RecordHolder(
            layoutInflater.inflate(R.layout.item_history_record, parent, false))
        override fun onBindViewHolder(holder: RecordHolder, position: Int) {
            val record = records[position]
            holder.itemView.findViewById<TextView>(R.id.history_record_title).apply {
                text = record.title
                CompactText.apply(this)
            }
            holder.itemView.findViewById<TextView>(R.id.history_record_meta).apply {
                text = getString(R.string.redesign_record_meta, record.date.iso, RoutineIdentity.displayName(record.heroId))
                CompactText.apply(this)
            }
            holder.itemView.findViewById<TextView>(R.id.history_record_coins).text = getString(R.string.personal_wallet, record.coins)
        }
    }
    private class RecordHolder(view: View) : RecyclerView.ViewHolder(view)
}
