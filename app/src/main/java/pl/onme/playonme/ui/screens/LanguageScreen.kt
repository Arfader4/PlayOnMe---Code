package pl.onme.playonme.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.onme.playonme.ui.Subjects
import pl.onme.playonme.ui.appViewModel
import pl.onme.playonme.ui.components.LoadingBox
import pl.onme.playonme.ui.components.PlayerLevelBar

@Composable
fun LanguageScreen(onPick: (subject: String) -> Unit) {
    val vm = appViewModel { LanguageViewModel(it.packs, it.progress) }
    val available by vm.subjectsWithPacks.collectAsState()
    val level by vm.playerLevel.collectAsState()

    ScreenColumn {
        Text("PlayOnMe Code", style = MaterialTheme.typography.headlineMedium)
        PlayerLevelBar(level)
        Text(
            "Wybierz język",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        val subjects = available
        if (subjects == null) {
            LoadingBox()
            return@ScreenColumn
        }
        // Najpierw znane przedmioty w ustalonej kolejności, potem ewentualne nowe z paczek.
        val all = Subjects.known + (subjects - Subjects.known.toSet()).sorted()
        for (subject in all) {
            val enabled = subject in subjects
            SubjectTile(
                name = Subjects.displayName(subject),
                subtitle = if (enabled) "Przewidź wynik" else "Wkrótce",
                enabled = enabled,
                onClick = { onPick(subject) },
            )
        }
    }
}

@Composable
private fun SubjectTile(name: String, subtitle: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val alpha = if (enabled) 1f else 0.4f
            Text(name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
        }
    }
}
