package au.edu.jcu.cp3406.wordwise.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.cp3406.wordwise.R
import au.edu.jcu.cp3406.wordwise.model.DailyReviewCount
import au.edu.jcu.cp3406.wordwise.model.LearningStats
import au.edu.jcu.cp3406.wordwise.ui.theme.WordWiseTheme
import au.edu.jcu.cp3406.wordwise.ui.theme.paperCardColors
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** Stateful entry point: the user statistics screen. */
@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StatsContent(uiState = uiState, modifier = modifier)
}

/**
 * Personal progress only: no rankings or comparisons with other learners. Every chart has a
 * text equivalent for TalkBack, and nothing relies on colour alone.
 */
@Composable
fun StatsContent(uiState: StatsUiState, modifier: Modifier = Modifier) {
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
                text = stringResource(R.string.nav_stats),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            when (uiState) {
                StatsUiState.Loading -> CircularProgressIndicator()
                is StatsUiState.Ready -> {
                    if (uiState.stats.totalReviews == 0) {
                        Text(
                            text = stringResource(R.string.stats_empty),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    SummaryTiles(uiState.stats)
                    WeekChart(uiState.stats.lastSevenDays, uiState.dailyGoal)
                    BoxChart(uiState.stats)
                    Text(
                        text = stringResource(R.string.stats_privacy),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryTiles(stats: LearningStats) {
    val accuracyText = stats.accuracy?.let { "${(it * 100).roundToInt()}%" }
        ?: stringResource(R.string.stats_no_value)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                value = "${stats.wordsMastered}",
                label = stringResource(R.string.stats_mastered, stats.totalWords),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = accuracyText,
                label = stringResource(R.string.stats_accuracy, stats.correctReviews, stats.totalReviews),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                value = "${stats.currentStreakDays}",
                label = pluralStringResource(R.plurals.stats_streak, stats.currentStreakDays),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = "${stats.wordsStarted}",
                label = stringResource(R.string.stats_started),
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = stringResource(R.string.stats_streak_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$value, $label" },
        colors = paperCardColors(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Reviews per day for the last week, with the learner's goal as a reference line. */
@Composable
private fun WeekChart(days: List<DailyReviewCount>, dailyGoal: Int) {
    val maxValue = (days.maxOfOrNull { it.count } ?: 0).coerceAtLeast(dailyGoal).coerceAtLeast(1)
    val chartHeight = 140.dp
    val locale = Locale.getDefault()
    val summary = days.joinToString("; ") { day ->
        val name = LocalDate.ofEpochDay(day.epochDay).dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        "$name ${day.count}"
    }
    val description = stringResource(R.string.stats_week_description, dailyGoal, summary)

    Card(Modifier.fillMaxWidth(), colors = paperCardColors()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.stats_week_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            // The bars are drawn for sighted users; TalkBack reads the same numbers as text.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { contentDescription = description },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                days.forEach { day ->
                    val date = LocalDate.ofEpochDay(day.epochDay)
                    val reachedGoal = day.count >= dailyGoal && dailyGoal > 0
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = "${day.count}", style = MaterialTheme.typography.labelMedium)
                        Box(
                            modifier = Modifier.height(chartHeight).fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(0.7f)
                                    .fillMaxHeight((day.count.toFloat() / maxValue).coerceAtLeast(0.02f))
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (reachedGoal) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.tertiary
                                        },
                                    ),
                            )
                        }
                        Text(
                            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.stats_week_legend, dailyGoal),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** How many words sit in each Leitner box: the shape of long-term memory. */
@Composable
private fun BoxChart(stats: LearningStats) {
    val maxCount = (stats.wordsPerBox.maxOrNull() ?: 0).coerceAtLeast(1)
    Card(Modifier.fillMaxWidth(), colors = paperCardColors()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.stats_boxes_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.stats_boxes_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            stats.wordsPerBox.forEachIndexed { index, count ->
                val box = index + 1
                val label = pluralStringResource(R.plurals.stats_box_row, count, box, count)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clearAndSetSemantics { contentDescription = label },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.stats_box_label, box),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(56.dp),
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        if (count > 0) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(count.toFloat() / maxCount)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        }
                    }
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(32.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun StatsPreview() {
    val today = LocalDate.of(2026, 11, 25)
    WordWiseTheme {
        StatsContent(
            uiState = StatsUiState.Ready(
                stats = LearningStats(
                    totalWords = 61,
                    wordsStarted = 24,
                    wordsMastered = 3,
                    wordsPerBox = listOf(8, 6, 4, 3, 3),
                    totalReviews = 52,
                    correctReviews = 39,
                    accuracy = 0.75,
                    reviewsToday = 10,
                    currentStreakDays = 4,
                    lastSevenDays = listOf(0, 6, 10, 0, 12, 8, 10).mapIndexed { i, c ->
                        DailyReviewCount(today.minusDays(6L - i).toEpochDay(), c)
                    },
                ),
                dailyGoal = 10,
            ),
        )
    }
}
