package au.edu.jcu.cp3406.wordwise.ui.addword

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.cp3406.wordwise.R
import au.edu.jcu.cp3406.wordwise.data.repository.AddWordResult
import au.edu.jcu.cp3406.wordwise.data.repository.DictionaryRepository
import au.edu.jcu.cp3406.wordwise.model.dictionary.DictionaryDefinition
import au.edu.jcu.cp3406.wordwise.model.dictionary.DictionaryEntry
import au.edu.jcu.cp3406.wordwise.model.dictionary.DictionaryMeaning
import au.edu.jcu.cp3406.wordwise.model.dictionary.LookupResult
import au.edu.jcu.cp3406.wordwise.util.PlaybackSource
import au.edu.jcu.cp3406.wordwise.util.PronunciationPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LookupStatus {
    data object Idle : LookupStatus
    data object Loading : LookupStatus
    data class Found(val entry: DictionaryEntry) : LookupStatus
    data object NotFound : LookupStatus
    data object InvalidInput : LookupStatus
    data object NoConnection : LookupStatus
    data object ServiceError : LookupStatus
}

/** One-off messages shown in a snackbar. */
enum class AddWordMessage(@StringRes val textRes: Int) {
    ADDED(R.string.add_word_added),
    ALREADY_IN_COLLECTION(R.string.add_word_already_added),
    AUDIO_FAILED(R.string.add_word_audio_failed),
    DEVICE_VOICE_USED(R.string.add_word_device_voice),
}

data class AddWordUiState(
    val query: String = "",
    val status: LookupStatus = LookupStatus.Idle,
    /** True once the current word is in the learner's collection, so it is not added twice. */
    val isInCollection: Boolean = false,
    val message: AddWordMessage? = null,
)

@HiltViewModel
class AddWordViewModel @Inject constructor(
    private val repository: DictionaryRepository,
    private val player: PronunciationPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddWordUiState())
    val uiState: StateFlow<AddWordUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    /** Looks the word up only when the learner asks, not on every keystroke. */
    fun search() {
        val query = _uiState.value.query
        searchJob?.cancel()
        _uiState.update { it.copy(status = LookupStatus.Loading, isInCollection = false) }
        searchJob = viewModelScope.launch {
            val status = repository.lookUp(query).toStatus()
            _uiState.update { it.copy(status = status) }
        }
    }

    fun addToCollection(meaning: DictionaryMeaning, definition: DictionaryDefinition) {
        val found = _uiState.value.status as? LookupStatus.Found ?: return
        viewModelScope.launch {
            val message = when (repository.addToCollection(found.entry, meaning, definition)) {
                is AddWordResult.Added -> AddWordMessage.ADDED
                AddWordResult.AlreadyInCollection -> AddWordMessage.ALREADY_IN_COLLECTION
            }
            _uiState.update { it.copy(isInCollection = true, message = message) }
        }
    }

    fun playPronunciation() {
        val entry = (_uiState.value.status as? LookupStatus.Found)?.entry ?: return
        viewModelScope.launch {
            val message = when (player.play(entry.term, entry.audioUrl)) {
                PlaybackSource.RECORDING -> null
                // Be open about it when the expected recording was replaced by the device voice.
                PlaybackSource.DEVICE_VOICE ->
                    if (entry.audioUrl != null) AddWordMessage.DEVICE_VOICE_USED else null
                null -> AddWordMessage.AUDIO_FAILED
            }
            if (message != null) _uiState.update { it.copy(message = message) }
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    override fun onCleared() {
        player.release()
    }

    private fun LookupResult.toStatus(): LookupStatus = when (this) {
        is LookupResult.Found -> LookupStatus.Found(entry)
        LookupResult.NotFound -> LookupStatus.NotFound
        LookupResult.InvalidInput -> LookupStatus.InvalidInput
        LookupResult.NoConnection -> LookupStatus.NoConnection
        is LookupResult.ServiceError -> LookupStatus.ServiceError
    }
}
