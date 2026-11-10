package ai.tireon.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
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
import ai.tireon.app.home.HomeViewModel

/**
 * 앱 전체 셸.
 *
 * 창 폭에 따라 하단 탭과 측면 레일을 바꾼다. 탭 전환은 백스택을 쌓지 않는다.
 */
@Composable
fun TireonApp(
    drivingViewModel: DrivingViewModel = viewModel(factory = viewModelFactory()),
    homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(
        ai.tireon.app.report.SessionRepositoryProvider.get(
            androidx.compose.ui.platform.LocalContext.current,
        ),
    )),
) {
    val driving by drivingViewModel.uiState.collectAsStateWithLifecycle()
    val homeState by homeViewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = androidx.compose.runtime.remember(context) {
        ai.tireon.app.report.SessionRepositoryProvider.get(context)
    }
    val reportViewModel: ai.tireon.app.report.ReportViewModel =
        viewModel(factory = ai.tireon.app.report.ReportViewModel.Factory(repository))
    val reportState by reportViewModel.state.collectAsStateWithLifecycle()

    val settingsViewModel: ai.tireon.app.settings.SettingsViewModel = viewModel(
        factory = ai.tireon.app.settings.SettingsViewModel.Factory(
            appVersion = ai.tireon.app.BuildConfig.VERSION_NAME,
            infoSource = ai.tireon.pleos.vehicle.PleosVehicleInfoSource(context),
        ),
    )
    val settingsInfo by settingsViewModel.info.collectAsStateWithLifecycle()

    val statsViewModel: ai.tireon.app.stats.StatsViewModel = viewModel(
        factory = ai.tireon.app.stats.StatsViewModel.Factory(repository),
    )
    val statsState by statsViewModel.state.collectAsStateWithLifecycle()

    // 설정에서 앱 소개를 다시 열 수 있다. 다시 볼 때는 완료 상태를 건드리지 않는다.
    var showOnboarding by rememberSaveable { mutableStateOf(false) }

    // 주행을 마치고 돌아오면 새 기록이 생긴다. 화면에 다시 들어올 때 읽어야
    // 통계와 목록이 최신 상태가 된다. ViewModel은 화면 전환에도 살아있으므로
    // 생성자에서 한 번 읽는 것만으로는 부족하다.
    LifecycleResumeEffect(Unit) {
        reportViewModel.refresh()
        statsViewModel.refresh()
        onPauseOrDispose { }
    }
    val launchPractice: (ai.tireon.core.mission.PracticeMission) -> Unit = { mission ->
        context.startActivity(
            ai.tireon.app.practice.PracticeActivity.intent(context, mission),
        )
    }
    val launchDriving: () -> Unit = {
        context.startActivity(
            android.content.Intent(context, ai.tireon.app.driving.DrivingSessionActivity::class.java),
        )
    }

    if (showOnboarding) {
        ai.tireon.app.onboarding.OnboardingScreen(
            onFinish = { showOnboarding = false },
            onClose = { showOnboarding = false },
        )
        return
    }

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
                            icon = {
                                Text(
                                    text = destination.glyph,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            },
                            label = { Text(text = stringResource(destination.labelRes)) },
                        )
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    TireonNavGraph(
                        navController = navController,
                        layout = layout,
                        driving = driving,
                        homeState = homeState,
                        drivingViewModel = drivingViewModel,
                        reportState = reportState,
                        settingsInfo = settingsInfo,
                        statsState = statsState,
                        onSelectRecord = reportViewModel::select,
                        onClearReportSelection = reportViewModel::clearSelection,
                        onStartPractice = launchPractice,
                        onStartDriving = launchDriving,
                        onSelectMission = launchPractice,
                        onShowOnboarding = { showOnboarding = true },
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
                                icon = {
                                    Text(
                                        text = destination.glyph,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                },
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
                        homeState = homeState,
                        drivingViewModel = drivingViewModel,
                        reportState = reportState,
                        settingsInfo = settingsInfo,
                        statsState = statsState,
                        onSelectRecord = reportViewModel::select,
                        onClearReportSelection = reportViewModel::clearSelection,
                        onStartPractice = launchPractice,
                        onStartDriving = launchDriving,
                        onSelectMission = launchPractice,
                        onShowOnboarding = { showOnboarding = true },
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
    homeState: HomeUiState,
    drivingViewModel: DrivingViewModel,
    reportState: ai.tireon.app.report.ReportUiState,
    settingsInfo: ai.tireon.app.settings.SettingsInfo,
    statsState: ai.tireon.app.stats.StatsUiState,
    onSelectRecord: (ai.tireon.core.report.DrivingSessionRecord) -> Unit,
    onClearReportSelection: () -> Unit,
    onStartPractice: (ai.tireon.core.mission.PracticeMission) -> Unit,
    onStartDriving: () -> Unit,
    onSelectMission: (ai.tireon.core.mission.PracticeMission) -> Unit,
    onShowOnboarding: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = TireonDestination.HOME.route,
    ) {
        composable(TireonDestination.HOME.route) {
            HomeScreen(
                state = homeState,
                driving = driving,
                layout = layout,
                onStartPractice = onStartPractice,
                onStartDriving = onStartDriving,
                onRefresh = drivingViewModel::refresh,
            )
        }
        composable(TireonDestination.PRACTICE.route) {
            PracticeEntryScreen(onSelectMission = onSelectMission)
        }
        composable(TireonDestination.REPORT.route) {
            val selectedDetail = reportState.selectedDetail
            if (selectedDetail != null) {
                ai.tireon.app.report.ReportDetailScreen(
                    detail = selectedDetail,
                    onBack = onClearReportSelection,
                )
            } else {
                ai.tireon.app.report.ReportScreen(
                    state = reportState,
                    statsDisplay = statsState.display,
                    onSelectRecord = onSelectRecord,
                )
            }
        }
        composable(TireonDestination.SETTINGS.route) {
            ai.tireon.app.settings.SettingsScreen(
                info = settingsInfo,
                onShowOnboarding = onShowOnboarding,
            )
        }
    }
}
