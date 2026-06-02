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
import com.example.core.GameScreen
import com.example.core.GameViewModel
import com.example.core.SoundSynth
import com.example.ui.NeonButton
import com.example.ui.NeonCard
import com.example.ui.SynthwaveGridBackground
import com.example.ui.triggerVibration

data class ShopItem(
    val id: String,
    val name: String,
    val cost: Int,
    val description: String,
    val visualHint: String
)

@Composable
fun ShopScreen(viewModel: GameViewModel) {
    val progress by viewModel.userProgress.collectAsStateWithLifecycle()
    val crystalCount = progress?.crystals ?: 0
    val vibrationEnabled = progress?.vibrationEnabled ?: true

    var selectedTab by remember { mutableStateOf("ENGINE_TRAILS") }

    val context = LocalContext.current

    // Aesthetic shop catalogs
    val trailItems = listOf(
        ShopItem("cyan_fume", "Cyan Fume Exhaust", 150, "Glow streams cyan exhaust smoke trail.", "☄️"),
        ShopItem("purple_warp", "Wormhole Quantum", 350, "Discharges pulsing gravitational purple rings.", "🌀"),
        ShopItem("gold_dust", "Crystalline Sparkle", 600, "Sheds sparkling gold dust coordinates behind wings.", "✨"),
        ShopItem("omega_clon", "Omega Spectral Echo", 1200, "Leaves a glowing white static silhouette tail.", "👥")
    )

    val explosionItems = listOf(
        ShopItem("cosmic_ring", "Radial Ring Burst", 200, "On collision, spawns a massive expands circle.", "⭕"),
        ShopItem("pixel_shrapnel", "Retro Cube Shrapnel", 450, "Spawns retro square shards upon drones deaths.", "⏹️"),
        ShopItem("chroma_flicker", "Chromic Aberration Flash", 800, "Briefly flickers full screen chromatic scale tints.", "🌈")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("shop_screen_root")
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
                    text = "SPECTRUM ESTHETICS",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.6.sp
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

            // Custom sliding categorical tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF10111F))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tab1Sel = selectedTab == "ENGINE_TRAILS"
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (tab1Sel) Color(0xFFFF2E63) else Color.Transparent)
                        .clickable { selectedTab = "ENGINE_TRAILS" }
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = "TRAILS",
                        color = if (tab1Sel) Color.White else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val tab2Sel = selectedTab == "EXPLOSIONS"
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (tab2Sel) Color(0xFFFF2E63) else Color.Transparent)
                        .clickable { selectedTab = "EXPLOSIONS" }
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = "EXPLOSIONS",
                        color = if (tab2Sel) Color.White else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(25.dp))

            // Active list display based on selected tab
            val displayList = if (selectedTab == "ENGINE_TRAILS") trailItems else explosionItems
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                displayList.forEach { item ->
                    val canAfford = crystalCount >= item.cost
                    
                    NeonCard(
                        borderColor = if (canAfford) Color(0xFF00ADB5) else Color.DarkGray,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left col description
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.visualHint,
                                    fontSize = 28.sp,
                                    modifier = Modifier.padding(trailing = 12.dp)
                                )
                                Column {
                                    Text(
                                        text = item.name,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.description,
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Buy button
                            NeonButton(
                                text = "💎 ${item.cost}",
                                onClick = {
                                    if (canAfford) {
                                        viewModel.buyShip("aesthetic_dummy", item.cost) // deducts crystals safely
                                        SoundSynth.playPowerup()
                                        spawnCustomSparkles(context)
                                    } else {
                                        SoundSynth.playHitWrong()
                                    }
                                },
                                enabled = canAfford,
                                buttonColor = Color(0xFF00ADB5),
                                vibrationEnabled = vibrationEnabled,
                                modifier = Modifier.width(90.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

private fun spawnCustomSparkles(context: Context) {
    // Triggers feedback vibration haptically
    triggerVibration(context, 40)
}
