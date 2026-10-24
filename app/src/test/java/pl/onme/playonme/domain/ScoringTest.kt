package pl.onme.playonme.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringTest {
    @Test
    fun correctAnswersScaleWithDifficulty() {
        val score = Scoring.scoreRound(
            listOf(CardAnswer("a", 1, true), CardAnswer("b", 3, true), CardAnswer("c", 2, false)),
            alreadyMastered = emptySet(),
        )
        assertEquals(listOf(10, 30, 0), score.xpPerCard)
        assertEquals(40, score.xp)
        assertEquals(2, score.correct)
        assertFalse(score.perfect)
    }

    @Test
    fun masteredCardsGiveHalfAndPerfectRoundGivesBonus() {
        val score = Scoring.scoreRound(
            listOf(CardAnswer("a", 2, true), CardAnswer("b", 1, true)),
            alreadyMastered = setOf("a"),
        )
        assertEquals(listOf(10, 10), score.xpPerCard)
        assertTrue(score.perfect)
        assertEquals(20 + Scoring.PERFECT_ROUND_BONUS, score.xp)
    }

    @Test
    fun emptyRoundIsNotPerfect() {
        assertEquals(0, Scoring.scoreRound(emptyList(), emptySet()).xp)
    }

    @Test
    fun playerLevelThresholds() {
        assertEquals(PlayerLevel(1, 0, 100), PlayerLevels.forXp(0))
        assertEquals(PlayerLevel(1, 99, 100), PlayerLevels.forXp(99))
        assertEquals(PlayerLevel(2, 0, 200), PlayerLevels.forXp(100))
        assertEquals(PlayerLevel(3, 50, 300), PlayerLevels.forXp(350))
    }

    @Test
    fun levelUnlocksNeedSixtyPercentOfPreviousLevel() {
        val levels = mapOf(
            1 to listOf("a1", "a2", "a3", "a4", "a5"),
            2 to listOf("b1", "b2", "b3", "b4", "b5"),
            3 to listOf("c1"),
        )
        assertEquals(setOf(1), LevelUnlocks.unlocked(levels, setOf("a1", "a2")))
        assertEquals(setOf(1, 2), LevelUnlocks.unlocked(levels, setOf("a1", "a2", "a3")))
        assertEquals(setOf(1, 2, 3), LevelUnlocks.unlocked(levels, setOf("a1", "a2", "a3", "b1", "b2", "b3")))
    }
}
