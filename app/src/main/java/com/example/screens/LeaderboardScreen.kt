package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.GameData
import com.example.core.GameScreen
import com.example.core.GameViewModel
import com.example.ui.NeonButton
import com.example.ui.NeonCard
import com.example.ui.SynthwaveGridBackground

data class LeaderboardEntry(
    val rank: Int,
    val pilotName: String,
    val score: Int,
    val sector: String
)

@Composable
fun LeaderboardScreen(viewModel: GameViewModel) {
    val progress by viewModel.userProgress.collectAsStateWithLifecycle()
    val highScores = progress?.getHighScores() ?: emptyMap()
    val vibrationEnabled = progress?.vibrationEnabled ?: true

    var selectedTab by remember { mutableStateOf("LOCAL_RECORDS") }

    // Mock global players definitions
    val globalPlayers = listOf(
        LeaderboardEntry(1, "Ω OMEGA_SPECTER", 85000, "Core 06"),
        LeaderboardEntry(2, "🌠 SpaceWarp_99", 64300, "Dark 05"),
        LeaderboardEntry(3, "⚡ NovaSlayer", 54100, "Prism 04"),
        LeaderboardEntry(4, "☄️ Cometeer", 42000, "Gateway 03"),
        LeaderboardEntry(5, "🛡️ PrismKnight", 31500, "Nebula 02")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("leaderboards_screen_root")
    ) {
        SynthwaveGridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // Navigation Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .testTag("back_to_menu_button")
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F1F35).copy(alpha = 0.85f))
                        .border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                        .clickable {
                            viewModel.changeScreen(GameScreen.MAIN_MENU)
                        }
                ) {
                    Text("◀", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "GALAXY SCOREBOARD",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.4.sp
                )

                Spacer(modifier = Modifier.width(44.dp))
            }

            Spacer(modifier = Modifier.height(25.dp))

            // Tab toggler (LOCAL vs GLOBAL)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF10111F))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val locActive = selectedTab == "LOCAL_RECORDS"
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (locActive) Color(0xFF3B82F6) else Color.Transparent)
                        .clickable { selectedTab = "LOCAL_RECORDS" }
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = "LOCAL RECORDS",
                        color = if (locActive) Color.White else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val globActive = selectedTab == "GLOBAL_CHAMPIONS"
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (globActive) Color(0xFF3B82F6) else Color.Transparent)
                        .clickable { selectedTab = "GLOBAL_CHAMPIONS" }
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = "GLOBAL SECTOR",
                        color = if (globActive) Color.White else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(25.dp))

            if (selectedTab == "LOCAL_RECORDS") {
                // Read local scores safely by World index
                val hasScores = highScores.values.any { it > 0 }
                
                if (hasScores) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GameData.worlds.forEach { world ->
                            val score = highScores[world.index] ?: 0
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0E111F).copy(alpha = 0.85f))
                                    .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "SECTOR 0${world.index}",
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = world.name,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = "$score PTS",
                                    color = Color(0xFF00ADB5),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                } else {
                    // Empty states
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No flight records simulated yet!\nEnter worlds portal coordinates to log pilot scores.",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                // Global champions mock charts
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Coming soon floating indicator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFF2E63).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFFF2E63).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "🛰️ Offline state active. Displaying mock server champion list.",
                            color = Color(0xFFFF2E63),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    globalPlayers.forEach { entry ->
                        val medalColor = when (entry.rank) {
                            1 -> Color(0xFFFBBF24) // Gold
                            2 -> Color(0xFFEEEEEE) // Silver
                            else -> Color(0xFFCD7F32) // Bronze
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0E111F).copy(alpha = 0.85f))
                                .border(1.dp, medalColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(medalColor.copy(alpha = 0.15f))
                                        .border(1.dp, medalColor, CircleShape)
                                ) {
                                    Text(
                                        text = "#${entry.rank}",
                                        color = medalColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = entry.pilotName,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Portal: ${entry.sector}",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Text(
                                text = "${entry.score}",
                                color = Color(0xFF00ADB5),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
