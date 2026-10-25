package pl.onme.playonme.domain

import kotlin.random.Random

/** Szybkie tryby gry na istniejących kartach. `key` trafia do bazy, więc nie zmieniaj go. */
enum class QuickMode(val key: String) {
    /** 60 s na start, dobra odpowiedź dodaje czas, zła zabiera. */
    BLITZ("blitz"),

    /** Karty bez końca, coraz trudniejsze; pierwszy błąd kończy grę. */
    SURVIVAL("survival"),

    /** 5 kart wspólnych dla danego dnia, jedna próba dziennie. */
    DAILY("daily");

    companion object {
        fun fromKey(key: String): QuickMode? = entries.firstOrNull { it.key == key }
    }
}

/** Zegar Blitza liczony w milisekundach. */
object BlitzClock {
    const val START_MS = 60_000L
    const val CORRECT_BONUS_MS = 2_000L
    const val WRONG_PENALTY_MS = 5_000L

    /** Nowy termin końca gry po odpowiedzi. */
    fun deadlineAfter(deadline: Long, correct: Boolean): Long =
        if (correct) deadline + CORRECT_BONUS_MS else deadline - WRONG_PENALTY_MS
}

object QuickDecks {
    const val DAILY_SIZE = 5

    /**
     * Talia Blitza: karty w losowej kolejności, po wyczerpaniu tasowane od nowa.
     * Ta sama karta nie trafia dwa razy z rzędu na styku talii.
     */
    fun <T> endless(items: List<T>, random: Random, ordered: (List<T>, Random) -> List<T> = { l, r -> l.shuffled(r) }): Sequence<T> =
        if (items.isEmpty()) emptySequence() else sequence {
            var last: T? = null
            while (true) {
                var round = ordered(items, random)
                if (items.size > 1 && round.first() == last) round = round.drop(1) + round.first()
                yieldAll(round)
                last = round.last()
            }
        }

    /** Kolejność serii: od najłatwiejszych do najtrudniejszych, w obrębie trudności losowo. */
    fun <T> byDifficulty(items: List<T>, random: Random, difficulty: (T) -> Int): List<T> =
        items.groupBy(difficulty).toSortedMap().values.flatMap { it.shuffled(random) }

    /**
     * Karty wyzwania dnia. Zależą tylko od dnia, przedmiotu i treści paczek,
     * więc każdy gracz z tymi samymi paczkami dostaje ten sam zestaw.
     */
    fun <T> daily(items: List<T>, subject: String, epochDay: Long, id: (T) -> String, count: Int = DAILY_SIZE): List<T> {
        val seed = epochDay * 31 + subject.hashCode()
        return items.sortedBy(id).shuffled(Random(seed)).take(count)
    }
}

object DailyStreak {
    /**
     * Ile dni z rzędu zagrano wyzwanie dnia. Seria trwa, jeśli ostatnie wyzwanie było dziś albo wczoraj
     * (dzisiejsze może jeszcze być przed graczem).
     *
     * @param playedDays dni (epochDay), w których zagrano wyzwanie, w dowolnej kolejności
     */
    fun current(playedDays: Collection<Long>, today: Long): Int {
        val days = playedDays.toSet()
        var day = if (today in days) today else today - 1
        var streak = 0
        while (day in days) {
            streak++
            day--
        }
        return streak
    }
}
