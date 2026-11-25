package au.edu.jcu.cp3406.wordwise.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.cp3406.wordwise.data.repository.SettingsRepository
import au.edu.jcu.cp3406.wordwise.data.repository.StudyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val totalWords: Int = 0,
    val wordsMastered: Int = 0,
    val reviewedToday: Int = 0,
    val dailyGoal: Int = 0,
) {
    val goalMet: Boolean get() = dailyGoal > 0 && reviewedToday >= dailyGoal
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: StudyRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val isLoading = MutableStateFlow(true)
    private val loadFailed = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            try {
                repository.ensureDeckLoaded()
            } catch (e: IOException) {
                loadFailed.value = true
            } catch (e: IllegalArgumentException) {
                // Malformed deck file (kotlinx SerializationException is an IllegalArgumentException).
                loadFailed.value = true
            } finally {
                isLoading.value = false
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        isLoading,
        loadFailed,
        repository.observeStats(),
        settingsRepository.settings,
    ) { loading, failed, stats, settings ->
        HomeUiState(
            isLoading = loading,
            loadFailed = failed,
            totalWords = stats.totalWords,
            wordsMastered = stats.wordsMastered,
            reviewedToday = stats.reviewsToday,
            dailyGoal = settings.dailyGoal,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )
}
