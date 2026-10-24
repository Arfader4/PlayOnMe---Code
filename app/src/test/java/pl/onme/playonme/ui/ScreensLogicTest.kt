package pl.onme.playonme.ui

import java.io.File
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.onme.playonme.data.pack.Answer
import pl.onme.playonme.data.pack.PackParser
import pl.onme.playonme.ui.screens.LevelsViewModel
import pl.onme.playonme.ui.screens.Question

class ScreensLogicTest {
    private val pack = PackParser.parse(File("src/main/assets/packs/csharp-basics.json").readText())

    @Test
    fun shuffledOptionsKeepTheCorrectAnswer() {
        for (seed in 0 until 50) {
            for (card in pack.cards) {
                val question = Question.from(card, Random(seed))!!
                val choice = card.answer as Answer.Choice
                assertEquals(choice.options.toSet(), question.options.toSet())
                assertEquals(choice.options[choice.correct], question.options[question.correctIndex])
            }
        }
    }

    @Test
    fun newPlayerSeesOnlyFirstLevelUnlocked() {
        val section = LevelsViewModel.buildSection(pack, mastered = emptySet())
        assertEquals(listOf(1, 2, 3, 4), section.levels.map { it.number })
        assertTrue(section.levels[0].unlocked)
        assertFalse(section.levels[1].unlocked)
        assertEquals("Wartości i referencje", section.levels[0].title)
    }

    @Test
    fun masteringThreeCardsUnlocksNextLevel() {
        val firstLevel = pack.cards.filter { it.level == 1 }.map { it.id }
        val section = LevelsViewModel.buildSection(pack, mastered = firstLevel.take(3).toSet())
        assertEquals(3, section.levels[0].mastered)
        assertTrue(section.levels[1].unlocked)
        assertFalse(section.levels[2].unlocked)
    }
}
