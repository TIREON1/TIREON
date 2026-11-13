package ai.tireon.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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

/** 주행 미션 선택 화면 경로. 탭이 아니라 홈에서만 들어간다. */
private const val DRIVING_MISSION_ROUTE = "driving-mission"

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

    // 차량 세션은 ViewModel이 처음 만들어질 때 한 번만 연다. 재구성마다 만들면
    // 쓰이지 않는 세션 객체가 계속 생긴다.
    val settingsFactory = androidx.compose.runtime.remember(context) {
        ai.tireon.app.settings.SettingsViewModel.Factory(
            appVersion = ai.tireon.app.BuildConfig.VERSION_NAME,
            infoSource = { ai.tireon.pleos.vehicle.PleosVehicleInfoSource(context.applicationContext) },
        )
    }
    val settingsViewModel: ai.tireon.app.settings.SettingsViewModel = viewModel(
        factory = settingsFactory,
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
        homeViewModel.refresh()
        reportViewModel.refresh()
        statsViewModel.refresh()
        settingsViewModel.load()
        onPauseOrDispose { }
    }
    val launchPractice: (ai.tireon.core.mission.PracticeMission) -> Unit = { mission ->
        context.startActivity(
            ai.tireon.app.practice.PracticeActivity.intent(context, mission),
        )
    }
    val startDriving: (ai.tireon.core.mission.DrivingMission?) -> Unit = { mission ->
        context.startActivity(
            ai.tireon.app.driving.DrivingSessionActivity.intent(context, mission),
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
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    header = { BrandMark(modifier = Modifier.padding(vertical = 24.dp)) },
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    destinations.forEach { destination ->
                        val selected = currentRoute?.hierarchy?.any {
                            it.route == destination.route
                        } == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigate(destination) },
                            icon = { DestinationIcon(destination) },
                            label = { Text(text = stringResource(destination.labelRes)) },
                            modifier = Modifier.padding(vertical = 12.dp),
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
                        onStartDriving = { navController.navigate(DRIVING_MISSION_ROUTE) },
                        onSelectDrivingMission = startDriving,
                        onSelectMission = launchPractice,
                        onShowOnboarding = { showOnboarding = true },
                    )
                }
            }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                        destinations.forEach { destination ->
                            val selected = currentRoute?.hierarchy?.any {
                                it.route == destination.route
                            } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigate(destination) },
                                icon = { DestinationIcon(destination) },
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
                        onStartDriving = { navController.navigate(DRIVING_MISSION_ROUTE) },
                        onSelectDrivingMission = startDriving,
                        onSelectMission = launchPractice,
                        onShowOnboarding = { showOnboarding = true },
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationIcon(destination: TireonDestination) {
    Icon(
        painter = painterResource(destination.iconRes),
        contentDescription = null,
        modifier = Modifier.size(36.dp),
    )
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
    onSelectDrivingMission: (ai.tireon.core.mission.DrivingMission?) -> Unit,
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
        composable(DRIVING_MISSION_ROUTE) {
            DrivingMissionEntryScreen(
                onSelect = { mission ->
                    onSelectDrivingMission(mission)
                    navController.popBackStack()
                },
            )
        }
        composable(TireonDestination.REPORT.route) {
            val selectedDetail = reportState.selectedDetail
            // 상세는 별도 목적지가 아니므로 시스템 뒤로가기도 목록으로 돌려보낸다.
            BackHandler(enabled = selectedDetail != null) {
                onClearReportSelection()
            }
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
