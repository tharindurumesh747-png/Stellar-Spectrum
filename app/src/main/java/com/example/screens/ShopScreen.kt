package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.core.ExplosionDefinition
import com.example.core.GameData
import com.example.core.GameScreen
import com.example.core.GameViewModel
import com.example.core.SoundSynth
import com.example.core.TrailDefinition
import com.example.ui.NeonButton
import com.example.ui.NeonCard
import com.example.ui.SynthwaveGridBackground
import com.example.ui.triggerVibration

@Composable
fun ShopScreen(viewModel: GameViewModel) {
    val progress by viewModel.userProgress.collectAsStateWithLifecycle()
    val crystalCount = progress?.crystals ?: 0
    val vibrationEnabled = progress?.vibrationEnabled ?: true
    val unlockedTrails = progress?.getUnlockedTrails() ?: emptyList()
    val unlockedExplosions = progress?.getUnlockedExplosions() ?: emptyList()
    val selectedTrail = progress?.selectedTrail ?: "default"
    val selectedExplosion = progress?.selectedExplosion ?: "default"

    var selectedTab by remember { mutableStateOf("ENGINE_TRAILS") }
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().testTag("shop_screen_root")) {
        SynthwaveGridBackground()

        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.testTag("back_to_menu_button").size(44.dp).clip(CircleShape)
                        .background(Color(0xFF1F1F35).copy(alpha = 0.85f))
                        .border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                        .clickable { viewModel.changeScreen(GameScreen.MAIN_MENU) }
                ) { Text("◀", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }

                Text("SPECTRUM ESTHETICS", fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    color = Color.White, letterSpacing = 1.6.sp)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF0E111F))
                        .padding(vertical = 6.dp, horizontal = 12.dp)
                ) {
                    Text("💎 $crystalCount", color = Color(0xFF00ADB5), fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(25.dp))

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF10111F)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tab1Sel = selectedTab == "ENGINE_TRAILS"
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                        .background(if (tab1Sel) Color(0xFFFF2E63) else Color.Transparent)
                        .clickable { selectedTab = "ENGINE_TRAILS" }.padding(vertical = 10.dp)
                ) { Text("TRAILS", color = if (tab1Sel) Color.White else Color.Gray,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold) }

                val tab2Sel = selectedTab == "EXPLOSIONS"
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                        .background(if (tab2Sel) Color(0xFFFF2E63) else Color.Transparent)
                        .clickable { selectedTab = "EXPLOSIONS" }.padding(vertical = 10.dp)
                ) { Text("EXPLOSIONS", color = if (tab2Sel) Color.White else Color.Gray,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }

            Spacer(modifier = Modifier.height(25.dp))

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (selectedTab == "ENGINE_TRAILS") {
                    GameData.trails.forEach { item ->
                        val isOwned = item.id == "default" || unlockedTrails.contains(item.id)
                        val isEquipped = selectedTrail == item.id
                        TrailExplosionCard(
                            name = item.name, description = item.description, hint = item.visualHint,
                            cost = item.cost, colorHex = item.colorHex,
                            crystalCount = crystalCount, isOwned = isOwned, isEquipped = isEquipped,
                            vibrationEnabled = vibrationEnabled,
                            onBuy = {
                                viewModel.buyTrail(item.id, item.cost)
                                triggerVibration(context, 40)
                            },
                            onEquip = { viewModel.selectTrail(item.id) }
                        )
                    }
                } else {
                    GameData.explosions.forEach { item ->
                        val isOwned = item.id == "default" || unlockedExplosions.contains(item.id)
                        val isEquipped = selectedExplosion == item.id
                        TrailExplosionCard(
                            name = item.name, description = item.description, hint = item.visualHint,
                            cost = item.cost, colorHex = item.colorHex,
                            crystalCount = crystalCount, isOwned = isOwned, isEquipped = isEquipped,
                            vibrationEnabled = vibrationEnabled,
                            onBuy = {
                                viewModel.buyExplosion(item.id, item.cost)
                                triggerVibration(context, 40)
                            },
                            onEquip = { viewModel.selectExplosion(item.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun TrailExplosionCard(
    name: String, description: String, hint: String, cost: Int, colorHex: Long,
    crystalCount: Int, isOwned: Boolean, isEquipped: Boolean, vibrationEnabled: Boolean,
    onBuy: () -> Unit, onEquip: () -> Unit
) {
    val canAfford = crystalCount >= cost
    val borderColor = when {
        isEquipped -> Color(0xFF10B981)
        isOwned -> Color(colorHex)
        canAfford -> Color(0xFF00ADB5)
        else -> Color.DarkGray
    }

    NeonCard(borderColor = borderColor, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(hint, fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        if (isEquipped) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("EQUIPPED", color = Color(0xFF10B981), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(description, color = Color.LightGray, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            when {
                isEquipped -> {
                    Box(
                        modifier = Modifier.width(90.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("ACTIVE", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
                isOwned -> {
                    NeonButton(
                        text = "EQUIP",
                        onClick = onEquip,
                        buttonColor = Color(0xFF00ADB5),
                        vibrationEnabled = vibrationEnabled,
                        modifier = Modifier.width(90.dp)
                    )
                }
                else -> {
                    NeonButton(
                        text = "💎 $cost",
                        onClick = {
                            if (canAfford) { onBuy(); SoundSynth.playPowerup() }
                            else SoundSynth.playHitWrong()
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
}
