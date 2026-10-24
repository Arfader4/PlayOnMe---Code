package pl.onme.playonme.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Postęp jednej karty. Klucz to (packId, cardId), dlatego id kart w paczkach muszą być stabilne. */
@Entity(tableName = "card_progress", primaryKeys = ["packId", "cardId"])
data class CardProgressEntity(
    val packId: String,
    val cardId: String,
    val timesSeen: Int,
    val timesCorrect: Int,
    val lastCorrect: Boolean,
    val lastAnsweredAt: Long,
    /** Kiedy karta pierwszy raz została zaliczona; null = jeszcze nie. */
    val firstCorrectAt: Long?,
)

/** Jedna zakończona runda. Suma xp ze wszystkich rund to łączne XP gracza. */
@Entity(tableName = "round_result", indices = [Index("packId", "level")])
data class RoundResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packId: String,
    val level: Int,
    val finishedAt: Long,
    val correct: Int,
    val total: Int,
    val xp: Int,
)
