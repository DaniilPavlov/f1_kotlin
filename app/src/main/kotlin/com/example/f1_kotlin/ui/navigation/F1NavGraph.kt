package com.example.f1_kotlin.ui.navigation

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.f1_kotlin.data.analytics.AnalyticsEvent
import com.example.f1_kotlin.data.deeplink.DeepLinkTarget
import com.example.f1_kotlin.di.AppEntryPoint
import com.example.f1_kotlin.domain.model.Constructor
import com.example.f1_kotlin.domain.model.Driver
import com.example.f1_kotlin.ui.components.F1AppBar
import com.example.f1_kotlin.ui.components.LiveSessionBanner
import com.example.f1_kotlin.ui.screens.circuits.CircuitDetailScreen
import com.example.f1_kotlin.ui.screens.circuits.CircuitsScreen
import com.example.f1_kotlin.ui.screens.constructor.ConstructorDetailScreen
import com.example.f1_kotlin.ui.screens.driver.DriverDetailScreen
import com.example.f1_kotlin.ui.screens.finishstatus.FinishStatusScreen
import com.example.f1_kotlin.ui.screens.h2h.H2hMode
import com.example.f1_kotlin.ui.screens.h2h.H2hScreen
import com.example.f1_kotlin.ui.screens.halloffame.HallOfFameScreen
import com.example.f1_kotlin.ui.screens.home.HomeScreen
import com.example.f1_kotlin.ui.screens.predictor.PredictorLeaderboardScreen
import com.example.f1_kotlin.ui.screens.predictor.PredictorScreen
import com.example.f1_kotlin.ui.screens.predictor.PredictorSeasonHistoryScreen
import com.example.f1_kotlin.ui.screens.predictor.PredictorWeekendDetailScreen
import com.example.f1_kotlin.ui.screens.profile.AuthRegisterScreen
import com.example.f1_kotlin.ui.screens.profile.AuthSignInScreen
import com.example.f1_kotlin.ui.screens.profile.ProfileScreen
import com.example.f1_kotlin.ui.screens.results.RaceInfoScreen
import com.example.f1_kotlin.ui.screens.results.RaceSearchScreen
import com.example.f1_kotlin.ui.screens.results.ResultsScreen
import com.example.f1_kotlin.ui.screens.rewind.SeasonRewindScreen
import com.example.f1_kotlin.ui.screens.schedule.ScheduleScreen
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1Chrome
import com.example.f1_kotlin.ui.theme.F1OnChrome
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.ui.theme.appColors
import com.example.f1_kotlin.util.LocalShareActionSetter
import dagger.hilt.android.EntryPointAccessors
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.collectLatest

sealed class BottomTab(
    val route: Any,
    val routeClass: KClass<out Any>,
    val labelRes: Int,
    val iconRes: Int,
    val analyticsTab: String,
) {
    data object Home : BottomTab(
        route = com.example.f1_kotlin.ui.navigation.Home,
        routeClass = com.example.f1_kotlin.ui.navigation.Home::class,
        labelRes = com.example.f1_kotlin.R.string.nav_home,
        iconRes = com.example.f1_kotlin.R.drawable.nav_home,
        analyticsTab = "home",
    )
    data object Results : BottomTab(
        route = com.example.f1_kotlin.ui.navigation.Results,
        routeClass = com.example.f1_kotlin.ui.navigation.Results::class,
        labelRes = com.example.f1_kotlin.R.string.nav_results,
        iconRes = com.example.f1_kotlin.R.drawable.nav_racing_car,
        analyticsTab = "results",
    )
    data object Schedule : BottomTab(
        route = com.example.f1_kotlin.ui.navigation.Schedule,
        routeClass = com.example.f1_kotlin.ui.navigation.Schedule::class,
        labelRes = com.example.f1_kotlin.R.string.nav_calendar,
        iconRes = com.example.f1_kotlin.R.drawable.nav_lights,
        analyticsTab = "schedule",
    )
    data object Predictor : BottomTab(
        route = com.example.f1_kotlin.ui.navigation.Predictor,
        routeClass = com.example.f1_kotlin.ui.navigation.Predictor::class,
        labelRes = com.example.f1_kotlin.R.string.nav_predictor,
        iconRes = com.example.f1_kotlin.R.drawable.nav_trophy,
        analyticsTab = "predictor",
    )
    data object Profile : BottomTab(
        route = com.example.f1_kotlin.ui.navigation.Profile,
        routeClass = com.example.f1_kotlin.ui.navigation.Profile::class,
        labelRes = com.example.f1_kotlin.R.string.nav_profile,
        iconRes = com.example.f1_kotlin.R.drawable.nav_helmet,
        analyticsTab = "profile",
    )
}

