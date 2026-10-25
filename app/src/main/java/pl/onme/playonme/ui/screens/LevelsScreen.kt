package pl.onme.playonme.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.onme.playonme.domain.QuickMode
import pl.onme.playonme.ui.Subjects
import pl.onme.playonme.ui.appViewModel
import pl.onme.playonme.ui.components.LoadingBox
import pl.onme.playonme.ui.components.MessageBox
import pl.onme.playonme.ui.components.ScreenHeader

@Composable
fun LevelsScreen(
    subject: String,
    onPickLevel: (packId: String, level: Int) -> Unit,
    onPickMode: (QuickMode) -> Unit,
    onBack: () -> Unit,
) {
    val vm = appViewModel(key = "levels-$subject") { LevelsViewModel(subject, it.packs, it.progress) }
    val sections by vm.sections.collectAsState()
    val quick by vm.quickModes.collectAsState()

    ScreenColumn {
        ScreenHeader(Subjects.displayName(subject), onBack = onBack)
        val list = sections
        when {
            list == null -> LoadingBox()
            list.isEmpty() -> MessageBox("Brak paczek dla tego języka.")
            else -> {
                quick?.let { QuickModesSection(it, onPickMode) }
                LevelSections(list, onPickLevel)
            }
        }
    }
}

@Composable
private fun LevelSections(list: List<PackSection>, onPickLevel: (packId: String, level: Int) -> Unit) {
    for (section in list) {
        Text(
            section.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        for (level in section.levels) {
            LevelTile(level, onClick = { onPickLevel(level.packId, level.number) })
        }
    }
}

@Composable
private fun QuickModesSection(info: QuickModesInfo, onPick: (QuickMode) -> Unit) {
    Text(
        "Szybka gra",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    // Wyzwanie dnia na górze: to ono ma ściągać do gry codziennie.
    for (mode in listOf(QuickMode.DAILY, QuickMode.BLITZ, QuickMode.SURVIVAL)) {
        val status = when (mode) {
            QuickMode.BLITZ -> info.bestBlitz?.let { "Rekord: $it" }
            QuickMode.SURVIVAL -> info.bestSurvival?.let { "Rekord: $it" }
            QuickMode.DAILY -> (if (info.dailyDone) "Zrobione ✓ · " else "") + streakText(info.dailyStreak)
        }
        QuickModeTile(mode, status, highlighted = mode == QuickMode.DAILY && !info.dailyDone, onClick = { onPick(mode) })
    }
}

@Composable
private fun QuickModeTile(mode: QuickMode, status: String?, highlighted: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = if (highlighted) BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(QuickModeTexts.title(mode), style = MaterialTheme.typography.titleMedium)
                status?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(QuickModeTexts.rules(mode), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LevelTile(level: LevelItem, onClick: () -> Unit) {
    val alpha = if (level.unlocked) 1f else 0.4f
    Surface(
        onClick = onClick,
        enabled = level.unlocked,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${level.number}. ${level.title}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                )
                Text(
                    if (level.unlocked) "${level.mastered}/${level.total}" else "🔒",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (level.unlocked) {
                LinearProgressIndicator(
                    progress = { level.mastered.toFloat() / level.total },
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    "Zalicz 60% poprzedniego poziomu, żeby odblokować.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
