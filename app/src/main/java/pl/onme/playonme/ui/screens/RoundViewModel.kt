package pl.onme.playonme.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.onme.playonme.data.ProgressRepository
import pl.onme.playonme.data.RoundOutcome
import pl.onme.playonme.data.pack.Answer
import pl.onme.playonme.data.pack.Card
import pl.onme.playonme.data.pack.CardType
import pl.onme.playonme.data.pack.PackRepository
import pl.onme.playonme.domain.CardAnswer

/** Pytanie z opcjami w kolejności, w jakiej widzi je gracz. */
data class Question(
    val card: Card,
    val options: List<String>,
    val correctIndex: Int,
) {
    companion object {
        /** Tasuje opcje; zwraca null dla kart, których runda jeszcze nie obsługuje. */
        fun from(card: Card, random: Random): Question? {
            if (card.type != CardType.PREDICT_OUTPUT) return null
            val choice = card.answer as? Answer.Choice ?: return null
            val order = choice.options.indices.shuffled(random)
            return Question(card, order.map { choice.options[it] }, order.indexOf(choice.correct))
        }
    }
}

sealed interface RoundState {
    data object Loading : RoundState
    data object Empty : RoundState

    data class Playing(
        val title: String,
        val questions: List<Question>,
        val index: Int,
        /** Wybrana opcja w bieżącym pytaniu; null = gracz jeszcze nie odpowiedział. */
        val selected: Int?,
        val answers: List<CardAnswer>,
    ) : RoundState {
        val current: Question get() = questions[index]
        val isLast: Boolean get() = index == questions.lastIndex
        val combo: Int get() = answers.takeLastWhile { it.correct }.size
    }

    data object Saving : RoundState
    data class Finished(val outcome: RoundOutcome) : RoundState
}

class RoundViewModel(
    private val packs: PackRepository,
    private val progress: ProgressRepository,
    private val packId: String,
    private val level: Int,
    private val random: Random = Random.Default,
) : ViewModel() {
    private val _state = MutableStateFlow<RoundState>(RoundState.Loading)
    val state: StateFlow<RoundState> = _state.asStateFlow()

    init {
        start()
    }

    fun start() {
        _state.value = RoundState.Loading
        viewModelScope.launch {
            val pack = packs.pack(packId)
            val questions = pack.cards
                .filter { it.level == level }
                .mapNotNull { Question.from(it, random) }
                .shuffled(random)
            val title = pack.levels.firstOrNull { it.number == level }?.title ?: "Poziom $level"
            _state.value = if (questions.isEmpty()) {
                RoundState.Empty
            } else {
                RoundState.Playing(title, questions, index = 0, selected = null, answers = emptyList())
            }
        }
    }

    fun select(option: Int) {
        val playing = _state.value as? RoundState.Playing ?: return
        if (playing.selected != null) return
        val question = playing.current
        val answer = CardAnswer(question.card.id, question.card.difficulty, option == question.correctIndex)
        _state.value = playing.copy(selected = option, answers = playing.answers + answer)
    }

    fun next() {
        val playing = _state.value as? RoundState.Playing ?: return
        if (playing.selected == null) return
        if (!playing.isLast) {
            _state.value = playing.copy(index = playing.index + 1, selected = null)
            return
        }
        _state.value = RoundState.Saving
        viewModelScope.launch {
            _state.value = RoundState.Finished(progress.recordRound(packId, level, playing.answers))
        }
    }
}
