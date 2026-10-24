package pl.onme.playonme.data.pack

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * Paczka zadań. Format jest wspólny ze StudyOnMe; opis w docs/pack-format.md,
 * walidacja w docs/schema/pack.schema.json.
 */
@Serializable
data class Pack(
    val schemaVersion: Int,
    val id: String,
    val title: String,
    val subject: String,
    val locale: String = "pl",
    val version: Int = 1,
    val description: String? = null,
    val levels: List<Level> = emptyList(),
    val cards: List<Card>,
)

@Serializable
data class Level(
    val number: Int,
    val title: String,
)

@Serializable
enum class CardType {
    @SerialName("predict_output") PREDICT_OUTPUT,
    @SerialName("find_bug") FIND_BUG,
    @SerialName("quiz") QUIZ,
    @SerialName("flashcard") FLASHCARD,
}

@Serializable
data class Card(
    val id: String,
    val type: CardType,
    val level: Int = 1,
    val difficulty: Int = 1,
    val tags: List<String> = emptyList(),
    val prompt: String,
    val code: String? = null,
    val codeLanguage: String? = null,
    val answer: Answer,
    val explanation: String,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("kind")
sealed interface Answer {
    /** Wybór jednej z opcji (MVP: „przewidź wynik”, quiz). */
    @Serializable
    @SerialName("choice")
    data class Choice(val options: List<String>, val correct: Int) : Answer

    /** Odpowiedź wpisywana; porównywana z listą akceptowanych wariantów. */
    @Serializable
    @SerialName("text")
    data class Text(val accepted: List<String>) : Answer

    /** Wskazanie linii z błędem (numeracja od 1) i opcjonalna poprawka. Na później. */
    @Serializable
    @SerialName("line")
    data class Line(val line: Int, val fix: String? = null) : Answer
}

object PackParser {
    // Nieznane pola ignorujemy, żeby StudyOnMe mogło dodawać swoje bez psucia gry.
    private val json = Json { ignoreUnknownKeys = true }

    const val SUPPORTED_SCHEMA_VERSION = 1

    fun parse(text: String): Pack {
        val pack = json.decodeFromString<Pack>(text)
        require(pack.schemaVersion <= SUPPORTED_SCHEMA_VERSION) {
            "Paczka ${pack.id} ma schemaVersion ${pack.schemaVersion}, obsługiwana jest $SUPPORTED_SCHEMA_VERSION"
        }
        return pack
    }
}
