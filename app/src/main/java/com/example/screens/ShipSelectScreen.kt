package com.example.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
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
import com.example.ui.drawProceduralShip
import com.example.ui.triggerVibration

@Composable
fun ShipSelectScreen(viewModel: GameViewModel) {
    val progress by viewModel.userProgress.collectAsStateWithLifecycle()
    val crystalCount = progress?.crystals ?: 0
    val unlockedShips = progress?.getUnlockedShips() ?: listOf("solar_wing")
    val selectedShip = progress?.selectedShip ?: "solar_wing"
    val vibrationEnabled = progress?.vibrationEnabled ?: true

    // Carousel state
    var currentIndex by remember { mutableStateOf(0) }
    
    // Sync current selection index with DB initial choice
    LaunchedEffect(selectedShip) {
        val dbIndex = GameData.ships.indexOfFirst { it.id == selectedShip }
        if (dbIndex != -1) {
            currentIndex = dbIndex
        }
    }

    val activeDef = GameData.ships[currentIndex]
    val isUnlocked = unlockedShips.contains(activeDef.id)
    val isSelected = selectedShip == activeDef.id

    // Smooth floating movement
    val infiniteTransition = rememberInfiniteTransition(label = "hover")
    val hoverOffset by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "ship_float"
    )

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ship_select_root")
    ) {
        SynthwaveGridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // Navigation Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .testTag("back_to_menu_button")
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F1F35).copy(alpha = 0.82f))
                        .border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                        .clickable {
                            viewModel.changeScreen(GameScreen.MAIN_MENU)
                        }
                ) {
                    Text(
                        text = "◀",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "SELECT YOUR FIGHTER",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.5.sp
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

            Spacer(modifier = Modifier.height(40.dp))

            // Ship Carousel Viewer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left cycle arrow
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F1F35).copy(alpha = 0.6f))
                        .border(1.dp, Color.LightGray.copy(alpha = 0.2f), CircleShape)
                        .clickable {
                            if (vibrationEnabled) triggerVibration(context, 15)
                            currentIndex = if (currentIndex > 0) currentIndex - 1 else GameData.ships.size - 1
                        }
                ) {
                    Text("◀", color = Color.White, fontSize = 15.sp)
                }

                // Main Render Vector Box
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(1.1f),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawProceduralShip(
                            shipId = activeDef.id,
                            center = Offset(size.width / 2f, (size.height / 2f) + hoverOffset),
                            size = 145f,
                            paintColor = Color(activeDef.colorHex)
                        )
                    }
                }

                // Right cycle arrow
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F1F35).copy(alpha = 0.6f))
                        .border(1.dp, Color.LightGray.copy(alpha = 0.2f), CircleShape)
                        .clickable {
                            if (vibrationEnabled) triggerVibration(context, 15)
                            currentIndex = (currentIndex + 1) % GameData.ships.size
                        }
                ) {
                    Text("▶", color = Color.White, fontSize = 15.sp)
                }
            }

            // Ship name display
            Text(
                text = activeDef.name,
                color = Color(activeDef.colorHex),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Statistics display Card
            NeonCard(
                borderColor = Color(activeDef.colorHex),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = "SPEC PERFORMANCE STATS",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // HP Stat row
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "🛡️ CHASSIS HP", color = Color.LightGray, fontSize = 12.sp)
                        Text(text = "${activeDef.hp} / 150", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { activeDef.hp / 150f },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = Color(activeDef.colorHex),
                        trackColor = Color(0xFF1F1F35)
                    )
                }

                // Firing Rate Stat row
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                    // Lower fireRateMs means FASTER shooting speed
                    val fireSpeedRatio = (600f - activeDef.fireRateMs) / 500f
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "☄️ WEAPONS FIRE-RATE COOLDOWN", color = Color.LightGray, fontSize = 12.sp)
                        Text(text = "${activeDef.fireRateMs}ms", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { fireSpeedRatio.coerceIn(0.1f, 1.0f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = Color(activeDef.colorHex),
                        trackColor = Color(0xFF1F1F35)
                    )
                }

                // Special ability descriptions
                Text(
                    text = "🔋 INTEGRATED SYSTEM ABILITY",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = activeDef.specialAbility,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                // Unlock criteria disclaimer info
                if (!isUnlocked) {
                    Text(
                        text = "🔒 Unlock condition: ${activeDef.unlockCondition}",
                        color = Color(0xFFFF2E63),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }

                // Purchase Actions button layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when {
                        isSelected -> {
                            NeonButton(
                                text = "⚡ ACTIVE FIGHTER",
                                onClick = {},
                                enabled = false,
                                buttonColor = Color.Gray,
                                vibrationEnabled = vibrationEnabled,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        isUnlocked -> {
                            NeonButton(
                                text = "SELECT FIGHTER",
                                onClick = {
                                    viewModel.selectShip(activeDef.id)
                                },
                                buttonColor = Color(0xFF00ADB5),
                                vibrationEnabled = vibrationEnabled,
                                testTag = "select_ship_action",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        else -> {
                            // Can purchase if cost > 0
                            val canBuy = activeDef.crystalCost > 0 && crystalCount >= activeDef.crystalCost
                            val btnText = if (activeDef.crystalCost > 0) "BUY FOR 💎 ${activeDef.crystalCost}" else "LOCKED (MISSION REQUIRED)"
                            
                            NeonButton(
                                text = btnText,
                                onClick = {
                                    if (activeDef.crystalCost > 0) {
                                        viewModel.buyShip(activeDef.id, activeDef.crystalCost)
                                    }
                                },
                                enabled = canBuy,
                                buttonColor = Color(0xFFFF2E63),
                                vibrationEnabled = vibrationEnabled,
                                testTag = "buy_ship_action",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
