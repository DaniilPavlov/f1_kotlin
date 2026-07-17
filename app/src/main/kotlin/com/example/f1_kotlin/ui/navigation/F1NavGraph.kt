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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.f1_kotlin.ui.components.F1AppBar
import com.example.f1_kotlin.ui.screens.circuits.CircuitDetailScreen
import com.example.f1_kotlin.ui.screens.circuits.CircuitsScreen
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

/**
 * Описание вкладки нижней навигации: route для NavHost, подпись и иконка.
 */
sealed class BottomTab(val route: String, val label: String, val iconRes: Int) {
    data object Home : BottomTab("home", "Главная", com.example.f1_kotlin.R.drawable.nav_home)
    data object Results : BottomTab("results", "Результаты", com.example.f1_kotlin.R.drawable.nav_racing_car)
    data object Schedule : BottomTab("schedule", "Календарь", com.example.f1_kotlin.R.drawable.nav_lights)
    data object HallOfFame : BottomTab("hall_of_fame", "Зал славы", com.example.f1_kotlin.R.drawable.nav_trophy)
    data object Circuits : BottomTab("circuits", "Трассы", com.example.f1_kotlin.R.drawable.nav_circuit)
}

private val tabs = listOf(
    BottomTab.Home,
    BottomTab.Results,
    BottomTab.Schedule,
    BottomTab.HallOfFame,
    BottomTab.Circuits,
)

/**
 * Корневой Composable приложения: Scaffold + NavHost + нижняя панель.
 *
 * [hiltViewModel] вместо [androidx.lifecycle.viewmodel.compose.viewModel] —
 * Hilt сам создаёт ViewModel с [F1Repository] внутри. Аргументы маршрута
 * (`season`, `round`, `circuitId`) попадают в [androidx.lifecycle.SavedStateHandle].
 *
 * На вложенных экранах (поиск, детали) нижняя панель скрывается — [showBottomBar].
 */
@Composable
fun F1App() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in tabs.map { it.route }
    val popBack: () -> Unit = { navController.popBackStack() }

    Scaffold(
        topBar = {
            when {
                currentRoute in tabs.map { it.route } -> F1AppBar()
                currentRoute == "race_search" -> F1AppBar(title = "Поиск гонки", onBack = popBack)
                currentRoute?.startsWith("race_info/") == true -> F1AppBar(title = "Подробная информация", onBack = popBack)
                currentRoute?.startsWith("circuit/") == true -> F1AppBar(title = "Информация о трассе", onBack = popBack)
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
            composable(BottomTab.Home.route) { HomeScreen(hiltViewModel()) }
            composable(BottomTab.Results.route) {
                ResultsScreen(
                    viewModel = hiltViewModel(),
                    onSearchRace = { navController.navigate("race_search") },
                    onRaceDetails = { race -> navController.navigate("race_info/${race.season}/${race.round}") },
                )
            }
            composable("race_search") {
                RaceSearchScreen(
                    viewModel = hiltViewModel(),
                    onRaceDetails = { race -> navController.navigate("race_info/${race.season}/${race.round}") },
                )
            }
            composable("race_info/{season}/{round}") {
                RaceInfoScreen(viewModel = hiltViewModel())
            }
            composable(BottomTab.Schedule.route) { ScheduleScreen(hiltViewModel()) }
            composable(BottomTab.HallOfFame.route) { HallOfFameScreen(hiltViewModel()) }
            composable(BottomTab.Circuits.route) {
                CircuitsScreen(
                    viewModel = hiltViewModel(),
                    onCircuitClick = { circuitId -> navController.navigate("circuit/$circuitId") },
                )
            }
            composable("circuit/{circuitId}") {
                CircuitDetailScreen(viewModel = hiltViewModel())
            }
        }
    }
}

/** Нижняя панель с 5 вкладками; активная — красные иконка и текст, неактивная — белые. */
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
                        contentDescription = tab.label,
                        modifier = Modifier.size(28.dp),
                        colorFilter = ColorFilter.tint(contentColor),
                    )
                    Text(
                        text = tab.label,
                        style = AppStyles.navBar.copy(color = contentColor),
                    )
                }
            }
        }
    }
}