private val tabs = listOf(
    BottomTab.Home,
    BottomTab.Results,
    BottomTab.Schedule,
    BottomTab.Predictor,
    BottomTab.Profile,
)

@Composable
fun F1App() {
    val context = LocalContext.current
    val entryPoint = rememberAppEntryPoint(context)
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = tabs.any { destination?.hasRoute(it.routeClass) == true }
    val popBack: () -> Unit = { navController.popBackStack() }
    var shareAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val liveController = remember { entryPoint.liveWeekendController() }
    val analytics = remember { entryPoint.analyticsGateway() }

    LaunchedEffect(Unit) {
        liveController.loadScoreboard()
        entryPoint.deepLinkBus().targets.collectLatest { target ->
            navigateDeepLink(navController, target, liveController.isLive)
        }
    }

    LaunchedEffect(destination?.route) {
        val route = destination?.route ?: return@LaunchedEffect
        val screenName = route.substringBefore('?').substringBefore('/')
            .substringAfterLast('.')
            .ifBlank { route }
        analytics.log(
            AnalyticsEvent.ScreenView(
                screenName = screenName,
                screenClass = destination.route,
            ),
        )
    }

    val onDriverClick: (Driver) -> Unit = { driver ->
        analytics.log(AnalyticsEvent.DriverOpened(driver.driverId, driver.fullName))
        navController.navigate(DriverDetail(driver.driverId))
    }
    val onConstructorClick: (Constructor) -> Unit = { constructor ->
        analytics.log(AnalyticsEvent.ConstructorOpened(constructor.constructorId, constructor.name))
        navController.navigate(ConstructorDetail(constructor.constructorId))
    }
    val onCircuitClick: (com.example.f1_kotlin.domain.model.Circuit) -> Unit = { circuit ->
        analytics.log(AnalyticsEvent.CircuitOpened(circuit.circuitId, circuit.circuitName))
        navController.navigate(CircuitDetail(circuit.circuitId))
    }

    CompositionLocalProvider(LocalShareActionSetter provides { shareAction = it }) {
        Scaffold(
            containerColor = appColors().white,
            topBar = {
                F1TopBar(
                    destination = destination,
                    backStackEntry = backStackEntry,
                    showBottomBar = showBottomBar,
                    popBack = popBack,
                    shareAction = shareAction,
                )
            },
            bottomBar = {
                if (showBottomBar) {
                    Column {
                        LiveSessionBanner(
                            controller = liveController,
                            onTap = {
                                navController.navigate(Results) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                        F1BottomBar(
                            currentDestination = destination,
                            onTabSelected = { tab ->
                                analytics.log(AnalyticsEvent.TabSwitched(tab.analyticsTab))
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            },
        ) { padding ->
            F1NavHost(
                navController = navController,
                onDriverClick = onDriverClick,
                onConstructorClick = onConstructorClick,
                onCircuitClick = onCircuitClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}

@Composable
private fun rememberAppEntryPoint(context: Context): AppEntryPoint {
    val app = context.applicationContext
    return remember(app) {
        EntryPointAccessors.fromApplication(app, AppEntryPoint::class.java)
    }
}

private fun navigateDeepLink(
    navController: NavHostController,
    target: DeepLinkTarget,
    isLive: Boolean,
) {
    when (target) {
        is DeepLinkTarget.Driver -> navController.navigate(DriverDetail(target.driverId))
        is DeepLinkTarget.Constructor -> navController.navigate(ConstructorDetail(target.constructorId))
        is DeepLinkTarget.Circuit -> navController.navigate(CircuitDetail(target.circuitId))
        DeepLinkTarget.RaceLive -> navController.navigate(Results) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        is DeepLinkTarget.Race -> {
            val route = if (isLive) Results else Schedule
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
}

@Suppress("CyclomaticComplexMethod")
@Composable
private fun F1TopBar(
    destination: NavDestination?,
    backStackEntry: androidx.navigation.NavBackStackEntry?,
    showBottomBar: Boolean,
    popBack: () -> Unit,
    shareAction: (() -> Unit)?,
) {
    when {
        showBottomBar && destination?.hasRoute<Profile>() == true -> F1AppBar(
            title = stringResource(com.example.f1_kotlin.R.string.profile_title),
        )
        showBottomBar && destination?.hasRoute<Predictor>() == true -> F1AppBar(
            title = stringResource(com.example.f1_kotlin.R.string.predictor_title),
        )
        showBottomBar -> F1AppBar()
        destination?.hasRoute<AuthSignIn>() == true -> F1AppBar(
            title = stringResource(com.example.f1_kotlin.R.string.auth_sign_in_title),
            onBack = popBack,
        )
        destination?.hasRoute<AuthRegister>() == true -> F1AppBar(
            title = stringResource(com.example.f1_kotlin.R.string.auth_register_title),
            onBack = popBack,
        )
        destination?.hasRoute<PredictorWeekendDetail>() == true -> F1AppBar(
            title = backStackEntry?.toRoute<PredictorWeekendDetail>()?.raceName
                ?.takeIf { it.isNotBlank() }
                ?: stringResource(com.example.f1_kotlin.R.string.predictor_title),
            onBack = popBack,
        )
        destination?.hasRoute<PredictorSeasonHistory>() == true -> F1AppBar(
            title = backStackEntry?.toRoute<PredictorSeasonHistory>()?.year
                ?: stringResource(com.example.f1_kotlin.R.string.predictor_history_title),
            onBack = popBack,
        )
        destination?.hasRoute<PredictorLeaderboard>() == true -> F1AppBar(
            title = stringResource(
                com.example.f1_kotlin.R.string.predictor_leaderboard_title,
                backStackEntry?.toRoute<PredictorLeaderboard>()?.year.orEmpty(),
            ),
            onBack = popBack,
        )
        destination?.hasRoute<RaceSearch>() == true -> F1AppBar(
            title = stringResource(com.example.f1_kotlin.R.string.race_search_title),
            onBack = popBack,
        )
        destination?.hasRoute<Circuits>() == true -> F1AppBar(
            title = stringResource(com.example.f1_kotlin.R.string.nav_circuits),
            onBack = popBack,
        )
        destination?.hasRoute<HallOfFame>() == true -> F1AppBar(
            title = stringResource(com.example.f1_kotlin.R.string.hall_of_fame_title),
            onBack = popBack,
        )
        destination?.hasRoute<SeasonRewind>() == true -> F1AppBar(
            title = stringResource(com.example.f1_kotlin.R.string.season_rewind_title),
            onBack = popBack,
        )
        destination?.hasRoute<H2hDrivers>() == true ||
            destination?.hasRoute<H2hConstructors>() == true -> F1AppBar(
            title = stringResource(com.example.f1_kotlin.R.string.h2h_title),
            onBack = popBack,
        )
        destination?.hasRoute<FinishStatus>() == true -> F1AppBar(
            title = stringResource(com.example.f1_kotlin.R.string.finish_status_title),
            onBack = popBack,
        )
        destination?.hasRoute<RaceInfo>() == true -> F1AppBar(
            title = stringResource(com.example.f1_kotlin.R.string.detailed_info),
            onBack = popBack,
            onShare = shareAction,
        )
        destination?.hasRoute<CircuitDetail>() == true -> F1AppBar(
            title = stringResource(com.example.f1_kotlin.R.string.circuit_info_title),
            onBack = popBack,
            onShare = shareAction,
        )
        destination?.hasRoute<DriverDetail>() == true -> F1AppBar(
            title = stringResource(com.example.f1_kotlin.R.string.driver),
            onBack = popBack,
            onShare = shareAction,
        )
        destination?.hasRoute<ConstructorDetail>() == true -> F1AppBar(
            title = stringResource(com.example.f1_kotlin.R.string.constructor),
            onBack = popBack,
            onShare = shareAction,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun F1NavHost(
    navController: NavHostController,
    onDriverClick: (Driver) -> Unit,
    onConstructorClick: (Constructor) -> Unit,
    onCircuitClick: (com.example.f1_kotlin.domain.model.Circuit) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Home,
        modifier = modifier,
    ) {
        composable<Home> {
            HomeScreen(
                viewModel = hiltViewModel(),
                newsViewModel = hiltViewModel(),
                onDriverClick = onDriverClick,
                onConstructorClick = onConstructorClick,
            )
        }
        composable<Results> {
            ResultsScreen(
                viewModel = hiltViewModel(),
                onSearchRace = { navController.navigate(RaceSearch) },
                onHallOfFame = { navController.navigate(HallOfFame) },
                onSeasonRewind = { navController.navigate(SeasonRewind) },
                onH2h = { navController.navigate(H2hDrivers) },
                onFinishStatus = { navController.navigate(FinishStatus) },
                onRaceDetails = { race ->
                    navController.navigate(RaceInfo(race.season, race.round))
                },
                onDriverClick = onDriverClick,
            )
        }
        composable<RaceSearch> {
            RaceSearchScreen(
                viewModel = hiltViewModel(),
                onRaceDetails = { race ->
                    navController.navigate(RaceInfo(race.season, race.round))
                },
                onDriverClick = onDriverClick,
            )
        }
        composable<HallOfFame> {
            HallOfFameScreen(
                viewModel = hiltViewModel(),
                onDriverClick = onDriverClick,
                onConstructorClick = onConstructorClick,
            )
        }
        composable<SeasonRewind> {
            SeasonRewindScreen(viewModel = hiltViewModel())
        }
        composable<H2hDrivers> {
            H2hScreen(
                driversViewModel = hiltViewModel(),
                constructorsViewModel = hiltViewModel(),
                initialMode = H2hMode.Drivers,
            )
        }
        composable<H2hConstructors> {
            H2hScreen(
                driversViewModel = hiltViewModel(),
                constructorsViewModel = hiltViewModel(),
                initialMode = H2hMode.Constructors,
            )
        }
        composable<FinishStatus> {
            FinishStatusScreen(viewModel = hiltViewModel())
        }
        composable<RaceInfo> {
            RaceInfoScreen(
                viewModel = hiltViewModel(),
                onDriverClick = onDriverClick,
            )
        }
        composable<Schedule> {
            ScheduleScreen(
                viewModel = hiltViewModel(),
                onCircuits = { navController.navigate(Circuits) },
            )
        }
        composable<Predictor> {
            PredictorScreen(
                viewModel = hiltViewModel(),
                onGoSignIn = { navController.navigate(AuthSignIn) },
                onGoProfile = {
                    navController.navigate(Profile) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenLeaderboard = { year, myPoints ->
                    navController.navigate(PredictorLeaderboard(year = year, myPoints = myPoints))
                },
                onOpenWeekend = { season, weekend ->
                    navController.navigate(
                        PredictorWeekendDetail(
                            season = season,
                            round = weekend.round,
                            raceName = weekend.raceName,
                        ),
                    )
                },
                onOpenSeason = { year ->
                    navController.navigate(PredictorSeasonHistory(year = year))
                },
            )
        }
        composable<PredictorWeekendDetail> {
            PredictorWeekendDetailScreen(
                viewModel = hiltViewModel(),
                onGoSignIn = {
                    navController.navigate(AuthSignIn) {
                        popUpTo(Predictor) { inclusive = false }
                    }
                },
                onBlocked = { navController.popBackStack() },
            )
        }
        composable<PredictorSeasonHistory> {
            PredictorSeasonHistoryScreen(
                viewModel = hiltViewModel(),
                onGoSignIn = {
                    navController.navigate(AuthSignIn) {
                        popUpTo(Predictor) { inclusive = false }
                    }
                },
                onBlocked = { navController.popBackStack() },
                onOpenWeekend = { season, weekend ->
                    navController.navigate(
                        PredictorWeekendDetail(
                            season = season,
                            round = weekend.round,
                            raceName = weekend.raceName,
                        ),
                    )
                },
            )
        }
        composable<PredictorLeaderboard> {
            PredictorLeaderboardScreen(
                viewModel = hiltViewModel(),
                onGoSignIn = {
                    navController.navigate(AuthSignIn) {
                        popUpTo(Predictor) { inclusive = false }
                    }
                },
                onBlocked = { navController.popBackStack() },
            )
        }
        composable<Profile> {
            ProfileScreen(
                viewModel = hiltViewModel(),
                onSignIn = { navController.navigate(AuthSignIn) },
            )
        }
        composable<AuthSignIn> {
            AuthSignInScreen(
                viewModel = hiltViewModel(),
                onSuccess = { navController.popBackStack() },
                onGoRegister = {
                    navController.navigate(AuthRegister) {
                        popUpTo(AuthSignIn) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<AuthRegister> {
            AuthRegisterScreen(
                viewModel = hiltViewModel(),
                onSuccess = { navController.popBackStack() },
                onGoSignIn = {
                    navController.navigate(AuthSignIn) {
                        popUpTo(AuthRegister) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<Circuits> {
            CircuitsScreen(
                viewModel = hiltViewModel(),
                onCircuitClick = { circuitId ->
                    navController.navigate(CircuitDetail(circuitId))
                },
            )
        }
        composable<CircuitDetail> {
            CircuitDetailScreen(
                viewModel = hiltViewModel(),
                onDriverClick = onDriverClick,
            )
        }
        composable<DriverDetail> {
            DriverDetailScreen(
                viewModel = hiltViewModel(),
                onConstructorClick = onConstructorClick,
                onCircuitClick = onCircuitClick,
            )
        }
        composable<ConstructorDetail> {
            ConstructorDetailScreen(
                viewModel = hiltViewModel(),
                onDriverClick = onDriverClick,
                onCircuitClick = onCircuitClick,
            )
        }
    }
}

@Composable
private fun F1BottomBar(
    currentDestination: NavDestination?,
    onTabSelected: (BottomTab) -> Unit,
) {
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(F1Red))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(F1Chrome)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            tabs.forEach { tab ->
                val label = stringResource(tab.labelRes)
                val selected = currentDestination?.hasRoute(tab.routeClass) == true
                val contentColor = if (selected) F1Red else F1OnChrome
                Column(
                    modifier = Modifier
                        .clickable { onTabSelected(tab) }
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(tab.iconRes),
                        contentDescription = label,
                        modifier = Modifier.size(28.dp),
                        colorFilter = ColorFilter.tint(contentColor),
                    )
                    Text(
                        text = label,
                        style = AppStyles.navBar.copy(color = contentColor),
                    )
                }
            }
        }
    }
}
