package com.example.test

import android.content.Intent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.test.Hero.HeroCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Device tests inspect rendered facts separately from proposals; they do not mutate user records. */
@RunWith(AndroidJUnit4::class)
class HeroDetailActivityTest {
    @Test
    fun detailShowsChosenPersonsFactsPracticeAndCitationSeparately() {
        val hero = HeroCatalog.all.last()
        val intent = Intent(ApplicationProvider.getApplicationContext(), HeroDetailActivity::class.java)
            .putExtra(HeroDetailActivity.EXTRA_HERO_ID, hero.id)
        ActivityScenario.launch<HeroDetailActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(hero.name, activity.findViewById<TextView>(R.id.heroDetailName).text.toString())
                assertEquals(com.example.test.Hero.HeroDayCatalog.NOTICE, activity.findViewById<TextView>(R.id.heroDetailBio).text.toString())
                val cards = activity.findViewById<LinearLayout>(R.id.historicalRoutineContainer)
                assertEquals(com.example.test.Hero.HeroDayCatalog.forHero(hero).blocks.size, cards.childCount)
                assertTrue(cards.childCount > 0)
                val firstCard = cards.getChildAt(0)
                val firstBlock = com.example.test.Hero.HeroDayCatalog.forHero(hero).blocks.minBy { it.suggestedTime }
                assertEquals(firstBlock.title, firstCard.findViewById<TextView>(R.id.day_preview_title).text.toString())
                assertTrue(firstCard.findViewById<TextView>(R.id.day_preview_subtitle).text.contains(firstBlock.suggestedTime))
                assertTrue(firstCard.findViewById<TextView>(R.id.day_grade).text == firstBlock.grade.label)
                firstCard.findViewById<View>(R.id.day_preview_details).performClick()
                                assertEquals(hero.portraitCredit, activity.findViewById<TextView>(R.id.heroPortraitCredit).text.toString())
            }
        }
    }

    @Test
    fun recreationKeepsTheSelectedPersonRatherThanDefaultingToAnother() {
        val hero = HeroCatalog.all.last()
        val intent = Intent(ApplicationProvider.getApplicationContext(), HeroDetailActivity::class.java)
            .putExtra(HeroDetailActivity.EXTRA_HERO_ID, hero.id)
        ActivityScenario.launch<HeroDetailActivity>(intent).use { scenario ->
            scenario.recreate()
            scenario.onActivity {
                assertEquals(hero.name, it.findViewById<TextView>(R.id.heroDetailName).text.toString())
                assertEquals(com.example.test.Hero.HeroDayCatalog.forHero(hero).blocks.size, it.findViewById<LinearLayout>(R.id.historicalRoutineContainer).childCount)
            }
        }
    }

    @Test
    fun invalidHeroIdShowsAnErrorInsteadOfAnotherPersonsContent() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), HeroDetailActivity::class.java)
            .putExtra(HeroDetailActivity.EXTRA_HERO_ID, "missing-person")
        ActivityScenario.launch<HeroDetailActivity>(intent).use { scenario ->
            scenario.onActivity {
                assertEquals(View.VISIBLE, it.findViewById<View>(R.id.heroDetailError).visibility)
                assertEquals(View.GONE, it.findViewById<View>(R.id.heroDetailContent).visibility)
            }
        }
    }

    @Test
    fun emptySearchStateAndQuerySurviveRecreation() {
        val query = "no-catalog-match-391823"
        ActivityScenario.launch(CharacterSelectActivity::class.java).use { scenario ->
            scenario.onActivity { it.findViewById<EditText>(R.id.heroSearch).setText(query) }
            scenario.recreate()
            scenario.onActivity {
                assertEquals(query, it.findViewById<EditText>(R.id.heroSearch).text.toString())
                assertEquals(View.VISIBLE, it.findViewById<View>(R.id.heroEmptyState).visibility)
                assertEquals(View.GONE, it.findViewById<View>(R.id.heroRecyclerView).visibility)
            }
        }
    }
}
