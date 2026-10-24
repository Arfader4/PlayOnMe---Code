package pl.onme.playonme

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.onme.playonme.data.pack.Answer
import pl.onme.playonme.data.pack.CardType
import pl.onme.playonme.data.pack.PackParser

class PackParserTest {
    private val packsDir = File("src/main/assets/packs")

    @Test
    fun allBundledPacksParseAndAreConsistent() {
        val files = packsDir.listFiles { f -> f.extension == "json" }.orEmpty()
        assertTrue("Brak paczek w ${packsDir.absolutePath}", files.isNotEmpty())
        for (file in files) {
            val pack = PackParser.parse(file.readText())
            assertEquals("id paczki musi pasować do nazwy pliku", file.nameWithoutExtension, pack.id)
            assertEquals("zduplikowane id kart w ${pack.id}", pack.cards.size, pack.cards.map { it.id }.toSet().size)
            val levels = pack.levels.map { it.number }.toSet()
            for (card in pack.cards) {
                if (levels.isNotEmpty()) {
                    assertTrue("${card.id}: poziom ${card.level} nie jest opisany w levels", card.level in levels)
                }
                val answer = card.answer
                if (answer is Answer.Choice) {
                    assertTrue("${card.id}: correct poza zakresem", answer.correct in answer.options.indices)
                }
            }
        }
    }

    @Test
    fun csharpBasicsHasFullMvpContent() {
        val pack = PackParser.parse(File(packsDir, "csharp-basics.json").readText())
        assertEquals(20, pack.cards.size)
        assertTrue(pack.cards.all { it.type == CardType.PREDICT_OUTPUT })
        assertEquals(setOf(1, 2, 3, 4), pack.cards.map { it.level }.toSet())
    }

    @Test
    fun parsesSampleCard() {
        val pack = PackParser.parse(File(packsDir, "csharp-basics.json").readText())
        val card = pack.cards.first { it.id == "cs-str-001" }
        assertEquals(CardType.PREDICT_OUTPUT, card.type)
        assertEquals(Answer.Choice(listOf("kot", "KOT", "pot", "POT"), 0), card.answer)
    }
}
