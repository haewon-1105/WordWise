package au.edu.jcu.cp3406.wordwise.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.cp3406.wordwise.data.repository.DictionaryRepository
import au.edu.jcu.cp3406.wordwise.data.repository.SettingsRepository
import au.edu.jcu.cp3406.wordwise.data.repository.StudyRepository
import au.edu.jcu.cp3406.wordwise.model.AnswerFeedback
import au.edu.jcu.cp3406.wordwise.model.StudySession
import au.edu.jcu.cp3406.wordwise.model.UserSettings
import au.edu.jcu.cp3406.wordwise.util.PronunciationPlayer
import au.edu.jcu.cp3406.wordwise.util.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

sealed interface StudyUiState {
    data object Loading : StudyUiState

    data class InSession(
        val session: StudySession,
        val lastFeedback: AnswerFeedback?,
        val readWordsAloud: Boolean,
    ) : StudyUiState

    /** No cards left for now: either the goal is met or nothing is due. */
    data class Finished(
        val answeredThisSession: Int,
        val correctThisSession: Int,
        val reviewedToday: Int,
        val dailyGoal: Int,
    ) : StudyUiState {
        val goalMet: Boolean get() = reviewedToday >= dailyGoal
    }

    data object LoadFailed : StudyUiState
}

@HiltViewModel
class StudyViewModel @Inject constructor(
    private val studyRepository: StudyRepository,
    private val dictionaryRepository: DictionaryRepository,
    private val settingsRepository: SettingsRepository,
    private val player: PronunciationPlayer,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow<StudyUiState>(StudyUiState.Loading)
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    private var settings = UserSettings()
    private var isSaving = false

    init {
        // Keep the read-aloud switch live without restarting the session.
        viewModelScope.launch {
            settingsRepository.settings.collect { latest ->
                settings = latest
                _uiState.update { state ->
                    if (state is StudyUiState.InSession) {
                        state.copy(readWordsAloud = latest.readWordsAloud)
                    } else {
                        state
                    }
                }
            }
        }
    }

    /**
     * Called whenever the Study screen is shown. Builds a fresh session unless the learner is
     * part-way through one, so a changed goal, difficulty or reset is picked up.
     */
    fun refreshIfIdle() {
        val state = _uiState.value
        val midSession = state is StudyUiState.InSession && state.session.answeredCount > 0
        if (!midSession) viewModelScope.launch { loadSession() }
    }

    fun flip() {
        _uiState.update { state ->
            if (state is StudyUiState.InSession) state.copy(session = state.session.flip()) else state
        }
    }

    fun answer(correct: Boolean) {
        val state = _uiState.value as? StudyUiState.InSession ?: return
        val card = state.session.current ?: return
        if (!state.session.isFlipped || isSaving) return

        isSaving = true
        viewModelScope.launch {
            try {
                val progress = studyRepository.recordAnswer(card, correct)
                val next = state.session.answer(correct)
                val feedback = AnswerFeedback.from(correct, progress, timeProvider.today().toEpochDay())
                _uiState.value = if (next.isFinished) {
                    finishedState(next.answeredCount, next.correctCount)
                } else {
                    StudyUiState.InSession(next, feedback, settings.readWordsAloud)
                }
            } finally {
                isSaving = false
            }
        }
    }

    /** Plays the current word, fetching and caching its pronunciation the first time. */
    fun playPronunciation() {
        val state = _uiState.value as? StudyUiState.InSession ?: return
        val word = state.session.current?.word ?: return
        viewModelScope.launch {
            val withSound = dictionaryRepository.withPronunciation(word)
            _uiState.update { current ->
                if (current is StudyUiState.InSession) {
                    current.copy(session = current.session.updateCurrentWord(withSound))
                } else {
                    current
                }
            }
            player.play(withSound.term, withSound.audioUrl)
        }
    }

    override fun onCleared() {
        player.release()
    }

    private suspend fun loadSession() {
        _uiState.value = StudyUiState.Loading
        _uiState.value = try {
            settings = settingsRepository.settings.first()
            studyRepository.ensureDeckLoaded()
            val cards = studyRepository.todaysSession(settings.dailyGoal, settings.difficulty)
            if (cards.isEmpty()) {
                finishedState(answered = 0, correct = 0)
            } else {
                StudyUiState.InSession(StudySession(cards), null, settings.readWordsAloud)
            }
        } catch (e: IOException) {
            StudyUiState.LoadFailed
        } catch (e: IllegalArgumentException) {
            StudyUiState.LoadFailed
        }
    }

    private suspend fun finishedState(answered: Int, correct: Int) = StudyUiState.Finished(
        answeredThisSession = answered,
        correctThisSession = correct,
        reviewedToday = studyRepository.observeReviewedToday().first(),
        dailyGoal = settings.dailyGoal,
    )
}
