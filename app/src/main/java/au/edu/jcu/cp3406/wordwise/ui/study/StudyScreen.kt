package au.edu.jcu.cp3406.wordwise.ui.study

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.cp3406.wordwise.R
import au.edu.jcu.cp3406.wordwise.model.AnswerFeedback
import au.edu.jcu.cp3406.wordwise.model.Flashcard
import au.edu.jcu.cp3406.wordwise.model.StudySession
import au.edu.jcu.cp3406.wordwise.model.Word
import au.edu.jcu.cp3406.wordwise.model.WordSource
import au.edu.jcu.cp3406.wordwise.ui.components.Sprout
import au.edu.jcu.cp3406.wordwise.ui.theme.ButtonShape
import au.edu.jcu.cp3406.wordwise.ui.theme.WordWiseTheme

/** Stateful entry point: the activity screen where flashcards are studied. */
@Composable
fun StudyScreen(
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddWord: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Each time the screen is shown, pick up a new day, a changed goal or a reset.
    LaunchedEffect(Unit) { viewModel.refreshIfIdle() }

    StudyContent(
        uiState = uiState,
        onFlip = viewModel::flip,
        onAnswer = viewModel::answer,
        onPlayPronunciation = viewModel::playPronunciation,
        onRetry = viewModel::refreshIfIdle,
        onOpenStats = onOpenStats,
        onOpenSettings = onOpenSettings,
        onAddWord = onAddWord,
        modifier = modifier,
    )
}

@Composable
fun StudyContent(
    uiState: StudyUiState,
    onFlip: () -> Unit,
    onAnswer: (Boolean) -> Unit,
    onPlayPronunciation: () -> Unit,
    onRetry: () -> Unit,
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
        // Keep the card a comfortable width on tablets and in landscape.
        Column(
            modifier = Modifier.widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (uiState) {
                StudyUiState.Loading -> Box(
                    Modifier.fillMaxWidth().padding(top = 48.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                StudyUiState.LoadFailed -> {
                    Text(stringResource(R.string.study_load_failed))
                    Button(shape = ButtonShape, onClick = onRetry) { Text(stringResource(R.string.action_try_again)) }
                }

                is StudyUiState.InSession -> SessionContent(
                    state = uiState,
                    onFlip = onFlip,
                    onAnswer = onAnswer,
                    onPlayPronunciation = onPlayPronunciation,
                )

                is StudyUiState.Finished -> FinishedContent(
                    state = uiState,
                    onOpenStats = onOpenStats,
                    onOpenSettings = onOpenSettings,
                    onAddWord = onAddWord,
                )
            }
        }
    }
}

@Composable
private fun SessionContent(
    state: StudyUiState.InSession,
    onFlip: () -> Unit,
    onAnswer: (Boolean) -> Unit,
    onPlayPronunciation: () -> Unit,
) {
    val session = state.session
    val card = session.current ?: return

    // Optional read-aloud, only when the learner switched it on in Settings.
    LaunchedEffect(card.word.id, state.readWordsAloud) {
        if (state.readWordsAloud) onPlayPronunciation()
    }

    // Progress toward the learner's own goal: always a fixed, finite number of cards.
    Text(
        text = stringResource(R.string.study_progress, session.answeredCount + 1, session.total),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
    LinearProgressIndicator(
        progress = { session.progress },
        modifier = Modifier.fillMaxWidth(),
    )
    state.lastFeedback?.let { FeedbackLine(it) }

    Flashcard(card = card, isFlipped = session.isFlipped, onFlip = onFlip)

    FilledTonalButton(shape = ButtonShape, onClick = onPlayPronunciation) {
        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.add_word_play), modifier = Modifier.padding(start = 8.dp))
    }

    if (session.isFlipped) {
        Text(
            text = stringResource(R.string.study_self_check),
            style = MaterialTheme.typography.bodyMedium,
        )
        // Both answers look the same: neither is pushed on the learner.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                shape = ButtonShape,
                onClick = { onAnswer(false) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.study_still_learning)) }
            OutlinedButton(
                shape = ButtonShape,
                onClick = { onAnswer(true) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.study_knew_it)) }
        }
    } else {
        Text(
            text = stringResource(R.string.study_think_first),
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(shape = ButtonShape, onClick = onFlip, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.study_show_meaning))
        }
    }
}

