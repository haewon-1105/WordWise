package au.edu.jcu.cp3406.wordwise.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import au.edu.jcu.cp3406.wordwise.R
import kotlinx.serialization.Serializable

/*
 * Type-safe navigation routes (Navigation 2.8+). Each route is a @Serializable object, so the
 * compiler checks every navigate() call instead of relying on string routes.
 */
@Serializable
data object HomeRoute

@Serializable
data object StudyRoute

@Serializable
data object StatsRoute

@Serializable
data object SettingsRoute

/** Not a tab: opened from Home and closed with the back button. */
@Serializable
data object AddWordRoute

/** The four core screens shown in the bottom navigation bar, in display order. */
enum class TopLevelDestination(
    val route: Any,
    val icon: ImageVector,
    @StringRes val labelRes: Int,
) {
    HOME(HomeRoute, Icons.Filled.Home, R.string.nav_home),
    STUDY(StudyRoute, Icons.Filled.School, R.string.nav_study),
    STATS(StatsRoute, Icons.Filled.BarChart, R.string.nav_stats),
    SETTINGS(SettingsRoute, Icons.Filled.Settings, R.string.nav_settings),
}
