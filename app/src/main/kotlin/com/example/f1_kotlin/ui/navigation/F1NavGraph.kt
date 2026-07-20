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
import androidx.compose.runtime.getValue
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
import com.example.f1_kotlin.ui.screens.halloffame.HallOfFameScreen
import com.example.f1_kotlin.ui.screens.home.HomeScreen
import com.example.f1_kotlin.ui.screens.results.RaceInfoScreen
import com.example.f1_kotlin.ui.screens.results.RaceSearchScreen
import com.example.f1_kotlin.ui.screens.results.ResultsScreen
import com.example.f1_kotlin.ui.screens.schedule.ScheduleScreen
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1Black
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.ui.theme.F1White

sealed class BottomTab(val route: String, val labelRes: Int, val iconRes: Int) {
    data object Home : BottomTab("home", com.example.f1_kotlin.R.string.nav_home, com.example.f1_kotlin.R.drawable.nav_home)
    data object Results : BottomTab("results", com.example.f1_kotlin.R.string.nav_results, com.example.f1_kotlin.R.drawable.nav_racing_car)
    data object Schedule : BottomTab("schedule", com.example.f1_kotlin.R.string.nav_calendar, com.example.f1_kotlin.R.drawable.nav_lights)
    data object HallOfFame : BottomTab("hall_of_fame", com.example.f1_kotlin.R.string.nav_hall_of_fame, com.example.f1_kotlin.R.drawable.nav_trophy)
    data object Circuits : BottomTab("circuits", com.example.f1_kotlin.R.string.nav_circuits, com.example.f1_kotlin.R.drawable.nav_circuit)
}

private val tabs = listOf(
    BottomTab.Home,
    BottomTab.Results,
    BottomTab.Schedule,
    BottomTab.HallOfFame,
    BottomTab.Circuits,
)

@Composable
fun F1App() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in tabs.map { it.route }
    val popBack: () -> Unit = { navController.popBackStack() }

    val onDriverClick: (DriverModel) -> Unit = { driver ->
        navController.navigate("driver/${driver.driverId}")
    }
    val onConstructorClick: (ConstructorModel) -> Unit = { constructor ->
        navController.navigate("constructor/${constructor.constructorId}")
    }

    Scaffold(
        topBar = {
            when {
                currentRoute in tabs.map { it.route } -> F1AppBar()
                currentRoute == "race_search" -> F1AppBar(title = stringResource(com.example.f1_kotlin.R.string.race_search_title), onBack = popBack)
                currentRoute?.startsWith("race_info/") == true -> F1AppBar(title = stringResource(com.example.f1_kotlin.R.string.detailed_info), onBack = popBack)
                currentRoute?.startsWith("circuit/") == true -> F1AppBar(title = stringResource(com.example.f1_kotlin.R.string.circuit_info_title), onBack = popBack)
                currentRoute?.startsWith("driver/") == true -> F1AppBar(title = stringResource(com.example.f1_kotlin.R.string.driver), onBack = popBack)
                currentRoute?.startsWith("constructor/") == true -> F1AppBar(title = stringResource(com.example.f1_kotlin.R.string.constructor), onBack = popBack)
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
            composable("race_info/{season}/{round}") {
                RaceInfoScreen(
                    viewModel = hiltViewModel(),
                    onDriverClick = onDriverClick,
                )
            }
            composable(BottomTab.Schedule.route) { ScheduleScreen(hiltViewModel()) }
            composable(BottomTab.HallOfFame.route) {
                HallOfFameScreen(
                    viewModel = hiltViewModel(),
                    onDriverClick = onDriverClick,
                    onConstructorClick = onConstructorClick,
                )
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
                )
            }
            composable("constructor/{constructorId}") {
                ConstructorDetailScreen(
                    viewModel = hiltViewModel(),
                    onDriverClick = onDriverClick,
                )
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
