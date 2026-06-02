package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.core.AchievementDefinition
import com.example.core.GameData
import com.example.core.GameScreen
import com.example.core.GameViewModel
import com.example.core.SoundSynth
import com.example.ui.NeonButton
import com.example.ui.NeonCard
import com.example.ui.SynthwaveGridBackground
import com.example.ui.triggerVibration

@Composable
fun AchievementsScreen(viewModel: GameViewModel) {
    val progress by viewModel.userProgress.collectAsStateWithLifecycle()
    val crystalCount = progress?.crystals ?: 0
    val unlockedIds = progress?.getUnlockedAchievements() ?: emptyList()
    val vibrationEnabled = progress?.vibrationEnabled ?: true

    // Details overlay state
    var selectedAchForPopup by remember { mutableStateOf<AchievementDefinition?>(null) }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("achievements_root")
    ) {
        SynthwaveGridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
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
                    text = "GALAXY RECORD MEDALS",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.4.sp
                )

                // Crystals
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

            Spacer(modifier = Modifier.height(30.dp))

            // Display medal unlocked ratio
            val unlockedRatioText = "${unlockedIds.size} / ${GameData.achievements.size}"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🎖️ TOTAL MEDALS UNLOCKED:", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = unlockedRatioText,
                    color = Color(0xFFFBBF24),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(25.dp))

            // 3-column Medal Grid list
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(GameData.achievements.size) { index ->
                    val ach = GameData.achievements[index]
                    val isCompleted = unlockedIds.contains(ach.id)

                    val badgeBg = if (isCompleted) Color(0xFF2A1F45) else Color(0xFF0C0E1F)
                    val badgeBorder = if (isCompleted) Color(0xFFFBBF24) else Color.DarkGray
                    val emoji = when (ach.id) {
                        "first_blood" -> "🩸"
                        "color_master" -> "🎨"
                        "galaxy_guardian" -> "🪐"
                        "cosmic_legend" -> "🌌"
                        "untouchable" -> "🛡️"
                        "boss_slayer_ach" -> "💀"
                        else -> "👑"
                    }

                    Column(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(badgeBg)
                            .border(width = 1.2.dp, color = badgeBorder, shape = RoundedCornerShape(12.dp))
                            .clickable {
                                if (vibrationEnabled) triggerVibration(context, 15)
                                selectedAchForPopup = ach
                            }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = emoji, fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ach.name,
                            color = if (isCompleted) Color.White else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Expanded medal popup detail overlay
        selectedAchForPopup?.let { ach ->
            val isCompleted = unlockedIds.contains(ach.id)
            val scoreStat = when (ach.id) {
                "first_blood" -> progress?.totalEnemiesKilled ?: 0
                "color_master" -> progress?.totalEnemiesKilled ?: 0
                "galaxy_guardian" -> progress?.getUnlockedWorlds()?.size ?: 1
                "cosmic_legend" -> progress?.highScoresStr?.split(",")?.mapNotNull { it.split(":").getOrNull(1)?.toIntOrNull() }?.maxOrNull() ?: 0
                "untouchable" -> if (unlockedIds.contains("untouchable")) 1 else 0
                "boss_slayer_ach" -> progress?.totalBossesKilled ?: 0
                else -> progress?.getUnlockedAchievements()?.size ?: 0
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { selectedAchForPopup = null },
                contentAlignment = Alignment.Center
            ) {
                NeonCard(
                    borderColor = if (isCompleted) Color(0xFFFBBF24) else Color.DarkGray,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .testTag("achievement_popup")
                        .clickable(enabled = false) {}
                ) {
                    Text(
                        text = if (isCompleted) "🏆 MEDAL EARNED" else "🔒 CALIBRATING CHALLENGE",
                        color = if (isCompleted) Color(0xFFFBBF24) else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = ach.name,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = ach.description,
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress metric
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Current Log:", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            text = "$scoreStat / ${ach.target}",
                            color = if (isCompleted) Color(0xFF10B981) else Color.White,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Reward amount row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Medal Jewels Reward:", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            text = "💎 ${ach.rewardCrystals} Crystals",
                            color = Color(0xFF00ADB5),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    NeonButton(
                        text = "CLOSE MEDAL CASE",
                        onClick = { selectedAchForPopup = null },
                        buttonColor = Color.Gray,
                        vibrationEnabled = vibrationEnabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
