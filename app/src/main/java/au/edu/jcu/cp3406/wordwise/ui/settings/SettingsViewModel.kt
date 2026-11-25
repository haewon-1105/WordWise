package au.edu.jcu.cp3406.wordwise.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.cp3406.wordwise.data.repository.SettingsRepository
import au.edu.jcu.cp3406.wordwise.data.repository.StudyRepository
import au.edu.jcu.cp3406.wordwise.model.Difficulty
import au.edu.jcu.cp3406.wordwise.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val studyRepository: StudyRepository,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettings(),
    )

    private val _progressReset = MutableStateFlow(false)

    /** True once after a reset, so the screen can confirm it in a snackbar. */
    val progressReset: StateFlow<Boolean> = _progressReset.asStateFlow()

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch { settingsRepository.setDailyGoal(goal) }
    }

    fun setDifficulty(difficulty: Difficulty) {
        viewModelScope.launch { settingsRepository.setDifficulty(difficulty) }
    }

    fun setReadWordsAloud(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReadWordsAloud(enabled) }
    }

    fun resetProgress() {
        viewModelScope.launch {
            studyRepository.resetProgress()
            _progressReset.value = true
        }
    }

    fun onResetMessageShown() {
        _progressReset.value = false
    }
}
