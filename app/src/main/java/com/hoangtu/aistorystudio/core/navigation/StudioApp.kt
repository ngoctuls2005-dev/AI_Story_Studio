package com.hoangtu.aistorystudio.core.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hoangtu.aistorystudio.feature.characters.CharactersScreen
import com.hoangtu.aistorystudio.feature.characters.CharacterManagementViewModel
import com.hoangtu.aistorystudio.feature.home.HomeScreen
import com.hoangtu.aistorystudio.feature.library.LibraryScreen
import com.hoangtu.aistorystudio.feature.novel.NovelScreen
import com.hoangtu.aistorystudio.feature.roleplay.RoleplayScreen
import com.hoangtu.aistorystudio.feature.settings.SettingsScreen
import com.hoangtu.aistorystudio.feature.settings.SettingsViewModel

@Composable
fun StudioApp(
    settingsViewModel: SettingsViewModel,
    characterViewModel: CharacterManagementViewModel
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: StudioDestination.HOME.route

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useNavigationRail) {
                    StudioNavigationBar(
                        currentRoute = currentRoute,
                        onDestinationSelected = navController::navigateToTopLevel
                    )
                }
            }
        ) { contentPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                if (useNavigationRail) {
                    StudioNavigationRail(
                        currentRoute = currentRoute,
                        onDestinationSelected = navController::navigateToTopLevel
                    )
                }
                StudioNavHost(
                    navController = navController,
                    settingsViewModel = settingsViewModel,
                    characterViewModel = characterViewModel,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun StudioNavigationBar(
    currentRoute: String,
    onDestinationSelected: (StudioDestination) -> Unit
) {
    BoxWithConstraints {
        val showLabels = maxWidth >= 360.dp
        NavigationBar {
            StudioDestination.entries.forEach { destination ->
                NavigationBarItem(
                    modifier = Modifier.testTag("navigation_" + destination.route),
                    selected = currentRoute == destination.route,
                    onClick = { onDestinationSelected(destination) },
                    icon = {
                        androidx.compose.material3.Icon(
                            imageVector = destination.icon,
                            contentDescription = if (showLabels) null else destination.label
                        )
                    },
                    label = if (showLabels) {
                        {
                            Text(
                                text = destination.label,
                                maxLines = 1,
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                            )
                        }
                    } else {
                        null
                    },
                    alwaysShowLabel = showLabels
                )
            }
        }
    }
}

@Composable
private fun StudioNavigationRail(
    currentRoute: String,
    onDestinationSelected: (StudioDestination) -> Unit
) {
    NavigationRail {
        StudioDestination.entries.forEach { destination ->
            NavigationRailItem(
                modifier = Modifier.testTag("navigation_" + destination.route),
                selected = currentRoute == destination.route,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    androidx.compose.material3.Icon(
                        imageVector = destination.icon,
                        contentDescription = null
                    )
                },
                label = { Text(destination.label) },
                alwaysShowLabel = true
            )
        }
    }
}

@Composable
private fun StudioNavHost(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    characterViewModel: CharacterManagementViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = StudioDestination.HOME.route,
        modifier = modifier
    ) {
        composable(StudioDestination.HOME.route) {
            HomeScreen(onNavigate = navController::navigateToTopLevel)
        }
        composable(StudioDestination.ROLEPLAY.route) {
            RoleplayScreen(
                onOpenCharacters = {
                    navController.navigateToTopLevel(StudioDestination.CHARACTERS)
                }
            )
        }
        composable(StudioDestination.NOVEL.route) {
            NovelScreen()
        }
        composable(StudioDestination.CHARACTERS.route) {
            CharactersScreen(characterViewModel)
        }
        composable(StudioDestination.LIBRARY.route) {
            LibraryScreen()
        }
        composable(StudioDestination.SETTINGS.route) {
            SettingsScreen(settingsViewModel)
        }
    }
}

private fun NavHostController.navigateToTopLevel(destination: StudioDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
