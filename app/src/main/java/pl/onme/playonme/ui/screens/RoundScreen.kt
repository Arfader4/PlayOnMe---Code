package pl.onme.playonme.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.onme.playonme.data.RoundOutcome
import pl.onme.playonme.ui.appViewModel
import pl.onme.playonme.ui.components.CodeBlock
import pl.onme.playonme.ui.components.LoadingBox
import pl.onme.playonme.ui.components.MessageBox
import pl.onme.playonme.ui.components.PlayerLevelBar
import pl.onme.playonme.ui.components.ScreenHeader
import pl.onme.playonme.ui.theme.CodeTextStyle

@Composable
fun RoundScreen(packId: String, level: Int, onExit: () -> Unit) {
    val vm = appViewModel(key = "round-$packId-$level") { RoundViewModel(it.packs, it.progress, packId, level) }
    val state by vm.state.collectAsState()

    when (val s = state) {
        RoundState.Loading, RoundState.Saving -> ScreenColumn { LoadingBox() }
        RoundState.Empty -> ScreenColumn {
            ScreenHeader("Runda", onBack = onExit)
            MessageBox("Na tym poziomie nie ma jeszcze zadań.")
        }
        is RoundState.Playing -> PlayingContent(s, onSelect = vm::select, onNext = vm::next, onExit = onExit)
        is RoundState.Finished -> SummaryContent(s.outcome, onRepeat = vm::start, onDone = onExit)
    }
}

@Composable
private fun PlayingContent(
    state: RoundState.Playing,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
) = ScreenColumn {
    val question = state.current
    ScreenHeader(state.title, onBack = onExit)
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text("Zadanie ${state.index + 1} z ${state.questions.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.combo >= 2) {
            Text("Combo ×${state.combo}", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
        }
    }
    LinearProgressIndicator(
        progress = { (state.index + if (state.selected != null) 1 else 0).toFloat() / state.questions.size },
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(question.card.prompt, style = MaterialTheme.typography.titleMedium)
    question.card.code?.let { CodeBlock(it) }

    question.options.forEachIndexed { i, option ->
        OptionTile(
            text = option,
            status = optionStatus(i, state.selected, question.correctIndex),
            enabled = state.selected == null,
            onClick = { onSelect(i) },
        )
    }

    if (state.selected != null) {
        val correct = state.selected == question.correctIndex
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (correct) "Dobrze!" else "Nie tym razem",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (correct) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                )
                Text(question.card.explanation, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.isLast) "Zakończ rundę" else "Dalej")
        }
    }
}

private enum class OptionStatus { NEUTRAL, CORRECT, WRONG, DIMMED }

private fun optionStatus(index: Int, selected: Int?, correctIndex: Int): OptionStatus = when {
    selected == null -> OptionStatus.NEUTRAL
    index == correctIndex -> OptionStatus.CORRECT
    index == selected -> OptionStatus.WRONG
    else -> OptionStatus.DIMMED
}

@Composable
private fun OptionTile(text: String, status: OptionStatus, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val borderColor = when (status) {
        OptionStatus.CORRECT -> colors.secondary
        OptionStatus.WRONG -> colors.error
        else -> colors.surfaceVariant
    }
    val textColor = if (status == OptionStatus.DIMMED) colors.onSurface.copy(alpha = 0.4f) else colors.onSurface
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = colors.surface,
        border = BorderStroke(if (status == OptionStatus.NEUTRAL || status == OptionStatus.DIMMED) 1.dp else 2.dp, borderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Odpowiedzi to zwykle fragmenty wyjścia programu, więc też monospace.
        Text(text, style = CodeTextStyle, color = textColor, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun SummaryContent(outcome: RoundOutcome, onRepeat: () -> Unit, onDone: () -> Unit) = ScreenColumn {
    val score = outcome.score
    Text("Podsumowanie", style = MaterialTheme.typography.headlineSmall)
    Text(
        "+${score.xp} XP",
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
    Text("Poprawne: ${score.correct} z ${score.total}", style = MaterialTheme.typography.titleMedium)
    if (score.perfect) {
        Highlight("Bezbłędna runda! Bonus wliczony.", MaterialTheme.colorScheme.secondary)
    }
    if (outcome.leveledUp) {
        Highlight("Awans na poziom ${outcome.levelAfter.level}!", MaterialTheme.colorScheme.tertiary)
    }
    PlayerLevelBar(outcome.levelAfter, modifier = Modifier.padding(vertical = 8.dp))
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Do poziomów") }
    OutlinedButton(onClick = onRepeat, modifier = Modifier.fillMaxWidth()) { Text("Zagraj jeszcze raz") }
}

@Composable
private fun Highlight(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
    }
}
