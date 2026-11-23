package au.edu.jcu.cp3406.wordwise.ui.study

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import au.edu.jcu.cp3406.wordwise.R
import au.edu.jcu.cp3406.wordwise.ui.components.PlaceholderScreen

/** Activity screen: flashcard study session. Built out in Step 4. */
@Composable
fun StudyScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.nav_study),
        message = stringResource(R.string.study_placeholder),
        modifier = modifier,
    )
}
