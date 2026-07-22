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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.f1_kotlin.data.model.ConstructorModel
import com.example.f1_kotlin.data.model.DriverModel
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

sealed class BottomTab(val route: String, val labelRes: Int, val iconRes: Int) {
    data object Home : BottomTab("home", com.example.f1_kotlin.R.string.nav_home, com.example.f1_kotlin.R.drawable.nav_home)
    data object Results : BottomTab("results", com.example.f1_kotlin.R.string.nav_results, com.example.f1_kotlin.R.drawable.nav_racing_car)
    data object Schedule : BottomTab("schedule", com.example.f1_kotlin.R.string.nav_calendar, com.example.f1_kotlin.R.drawable.nav_lights)
    data object News : BottomTab("news", com.example.f1_kotlin.R.string.nav_news, com.example.f1_kotlin.R.drawable.nav_trophy)
    data object Circuits : BottomTab("circuits", com.example.f1_kotlin.R.string.nav_circuits, com.example.f1_kotlin.R.drawable.nav_circuit)
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
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in tabs.map { it.route }
    val popBack: () -> Unit = { navController.popBackStack() }
    var shareAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val onDriverClick: (DriverModel) -> Unit = { driver ->
        navController.navigate("driver/${driver.driverId}")
    }
    val onConstructorClick: (ConstructorModel) -> Unit = { constructor ->
        navController.navigate("constructor/${constructor.constructorId}")
    }
    val onCircuitClick: (com.example.f1_kotlin.data.model.CircuitModel) -> Unit = { circuit ->
        navController.navigate("circuit/${circuit.circuitId}")
    }

    CompositionLocalProvider(LocalShareActionSetter provides { shareAction = it }) {
        Scaffold(
            topBar = {
                when {
                    currentRoute in tabs.map { it.route } -> F1AppBar()
                    currentRoute == "race_search" -> F1AppBar(title = stringResource(com.example.f1_kotlin.R.string.race_search_title), onBack = popBack)
                    currentRoute == "hall_of_fame" -> F1AppBar(title = stringResource(com.example.f1_kotlin.R.string.hall_of_fame_title), onBack = popBack)
                    currentRoute == "h2h" -> F1AppBar(title = stringResource(com.example.f1_kotlin.R.string.h2h_title), onBack = popBack)
                    currentRoute == "h2h_constructors" -> F1AppBar(title = stringResource(com.example.f1_kotlin.R.string.h2h_constructors_title), onBack = popBack)
                    currentRoute == "finish_status" -> F1AppBar(title = stringResource(com.example.f1_kotlin.R.string.finish_status_title), onBack = popBack)
                    currentRoute?.startsWith("race_info/") == true -> F1AppBar(
                        title = stringResource(com.example.f1_kotlin.R.string.detailed_info),
                        onBack = popBack,
                        onShare = shareAction,
                    )
                    currentRoute?.startsWith("circuit/") == true -> F1AppBar(title = stringResource(com.example.f1_kotlin.R.string.circuit_info_title), onBack = popBack)
                    currentRoute?.startsWith("driver/") == true -> F1AppBar(
                        title = stringResource(com.example.f1_kotlin.R.string.driver),
                        onBack = popBack,
                        onShare = shareAction,
                    )
                    currentRoute?.startsWith("constructor/") == true -> F1AppBar(
                        title = stringResource(com.example.f1_kotlin.R.string.constructor),
                        onBack = popBack,
                        onShare = shareAction,
                    )
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    F1BottomBar(
                        currentRoute = currentRoute,
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
            NavHost(
                navController = navController,
                startDestination = BottomTab.Home.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                composable(BottomTab.Home.route) {
                    HomeScreen(
                        viewModel = hiltViewModel(),
                        onDriverClick = onDriverClick,
                        onConstructorClick = onConstructorClick,
                    )
                }
                composable(BottomTab.Results.route) {
                    ResultsScreen(
                        viewModel = hiltViewModel(),
                        onSearchRace = { navController.navigate("race_search") },
                        onHallOfFame = { navController.navigate("hall_of_fame") },
                        onH2hDrivers = { navController.navigate("h2h") },
                        onH2hConstructors = { navController.navigate("h2h_constructors") },
                        onFinishStatus = { navController.navigate("finish_status") },
                        onRaceDetails = { race -> navController.navigate("race_info/${race.season}/${race.round}") },
                        onDriverClick = onDriverClick,
                    )
                }
                composable("race_search") {
                    RaceSearchScreen(
                        viewModel = hiltViewModel(),
                        onRaceDetails = { race -> navController.navigate("race_info/${race.season}/${race.round}") },
                        onDriverClick = onDriverClick,
                    )
                }
            composable("hall_of_fame") {
                HallOfFameScreen(
                    viewModel = hiltViewModel(),
                    onDriverClick = onDriverClick,
                    onConstructorClick = onConstructorClick,
                )
            }
            composable("h2h") {
                H2hDriversScreen(viewModel = hiltViewModel())
            }
            composable("h2h_constructors") {
                H2hConstructorsScreen(viewModel = hiltViewModel())
            }
            composable("finish_status") {
                FinishStatusScreen(viewModel = hiltViewModel())
            }
            composable("race_info/{season}/{round}") {
                RaceInfoScreen(
                    viewModel = hiltViewModel(),
                    onDriverClick = onDriverClick,
                )
            }
            composable(BottomTab.Schedule.route) { ScheduleScreen(hiltViewModel()) }
            composable(BottomTab.News.route) {
                NewsScreen(viewModel = hiltViewModel())
            }
            composable(BottomTab.Circuits.route) {
                CircuitsScreen(
                    viewModel = hiltViewModel(),
                    onCircuitClick = { circuitId -> navController.navigate("circuit/$circuitId") },
                )
            }
            composable("circuit/{circuitId}") {
                CircuitDetailScreen(
                    viewModel = hiltViewModel(),
                    onDriverClick = onDriverClick,
                )
            }
            composable("driver/{driverId}") {
                DriverDetailScreen(
                    viewModel = hiltViewModel(),
                    onConstructorClick = onConstructorClick,
                    onCircuitClick = onCircuitClick,
                )
            }
            composable("constructor/{constructorId}") {
                ConstructorDetailScreen(
                    viewModel = hiltViewModel(),
                    onDriverClick = onDriverClick,
                    onCircuitClick = onCircuitClick,
                )
            }
            }
        }
    }
}

@Composable
private fun F1BottomBar(currentRoute: String?, onTabSelected: (BottomTab) -> Unit) {
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
                val selected = currentRoute == tab.route
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
