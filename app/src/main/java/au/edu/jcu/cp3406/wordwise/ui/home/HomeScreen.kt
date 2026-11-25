package au.edu.jcu.cp3406.wordwise.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.cp3406.wordwise.R
import au.edu.jcu.cp3406.wordwise.ui.theme.WordWiseTheme

/** Stateful entry point used by the NavHost: gets its state from [HomeViewModel]. */
@Composable
fun HomeScreen(
    onStartSession: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddWord: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        uiState = uiState,
        onStartSession = onStartSession,
        onOpenStats = onOpenStats,
        onOpenSettings = onOpenSettings,
        onAddWord = onAddWord,
        modifier = modifier,
    )
}

/** Stateless landing page: what the app does, today's progress and shortcuts. */
@Composable
fun HomeContent(
    uiState: HomeUiState,
    onStartSession: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddWord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.home_greeting),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_today_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                when {
                    uiState.isLoading -> {
                        Text(stringResource(R.string.home_loading))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    uiState.loadFailed -> Text(
                        text = stringResource(R.string.home_load_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                    else -> {
                        Text(
                            text = stringResource(
                                R.string.home_deck_summary,
                                uiState.totalWords,
                                uiState.wordsMastered,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(
                                R.string.home_reviewed_today,
                                uiState.reviewedToday,
                                uiState.dailyGoal,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (uiState.dailyGoal > 0) {
                            LinearProgressIndicator(
                                progress = {
                                    (uiState.reviewedToday.toFloat() / uiState.dailyGoal).coerceAtMost(1f)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (uiState.goalMet) {
                            Text(
                                text = stringResource(R.string.home_goal_met),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                // Once the goal is met, the main action becomes reviewing progress, not more cards.
                Button(
                    onClick = if (uiState.goalMet) onOpenStats else onStartSession,
                    enabled = !uiState.isLoading && !uiState.loadFailed,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (uiState.goalMet) R.string.home_shortcut_stats else R.string.home_start_session,
                        ),
                    )
                }
            }
        }

        // Shortcuts use the same button weight so neither is visually pushed on the user.
        OutlinedButton(onClick = onAddWord, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_add_word))
        }
        OutlinedButton(onClick = onOpenStats, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_shortcut_stats))
        }
        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_shortcut_settings))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    WordWiseTheme {
        HomeContent(
            uiState = HomeUiState(isLoading = false, totalWords = 60, wordsMastered = 4, reviewedToday = 7, dailyGoal = 10),
            onStartSession = {},
            onOpenStats = {},
            onOpenSettings = {},
            onAddWord = {},
        )
    }
}
