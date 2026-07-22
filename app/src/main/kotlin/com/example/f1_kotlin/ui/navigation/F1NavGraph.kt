package com.example.f1_kotlin.ui.navigation

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.f1_kotlin.domain.model.Constructor
import com.example.f1_kotlin.domain.model.Driver
import com.example.f1_kotlin.ui.components.F1AppBar
import com.example.f1_kotlin.ui.screens.circuits.CircuitDetailScreen
import com.example.f1_kotlin.ui.screens.circuits.CircuitsScreen
import com.example.f1_kotlin.ui.screens.constructor.ConstructorDetailScreen
import com.example.f1_kotlin.ui.screens.driver.DriverDetailScreen
import com.example.f1_kotlin.ui.screens.finishstatus.FinishStatusScreen
import com.example.f1_kotlin.ui.screens.h2h.H2hConstructorsScreen
import com.example.f1_kotlin.ui.screens.h2h.H2hDriversScreen
import com.example.f1_kotlin.ui.screens.halloffame.HallOfFameScreen
import com.example.f1_kotlin.ui.screens.home.HomeScreen
import com.example.f1_kotlin.ui.screens.news.NewsScreen
import com.example.f1_kotlin.ui.screens.results.RaceInfoScreen
import com.example.f1_kotlin.ui.screens.results.RaceSearchScreen
import com.example.f1_kotlin.ui.screens.results.ResultsScreen
import com.example.f1_kotlin.ui.screens.schedule.ScheduleScreen
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1Black
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.ui.theme.F1White
import com.example.f1_kotlin.util.LocalShareActionSetter
import kotlin.reflect.KClass

sealed class BottomTab(
    val route: Any,
    val routeClass: KClass<out Any>,
    val labelRes: Int,
    val iconRes: Int,
) {
    data object Home : BottomTab(
        route = com.example.f1_kotlin.ui.navigation.Home,
        routeClass = com.example.f1_kotlin.ui.navigation.Home::class,
        labelRes = com.example.f1_kotlin.R.string.nav_home,
        iconRes = com.example.f1_kotlin.R.drawable.nav_home,
    )
    data object Results : BottomTab(
        route = com.example.f1_kotlin.ui.navigation.Results,
        routeClass = com.example.f1_kotlin.ui.navigation.Results::class,
        labelRes = com.example.f1_kotlin.R.string.nav_results,
        iconRes = com.example.f1_kotlin.R.drawable.nav_racing_car,
    )
    data object Schedule : BottomTab(
        route = com.example.f1_kotlin.ui.navigation.Schedule,
        routeClass = com.example.f1_kotlin.ui.navigation.Schedule::class,
        labelRes = com.example.f1_kotlin.R.string.nav_calendar,
        iconRes = com.example.f1_kotlin.R.drawable.nav_lights,
    )
    data object News : BottomTab(
        route = com.example.f1_kotlin.ui.navigation.News,
        routeClass = com.example.f1_kotlin.ui.navigation.News::class,
        labelRes = com.example.f1_kotlin.R.string.nav_news,
        iconRes = com.example.f1_kotlin.R.drawable.nav_trophy,
    )
    data object Circuits : BottomTab(
        route = com.example.f1_kotlin.ui.navigation.Circuits,
        routeClass = com.example.f1_kotlin.ui.navigation.Circuits::class,
        labelRes = com.example.f1_kotlin.R.string.nav_circuits,
        iconRes = com.example.f1_kotlin.R.drawable.nav_circuit,
    )
}

private val tabs = listOf(
    BottomTab.Home,
    BottomTab.Results,
    BottomTab.Schedule,
    BottomTab.News,
    BottomTab.Circuits,
)

@Composable
fun F1App() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = tabs.any { destination?.hasRoute(it.routeClass) == true }
    val popBack: () -> Unit = { navController.popBackStack() }
    var shareAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val onDriverClick: (Driver) -> Unit = { driver ->
        navController.navigate(DriverDetail(driver.driverId))
    }
    val onConstructorClick: (Constructor) -> Unit = { constructor ->
        navController.navigate(ConstructorDetail(constructor.constructorId))
    }
    val onCircuitClick: (com.example.f1_kotlin.domain.model.Circuit) -> Unit = { circuit ->
        navController.navigate(CircuitDetail(circuit.circuitId))
    }

    CompositionLocalProvider(LocalShareActionSetter provides { shareAction = it }) {
        Scaffold(
            topBar = {
                F1TopBar(
                    destination = destination,
                    showBottomBar = showBottomBar,
                    popBack = popBack,
                    shareAction = shareAction,
                )
            },
            bottomBar = {
                if (showBottomBar) {
                    F1BottomBar(
                        currentDestination = destination,
                        onTabSelected = { tab ->
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
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
private fun F1TopBar(
    destination: NavDestination?,
    showBottomBar: Boolean,
    popBack: () -> Unit,
    shareAction: (() -> Unit)?,
) {
    when {
        showBottomBar -> F1AppBar()
        destination?.hasRoute<RaceSearch>() == true -> F1AppBar(
            title = stringResource(com.example.f1_kotlin.R.string.race_search_title),
            onBack = popBack,
        )
        destination?.hasRoute<HallOfFame>() == true -> F1AppBar(
            title = stringResource(com.example.f1_kotlin.R.string.hall_of_fame_title),
            onBack = popBack,
        )
        destination?.hasRoute<H2hDrivers>() == true -> F1AppBar(
            title = stringResource(com.example.f1_kotlin.R.string.h2h_title),
            onBack = popBack,
        )
        destination?.hasRoute<H2hConstructors>() == true -> F1AppBar(
            title = stringResource(com.example.f1_kotlin.R.string.h2h_constructors_title),
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
                onDriverClick = onDriverClick,
                onConstructorClick = onConstructorClick,
            )
        }
        composable<Results> {
            ResultsScreen(
                viewModel = hiltViewModel(),
                onSearchRace = { navController.navigate(RaceSearch) },
                onHallOfFame = { navController.navigate(HallOfFame) },
                onH2hDrivers = { navController.navigate(H2hDrivers) },
                onH2hConstructors = { navController.navigate(H2hConstructors) },
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
        composable<H2hDrivers> {
            H2hDriversScreen(viewModel = hiltViewModel())
        }
        composable<H2hConstructors> {
            H2hConstructorsScreen(viewModel = hiltViewModel())
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
        composable<Schedule> { ScheduleScreen(hiltViewModel()) }
        composable<News> {
            NewsScreen(viewModel = hiltViewModel())
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
                .background(F1Black)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            tabs.forEach { tab ->
                val label = stringResource(tab.labelRes)
                val selected = currentDestination?.hasRoute(tab.routeClass) == true
                val contentColor = if (selected) F1Red else F1White
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
