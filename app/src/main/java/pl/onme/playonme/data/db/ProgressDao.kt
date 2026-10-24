package pl.onme.playonme.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT COALESCE(SUM(xp), 0) FROM round_result")
    fun totalXp(): Flow<Int>

    @Query("SELECT COALESCE(SUM(xp), 0) FROM round_result")
    suspend fun totalXpNow(): Int

    @Query("SELECT * FROM round_result WHERE packId = :packId AND level = :level ORDER BY finishedAt DESC LIMIT 1")
    suspend fun lastRound(packId: String, level: Int): RoundResultEntity?
}
