package au.edu.jcu.cp3406.wordwise.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import au.edu.jcu.cp3406.wordwise.R
import au.edu.jcu.cp3406.wordwise.ui.components.PlaceholderScreen

/** Settings screen: daily goal, difficulty and sound. Built out in Step 4. */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.nav_settings),
        message = stringResource(R.string.settings_placeholder),
        modifier = modifier,
    )
}
