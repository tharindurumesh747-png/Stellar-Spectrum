package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.GameScreen
import com.example.core.GameViewModel
import com.example.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: GameViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val currentScreen by viewModel.activeScreen.collectAsStateWithLifecycle()

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          Box(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
              GameScreen.SPLASH -> SplashScreen(viewModel)
              GameScreen.MAIN_MENU -> MainMenuScreen(viewModel)
              GameScreen.SHIP_SELECT -> ShipSelectScreen(viewModel)
              GameScreen.WORLD_SELECT -> WorldSelectScreen(viewModel)
              GameScreen.GAMEPLAY -> GameplayScreen(viewModel)
              GameScreen.SHOP -> ShopScreen(viewModel)
              GameScreen.ACHIEVEMENTS -> AchievementsScreen(viewModel)
              GameScreen.MISSIONS -> MissionsScreen(viewModel)
              GameScreen.LEADERBOARD -> LeaderboardScreen(viewModel)
              GameScreen.SETTINGS -> SettingsScreen(viewModel)
            }
          }
        }
      }
    }
  }
}

