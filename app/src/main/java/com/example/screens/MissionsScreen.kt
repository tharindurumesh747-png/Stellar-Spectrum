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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.GameData
import com.example.core.GameScreen
import com.example.core.GameViewModel
import com.example.ui.NeonButton
import com.example.ui.NeonCard
import com.example.ui.SynthwaveGridBackground

@Composable
fun MissionsScreen(viewModel: GameViewModel) {
    val progress by viewModel.userProgress.collectAsStateWithLifecycle()
    val crystalCount = progress?.crystals ?: 0
    val missionsProgress = progress?.getMissionsProgress() ?: emptyMap()
    val vibrationEnabled = progress?.vibrationEnabled ?: true

    var selectedTab by remember { mutableStateOf("DAILY") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("missions_root")
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
                    text = "SECTOR COMMAND ROSTER",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.4.sp
                )

                // Crystal display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0E111F))
                        .padding(vertical = 6.dp, horizontal = 12.dp)
                ) {
                    Text(
                        text = "💎 $crystalCount",
                        color = Color(0xFF00ADB5),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(25.dp))

            // Sliding tabs (DAILY vs WEEKLY)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF10111F))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val dailyActive = selectedTab == "DAILY"
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (dailyActive) Color(0xFF10B981) else Color.Transparent)
                        .clickable { selectedTab = "DAILY" }
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = "DAILY CONTRACTS",
                        color = if (dailyActive) Color.White else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val weeklyActive = selectedTab == "WEEKLY"
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (weeklyActive) Color(0xFF10B981) else Color.Transparent)
                        .clickable { selectedTab = "WEEKLY" }
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = "WEEKLY CAMPAIGNS",
                        color = if (weeklyActive) Color.White else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            // Time timer display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedTab == "DAILY") "⏰ Resets in: 19h 28m 10s" else "⏰ Resets in: 5d 19h 28m",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Missions list filters
            val filteredMissions = GameData.missions.filter { it.isWeekly == (selectedTab == "WEEKLY") }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                filteredMissions.forEach { mission ->
                    val currentProg = missionsProgress[mission.id] ?: 0
                    val isComplete = currentProg >= mission.target
                    
                    val progressRatio = (currentProg.toFloat() / mission.target.toFloat()).coerceIn(0f, 1f)

                    NeonCard(
                        borderColor = if (isComplete) Color(0xFF10B981) else Color(0xFF00ADB5),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = mission.name,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = mission.description,
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress metric bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "PROGRESS:", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "$currentProg / ${mission.target}",
                                color = if (isComplete) Color(0xFF10B981) else Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isComplete) Color(0xFF10B981) else Color(0xFF00ADB5),
                            trackColor = Color(0xFF1F1F35)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))

                        Spacer(modifier = Modifier.height(12.dp))

                        // Rewards Claim footer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Reward: ", color = Color.Gray, fontSize = 12.sp)
                                Text(
                                    text = "💎 ${mission.rewardCrystals} Crystals",
                                    color = Color(0xFF00ADB5),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            NeonButton(
                                text = if (isComplete) "CLAIM INFO 💎" else "IN PROGRESS",
                                onClick = {
                                    viewModel.claimMissionReward(mission.id)
                                },
                                enabled = isComplete,
                                buttonColor = Color(0xFF10B981),
                                vibrationEnabled = vibrationEnabled,
                                testTag = "claim_mission_reward_btn_${mission.id}",
                                modifier = Modifier.width(130.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
