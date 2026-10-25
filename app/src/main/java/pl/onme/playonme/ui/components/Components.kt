package pl.onme.playonme.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import pl.onme.playonme.domain.PlayerLevel
import pl.onme.playonme.ui.code.TokenKind
import pl.onme.playonme.ui.code.Tokenizer
import pl.onme.playonme.ui.theme.CodeTextStyle
import pl.onme.playonme.ui.theme.SyntaxColors

/** Nagłówek ekranu z opcjonalnym przyciskiem powrotu. */
@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (onBack != null) {
            TextButton(onClick = onBack) { Text("← Wstecz") }
            Spacer(Modifier.width(4.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}

/** Poziom gracza i pasek XP do następnego poziomu. */
@Composable
fun PlayerLevelBar(level: PlayerLevel, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(level.progress, animationSpec = tween(durationMillis = 900), label = "levelProgress")
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Poziom ${level.level}", style = MaterialTheme.typography.titleMedium)
            Text(
                "${level.xpIntoLevel} / ${level.xpForNextLevel} XP",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

/** Blok kodu z numerami linii i kolorowaniem; długie linie przewijają się w poziomie zamiast zawijać. */
@Composable
fun CodeBlock(code: String, language: String?, modifier: Modifier = Modifier) {
    val highlighted = remember(code, language) { highlight(code, language) }
    val lines = code.lines()
    val numberWidth = lines.size.toString().length
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Text(
                text = lines.indices.joinToString("\n") { (it + 1).toString().padStart(numberWidth) },
                style = CodeTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = highlighted,
                style = CodeTextStyle,
                softWrap = false,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            )
        }
    }
}

private fun highlight(code: String, language: String?): AnnotatedString = buildAnnotatedString {
    for (token in Tokenizer.tokenize(code, language)) {
        val color = when (token.kind) {
            TokenKind.KEYWORD -> SyntaxColors.keyword
            TokenKind.TYPE -> SyntaxColors.type
            TokenKind.FUNCTION -> SyntaxColors.function
            TokenKind.STRING -> SyntaxColors.string
            TokenKind.NUMBER -> SyntaxColors.number
            TokenKind.COMMENT -> SyntaxColors.comment
            TokenKind.PLAIN -> null
        }
        if (color == null) append(token.text) else withStyle(SpanStyle(color = color)) { append(token.text) }
    }
}

@Composable
fun LoadingBox() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun MessageBox(text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
