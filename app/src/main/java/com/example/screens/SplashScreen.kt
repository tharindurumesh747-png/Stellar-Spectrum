package com.example.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.GameScreen
import com.example.core.GameViewModel
import com.example.ui.SynthwaveGridBackground
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(viewModel: GameViewModel) {
    val alphaAnim = remember { Animatable(0f) }
    val progressValue = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Logo fade-in
        alphaAnim.animateTo(1f, animationSpec = tween(1000))
        
        // Progress bar simulation over 2 seconds
        progressValue.animateTo(1.0f, animationSpec = tween(1500))
        
        delay(300) // Small cinematic pause
        viewModel.changeScreen(GameScreen.MAIN_MENU)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("splash_screen_root")
    ) {
        // Background parallax wireframe grid
        SynthwaveGridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .alpha(alphaAnim.value)
                    .padding(bottom = 8.dp)
            ) {
                // Main Title Glow
                Text(
                    text = "STELLAR",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFBD00FF), // Neon Purple
                    modifier = Modifier.testTag("splash_title_top")
                )
            }
            Box(
                modifier = Modifier
                    .alpha(alphaAnim.value)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "SPECTRUM",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF00ADB5), // Neon Cyan
                    modifier = Modifier.testTag("splash_title_bottom")
                )
            }

            Box(
                modifier = Modifier
                    .alpha(alphaAnim.value)
                    .padding(bottom = 60.dp)
            ) {
                Text(
                    text = "COLOR • SHOOT • SURVIVE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF2E63), // Neon Magenta
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 2.sp
                )
            }

            // Custom styled progress bar loading animation
            LinearProgressIndicator(
                progress = { progressValue.value },
                modifier = Modifier
                    .width(220.dp)
                    .height(6.dp)
                    .testTag("splash_loading_bar"),
                color = Color(0xFF00ADB5),
                trackColor = Color(0xFF1F1F35)
            )
        }
    }
}
