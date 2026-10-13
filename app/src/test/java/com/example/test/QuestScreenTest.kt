package com.example.test

import android.content.Intent
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.test.Hero.HeroCatalog
import com.example.test.Hero.HeroDayCatalog
import com.example.test.data.AppRepository
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-mdpi")
class QuestScreenTest {
    private fun settle(activity: AppCompatActivity) {
        repeat(3) {
            shadowOf(Looper.getMainLooper()).idle()
            var done = false
            AppIo.run(activity, { true }) { done = true }
            val deadline = System.nanoTime() + 5_000_000_000L
            while (!done && System.nanoTime() < deadline) {
                shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(10)
            }
            assertTrue(done)
        }
        activity.window.decorView.apply {
            measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY))
            layout(0, 0, 360, 800)
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test fun goalDropdownFindsTheHeroInsteadOfShowingAnonymousPlans() {
        val controller = Robolectric.buildActivity(CharacterSelectActivity::class.java).setup().visible()
        try {
            val activity = controller.get()
            settle(activity)
            val goal = activity.findViewById<MaterialAutoCompleteTextView>(R.id.hero_goal)
            val index = com.example.test.Hero.HeroQuestCatalog.goals.indexOf("개발·문제 해결")
            goal.showDropDown()
            // Material uses a separate modal popup when touch exploration is enabled.
            val modal = org.robolectric.util.ReflectionHelpers.getField<androidx.appcompat.widget.ListPopupWindow>(goal, "modalListPopup")
            val standard = org.robolectric.util.ReflectionHelpers.getField<android.widget.ListPopupWindow>(goal, "mPopup")
            val choices = modal.listView ?: standard.listView!!
            assertTrue(choices.performItemClick(TextView(activity), index, index.toLong()))
            settle(activity)
            val adapter = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.heroRecyclerView).adapter
                as com.example.test.Hero.HeroAdapter
            assertEquals(listOf("ada_lovelace"), adapter.currentList.map { it.id })
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun actualTimelineActionOpensChecklistAndClickPersistsWithoutReward() {
        val context = RuntimeEnvironment.getApplication()
        val hero = HeroCatalog.default
        val block = HeroDayCatalog.forHero(hero).blocks.first()
        val id = AppRepository(context).use { it.startHeroDay(hero).first() }
        val controller = Robolectric.buildActivity(MainActivity::class.java,
            Intent(context, MainActivity::class.java).putExtra(RoutineActivity.HERO_ID, hero.id)).setup().visible()
        try {
            val activity = controller.get()
            settle(activity)
            val list = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_routine_list)
            list.scrollToPosition(1)
            settle(activity)
            val row = list.findViewHolderForAdapterPosition(1)!!.itemView
            assertTrue(row.findViewById<View>(R.id.timeline_detail).performClick())
            settle(activity)
            activity.supportFragmentManager.executePendingTransactions()
            val sheet = activity.supportFragmentManager.findFragmentByTag("personal_detail")!!
            val container = sheet.requireView().findViewById<android.widget.LinearLayout>(R.id.quest_detail_container)
            assertEquals(block.quests.size, container.childCount)
            val check = container.getChildAt(0).findViewById<MaterialCheckBox>(R.id.quest_check)
            assertTrue(check.isEnabled)
            assertFalse(check.isChecked)
            // CompoundButton toggles even when performClick returns false (no OnClickListener).
            check.performClick()
            settle(activity)
            assertTrue(check.isChecked)
            assertEquals(activity.getString(R.string.quest_progress, 1, block.quests.size),
                sheet.requireView().findViewById<TextView>(R.id.quest_detail_progress).text.toString())
            AppRepository(context).use {
                assertEquals(setOf(block.quests.first().id), it.questChecks(id, com.example.test.Routine.RoutineDate.today()))
                assertEquals(0L, it.balance())
                assertTrue(it.history().isEmpty())
            }
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun previewShowsConcreteStepsAndSourceDisclosureAtExpandedFont() {
        RuntimeEnvironment.setFontScale(2f)
        val intent = Intent(RuntimeEnvironment.getApplication(), HeroDetailActivity::class.java)
            .putExtra(HeroDetailActivity.EXTRA_HERO_ID, "ada_lovelace")
        val controller = Robolectric.buildActivity(HeroDetailActivity::class.java, intent).setup().visible()
        try {
            val activity = controller.get()
            settle(activity)
            val work = HeroDayCatalog.forHero(HeroCatalog.getOrDefault("ada_lovelace")).blocks.single { it.kind == com.example.test.Hero.DayKind.WORK }
            HeroEvidenceSheet.forBlock("ada_lovelace", work.id).showNow(activity.supportFragmentManager, "quest_preview")
            val root = activity.supportFragmentManager.findFragmentByTag("quest_preview")!!.requireView()
            val quests = root.findViewById<android.widget.LinearLayout>(R.id.evidence_quests)
            assertEquals(3, quests.childCount)
            assertTrue(quests.getChildAt(1).findViewById<TextView>(R.id.quest_instruction).text.contains("입력·처리·결과"))
            assertEquals(View.GONE, root.findViewById<View>(R.id.evidence_citation_group).visibility)
            assertTrue(root.findViewById<View>(R.id.evidence_toggle).performClick())
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.evidence_citation_group).visibility)
            assertEquals(View.GONE, quests.getChildAt(0).findViewById<View>(R.id.quest_check).visibility)
        } finally { controller.pause().stop().destroy() }
    }
}
