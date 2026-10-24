package pl.onme.playonme.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.onme.playonme.data.ProgressRepository
import pl.onme.playonme.data.pack.PackRepository
import pl.onme.playonme.domain.PlayerLevel
import pl.onme.playonme.domain.PlayerLevels

class LanguageViewModel(
    private val packs: PackRepository,
    progress: ProgressRepository,
) : ViewModel() {
    private val _subjectsWithPacks = MutableStateFlow<Set<String>?>(null)

    /** Przedmioty, dla których jest choć jedna paczka; null = jeszcze się ładuje. */
    val subjectsWithPacks: StateFlow<Set<String>?> = _subjectsWithPacks.asStateFlow()

    val playerLevel: StateFlow<PlayerLevel> =
        progress.playerLevel.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerLevels.forXp(0))

    init {
        viewModelScope.launch {
            _subjectsWithPacks.value = packs.allPacks().map { it.subject }.toSet()
        }
    }
}
