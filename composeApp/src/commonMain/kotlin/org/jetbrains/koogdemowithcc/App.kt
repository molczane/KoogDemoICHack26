package org.jetbrains.koogdemowithcc

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.koogdemowithcc.navigation.Screen
import org.jetbrains.koogdemowithcc.ui.components.BottomNavBar
import org.jetbrains.koogdemowithcc.ui.theme.AppTheme
import org.jetbrains.koogdemowithcc.ui.tripplan.TripPlanScreen
import org.jetbrains.koogdemowithcc.ui.weather.WeatherScreen

@Composable
fun App() {
    AppTheme(darkTheme = true) {
        var currentScreen: Screen by remember { mutableStateOf(Screen.Weather) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                BottomNavBar(
                    currentScreen = currentScreen,
                    onNavigate = { screen -> currentScreen = screen }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    Screen.Weather -> WeatherScreen()
                    Screen.TripPlan -> TripPlanScreen()
                }
            }
        }
    }
}
