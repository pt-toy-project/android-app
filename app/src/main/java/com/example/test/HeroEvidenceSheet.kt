package com.example.test

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.test.Hero.HeroCatalog
import com.example.test.Hero.HeroDayCatalog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** Long evidence and source descriptions stay out of the preview cards. */
class HeroEvidenceSheet : BottomSheetDialogFragment(R.layout.sheet_hero_evidence) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val hero = HeroCatalog.all.firstOrNull { it.id == requireArguments().getString("hero") }
        val block = hero?.let { HeroDayCatalog.find(it.id, requireArguments().getString("block").orEmpty()) }
        if (hero == null || block == null) {
            view.findViewById<TextView>(R.id.evidence_title).setText(R.string.routine_invalid)
            view.findViewById<Button>(R.id.evidence_open).isEnabled = false
            return
        }
        view.findViewById<TextView>(R.id.evidence_title).text = block.title
        QuestViews.bind(view.findViewById(R.id.evidence_quests), block.quests)
        view.findViewById<Button>(R.id.evidence_toggle).setOnClickListener {
            val group = view.findViewById<View>(R.id.evidence_citation_group)
            val expanded = group.visibility != View.VISIBLE
            group.visibility = if (expanded) View.VISIBLE else View.GONE
            view.findViewById<Button>(R.id.evidence_toggle).setText(
                if (expanded) R.string.quest_source_hide else R.string.quest_source_toggle)
        }
        view.findViewById<TextView>(R.id.evidence_grade).text = block.grade.label
        view.findViewById<TextView>(R.id.evidence_practice).text = block.description
        view.findViewById<TextView>(R.id.evidence_times).text = getString(R.string.evidence_time_labels,
            block.suggestedTime, block.historicalTime ?: getString(R.string.evidence_not_recorded))
        val original = hero.routines.firstOrNull { it.id == block.id }
        view.findViewById<TextView>(R.id.evidence_fact).text = original?.let {
            getString(R.string.evidence_previous_note, it.historicalFact)
        }.orEmpty()
        view.findViewById<TextView>(R.id.evidence_source).text = getString(R.string.evidence_citation,
            block.source.title, block.source.locator, block.source.url)
        view.findViewById<Button>(R.id.evidence_open).setOnClickListener {
            val uri = Uri.parse(block.source.url)
            if (uri.scheme != "https" || uri.host.isNullOrBlank()) return@setOnClickListener
            try { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            catch (_: ActivityNotFoundException) { Toast.makeText(requireContext(), R.string.hero_browser_unavailable, Toast.LENGTH_LONG).show() }
        }
    }

    companion object {
        fun forBlock(heroId: String, blockId: String) = HeroEvidenceSheet().apply {
            arguments = Bundle().apply { putString("hero", heroId); putString("block", blockId) }
        }
    }
}
