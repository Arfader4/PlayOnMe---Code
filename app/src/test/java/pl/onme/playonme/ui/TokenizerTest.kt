package pl.onme.playonme.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test
import pl.onme.playonme.data.pack.PackParser
import pl.onme.playonme.ui.code.Token
import pl.onme.playonme.ui.code.TokenKind.COMMENT
import pl.onme.playonme.ui.code.TokenKind.FUNCTION
import pl.onme.playonme.ui.code.TokenKind.KEYWORD
import pl.onme.playonme.ui.code.TokenKind.NUMBER
import pl.onme.playonme.ui.code.TokenKind.PLAIN
import pl.onme.playonme.ui.code.TokenKind.STRING
import pl.onme.playonme.ui.code.TokenKind.TYPE
import pl.onme.playonme.ui.code.Tokenizer

class TokenizerTest {
    @Test
    fun recognisesBasicCSharpTokens() {
        val tokens = Tokenizer.tokenize("var p = new Point { X = 1 }; // komentarz", "csharp")
        assertEquals(
            listOf(
                Token(KEYWORD, "var"), Token(PLAIN, " p = "), Token(KEYWORD, "new"), Token(PLAIN, " "),
                Token(TYPE, "Point"), Token(PLAIN, " { "), Token(TYPE, "X"), Token(PLAIN, " = "),
                Token(NUMBER, "1"), Token(PLAIN, " }; "), Token(COMMENT, "// komentarz"),
            ),
            tokens,
        )
    }

    @Test
    fun stringsAndCalls() {
        val tokens = Tokenizer.tokenize("Console.WriteLine(\$\"{a} \\\"x\\\"\");", "csharp")
        assertEquals(
            listOf(
                Token(TYPE, "Console"), Token(PLAIN, "."), Token(FUNCTION, "WriteLine"), Token(PLAIN, "("),
                Token(STRING, "\$\"{a} \\\"x\\\"\""), Token(PLAIN, ");"),
            ),
            tokens,
        )
    }

    @Test
    fun charLiteralAndVerbatimString() {
        val tokens = Tokenizer.tokenize("s.Replace('k', 'p'); @\"a\"\"b\"", "csharp")
        assertEquals(Token(STRING, "'k'"), tokens[3])
        assertEquals(Token(STRING, "@\"a\"\"b\""), tokens.last())
    }

    @Test
    fun tokensAlwaysReassembleToTheInput() {
        val pack = PackParser.parse(File("src/main/assets/packs/csharp-basics.json").readText())
        for (card in pack.cards) {
            val code = card.code ?: continue
            assertEquals(card.id, code, Tokenizer.tokenize(code, card.codeLanguage).joinToString("") { it.text })
        }
    }
}
