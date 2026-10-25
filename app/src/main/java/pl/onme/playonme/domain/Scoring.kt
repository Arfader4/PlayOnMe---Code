package pl.onme.playonme.domain

/** Odpowiedź gracza na jedną kartę w rundzie. */
data class CardAnswer(
    val cardId: String,
    val difficulty: Int,
    val correct: Boolean,
)

/** Wynik rundy przed zapisem do bazy. */
data class RoundScore(
    val xp: Int,
    val correct: Int,
    val total: Int,
    val perfect: Boolean,
    /** XP za każdą kartę, w kolejności odpowiedzi. */
    val xpPerCard: List<Int>,
)

/**
 * Zasady XP. Celowo proste, bez combo i serii (te przyjdą później):
 * - dobra odpowiedź: 10 × trudność,
 * - karta już wcześniej zaliczona daje połowę, żeby nie opłacało się farmić łatwych poziomów,
 * - zła odpowiedź: 0,
 * - bezbłędna runda: +20.
 */
object Scoring {
    const val XP_PER_DIFFICULTY = 10
    const val PERFECT_ROUND_BONUS = 20

    fun scoreRound(answers: List<CardAnswer>, alreadyMastered: Set<String>): RoundScore {
        val perCard = answers.map { answer ->
            when {
                !answer.correct -> 0
                answer.cardId in alreadyMastered -> XP_PER_DIFFICULTY * answer.difficulty / 2
                else -> XP_PER_DIFFICULTY * answer.difficulty
            }
        }
        val correct = answers.count { it.correct }
        val perfect = answers.isNotEmpty() && correct == answers.size
        val bonus = if (perfect) PERFECT_ROUND_BONUS else 0
        return RoundScore(
            xp = perCard.sum() + bonus,
            correct = correct,
            total = answers.size,
            perfect = perfect,
            xpPerCard = perCard,
        )
    }

    /**
     * Wynik szybkiego trybu (Blitz, seria, wyzwanie dnia). Karta może wrócić w tej samej grze,
     * więc po pierwszym zaliczeniu liczy się już jako zaliczona (połowa XP).
     * Bonus za bezbłędność tylko tam, gdzie liczba kart jest stała (wyzwanie dnia).
     */
    fun scoreRun(answers: List<CardAnswer>, alreadyMastered: Set<String>, perfectBonus: Boolean): RoundScore {
        val mastered = alreadyMastered.toMutableSet()
        val perCard = answers.map { answer ->
            val xp = when {
                !answer.correct -> 0
                answer.cardId in mastered -> XP_PER_DIFFICULTY * answer.difficulty / 2
                else -> XP_PER_DIFFICULTY * answer.difficulty
            }
            if (answer.correct) mastered += answer.cardId
            xp
        }
        val correct = answers.count { it.correct }
        val perfect = answers.isNotEmpty() && correct == answers.size
        val bonus = if (perfect && perfectBonus) PERFECT_ROUND_BONUS else 0
        return RoundScore(
            xp = perCard.sum() + bonus,
            correct = correct,
            total = answers.size,
            perfect = perfect,
            xpPerCard = perCard,
        )
    }
}

/** Poziom gracza wyliczany z łącznego XP. */
data class PlayerLevel(
    val level: Int,
    val xpIntoLevel: Int,
    val xpForNextLevel: Int,
) {
    val progress: Float get() = xpIntoLevel.toFloat() / xpForNextLevel
}

object PlayerLevels {
    /** Próg wejścia na poziom n: 0, 100, 300, 600, 1000… (kolejne skoki co 100 większe). */
    fun thresholdFor(level: Int): Int = 100 * (level - 1) * level / 2

    fun forXp(totalXp: Int): PlayerLevel {
        var level = 1
        while (totalXp >= thresholdFor(level + 1)) level++
        val start = thresholdFor(level)
        return PlayerLevel(
            level = level,
            xpIntoLevel = totalXp - start,
            xpForNextLevel = thresholdFor(level + 1) - start,
        )
    }
}

/**
 * Odblokowanie poziomów paczki: poziom 1 jest zawsze otwarty,
 * kolejny otwiera się, gdy w poprzednim zaliczono co najmniej 60% kart.
 */
object LevelUnlocks {
    const val REQUIRED_SHARE = 0.6

    /**
     * @param cardsPerLevel id kart na każdym poziomie paczki
     * @param mastered id kart zaliczonych co najmniej raz
     * @return numery odblokowanych poziomów
     */
    fun unlocked(cardsPerLevel: Map<Int, List<String>>, mastered: Set<String>): Set<Int> {
        val result = mutableSetOf<Int>()
        for (level in cardsPerLevel.keys.sorted()) {
            if (result.isEmpty()) {
                result += level
                continue
            }
            val previous = cardsPerLevel.getValue(result.max())
            val share = previous.count { it in mastered }.toDouble() / previous.size
            if (share < REQUIRED_SHARE) break
            result += level
        }
        return result
    }
}
