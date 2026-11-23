package au.edu.jcu.cp3406.wordwise.ui.stats

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import au.edu.jcu.cp3406.wordwise.R
import au.edu.jcu.cp3406.wordwise.ui.components.PlaceholderScreen

/** User statistics screen: progress and learning metrics. Built out in Step 4. */
@Composable
fun StatsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.nav_stats),
        message = stringResource(R.string.stats_placeholder),
        modifier = modifier,
    )
}
