package pl.onme.playonme.ui.code

enum class TokenKind { PLAIN, KEYWORD, TYPE, FUNCTION, STRING, NUMBER, COMMENT }

data class Token(val kind: TokenKind, val text: String)

/**
 * Prosty tokenizer do kolorowania kodu w kartach. Nie jest parserem: ma tylko dobrze
 * wyglądać dla krótkich fragmentów. Sklejenie wszystkich tokenów daje dokładnie wejście.
 */
object Tokenizer {
    private val csharpKeywords = setOf(
        "abstract", "as", "async", "await", "base", "bool", "break", "case", "catch", "char", "class",
        "const", "continue", "decimal", "default", "do", "double", "else", "enum", "false", "finally",
        "float", "for", "foreach", "get", "if", "in", "init", "int", "interface", "internal", "is",
        "lock", "long", "namespace", "new", "null", "object", "out", "override", "private", "protected",
        "public", "readonly", "record", "ref", "return", "sealed", "set", "static", "string", "struct",
        "switch", "this", "throw", "true", "try", "typeof", "using", "var", "virtual", "void", "while",
        "yield",
    )

    fun tokenize(code: String, language: String?): List<Token> {
        val keywords = when (language) {
            "csharp" -> csharpKeywords
            else -> emptySet()
        }
        val tokens = mutableListOf<Token>()
        var i = 0

        fun add(kind: TokenKind, end: Int) {
            val text = code.substring(i, end)
            val last = tokens.lastOrNull()
            if (last != null && last.kind == kind && kind == TokenKind.PLAIN) {
                tokens[tokens.lastIndex] = Token(kind, last.text + text)
            } else {
                tokens += Token(kind, text)
            }
            i = end
        }

        while (i < code.length) {
            val c = code[i]
            when {
                code.startsWith("//", i) -> add(TokenKind.COMMENT, code.indexOf('\n', i).let { if (it < 0) code.length else it })
                code.startsWith("/*", i) -> add(TokenKind.COMMENT, code.indexOf("*/", i + 2).let { if (it < 0) code.length else it + 2 })
                c == '"' || (c == '$' || c == '@') && code.getOrNull(i + 1) == '"' -> add(TokenKind.STRING, stringEnd(code, i))
                c == '\'' -> add(TokenKind.STRING, charEnd(code, i))
                c.isDigit() -> add(TokenKind.NUMBER, scanWhile(code, i) { it.isLetterOrDigit() || it == '.' || it == '_' })
                c.isLetter() || c == '_' -> {
                    val end = scanWhile(code, i) { it.isLetterOrDigit() || it == '_' }
                    val word = code.substring(i, end)
                    val next = code.getOrNull(scanWhile(code, end) { it == ' ' })
                    val kind = when {
                        word in keywords -> TokenKind.KEYWORD
                        next == '(' -> TokenKind.FUNCTION
                        word[0].isUpperCase() -> TokenKind.TYPE
                        else -> TokenKind.PLAIN
                    }
                    add(kind, end)
                }
                else -> add(TokenKind.PLAIN, i + 1)
            }
        }
        return tokens
    }

    private inline fun scanWhile(code: String, start: Int, predicate: (Char) -> Boolean): Int {
        var j = start
        while (j < code.length && predicate(code[j])) j++
        return j
    }

    /** Koniec literału "…", $"…" lub @"…" (w @"…" cudzysłów ucieka się przez ""). */
    private fun stringEnd(code: String, start: Int): Int {
        val verbatim = code[start] == '@'
        var j = code.indexOf('"', start) + 1
        while (j < code.length) {
            when {
                verbatim && code.startsWith("\"\"", j) -> j += 2
                !verbatim && code[j] == '\\' -> j += 2
                code[j] == '"' -> return j + 1
                code[j] == '\n' && !verbatim -> return j
                else -> j++
            }
        }
        return code.length
    }

    private fun charEnd(code: String, start: Int): Int {
        var j = start + 1
        while (j < code.length) {
            when (code[j]) {
                '\\' -> j += 2
                '\'' -> return j + 1
                '\n' -> return j
                else -> j++
            }
        }
        return code.length
    }
}
