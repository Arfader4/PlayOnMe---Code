package pl.onme.playonme.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pl.onme.playonme.data.ProgressRepository
import pl.onme.playonme.data.pack.Pack
import pl.onme.playonme.data.pack.PackRepository
import pl.onme.playonme.domain.LevelUnlocks
import pl.onme.playonme.domain.QuickMode

data class LevelItem(
    val packId: String,
    val number: Int,
    val title: String,
    val mastered: Int,
    val total: Int,
    val unlocked: Boolean,
)

data class PackSection(
    val packId: String,
    val title: String,
    val levels: List<LevelItem>,
)

/** Rekordy i stan wyzwania dnia do kafelków szybkich trybów. */
data class QuickModesInfo(
    val bestBlitz: Int?,
    val bestSurvival: Int?,
    val dailyDone: Boolean,
    val dailyStreak: Int,
)

class LevelsViewModel(
    subject: String,
    packs: PackRepository,
    private val progress: ProgressRepository,
    today: Long = LocalDate.now().toEpochDay(),
) : ViewModel() {

    /** null = jeszcze się ładuje. */
    val quickModes: StateFlow<QuickModesInfo?> =
        combine(
            progress.bestCorrect(QuickMode.BLITZ, subject),
            progress.bestCorrect(QuickMode.SURVIVAL, subject),
            progress.dailyResult(subject, today),
            progress.dailyStreak(today),
        ) { blitz, survival, daily, streak -> QuickModesInfo(blitz, survival, daily != null, streak) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** null = jeszcze się ładuje. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val sections: StateFlow<List<PackSection>?> =
        flow { emit(packs.packsForSubject(subject)) }
            .flatMapLatest { list ->
                if (list.isEmpty()) flowOf(emptyList<PackSection>()) else combine(list.map(::sectionFlow)) { it.toList() }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun sectionFlow(pack: Pack): Flow<PackSection> =
        progress.masteredCardIds(pack.id).map { mastered -> buildSection(pack, mastered) }

    companion object {
        fun buildSection(pack: Pack, mastered: Set<String>): PackSection {
            val cardsPerLevel = pack.cards.groupBy({ it.level }, { it.id })
            val unlocked = LevelUnlocks.unlocked(cardsPerLevel, mastered)
            val titles = pack.levels.associate { it.number to it.title }
            val levels = cardsPerLevel.keys.sorted().map { number ->
                val ids = cardsPerLevel.getValue(number)
                LevelItem(
                    packId = pack.id,
                    number = number,
                    title = titles[number] ?: "Poziom $number",
                    mastered = ids.count { it in mastered },
                    total = ids.size,
                    unlocked = number in unlocked,
                )
            }
            return PackSection(pack.id, pack.title, levels)
        }
    }
}
