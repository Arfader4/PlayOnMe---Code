package pl.onme.playonme.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.onme.playonme.data.ModeOutcome
import pl.onme.playonme.domain.BlitzClock
import pl.onme.playonme.domain.QuickDecks
import pl.onme.playonme.domain.QuickMode
import pl.onme.playonme.ui.appViewModel
import pl.onme.playonme.ui.components.CodeBlock
import pl.onme.playonme.ui.components.LoadingBox
import pl.onme.playonme.ui.components.MessageBox
import pl.onme.playonme.ui.components.PlayerLevelBar
import pl.onme.playonme.ui.components.ScreenHeader

/** Nazwy i krótkie zasady trybów, wspólne dla kafelków i ekranu gry. */
object QuickModeTexts {
    fun title(mode: QuickMode): String = when (mode) {
        QuickMode.BLITZ -> "Blitz 60 s"
        QuickMode.SURVIVAL -> "Seria"
        QuickMode.DAILY -> "Wyzwanie dnia"
    }

    fun rules(mode: QuickMode): String = when (mode) {
        QuickMode.BLITZ -> "Ile zadań zdążysz? Dobra odpowiedź +2 s, zła −5 s."
        QuickMode.SURVIVAL -> "Coraz trudniej, pierwszy błąd kończy grę."
        QuickMode.DAILY -> "${QuickDecks.DAILY_SIZE} zadań na dziś, jedna próba."
    }
}

