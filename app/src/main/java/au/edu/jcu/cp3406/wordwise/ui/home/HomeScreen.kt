package au.edu.jcu.cp3406.wordwise.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.cp3406.wordwise.R
import au.edu.jcu.cp3406.wordwise.ui.theme.ButtonShape
import au.edu.jcu.cp3406.wordwise.ui.theme.WordWiseTheme
import au.edu.jcu.cp3406.wordwise.ui.theme.paperCardColors
import kotlin.math.roundToInt

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

/**
 * Stateless landing page. Today's goal is the one strong element on the screen; everything
 * else is a quiet shortcut. Once the goal is met, the main action becomes "see my progress",
 * never "keep going".
 */
@Composable
fun HomeContent(
    uiState: HomeUiState,
    onStartSession: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddWord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.home_brand),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.home_hero),
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GoalCard(uiState = uiState, onStartSession = onStartSession, onOpenStats = onOpenStats)

            Text(
                text = stringResource(R.string.home_space_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .semantics { heading() },
            )
            // Equal-weight shortcuts: none of them is pushed on the learner.
            ShortcutRow(
                icon = Icons.Outlined.LibraryAdd,
                title = stringResource(R.string.home_add_word_title),
                subtitle = stringResource(R.string.home_add_word_subtitle),
                onClick = onAddWord,
            )
            ShortcutRow(
                icon = Icons.Outlined.BarChart,
                title = stringResource(R.string.home_progress_title),
                subtitle = stringResource(R.string.home_progress_subtitle),
                onClick = onOpenStats,
            )
            ShortcutRow(
                icon = Icons.Outlined.Tune,
                title = stringResource(R.string.home_preferences_title),
                subtitle = stringResource(R.string.home_preferences_subtitle),
                onClick = onOpenSettings,
            )
        }
    }
}

@Composable
private fun GoalCard(
    uiState: HomeUiState,
    onStartSession: () -> Unit,
    onOpenStats: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.primary, contentColor = colors.onPrimary),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.home_goal_label),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.semantics { heading() },
            )
            when {
                uiState.isLoading -> {
                    Text(stringResource(R.string.home_loading))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.onPrimary,
                        trackColor = colors.onPrimary.copy(alpha = 0.25f),
                    )
                }
                uiState.loadFailed -> Text(stringResource(R.string.home_load_error))
                else -> GoalProgress(uiState)
            }
            Button(
                onClick = if (uiState.goalMet) onOpenStats else onStartSession,
                enabled = !uiState.isLoading && !uiState.loadFailed,
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.onPrimary,
                    contentColor = colors.primary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) {
                Text(
                    text = stringResource(
                        if (uiState.goalMet) R.string.home_shortcut_stats else R.string.home_start_session,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun GoalProgress(uiState: HomeUiState) {
    val colors = MaterialTheme.colorScheme
    val fraction = if (uiState.dailyGoal > 0) {
        (uiState.reviewedToday.toFloat() / uiState.dailyGoal).coerceIn(0f, 1f)
    } else {
        0f
    }
    val percent = (fraction * 100).roundToInt()
    val description = stringResource(
        R.string.home_goal_description,
        uiState.reviewedToday,
        uiState.dailyGoal,
        percent,
    )
    // TalkBack reads one clear sentence instead of "7", "/", "10" and a ring.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_goal_count, uiState.reviewedToday, uiState.dailyGoal),
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = stringResource(R.string.home_goal_unit),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { fraction },
                modifier = Modifier.size(72.dp),
                color = colors.onPrimary,
                trackColor = colors.onPrimary.copy(alpha = 0.25f),
                strokeWidth = 6.dp,
            )
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
    Text(
        text = stringResource(R.string.home_deck_summary, uiState.totalWords, uiState.wordsMastered),
        style = MaterialTheme.typography.bodySmall,
    )
    if (uiState.goalMet) {
        Text(
            text = stringResource(R.string.home_goal_met),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ShortcutRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = paperCardColors(),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.padding(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 1000)
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

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun HomeGoalMetPreview() {
    WordWiseTheme(darkTheme = true) {
        Surface {
            HomeContent(
                uiState = HomeUiState(isLoading = false, totalWords = 60, wordsMastered = 4, reviewedToday = 10, dailyGoal = 10),
                onStartSession = {},
                onOpenStats = {},
                onOpenSettings = {},
                onAddWord = {},
            )
        }
    }
}
