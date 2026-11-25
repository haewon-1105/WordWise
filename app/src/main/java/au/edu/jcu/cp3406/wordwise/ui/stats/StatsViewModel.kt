package au.edu.jcu.cp3406.wordwise.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.cp3406.wordwise.data.repository.SettingsRepository
import au.edu.jcu.cp3406.wordwise.data.repository.StudyRepository
import au.edu.jcu.cp3406.wordwise.model.LearningStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface StatsUiState {
    data object Loading : StatsUiState
    data class Ready(val stats: LearningStats, val dailyGoal: Int) : StatsUiState
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    studyRepository: StudyRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    /** Updates by itself whenever an answer is saved, because the queries are Room Flows. */
    val uiState: StateFlow<StatsUiState> = combine(
        studyRepository.observeStats(),
        settingsRepository.settings,
    ) { stats, settings ->
        StatsUiState.Ready(stats, settings.dailyGoal)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState.Loading,
    )
}
