package com.example.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import com.example.core.GameScreen
import com.example.core.GameViewModel
import com.example.ui.NeonButton
import com.example.ui.NeonCard
import com.example.ui.SynthwaveGridBackground
import com.example.ui.triggerVibration

@Composable
fun MainMenuScreen(viewModel: GameViewModel) {
    val progress by viewModel.userProgress.collectAsStateWithLifecycle()
    val isDailyAvailable by viewModel.dailyRewardAvailable.collectAsStateWithLifecycle()
    val showDailyPopup by viewModel.showDailyRewardPopup.collectAsStateWithLifecycle()

    val crystalCount = progress?.crystals ?: 0
    val vibrationEnabled = progress?.vibrationEnabled ?: true

    // Infinite logo pulsing scale
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
    val scaleVal by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "logo_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_menu_root")
    ) {
        // Starfield Scrolling Background
        SynthwaveGridBackground()

        // Core Hub Panel Scrollable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(35.dp))

            // Upper Stats Row (Daily + Crystals)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top Left: Daily Reward Button (Glows if available)
                val dailyBgColor = if (isDailyAvailable) Color(0xFFFF2E63) else Color(0xFF1F1F35)
                val dailyBorderColor = if (isDailyAvailable) Color(0xFFFF2E63) else Color(0xFF3F3F5F)
                val textCol = if (isDailyAvailable) Color.White else Color.Gray

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .testTag("daily_reward_button")
                        .clip(RoundedCornerShape(30.dp))
                        .background(dailyBgColor.copy(alpha = 0.85f))
                        .border(1.dp, dailyBorderColor, RoundedCornerShape(30.dp))
                        .clickable {
                            viewModel.showDailyRewardPopup.value = true
                        }
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                ) {
                    Text(
                        text = if (isDailyAvailable) "🎁 CLAIM DAILY!" else "🎁 Daily Claimed",
                        color = textCol,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Top Right: Crystal display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .testTag("crystal_display")
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0E111F).copy(alpha = 0.9f))
                        .border(1.dp, Color(0xFF00ADB5).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                ) {
                    Text(
                        text = "💎 $crystalCount",
                        color = Color(0xFF00ADB5),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Pulse Pulsing Game Logo
            Column(
                modifier = Modifier
                    .padding(bottom = 35.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "STELLAR",
                    color = Color(0xFFBD00FF),
                    fontSize = 46.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 4.sp,
                    modifier = Modifier.testTag("menu_logo_top")
                )
                Text(
                    text = "SPECTRUM",
                    color = Color(0xFF00ADB5),
                    fontSize = 46.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 4.sp,
                    modifier = Modifier.testTag("menu_logo_bottom")
                )
            }

            // Menu Options Stack
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NeonButton(
                    text = "▶ PLAY GAME",
                    onClick = { viewModel.changeScreen(GameScreen.WORLD_SELECT) },
                    buttonColor = Color(0xFF00ADB5), // Cyan
                    vibrationEnabled = vibrationEnabled,
                    testTag = "play_button",
                    modifier = Modifier.fillMaxWidth()
                )

                NeonButton(
                    text = "🚀 SHIP SELECT",
                    onClick = { viewModel.changeScreen(GameScreen.SHIP_SELECT) },
                    buttonColor = Color(0xFFBD00FF), // Purple
                    vibrationEnabled = vibrationEnabled,
                    testTag = "ship_select_button",
                    modifier = Modifier.fillMaxWidth()
                )

                NeonButton(
                    text = "🛒 SPECTRUM SHOP",
                    onClick = { viewModel.changeScreen(GameScreen.SHOP) },
                    buttonColor = Color(0xFFFF2E63), // Pink
                    vibrationEnabled = vibrationEnabled,
                    testTag = "shop_button",
                    modifier = Modifier.fillMaxWidth()
                )

                NeonButton(
                    text = "🎯 DAILY MISSIONS",
                    onClick = { viewModel.changeScreen(GameScreen.MISSIONS) },
                    buttonColor = Color(0xFF10B981), // Green
                    vibrationEnabled = vibrationEnabled,
                    testTag = "missions_button",
                    modifier = Modifier.fillMaxWidth()
                )

                NeonButton(
                    text = "🏅 ACHIEVEMENTS",
                    onClick = { viewModel.changeScreen(GameScreen.ACHIEVEMENTS) },
                    buttonColor = Color(0xFFFBBF24), // Gold
                    vibrationEnabled = vibrationEnabled,
                    testTag = "achievements_button",
                    modifier = Modifier.fillMaxWidth()
                )

                NeonButton(
                    text = "🏆 LEADERBOARD",
                    onClick = { viewModel.changeScreen(GameScreen.LEADERBOARD) },
                    buttonColor = Color(0xFF3B82F6), // Blue
                    vibrationEnabled = vibrationEnabled,
                    testTag = "leaderboard_button",
                    modifier = Modifier.fillMaxWidth()
                )

                NeonButton(
                    text = "⚙️ SETTINGS",
                    onClick = { viewModel.changeScreen(GameScreen.SETTINGS) },
                    buttonColor = Color.LightGray,
                    vibrationEnabled = vibrationEnabled,
                    testTag = "settings_button",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(55.dp))

            // Version stamp bottom
            Text(
                text = "v1.0.0 • Built with Kotlin & Compose",
                color = Color.Gray.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif
            )
        }

        // Daily Login rewards 7 days Popup Overlay
        if (showDailyPopup) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { viewModel.showDailyRewardPopup.value = false },
                contentAlignment = Alignment.Center
            ) {
                // Prevent tap propagates inside the dialog
                NeonCard(
                    borderColor = Color(0xFFFF2E63),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .testTag("daily_reward_popup_card")
                        .clickable(enabled = false) {}
                ) {
                    Text(
                        text = "🛰️ DAILY FLIGHT REWARDS",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFFF2E63).copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Claim crystals increments daily. Collect consecutive streaks to achieve max gemstone outputs!",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 7 days Grid display
                    val currentDay = progress?.dailyRewardDay ?: 0
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(7) { index ->
                            val dNum = index + 1
                            val isClaimed = dNum <= currentDay && !isDailyAvailable
                            val isClaimableToday = isDailyAvailable && dNum == (currentDay % 7) + 1
                            
                            val dayBg = when {
                                isClaimed -> Color(0xFF1F1F35)
                                isClaimableToday -> Color(0xFFFF2E63)
                                else -> Color(0xFF0A0C16)
                            }
                            val dayBorder = when {
                                isClaimed -> Color.Gray.copy(alpha = 0.3f)
                                isClaimableToday -> Color(0xFFFF2E63)
                                else -> Color.DarkGray
                            }

                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(dayBg)
                                    .border(1.dp, dayBorder, RoundedCornerShape(8.dp))
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "DAY $dNum",
                                    color = if (isClaimableToday) Color.White else Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "💎 ${dNum * 50}",
                                    color = if (isClaimableToday) Color.White else Color(0xFF00ADB5),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isClaimed) {
                                    Text("✔️", fontSize = 9.sp, modifier = Modifier.padding(top=2.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Claim button
                    NeonButton(
                        text = if (isDailyAvailable) "COLLECT REWARD 💎" else "ALREADY CLAIMED TODAY",
                        onClick = { viewModel.claimDailyReward() },
                        enabled = isDailyAvailable,
                        buttonColor = Color(0xFFFF2E63),
                        vibrationEnabled = vibrationEnabled,
                        testTag = "claim_reward_action",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Tap off card to dismiss",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}