/** Says where the last card went, so spaced repetition is transparent, not a black box. */
@Composable
private fun FeedbackLine(feedback: AnswerFeedback) {
    val text = if (feedback.correct) {
        pluralStringResource(
            R.plurals.study_feedback_correct,
            feedback.daysUntilNextReview,
            feedback.box,
            feedback.daysUntilNextReview,
        )
    } else {
        stringResource(R.string.study_feedback_again)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.Schedule,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 6.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/** A card that turns over with a 3D flip. Tapping it, or the button below, shows the meaning. */
@Composable
private fun Flashcard(card: Flashcard, isFlipped: Boolean, onFlip: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "cardFlip",
    )
    val showBack = rotation > 90f
    val sideDescription = stringResource(
        if (isFlipped) R.string.study_card_meaning_side else R.string.study_card_word_side,
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable(
                onClickLabel = stringResource(R.string.study_turn_card),
                role = Role.Button,
                onClick = onFlip,
            )
            .semantics { stateDescription = sideDescription },
        colors = CardDefaults.cardColors(
            containerColor = if (showBack) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 260.dp)
                // Counter-rotate the back so its text is not mirrored.
                .graphicsLayer { if (showBack) rotationY = 180f }
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showBack) CardBack(card.word) else CardFront(card)
        }
    }
}

@Composable
private fun CardFront(card: Flashcard) {
    val word = card.word
    if (card.isNew) {
        Text(
            text = stringResource(R.string.study_new_word),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
    Text(
        text = word.term,
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        textAlign = TextAlign.Center,
    )
    if (word.partOfSpeech.isNotEmpty()) {
        Text(
            text = word.partOfSpeech,
            style = MaterialTheme.typography.bodyLarge,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
    word.phonetic?.let {
        Text(text = it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun CardBack(word: Word) {
    Text(
        text = word.definition,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        textAlign = TextAlign.Center,
    )
    word.example?.let {
        Text(
            text = stringResource(R.string.add_word_example, it),
            style = MaterialTheme.typography.bodyLarge,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FinishedContent(
    state: StudyUiState.Finished,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddWord: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.goalMet) {
            Sprout()
        } else {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = stringResource(if (state.goalMet) R.string.study_done_title else R.string.study_nothing_due_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = if (state.goalMet) {
                stringResource(R.string.study_done_body, state.reviewedToday, state.dailyGoal)
            } else {
                stringResource(R.string.study_nothing_due_body)
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (state.goalMet) {
            Text(
                text = stringResource(R.string.study_done_tagline),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (state.answeredThisSession > 0) {
            Text(
                text = stringResource(
                    R.string.study_session_summary,
                    state.correctThisSession,
                    state.answeredThisSession,
                ),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
        Button(shape = ButtonShape, onClick = onOpenStats, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_shortcut_stats))
        }
        // The app does not offer "keep going"; the learner can change their own goal instead.
        OutlinedButton(shape = ButtonShape, onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_shortcut_settings))
        }
        if (!state.goalMet) {
            TextButton(onClick = onAddWord) { Text(stringResource(R.string.home_add_word)) }
        }
    }
}

private val previewCard = Flashcard(
    word = Word(
        id = 1,
        term = "hypothesis",
        partOfSpeech = "noun",
        definition = "A proposed explanation that can be tested.",
        example = "The experiment supported the hypothesis.",
        level = 3,
        source = WordSource.USER,
        phonetic = "/haɪˈpɒθəsɪs/",
    ),
    progress = null,
)

@Preview(showBackground = true)
@Composable
private fun StudyFrontPreview() {
    WordWiseTheme {
        StudyContent(
            uiState = StudyUiState.InSession(
                session = StudySession(listOf(previewCard, previewCard.copy(word = previewCard.word.copy(id = 2)))),
                lastFeedback = AnswerFeedback(correct = true, box = 3, daysUntilNextReview = 4),
                readWordsAloud = false,
            ),
            onFlip = {}, onAnswer = {}, onPlayPronunciation = {}, onRetry = {},
            onOpenStats = {}, onOpenSettings = {}, onAddWord = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StudyDonePreview() {
    WordWiseTheme {
        StudyContent(
            uiState = StudyUiState.Finished(
                answeredThisSession = 10,
                correctThisSession = 8,
                reviewedToday = 10,
                dailyGoal = 10,
            ),
            onFlip = {}, onAnswer = {}, onPlayPronunciation = {}, onRetry = {},
            onOpenStats = {}, onOpenSettings = {}, onAddWord = {},
        )
    }
}
