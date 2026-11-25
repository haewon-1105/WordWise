package au.edu.jcu.cp3406.wordwise.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.cp3406.wordwise.R
import au.edu.jcu.cp3406.wordwise.model.Difficulty
import au.edu.jcu.cp3406.wordwise.model.UserSettings
import au.edu.jcu.cp3406.wordwise.ui.theme.WordWiseTheme
import kotlin.math.roundToInt

/** Stateful entry point used by the NavHost. */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val progressReset by viewModel.progressReset.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(progressReset) {
        if (progressReset) {
            snackbarHostState.showSnackbar(context.getString(R.string.settings_reset_done))
            viewModel.onResetMessageShown()
        }
    }

    SettingsContent(
        settings = settings,
        snackbarHostState = snackbarHostState,
        onDailyGoalChange = viewModel::setDailyGoal,
        onDifficultyChange = viewModel::setDifficulty,
        onReadAloudChange = viewModel::setReadWordsAloud,
        onResetProgress = viewModel::resetProgress,
        modifier = modifier,
    )
}

/** Stateless settings page: daily goal, difficulty, sound and the learner's data. */
@Composable
fun SettingsContent(
    settings: UserSettings,
    snackbarHostState: SnackbarHostState,
    onDailyGoalChange: (Int) -> Unit,
    onDifficultyChange: (Difficulty) -> Unit,
    onReadAloudChange: (Boolean) -> Unit,
    onResetProgress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = stringResource(R.string.nav_settings),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )

            DailyGoalSection(settings.dailyGoal, onDailyGoalChange)
            HorizontalDivider()
            DifficultySection(settings.difficulty, onDifficultyChange)
            HorizontalDivider()
            SoundSection(settings.readWordsAloud, onReadAloudChange)
            HorizontalDivider()
            DataSection(onResetClick = { showResetDialog = true })
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.settings_reset_dialog_title)) },
            text = { Text(stringResource(R.string.settings_reset_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    onResetProgress()
                }) {
                    Text(stringResource(R.string.settings_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionTitle(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun DailyGoalSection(goal: Int, onGoalChange: (Int) -> Unit) {
    // The slider moves freely while dragging and saves once the learner lets go.
    var sliderValue by remember(goal) { mutableFloatStateOf(goal.toFloat()) }
    val shownGoal = sliderValue.roundToInt()
    val goalLabel = stringResource(R.string.settings_goal_value, shownGoal)
    val stepCount = (UserSettings.MAX_DAILY_GOAL - UserSettings.MIN_DAILY_GOAL) /
        UserSettings.DAILY_GOAL_STEP - 1

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(R.string.settings_goal_title)
        Text(text = goalLabel, style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onGoalChange(sliderValue.roundToInt()) },
            valueRange = UserSettings.MIN_DAILY_GOAL.toFloat()..UserSettings.MAX_DAILY_GOAL.toFloat(),
            steps = stepCount,
            // TalkBack reads "10 cards a day" instead of a percentage.
            modifier = Modifier.semantics { stateDescription = goalLabel },
        )
        Text(
            text = stringResource(R.string.settings_goal_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DifficultySection(selected: Difficulty, onSelect: (Difficulty) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle(R.string.settings_difficulty_title)
        Column(Modifier.selectableGroup()) {
            Difficulty.entries.forEach { difficulty ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // The whole row is one large touch target and one TalkBack item.
                        .selectable(
                            selected = difficulty == selected,
                            onClick = { onSelect(difficulty) },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = difficulty == selected, onClick = null)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(
                            text = stringResource(difficulty.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(difficulty.descriptionRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundSection(readAloud: Boolean, onChange: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle(R.string.settings_sound_title)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = readAloud, onValueChange = onChange, role = Role.Switch)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_read_aloud),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.settings_read_aloud_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = readAloud, onCheckedChange = null)
        }
    }
}

@Composable
private fun DataSection(onResetClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(R.string.settings_data_title)
        Text(
            text = stringResource(R.string.settings_data_body),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(onClick = onResetClick) {
            Text(
                text = stringResource(R.string.settings_reset_button),
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            text = stringResource(R.string.settings_sources),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val Difficulty.labelRes: Int
    get() = when (this) {
        Difficulty.EASY -> R.string.difficulty_easy
        Difficulty.MEDIUM -> R.string.difficulty_medium
        Difficulty.HARD -> R.string.difficulty_hard
    }

private val Difficulty.descriptionRes: Int
    get() = when (this) {
        Difficulty.EASY -> R.string.difficulty_easy_help
        Difficulty.MEDIUM -> R.string.difficulty_medium_help
        Difficulty.HARD -> R.string.difficulty_hard_help
    }

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    WordWiseTheme {
        SettingsContent(
            settings = UserSettings(),
            snackbarHostState = SnackbarHostState(),
            onDailyGoalChange = {},
            onDifficultyChange = {},
            onReadAloudChange = {},
            onResetProgress = {},
        )
    }
}
