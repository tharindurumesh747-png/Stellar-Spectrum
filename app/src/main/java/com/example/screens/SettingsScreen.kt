package com.example.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.core.GameScreen
import com.example.core.GameViewModel
import com.example.core.SoundSynth
import com.example.ui.NeonButton
import com.example.ui.NeonCard
import com.example.ui.SynthwaveGridBackground
import com.example.ui.triggerVibration

@Composable
fun SettingsScreen(viewModel: GameViewModel) {
    val progress by viewModel.userProgress.collectAsStateWithLifecycle()
    val vibrationEnabled = progress?.vibrationEnabled ?: true

    // Option state variables cached
    val soundCached = progress?.soundEnabled ?: true
    val musicCached = progress?.musicEnabled ?: true
    val vibeCached = progress?.vibrationEnabled ?: true

    var showClearWarningPopup by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen_root")
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
                    text = "SYSTEM CONTROLLER",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.4.sp
                )

                Spacer(modifier = Modifier.width(44.dp))
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Settings options items lists
            NeonCard(
                borderColor = Color.LightGray,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "AUDIO & HAPTICS SETTINGS",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 1. Sound FX toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "☄️ SOUND SYNTH FX", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Laser sweeps and noise pops", color = Color.LightGray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = soundCached,
                        onCheckedChange = { viewModel.toggleSounds(it, musicCached, vibeCached) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00ADB5)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(14.dp))

                // 2. Music toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "🎵 SYNTHWAVE BGM CHORDS", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Real-time synthesized music chords", color = Color.LightGray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = musicCached,
                        onCheckedChange = { viewModel.toggleSounds(soundCached, it, vibeCached) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00ADB5)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(14.dp))

                // 3. Vibration toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "📳 HAPTIC COGNITIVE VIBRATIONS", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Haptic ticks when striking enemies", color = Color.LightGray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = vibeCached,
                        onCheckedChange = { viewModel.toggleSounds(soundCached, musicCached, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00ADB5)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sub options Card (Bonus & Reset profiles)
            NeonCard(
                borderColor = Color.Gray,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "GENERAL SYSTEMS",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Language
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🌐 SECTOR DIALECT", color = Color.LightGray, fontSize = 14.sp)
                    Text("English (U.S.) ⚡", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(10.dp))

                // Rate US play store trigger
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.rumesh.games.stellarspectrum"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.rumesh.games.stellarspectrum"))
                                context.startActivity(intent)
                            }
                        }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("★ STRIKE RATING US", color = Color.LightGray, fontSize = 14.sp)
                    Text("PLAY STORE 🔗", color = Color(0xFF00ADB5), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Format database button
                NeonButton(
                    text = "💥 DEPLOY DESTRUCTIVE LOG DATA RESET",
                    onClick = { showClearWarningPopup = true },
                    buttonColor = Color(0xFFFF2E63),
                    vibrationEnabled = vibeCached,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Developers information credits
            NeonCard(
                borderColor = Color(0xFFBD00FF),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ENGINE REPUTATIONS CREDIT",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Stellar Spectrum (Model-Conversion Edition)\nBuilt with Kotlin, Jetpack Compose, & Room Databases.\nOriginal Architecture Spec: Python & Pygame Buildozer blueprint.\n\nMaster Coder: Google Antigravity Agent Core AI.\nCopyright © 2026. All rights preserved.",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // WARNING Modal confirmation popup overlay
        if (showClearWarningPopup) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable { showClearWarningPopup = false },
                contentAlignment = Alignment.Center
            ) {
                NeonCard(
                    borderColor = Color(0xFFFF2E63),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .testTag("clear_save_popup")
                        .clickable(enabled = false) {}
                ) {
                    Text(
                        text = "⚠️ MEMORY FORMAT STRIKE WARNING",
                        color = Color(0xFFFF2E63),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "CONFIRM SYSTEM RESET?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "This action is destructive and irreversible! Clears all crystal reserves, unlocked combat ships, galactic high scores, and achievement counts completely from memory storage.",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NeonButton(
                            text = "ABORT RESET",
                            onClick = { showClearWarningPopup = false },
                            buttonColor = Color.Gray,
                            vibrationEnabled = vibrationEnabled,
                            modifier = Modifier.weight(1f)
                        )

                        NeonButton(
                            text = "YES, PURGE MEMORY!",
                            onClick = {
                                showClearWarningPopup = false
                                viewModel.resetAllProfileData()
                                viewModel.changeScreen(GameScreen.SPLASH) // returns to splash triggers reload
                            },
                            buttonColor = Color(0xFFFF2E63),
                            vibrationEnabled = vibrationEnabled,
                            testTag = "confirm_reset_action",
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                }
            }
        }
    }
}
