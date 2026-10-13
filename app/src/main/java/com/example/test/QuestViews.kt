package com.example.test

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.test.Hero.DayQuest
import com.google.android.material.checkbox.MaterialCheckBox

/** Two lines per task; complete instructions remain readable at expanded font sizes. */
object QuestViews {
    fun bind(container: LinearLayout, quests: List<DayQuest>, checked: Set<String> = emptySet(),
             editable: Boolean = false,
             onToggle: ((DayQuest, Boolean, (Boolean) -> Unit) -> Unit)? = null) {
        container.removeAllViews()
        quests.forEach { quest ->
            val row = android.view.LayoutInflater.from(container.context).inflate(R.layout.item_day_quest, container, false)
            val check = row.findViewById<MaterialCheckBox>(R.id.quest_check)
            val instruction = row.findViewById<TextView>(R.id.quest_instruction)
            row.findViewById<TextView>(R.id.quest_measure).text =
                container.context.getString(R.string.quest_row_meta, "제안 ${quest.measure}", quest.grade.label)
            check.text = quest.instruction
            check.contentDescription = container.context.getString(R.string.quest_check_label,
                quest.instruction, quest.measure, quest.grade.label)
            check.tag = quest.id
            if (onToggle == null) {
                check.visibility = View.GONE
                instruction.text = quest.instruction
            } else {
                instruction.visibility = View.GONE
                check.isChecked = quest.id in checked
                check.isEnabled = editable
                var binding = false
                var saved = check.isChecked
                check.setOnCheckedChangeListener { _, requested ->
                    if (!binding) {
                        check.isEnabled = false
                        onToggle(quest, requested) { success ->
                            if (success) saved = requested
                            binding = true
                            check.isChecked = saved
                            binding = false
                            check.isEnabled = editable
                        }
                    }
                }
            }
            container.addView(row)
        }
    }
}
