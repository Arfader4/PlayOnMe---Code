package pl.onme.playonme.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickModesTest {
    @Test
    fun blitzClockAddsAndSubtracts() {
        assertEquals(12_000L, BlitzClock.deadlineAfter(10_000L, correct = true))
        assertEquals(5_000L, BlitzClock.deadlineAfter(10_000L, correct = false))
    }

    @Test
    fun endlessDeckCyclesWithoutRepeatAtTheSeam() {
        val items = listOf("a", "b", "c")
        repeat(50) { seed ->
            val drawn = QuickDecks.endless(items, Random(seed)).take(30).toList()
            assertEquals(30, drawn.size)
            drawn.chunked(3).forEach { assertEquals(items.toSet(), it.toSet()) }
            drawn.zipWithNext().forEach { (x, y) -> assertNotEquals(x, y) }
        }
    }

    @Test
    fun endlessDeckOfNothingIsEmpty() {
        assertTrue(QuickDecks.endless(emptyList<String>(), Random(1)).none())
    }

    @Test
    fun survivalGoesFromEasyToHard() {
        val items = listOf("h" to 3, "e1" to 1, "m" to 2, "e2" to 1)
        val ordered = QuickDecks.byDifficulty(items, Random(7)) { it.second }
        assertEquals(listOf(1, 1, 2, 3), ordered.map { it.second })
    }

    @Test
    fun dailyIsStablePerDayAndSubject() {
        val items = (1..20).map { "card-$it" }
        val today = QuickDecks.daily(items, "csharp", 20_000L, id = { it })
        assertEquals(QuickDecks.DAILY_SIZE, today.size)
        assertEquals(today.size, today.toSet().size)
        // Kolejność wejścia nie ma znaczenia, liczy się dzień i przedmiot.
        assertEquals(today, QuickDecks.daily(items.reversed(), "csharp", 20_000L, id = { it }))
        assertNotEquals(today, QuickDecks.daily(items, "csharp", 20_001L, id = { it }))
    }

    @Test
    fun dailyStreakCountsBackFromTodayOrYesterday() {
        assertEquals(0, DailyStreak.current(emptyList(), today = 100))
        assertEquals(3, DailyStreak.current(listOf(98, 99, 100), today = 100))
        // Dzisiejsze jeszcze nie zagrane: seria z wczoraj trwa.
        assertEquals(2, DailyStreak.current(listOf(98, 99), today = 100))
        // Przerwa przedwczoraj: seria się urwała.
        assertEquals(0, DailyStreak.current(listOf(97, 98), today = 100))
        assertEquals(1, DailyStreak.current(listOf(95, 96, 100), today = 100))
    }

    @Test
    fun modeKeysRoundTrip() {
        QuickMode.entries.forEach { assertEquals(it, QuickMode.fromKey(it.key)) }
    }
}
