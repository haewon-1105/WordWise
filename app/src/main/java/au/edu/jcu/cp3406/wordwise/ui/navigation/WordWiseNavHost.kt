package au.edu.jcu.cp3406.wordwise.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import au.edu.jcu.cp3406.wordwise.ui.addword.AddWordScreen
import au.edu.jcu.cp3406.wordwise.ui.home.HomeScreen
import au.edu.jcu.cp3406.wordwise.ui.settings.SettingsScreen
import au.edu.jcu.cp3406.wordwise.ui.stats.StatsScreen
import au.edu.jcu.cp3406.wordwise.ui.study.StudyScreen

/**
 * Root of the UI: a Scaffold with a bottom navigation bar and a NavHost holding the four
 * core screens. Screens receive navigation callbacks instead of the NavController, which keeps
 * them testable in isolation.
 */
@Composable
fun WordWiseNavHost(
    navController: NavHostController = rememberNavController(),
) {
    Scaffold(
        bottomBar = { WordWiseBottomBar(navController) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onStartSession = { navController.navigateToTopLevel(TopLevelDestination.STUDY) },
                    onOpenStats = { navController.navigateToTopLevel(TopLevelDestination.STATS) },
                    onOpenSettings = { navController.navigateToTopLevel(TopLevelDestination.SETTINGS) },
                    onAddWord = { navController.navigate(AddWordRoute) },
                )
            }
            composable<AddWordRoute> {
                AddWordScreen(onBack = { navController.popBackStack() })
            }
            composable<StudyRoute> {
                StudyScreen(
                    onOpenStats = { navController.navigateToTopLevel(TopLevelDestination.STATS) },
                    onOpenSettings = { navController.navigateToTopLevel(TopLevelDestination.SETTINGS) },
                    onAddWord = { navController.navigate(AddWordRoute) },
                )
            }
            composable<StatsRoute> { StatsScreen() }
            composable<SettingsRoute> { SettingsScreen() }
        }
    }
}

@Composable
private fun WordWiseBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any {
                it.hasRoute(destination.route::class)
            } == true
            val label = stringResource(destination.labelRes)
            NavigationBarItem(
                selected = selected,
                onClick = { navController.navigateToTopLevel(destination) },
                // The visible label already names the item, so the icon is decorative for TalkBack.
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(label) },
            )
        }
    }
}

/**
 * Standard top-level navigation: pop back to the start destination, keep one copy of each
 * screen and restore its state when the user returns to that tab.
 */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
