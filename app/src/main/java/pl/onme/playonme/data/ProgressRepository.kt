package pl.onme.playonme.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.onme.playonme.data.db.CardProgressEntity
import pl.onme.playonme.data.db.ModeResultEntity
import pl.onme.playonme.data.db.PlayOnMeDatabase
import pl.onme.playonme.data.db.RoundResultEntity
import pl.onme.playonme.domain.CardAnswer
import pl.onme.playonme.domain.DailyStreak
import pl.onme.playonme.domain.PlayerLevel
import pl.onme.playonme.domain.PlayerLevels
import pl.onme.playonme.domain.QuickMode
import pl.onme.playonme.domain.RoundScore
import pl.onme.playonme.domain.Scoring

/** Co pokazać na ekranie podsumowania po zapisaniu rundy. */
data class RoundOutcome(
    val score: RoundScore,
    val levelBefore: PlayerLevel,
    val levelAfter: PlayerLevel,
) {
    val leveledUp: Boolean get() = levelAfter.level > levelBefore.level
}

/** Odpowiedź w szybkim trybie; karty mieszają się tam z kilku paczek tego samego przedmiotu. */
data class ModeAnswer(val packId: String, val answer: CardAnswer)

/** Co pokazać po grze w szybkim trybie. */
data class ModeOutcome(
    val mode: QuickMode,
    val score: RoundScore,
    /** Rekord przed tą grą; null = pierwsza gra w tym trybie. */
    val bestBefore: Int?,
    val levelBefore: PlayerLevel,
    val levelAfter: PlayerLevel,
    val dailyStreak: Int,
) {
    val newRecord: Boolean get() = score.correct > 0 && score.correct > (bestBefore ?: 0)
    val leveledUp: Boolean get() = levelAfter.level > levelBefore.level
}

class ProgressRepository(
    private val db: PlayOnMeDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val dao = db.progressDao()

    val playerLevel: Flow<PlayerLevel> = dao.totalXp().map(PlayerLevels::forXp)

    fun masteredCardIds(packId: String): Flow<Set<String>> = dao.masteredCardIds(packId).map { it.toSet() }

    /** Liczy XP, zapisuje rundę i postęp kart w jednej transakcji. */
    suspend fun recordRound(packId: String, level: Int, answers: List<CardAnswer>): RoundOutcome =
        db.withTransaction {
            val now = clock()
            val existing = dao.progressForPack(packId).associateBy { it.cardId }
            val mastered = existing.values.filter { it.firstCorrectAt != null }.map { it.cardId }.toSet()
            val score = Scoring.scoreRound(answers, mastered)
            val xpBefore = dao.totalXpNow()

            dao.upsertProgress(updatedProgress(packId, existing, answers, now))
            dao.insertRound(
                RoundResultEntity(
                    packId = packId,
                    level = level,
                    finishedAt = now,
                    correct = score.correct,
                    total = score.total,
                    xp = score.xp,
                )
            )
            RoundOutcome(
                score = score,
                levelBefore = PlayerLevels.forXp(xpBefore),
                levelAfter = PlayerLevels.forXp(xpBefore + score.xp),
            )
        }

    fun bestCorrect(mode: QuickMode, subject: String): Flow<Int?> = dao.bestCorrect(mode.key, subject)

    fun dailyResult(subject: String, day: Long): Flow<ModeResultEntity?> = dao.dailyResult(subject, day)

    suspend fun dailyResultNow(subject: String, day: Long): ModeResultEntity? = dao.dailyResultNow(subject, day)

    fun dailyStreak(today: Long): Flow<Int> = dao.dailyDays().map { DailyStreak.current(it, today) }

    /** Zapisuje grę w szybkim trybie: XP, rekord i postęp kart (karty mogą pochodzić z kilku paczek). */
    suspend fun recordModeRun(mode: QuickMode, subject: String, day: Long, answers: List<ModeAnswer>): ModeOutcome =
        db.withTransaction {
            val now = clock()
            val byPack = answers.groupBy { it.packId }
            val existing = byPack.keys.associateWith { packId -> dao.progressForPack(packId).associateBy { it.cardId } }
            // Id kart są unikalne tylko w paczce, więc do liczenia XP kluczem jest para paczka/karta.
            val mastered = existing.flatMap { (packId, cards) ->
                cards.values.filter { it.firstCorrectAt != null }.map { modeKey(packId, it.cardId) }
            }.toSet()
            val score = Scoring.scoreRun(
                answers.map { it.answer.copy(cardId = modeKey(it.packId, it.answer.cardId)) },
                mastered,
                perfectBonus = mode == QuickMode.DAILY,
            )
            val xpBefore = dao.totalXpNow()
            val bestBefore = dao.bestCorrectNow(mode.key, subject)

            for ((packId, packAnswers) in byPack) {
                dao.upsertProgress(updatedProgress(packId, existing.getValue(packId), packAnswers.map { it.answer }, now))
            }
            dao.insertModeResult(
                ModeResultEntity(
                    mode = mode.key,
                    subject = subject,
                    day = day,
                    finishedAt = now,
                    correct = score.correct,
                    total = score.total,
                    xp = score.xp,
                )
            )
            ModeOutcome(
                mode = mode,
                score = score,
                bestBefore = bestBefore,
                levelBefore = PlayerLevels.forXp(xpBefore),
                levelAfter = PlayerLevels.forXp(xpBefore + score.xp),
                dailyStreak = DailyStreak.current(dao.dailyDaysNow(), day),
            )
        }

    private fun updatedProgress(
        packId: String,
        existing: Map<String, CardProgressEntity>,
        answers: List<CardAnswer>,
        now: Long,
    ): List<CardProgressEntity> = answers.groupBy { it.cardId }.map { (cardId, cardAnswers) ->
        val old = existing[cardId]
        val anyCorrect = cardAnswers.any { it.correct }
        CardProgressEntity(
            packId = packId,
            cardId = cardId,
            timesSeen = (old?.timesSeen ?: 0) + cardAnswers.size,
            timesCorrect = (old?.timesCorrect ?: 0) + cardAnswers.count { it.correct },
            lastCorrect = cardAnswers.last().correct,
            lastAnsweredAt = now,
            firstCorrectAt = old?.firstCorrectAt ?: if (anyCorrect) now else null,
        )
    }

    private fun modeKey(packId: String, cardId: String) = "$packId/$cardId"
}