@Composable
fun QuickPlayScreen(subject: String, mode: QuickMode, onExit: () -> Unit) {
    val vm = appViewModel(key = "quick-$subject-${mode.key}") { QuickPlayViewModel(it.packs, it.progress, subject, mode) }
    val state by vm.state.collectAsState()

    when (val s = state) {
        QuickState.Loading, QuickState.Saving -> ScreenColumn { LoadingBox() }
        QuickState.Empty -> ScreenColumn {
            ScreenHeader(QuickModeTexts.title(mode), onBack = onExit)
            MessageBox("Brak zadań do tego trybu.")
        }
        is QuickState.DailyDone -> ScreenColumn {
            ScreenHeader(QuickModeTexts.title(mode), onBack = onExit)
            Text("Dzisiejsze wyzwanie już za Tobą.", style = MaterialTheme.typography.titleMedium)
            Text("Wynik: ${s.correct} z ${s.total}", style = MaterialTheme.typography.bodyLarge)
            Highlight(streakText(s.streak), MaterialTheme.colorScheme.tertiary)
            Text("Nowe zadania jutro.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("Wróć") }
        }
        is QuickState.Playing -> PlayingContent(s, onSelect = vm::select, onNext = vm::next, onStop = vm::stop)
        is QuickState.Finished -> FinishedContent(s, onRepeat = vm::start, onDone = onExit)
    }
}

@Composable
private fun PlayingContent(
    state: QuickState.Playing,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
) = ScreenColumn {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(QuickModeTexts.title(state.mode), style = MaterialTheme.typography.headlineSmall)
        TextButton(onClick = onStop) { Text("Zakończ") }
    }
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(
            if (state.total != null) "Zadanie ${state.number} z ${state.total}" else "Wynik: ${state.correctCount}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (state.total == null) FontWeight.Bold else null,
        )
        if (state.combo >= 2) {
            Text("Combo ×${state.combo}", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
        }
    }
    state.remainingMs?.let { BlitzTimer(it) }
    AnimatedContent(
        targetState = state,
        contentKey = { it.number },
        transitionSpec = {
            (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
        },
        label = "quickQuestion",
    ) { shown ->
        QuestionBody(shown, onSelect, onNext)
    }
}

@Composable
private fun BlitzTimer(remainingMs: Long) {
    val seconds = (remainingMs + 999) / 1000
    val low = remainingMs < 10_000
    val color by animateColorAsState(
        if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        label = "timerColor",
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$seconds s", style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
        // Pasek względem czasu startowego; po bonusach może być pełny dłużej.
        LinearProgressIndicator(
            progress = { (remainingMs.toFloat() / BlitzClock.START_MS).coerceIn(0f, 1f) },
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun QuestionBody(state: QuickState.Playing, onSelect: (Int) -> Unit, onNext: () -> Unit) {
    val question = state.current.question
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(question.card.prompt, style = MaterialTheme.typography.titleMedium)
        question.card.code?.let { CodeBlock(it, question.card.codeLanguage) }

        question.options.forEachIndexed { i, option ->
            OptionTile(
                text = option,
                status = optionStatus(i, state.selected, question.correctIndex),
                enabled = state.selected == null,
                onClick = { onSelect(i) },
            )
        }

        // Blitz i dobra odpowiedź w serii przechodzą dalej same; wyjaśnienia czekają w podsumowaniu.
        val waitsForPlayer = state.mode == QuickMode.DAILY || (state.mode == QuickMode.SURVIVAL && state.lastCorrect == false)
        AnimatedVisibility(
            visible = state.selected != null && waitsForPlayer,
            enter = fadeIn() + expandVertically(),
        ) {
            val correct = state.lastCorrect == true
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExplanationCard(correct, question.card.explanation)
                val last = state.mode == QuickMode.SURVIVAL || state.number == state.total
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                    Text(if (last) "Zobacz wynik" else "Dalej")
                }
            }
        }
    }
}

@Composable
private fun ExplanationCard(correct: Boolean, explanation: String) {
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
            Text(explanation, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FinishedContent(state: QuickState.Finished, onRepeat: () -> Unit, onDone: () -> Unit) = ScreenColumn {
    val outcome = state.outcome
    val score = outcome.score
    var xpTarget by remember { mutableIntStateOf(0) }
    LaunchedEffect(score.xp) { xpTarget = score.xp }
    val xpShown by animateIntAsState(xpTarget, animationSpec = tween(durationMillis = 900), label = "xp")

    Text(QuickModeTexts.title(outcome.mode), style = MaterialTheme.typography.headlineSmall)
    Text(resultLine(outcome), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
    Text("+$xpShown XP", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
    if (outcome.newRecord) {
        Highlight(
            if (outcome.bestBefore == null) "Pierwszy rekord!" else "Nowy rekord! Poprzedni: ${outcome.bestBefore}",
            MaterialTheme.colorScheme.secondary,
        )
    } else if (outcome.mode != QuickMode.DAILY && outcome.bestBefore != null) {
        Text("Rekord: ${outcome.bestBefore}", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (outcome.mode == QuickMode.DAILY) {
        if (score.perfect) Highlight("Bezbłędnie! Bonus wliczony.", MaterialTheme.colorScheme.secondary)
        Highlight(streakText(outcome.dailyStreak), MaterialTheme.colorScheme.tertiary)
    }
    if (outcome.leveledUp) {
        Highlight("Awans na poziom ${outcome.levelAfter.level}!", MaterialTheme.colorScheme.tertiary)
    }
    PlayerLevelBar(outcome.levelAfter, modifier = Modifier.padding(vertical = 8.dp))
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Wróć") }
    if (outcome.mode != QuickMode.DAILY) {
        OutlinedButton(onClick = onRepeat, modifier = Modifier.fillMaxWidth()) { Text("Zagraj jeszcze raz") }
    }

    if (state.missed.isNotEmpty()) {
        Text("Co przegapiłeś", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        for (question in state.missed) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(question.card.prompt, style = MaterialTheme.typography.bodyLarge)
                question.card.code?.let { CodeBlock(it, question.card.codeLanguage) }
                Text(
                    "Odpowiedź: ${question.options[question.correctIndex]}",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                )
                Text(question.card.explanation, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun resultLine(outcome: ModeOutcome): String = when (outcome.mode) {
    QuickMode.DAILY -> "${outcome.score.correct} z ${outcome.score.total}"
    QuickMode.BLITZ, QuickMode.SURVIVAL -> "${outcome.score.correct} poprawnych"
}

internal fun streakText(days: Int): String = when {
    days <= 0 -> "Zacznij serię dni"
    days == 1 -> "1 dzień z rzędu"
    else -> "$days dni z rzędu"
}
