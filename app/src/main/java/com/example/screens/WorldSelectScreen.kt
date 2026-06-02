package com.example.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.triggerVibration

@Composable
fun WorldSelectScreen(viewModel: GameViewModel) {
    val progress by viewModel.userProgress.collectAsStateWithLifecycle()
    val unlockedWorlds = progress?.getUnlockedWorlds() ?: listOf(1)
    val highScores = progress?.getHighScores() ?: emptyMap()
    val vibrationEnabled = progress?.vibrationEnabled ?: true

    // Chosen world popup trigger
    var selectedWorldIdForPopup by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("world_select_root")
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
                    text = "CHOOSE PORTAL SECTOR",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.8.sp
                )

                // Placeholder layout alignment balance
                Spacer(modifier = Modifier.width(44.dp))
            }

            Spacer(modifier = Modifier.height(25.dp))

            // Worlds list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GameData.worlds.forEach { world ->
                    val isUnlocked = unlockedWorlds.contains(world.index)
                    val worldScore = highScores[world.index] ?: 0
                    
                    val worldBorderColor = if (isUnlocked) Color(0xFF00ADB5) else Color.DarkGray
                    val worldGlowAlpha = if (isUnlocked) 0.15f else 0.04f

                    val cardGradColors = world.bgGradientHexes.map { Color(it) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        cardGradColors[0].copy(alpha = 0.4f),
                                        cardGradColors[1].copy(alpha = 0.15f)
                                    )
                                )
                            )
                            .border(
                                width = 1.2.dp,
                                color = worldBorderColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                if (isUnlocked) {
                                    if (vibrationEnabled) triggerVibration(context, 20)
                                    selectedWorldIdForPopup = world.index
                                } else {
                                    if (vibrationEnabled) triggerVibration(context, 70)
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SECTOR 0${world.index}: ",
                                    color = Color(0xFFFF2E63).copy(alpha = 0.82f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = world.name,
                                    color = if (isUnlocked) Color.White else Color.Gray,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "👾 Boss: ${world.bossName}",
                                color = Color.LightGray.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            
                            if (isUnlocked) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "🏆 RECORD: $worldScore PTS",
                                    color = Color(0xFF00ADB5),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Status Lock indicators
                        if (isUnlocked) {
                            Text(
                                text = "READY",
                                color = Color(0xFF10B981),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                    .padding(vertical = 4.dp, horizontal = 10.dp)
                            )
                        } else {
                            Text(
                                text = "LOCKED",
                                color = Color.Red,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Red.copy(alpha = 0.12f))
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        // Sector Launch detailed Popup Overlay
        selectedWorldIdForPopup?.let { wId ->
            val activeWorld = GameData.worlds.first { it.index == wId }
            val bestScore = highScores[wId] ?: 0

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { selectedWorldIdForPopup = null },
                contentAlignment = Alignment.Center
            ) {
                NeonCard(
                    borderColor = Color(0xFF00ADB5),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .testTag("world_launch_popup")
                        .clickable(enabled = false) {} // block click bypass
                ) {
                    Text(
                        text = "🚀 PORTAL TELEPORT COORDINATES",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = activeWorld.name,
                        color = Color(0xFF00ADB5),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = Color(0xFF00ADB5).copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = activeWorld.description,
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    // Difficulty Stars drawing row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DIFFICULTY RISK: ",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        repeat(activeWorld.difficulty) {
                            Text("★", color = Color(0xFFFF2E63), fontSize = 16.sp, modifier = Modifier.padding(horizontal = 1.dp))
                        }
                    }

                    // Score record row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "🌌 HISTORIC RECORD HIGH:", color = Color.LightGray, fontSize = 13.sp)
                        Text(
                            text = "$bestScore PTS",
                            color = Color(0xFF00ADB5),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Action launch buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NeonButton(
                            text = "CLOSE",
                            onClick = { selectedWorldIdForPopup = null },
                            buttonColor = Color.Gray,
                            vibrationEnabled = vibrationEnabled,
                            modifier = Modifier.weight(1f)
                        )

                        NeonButton(
                            text = "ENTER PORTAL",
                            onClick = {
                                selectedWorldIdForPopup = null
                                viewModel.selectWorldAndStart(wId)
                            },
                            buttonColor = Color(0xFF10B981), // success green launch
                            vibrationEnabled = vibrationEnabled,
                            testTag = "enter_world_portal",
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                }
            }
        }
    }
}
