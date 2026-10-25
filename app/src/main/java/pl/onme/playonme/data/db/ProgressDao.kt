package pl.onme.playonme.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// XP gracza to suma ze zwykłych rund i z szybkich trybów.
private const val TOTAL_XP =
    "SELECT (SELECT COALESCE(SUM(xp), 0) FROM round_result) + (SELECT COALESCE(SUM(xp), 0) FROM mode_result)"

@Dao
interface ProgressDao {
    @Query("SELECT * FROM card_progress WHERE packId = :packId")
    suspend fun progressForPack(packId: String): List<CardProgressEntity>

    @Query("SELECT cardId FROM card_progress WHERE packId = :packId AND firstCorrectAt IS NOT NULL")
    fun masteredCardIds(packId: String): Flow<List<String>>

    @Upsert
    suspend fun upsertProgress(progress: List<CardProgressEntity>)

    @Insert
    suspend fun insertRound(round: RoundResultEntity): Long

    @Query(TOTAL_XP)
    fun totalXp(): Flow<Int>

    @Query(TOTAL_XP)
    suspend fun totalXpNow(): Int

    @Query("SELECT * FROM round_result WHERE packId = :packId AND level = :level ORDER BY finishedAt DESC LIMIT 1")
    suspend fun lastRound(packId: String, level: Int): RoundResultEntity?

    @Insert
    suspend fun insertModeResult(result: ModeResultEntity): Long

    /** Rekord trybu: najwięcej poprawnych odpowiedzi w jednej grze; null = jeszcze nie grano. */
    @Query("SELECT MAX(correct) FROM mode_result WHERE mode = :mode AND subject = :subject")
    fun bestCorrect(mode: String, subject: String): Flow<Int?>

    @Query("SELECT MAX(correct) FROM mode_result WHERE mode = :mode AND subject = :subject")
    suspend fun bestCorrectNow(mode: String, subject: String): Int?

    @Query("SELECT * FROM mode_result WHERE mode = 'daily' AND subject = :subject AND day = :day LIMIT 1")
    fun dailyResult(subject: String, day: Long): Flow<ModeResultEntity?>

    @Query("SELECT * FROM mode_result WHERE mode = 'daily' AND subject = :subject AND day = :day LIMIT 1")
    suspend fun dailyResultNow(subject: String, day: Long): ModeResultEntity?

    /** Dni, w których zagrano wyzwanie dnia w dowolnym przedmiocie. */
    @Query("SELECT DISTINCT day FROM mode_result WHERE mode = 'daily'")
    fun dailyDays(): Flow<List<Long>>

    @Query("SELECT DISTINCT day FROM mode_result WHERE mode = 'daily'")
    suspend fun dailyDaysNow(): List<Long>
}
