package pl.onme.playonme.ui.screens

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pl.onme.playonme.data.ModeAnswer
import pl.onme.playonme.data.ModeOutcome
import pl.onme.playonme.data.ProgressRepository
import pl.onme.playonme.data.pack.PackRepository
import pl.onme.playonme.domain.BlitzClock
import pl.onme.playonme.domain.CardAnswer
import pl.onme.playonme.domain.LevelUnlocks
import pl.onme.playonme.domain.QuickDecks
import pl.onme.playonme.domain.QuickMode

/** Pytanie razem z paczką, z której pochodzi (postęp kart zapisujemy per paczka). */
data class QuickQuestion(val packId: String, val question: Question)

sealed interface QuickState {
    data object Loading : QuickState
    data object Empty : QuickState

    /** Dzisiejsze wyzwanie już rozegrane. */
    data class DailyDone(val correct: Int, val total: Int, val streak: Int) : QuickState

    data class Playing(
        val mode: QuickMode,
        val current: QuickQuestion,
        /** Numer bieżącego pytania od 1. */
        val number: Int,
        /** Liczba pytań w grze; null = bez końca (Blitz, seria). */
        val total: Int?,
        val selected: Int?,
        val answers: List<ModeAnswer>,
        /** Pozostały czas w Blitzu; null w pozostałych trybach. */
        val remainingMs: Long?,
    ) : QuickState {
        val correctCount: Int get() = answers.count { it.answer.correct }
        val combo: Int get() = answers.takeLastWhile { it.answer.correct }.size
        val lastCorrect: Boolean? get() = if (selected == null) null else answers.lastOrNull()?.answer?.correct
    }

    data object Saving : QuickState

    /** @param missed pytania z błędną odpowiedzią, do przejrzenia z wyjaśnieniem */
    data class Finished(val outcome: ModeOutcome, val missed: List<Question>) : QuickState
}

class QuickPlayViewModel(
    private val packs: PackRepository,
    private val progress: ProgressRepository,
    private val subject: String,
    private val mode: QuickMode,
    private val random: Random = Random.Default,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val today: () -> Long = { LocalDate.now().toEpochDay() },
) : ViewModel() {
    private val _state = MutableStateFlow<QuickState>(QuickState.Loading)
    val state: StateFlow<QuickState> = _state.asStateFlow()

    private var deck: Iterator<QuickQuestion> = emptyList<QuickQuestion>().iterator()
    private var day = 0L
    private var deadline = 0L
    private var missed = mutableListOf<Question>()
    private var timer: Job? = null
    private var advance: Job? = null

    init {
        start()
    }

    fun start() {
        timer?.cancel()
        advance?.cancel()
        _state.value = QuickState.Loading
        viewModelScope.launch {
            day = today()
            if (mode == QuickMode.DAILY) {
                progress.dailyResultNow(subject, day)?.let { done ->
                    _state.value = QuickState.DailyDone(done.correct, done.total, progress.dailyStreak(day).first())
                    return@launch
                }
            }
            val questions = loadQuestions()
            val total = if (mode == QuickMode.DAILY) questions.size else null
            deck = when (mode) {
                QuickMode.BLITZ -> QuickDecks.endless(questions, random)
                QuickMode.SURVIVAL -> QuickDecks.endless(questions, random) { list, r ->
                    QuickDecks.byDifficulty(list, r) { it.question.card.difficulty }
                }
                QuickMode.DAILY -> questions.asSequence()
            }.iterator()
            if (!deck.hasNext()) {
                _state.value = QuickState.Empty
                return@launch
            }
            missed = mutableListOf()
            _state.value = QuickState.Playing(
                mode = mode,
                current = deck.next(),
                number = 1,
                total = total,
                selected = null,
                answers = emptyList(),
                remainingMs = if (mode == QuickMode.BLITZ) BlitzClock.START_MS else null,
            )
            if (mode == QuickMode.BLITZ) startTimer()
        }
    }

    fun select(option: Int) {
        val playing = _state.value as? QuickState.Playing ?: return
        if (playing.selected != null) return
        val question = playing.current.question
        val correct = option == question.correctIndex
        val answer = ModeAnswer(playing.current.packId, CardAnswer(question.card.id, question.card.difficulty, correct))
        if (!correct) missed += question
        if (mode == QuickMode.BLITZ) deadline = BlitzClock.deadlineAfter(deadline, correct)
        _state.value = playing.copy(
            selected = option,
            answers = playing.answers + answer,
            remainingMs = if (mode == QuickMode.BLITZ) remaining() else null,
        )
        // W Blitzu i przy dobrej odpowiedzi w serii nie czekamy na „Dalej”, żeby gra miała tempo.
        val auto = mode == QuickMode.BLITZ || (mode == QuickMode.SURVIVAL && correct)
        if (auto) {
            advance = viewModelScope.launch {
                delay(FEEDBACK_MS)
                next()
            }
        }
    }

    fun next() {
        val playing = _state.value as? QuickState.Playing ?: return
        if (playing.selected == null) return
        advance?.cancel()
        val lastWrong = playing.lastCorrect == false
        val gameOver = (mode == QuickMode.SURVIVAL && lastWrong) ||
            (mode == QuickMode.BLITZ && remaining() <= 0) ||
            !deck.hasNext()
        if (gameOver) finish(playing) else {
            _state.value = playing.copy(current = deck.next(), number = playing.number + 1, selected = null)
        }
    }

    /** Przycisk „Zakończ” w trakcie gry: liczy się to, co już odpowiedziane. */
    fun stop() {
        val playing = _state.value as? QuickState.Playing ?: return
        finish(playing)
    }

    private fun finish(playing: QuickState.Playing) {
        timer?.cancel()
        advance?.cancel()
        _state.value = QuickState.Saving
        viewModelScope.launch {
            val outcome = progress.recordModeRun(mode, subject, day, playing.answers)
            _state.value = QuickState.Finished(outcome, missed.distinctBy { it.card.id })
        }
    }

    private fun startTimer() {
        deadline = clock() + BlitzClock.START_MS
        timer = viewModelScope.launch {
            while (isActive) {
                delay(TICK_MS)
                val playing = _state.value as? QuickState.Playing ?: continue
                val left = remaining()
                if (left <= 0) {
                    finish(playing)
                    return@launch
                }
                _state.value = playing.copy(remainingMs = left)
            }
        }
    }

    private fun remaining(): Long = (deadline - clock()).coerceAtLeast(0)

    /** Blitz i seria biorą karty z odblokowanych poziomów; wyzwanie dnia ze wszystkich, żeby było wspólne. */
    private suspend fun loadQuestions(): List<QuickQuestion> {
        val all = packs.packsForSubject(subject).flatMap { pack ->
            val cards = if (mode == QuickMode.DAILY) pack.cards else {
                val mastered = progress.masteredCardIds(pack.id).first()
                val unlocked = LevelUnlocks.unlocked(pack.cards.groupBy({ it.level }, { it.id }), mastered)
                pack.cards.filter { it.level in unlocked }
            }
            cards.mapNotNull { card -> Question.from(card, random)?.let { QuickQuestion(pack.id, it) } }
        }
        return if (mode == QuickMode.DAILY) {
            QuickDecks.daily(all, subject, day, id = { "${it.packId}/${it.question.card.id}" })
        } else {
            all
        }
    }

    companion object {
        const val FEEDBACK_MS = 600L
        const val TICK_MS = 100L
    }
}
