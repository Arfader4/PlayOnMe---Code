package pl.onme.playonme.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Etap 1: szkielety ekranów, żeby nawigacja działała od początku.
// Prawdziwa zawartość przychodzi w etapie 4.

@Composable
private fun ScreenScaffold(title: String, content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            content()
        }
    }
}

@Composable
fun LanguageScreen(onPick: (subject: String) -> Unit) = ScreenScaffold("Wybierz język") {
    Button(onClick = { onPick("csharp") }) { Text("C#") }
    OutlinedButton(onClick = {}, enabled = false) { Text("T-SQL (wkrótce)") }
}

@Composable
fun LevelsScreen(subject: String, onPickLevel: (packId: String, level: Int) -> Unit, onBack: () -> Unit) =
    ScreenScaffold("Poziomy: $subject") {
        Button(onClick = { onPickLevel("csharp-basics", 1) }) { Text("Poziom 1") }
        OutlinedButton(onClick = onBack) { Text("Wstecz") }
    }

@Composable
fun RoundScreen(packId: String, level: Int, onFinished: (xp: Int, correct: Int, total: Int) -> Unit) =
    ScreenScaffold("Runda: $packId, poziom $level") {
        Text("Tu pojawi się zadanie „przewidź wynik”.")
        Button(onClick = { onFinished(0, 0, 0) }) { Text("Zakończ rundę") }
    }

@Composable
fun SummaryScreen(xp: Int, correct: Int, total: Int, onDone: () -> Unit) = ScreenScaffold("Podsumowanie") {
    Text("XP: +$xp")
    Text("Poprawne: $correct / $total")
    Button(onClick = onDone) { Text("Dalej") }
}
