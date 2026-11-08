package ai.tireon.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ai.tireon.app.R
import ai.tireon.app.driving.DrivingViewModel
import ai.tireon.app.home.HomeScreen
import ai.tireon.app.home.HomeUiState

/**
 * 앱 전체 셸.
 *
 * 창 폭에 따라 하단 탭과 측면 레일을 바꾼다. 탭 전환은 백스택을 쌓지 않는다.
 */
@Composable
fun TireonApp(
    drivingViewModel: DrivingViewModel = viewModel(factory = viewModelFactory()),
) {
    val driving by drivingViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val layout = WindowLayoutPolicy.of(maxWidth.value.toInt())

        val destinations = TireonDestination.entries
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination

        val navigate: (TireonDestination) -> Unit = { destination ->
            navController.navigate(destination.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        if (layout == WindowLayout.EXPANDED) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail {
                    destinations.forEach { destination ->
                        val selected = currentRoute?.hierarchy?.any {
                            it.route == destination.route
                        } == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigate(destination) },
                            icon = { },
                            label = { Text(text = stringResource(destination.labelRes)) },
                        )
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    TireonNavGraph(
                        navController = navController,
                        layout = layout,
                        driving = driving,
                        drivingViewModel = drivingViewModel,
                    )
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        destinations.forEach { destination ->
                            val selected = currentRoute?.hierarchy?.any {
                                it.route == destination.route
                            } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigate(destination) },
                                icon = { },
                                label = { Text(text = stringResource(destination.labelRes)) },
                            )
                        }
                    }
                },
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    TireonNavGraph(
                        navController = navController,
                        layout = layout,
                        driving = driving,
                        drivingViewModel = drivingViewModel,
                    )
                }
            }
        }
    }
}

@Composable
private fun TireonNavGraph(
    navController: androidx.navigation.NavHostController,
    layout: WindowLayout,
    driving: ai.tireon.app.driving.DrivingUiState,
    drivingViewModel: DrivingViewModel,
) {
    NavHost(
        navController = navController,
        startDestination = TireonDestination.HOME.route,
    ) {
        composable(TireonDestination.HOME.route) {
            HomeScreen(
                state = HomeUiState.empty(),
                driving = driving,
                layout = layout,
                onStartPractice = {
                    navController.navigate(TireonDestination.PRACTICE.route) {
                        launchSingleTop = true
                    }
                },
                onRefresh = drivingViewModel::refresh,
            )
        }
        composable(TireonDestination.PRACTICE.route) {
            PlaceholderScreen(
                title = stringResource(R.string.practice_title),
                description = stringResource(R.string.practice_description),
            )
        }
        composable(TireonDestination.REPORT.route) {
            PlaceholderScreen(
                title = stringResource(R.string.report_title),
                description = stringResource(R.string.report_description),
            )
        }
        composable(TireonDestination.SETTINGS.route) {
            PlaceholderScreen(
                title = stringResource(R.string.settings_title),
                description = stringResource(R.string.settings_description),
            )
        }
    }
}
