package pl.onme.playonme.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.onme.playonme.data.db.CardProgressEntity
import pl.onme.playonme.data.db.PlayOnMeDatabase
import pl.onme.playonme.data.db.RoundResultEntity
import pl.onme.playonme.domain.CardAnswer
import pl.onme.playonme.domain.PlayerLevel
import pl.onme.playonme.domain.PlayerLevels
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

            val updated = answers.groupBy { it.cardId }.map { (cardId, cardAnswers) ->
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
            dao.upsertProgress(updated)
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
}
