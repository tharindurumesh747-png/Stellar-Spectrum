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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
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
import com.example.entities.EnemyType
import com.example.entities.EnergyColor
import com.example.entities.ParticleType
import com.example.ui.NeonButton
import com.example.ui.NeonCard
import com.example.ui.drawProceduralShip
import com.example.ui.triggerVibration
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import android.view.MotionEvent

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameplayScreen(viewModel: GameViewModel) {
    val progress by viewModel.userProgress.collectAsStateWithLifecycle()
    val playState by viewModel.gameplayState.collectAsStateWithLifecycle()
    val playerShip by viewModel.playerShipState.collectAsStateWithLifecycle()
    val vibrationEnabled = progress?.vibrationEnabled ?: true

    var isPaused by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Track last touch position for continuous drag
    var lastTouchX by remember { mutableStateOf(0f) }
    var lastTouchY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Background star layers parallax
    val starCount = 35
    val starsList = remember {
        List(starCount) {
            Offset(
                x = (0..1080).random().toFloat(),
                y = (0..1920).random().toFloat()
            ) to (1..3).random()
        }
    }

    // 60 FPS game loop
    var lastTimeNanos by remember { mutableStateOf(System.nanoTime()) }
    LaunchedEffect(isPaused, playState.gameEnded, playState.levelComplete) {
        while (!isPaused && !playState.gameEnded && !playState.levelComplete) {
            delay(16)
            val now = System.nanoTime()
            val dt = ((now - lastTimeNanos) / 1_000_000_000f).coerceIn(0.01f, 0.033f)
            lastTimeNanos = now
            viewModel.updateGame(dt)

            // Auto-fire toward center-top when dragging
            if (isDragging) {
                viewModel.fireBulletAt(lastTouchX, lastTouchY - 200f)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("gameplay_root")
            .background(Color(0xFF030308))
            .pointerInteropFilter { event ->
                val tx = event.x
                val ty = event.y
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isDragging = true
                        lastTouchX = tx
                        lastTouchY = ty
                        // Move ship to touch position immediately
                        viewModel.playerShipState.value.targetX = tx.coerceIn(80f, 1000f)
                        viewModel.playerShipState.value.targetY = ty.coerceIn(200f, 1600f)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        lastTouchX = tx
                        lastTouchY = ty
                        // Continuously update ship target to follow finger
                        viewModel.playerShipState.value.targetX = tx.coerceIn(80f, 1000f)
                        viewModel.playerShipState.value.targetY = ty.coerceIn(200f, 1600f)
                        // Auto fire toward top while moving
                        viewModel.fireBulletAt(tx, ty - 300f)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isDragging = false
                        true
                    }
                    else -> false
                }
            }
    ) {

        // Canvas for stars + all entities
        Canvas(modifier = Modifier
            .fillMaxSize()
            .testTag("gameplay_canvas")) {
            val spaceWidth = size.width
            val spaceHeight = size.height

            // 1. Star parallax layers
            starsList.forEach { (coord, speedTier) ->
                val scrollFactor = 80f * speedTier
                val timeFactor = (System.currentTimeMillis() % 1000000) / 1000f
                val activeY = (coord.y + timeFactor * scrollFactor) % spaceHeight
                val bubbleColor = when (speedTier) {
                    3 -> Color(0xFF00ADB5).copy(alpha = 0.55f)
                    2 -> Color(0xFFBD00FF).copy(alpha = 0.42f)
                    else -> Color.White.copy(alpha = 0.35f)
                }
                drawCircle(color = bubbleColor, radius = speedTier.toFloat() * 1.2f,
                    center = Offset(coord.x, activeY))
            }

            // 2. Draw enemies
            playState.activeEnemies.forEach { enemy ->
                val glowAlpha = 0.22f + 0.12f * sin(
                    (System.currentTimeMillis() - enemy.spawnTime).toDouble() / 150.0).toFloat()

                when (enemy.type) {
                    EnemyType.PULSE -> {
                        val diamondPath = Path().apply {
                            moveTo(enemy.x, enemy.y - 32f)
                            lineTo(enemy.x + 32f, enemy.y)
                            lineTo(enemy.x, enemy.y + 32f)
                            lineTo(enemy.x - 32f, enemy.y)
                            close()
                        }
                        drawPath(diamondPath, enemy.auraColor.composeColor.copy(alpha = glowAlpha),
                            style = Stroke(width = 8.dp.toPx()))
                        drawPath(diamondPath, enemy.auraColor.composeColor,
                            style = Stroke(width = 3.dp.toPx()))
                        drawPath(diamondPath, Color.White, style = Stroke(width = 1.dp.toPx()))
                    }
                    EnemyType.SPLIT -> {
                        val hexPath = Path().apply {
                            for (i in 0..5) {
                                val angle = i * Math.PI / 3.0
                                val px = enemy.x + cos(angle).toFloat() * 36f
                                val py = enemy.y + sin(angle).toFloat() * 36f
                                if (i == 0) moveTo(px, py) else lineTo(px, py)
                            }
                            close()
                        }
                        drawPath(hexPath, enemy.auraColor.composeColor.copy(alpha = glowAlpha),
                            style = Stroke(width = 8.dp.toPx()))
                        drawPath(hexPath, enemy.auraColor.composeColor,
                            style = Stroke(width = 3.dp.toPx()))
                    }
                    EnemyType.WARP -> {
                        val starPath = Path().apply {
                            val numPoints = 6
                            for (i in 0 until numPoints * 2) {
                                val radius = if (i % 2 == 0) 38f else 18f
                                val angle = i * Math.PI / numPoints
                                val px = enemy.x + cos(angle).toFloat() * radius
                                val py = enemy.y + sin(angle).toFloat() * radius
                                if (i == 0) moveTo(px, py) else lineTo(px, py)
                            }
                            close()
                        }
                        drawPath(starPath, enemy.auraColor.composeColor.copy(alpha = glowAlpha),
                            style = Stroke(width = 8.dp.toPx()))
                        drawPath(starPath, enemy.auraColor.composeColor,
                            style = Stroke(width = 3.dp.toPx()))
                    }
                    EnemyType.SHIELD -> {
                        drawCircle(enemy.auraColor.composeColor.copy(alpha = 0.12f),
                            48f, Offset(enemy.x, enemy.y))
                        drawCircle(enemy.auraColor.composeColor, 48f, Offset(enemy.x, enemy.y),
                            style = Stroke(width = 3.dp.toPx()))
                        val tPulse = (System.currentTimeMillis() % 1000).toFloat() / 1000f
                        drawCircle(enemy.auraColor.composeColor.copy(alpha = 1f - tPulse),
                            32f + tPulse * 16f, Offset(enemy.x, enemy.y),
                            style = Stroke(width = 1.dp.toPx()))
                    }
                    EnemyType.PRISM -> {
                        val triPath = Path().apply {
                            for (i in 0..2) {
                                val angle = (i * 2.0 * Math.PI / 3.0) +
                                    Math.toRadians(enemy.rotationAngle.toDouble())
                                val px = enemy.x + cos(angle).toFloat() * 34f
                                val py = enemy.y + sin(angle).toFloat() * 34f
                                if (i == 0) moveTo(px, py) else lineTo(px, py)
                            }
                            close()
                        }
                        drawPath(triPath, enemy.auraColor.composeColor.copy(alpha = glowAlpha),
                            style = Stroke(width = 8.dp.toPx()))
                        drawPath(triPath, enemy.auraColor.composeColor,
                            style = Stroke(width = 3.dp.toPx()))
                    }
                    EnemyType.BOSS -> {
                        drawCircle(enemy.auraColor.composeColor.copy(alpha = 0.1f),
                            130f, Offset(enemy.x, enemy.y))
                        drawCircle(enemy.auraColor.composeColor, 110f, Offset(enemy.x, enemy.y),
                            style = Stroke(width = 5.dp.toPx()))
                        drawCircle(Color.White, 80f, Offset(enemy.x, enemy.y),
                            style = Stroke(width = 1.dp.toPx()))
                        for (i in 0..7) {
                            val angle = (i * Math.PI / 4.0) +
                                Math.toRadians(enemy.rotationAngle.toDouble())
                            val px1 = enemy.x + cos(angle).toFloat() * 80f
                            val py1 = enemy.y + sin(angle).toFloat() * 80f
                            val px2 = enemy.x + cos(angle).toFloat() * 110f
                            val py2 = enemy.y + sin(angle).toFloat() * 110f
                            drawLine(enemy.auraColor.composeColor, Offset(px1, py1),
                                Offset(px2, py2), 3.dp.toPx())
                        }
                    }
                }
            }

            // 3. Player lasers
            playState.activeBullets.forEach { bullet ->
                val beamColor = bullet.color.composeColor
                val beamSize = if (bullet.color == EnergyColor.BLUE) 12f else 6f
                drawLine(beamColor.copy(alpha = 0.35f),
                    Offset(bullet.x, bullet.y + 15f), Offset(bullet.x, bullet.y - 15f),
                    beamSize * 2.5f)
                drawLine(beamColor,
                    Offset(bullet.x, bullet.y + 12f), Offset(bullet.x, bullet.y - 12f),
                    beamSize)
                drawLine(Color.White,
                    Offset(bullet.x, bullet.y + 6f), Offset(bullet.x, bullet.y - 6f),
                    beamSize * 0.4f)
            }

            // 4. Player ship
            drawProceduralShip(
                shipId = playerShip.selectedShipId,
                center = Offset(playerShip.x, playerShip.y),
                size = 110f,
                paintColor = playerShip.currentWeaponColor.composeColor
            )

            // Shield bubble
            if (playerShip.isShieldActive) {
                val tCycle = (System.currentTimeMillis() % 600) / 600f
                val shieldColor = playerShip.currentWeaponColor.composeColor
                drawCircle(shieldColor.copy(alpha = 0.2f), 90f, Offset(playerShip.x, playerShip.y))
                drawCircle(shieldColor.copy(alpha = 1f - tCycle), 80f + tCycle * 18f,
                    Offset(playerShip.x, playerShip.y), style = Stroke(width = 2.dp.toPx()))
            }

            // 5. Particles
            playState.activeParticles.forEach { part ->
                when (part.type) {
                    ParticleType.EXPLOSION -> drawCircle(
                        part.color.copy(alpha = part.life / part.maxLife),
                        part.size * (part.life / part.maxLife),
                        Offset(part.x, part.y))
                    ParticleType.TRAIL -> drawCircle(
                        part.color.copy(alpha = (part.life / part.maxLife) * 0.5f),
                        part.size * (part.life / part.maxLife) * 0.8f,
                        Offset(part.x, part.y))
                    ParticleType.SHOCKWAVE -> {
                        val fraction = 1.0f - (part.life / part.maxLife)
                        drawCircle(part.color.copy(alpha = (part.life / part.maxLife) * 0.8f),
                            40f + fraction * 180f, Offset(part.x, part.y),
                            style = Stroke(width = 3.dp.toPx()))
                    }
                    ParticleType.SCREEN_FLASH -> drawRect(
                        part.color.copy(alpha = (part.life / part.maxLife) * 0.3f), size = size)
                    else -> drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.YELLOW
                            textSize = part.size
                            style = android.graphics.Paint.Style.FILL
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                        drawText(part.text, part.x, part.y, paint)
                    }
                }
            }

            // Drag target indicator — shows where ship is moving
            if (isDragging) {
                val col = playerShip.currentWeaponColor.composeColor
                drawCircle(col.copy(alpha = 0.15f), 35f, Offset(lastTouchX, lastTouchY))
                drawCircle(col.copy(alpha = 0.4f), 35f, Offset(lastTouchX, lastTouchY),
                    style = Stroke(width = 2f))
            }
        }

        // ── TOP HUD BAR ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .testTag("gameplay_hud"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.width(130.dp)) {
                val ratio = playerShip.hp.toFloat() / playerShip.maxHp.toFloat()
                val progressColor = when {
                    ratio > 0.6f -> Color(0xFF10B981)
                    ratio > 0.3f -> Color(0xFFFBBF24)
                    else -> Color(0xFFFF2E63)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("❤️ ", fontSize = 11.sp)
                    Text("HP: ${playerShip.hp}/${playerShip.maxHp}",
                        color = Color.White, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth().height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF10101C))
                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                ) {
                    Box(modifier = Modifier.fillMaxHeight()
                        .fillMaxWidth(ratio.coerceIn(0f, 1f))
                        .background(progressColor))
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${playState.score}", fontSize = 24.sp, fontWeight = FontWeight.Black,
                    color = Color.White, fontFamily = FontFamily.Monospace)
                Text("WAVE ${playState.currentWave} / ${playState.totalWaves}",
                    fontSize = 10.sp, color = Color.LightGray,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("💎 ${playState.crystalsCollectedThisRun}", fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, color = Color(0xFF00ADB5),
                    fontFamily = FontFamily.Monospace)
                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier.testTag("pause_game_button")
                        .size(36.dp).clip(CircleShape)
                        .background(Color(0xFF1F1F35).copy(alpha = 0.8f))
                        .border(1.dp, Color.DarkGray, CircleShape)
                        .clickable { isPaused = true }
                ) { Text("⏸", color = Color.White, fontSize = 13.sp) }
            }
        }

        // Boss HP bar
        if (playState.isBossFight && playState.activeEnemies.isNotEmpty()) {
            val bossObj = playState.activeEnemies.first()
            val hpRatio = bossObj.hp.toFloat() / bossObj.maxHp.toFloat()
            Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                .padding(top = 100.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⚠️ BOSS ACTIVE - PHASE ${bossObj.bossPhase}",
                    color = Color(0xFFFF2E63), fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth().height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.5.dp, Color(0xFFFF2E63), RoundedCornerShape(5.dp))
                ) {
                    Box(modifier = Modifier.fillMaxHeight()
                        .fillMaxWidth(hpRatio.coerceIn(0f, 1f))
                        .background(Color(0xFFFF2E63)))
                }
            }
        }

        // ── BOTTOM CONTROLS ──────────────────────────────────────
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(14.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {

                // Color wheel 2x2
                Column(modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.DarkGray.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ColorDotItem(EnergyColor.RED,
                            playerShip.currentWeaponColor == EnergyColor.RED) {
                            viewModel.switchWeaponColor(EnergyColor.RED) }
                        ColorDotItem(EnergyColor.BLUE,
                            playerShip.currentWeaponColor == EnergyColor.BLUE) {
                            viewModel.switchWeaponColor(EnergyColor.BLUE) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ColorDotItem(EnergyColor.GREEN,
                            playerShip.currentWeaponColor == EnergyColor.GREEN) {
                            viewModel.switchWeaponColor(EnergyColor.GREEN) }
                        ColorDotItem(EnergyColor.PURPLE,
                            playerShip.currentWeaponColor == EnergyColor.PURPLE) {
                            viewModel.switchWeaponColor(EnergyColor.PURPLE) }
                    }
                }

                // Combo display
                if (playState.comboMultiplier > 1) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFBBF24).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(vertical = 4.dp, horizontal = 12.dp)) {
                        Text("${playState.comboMultiplier}X COMBO",
                            color = Color(0xFFFBBF24), fontSize = 14.sp,
                            fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.height(2.dp))
                        Box(modifier = Modifier.width(60.dp).height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp)).background(Color.DarkGray)) {
                            Box(modifier = Modifier.fillMaxHeight()
                                .fillMaxWidth(playState.comboTimerRemaining / 2.5f)
                                .background(Color(0xFFFBBF24)))
                        }
                    }
                }

                // Shield button
                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier.testTag("shield_button").size(76.dp).clip(CircleShape)
                        .background(Color(0xFF10111F).copy(alpha = 0.85f))
                        .clickable(enabled = playerShip.shieldCharge >= 1.0f) {
                            viewModel.activateShield() }) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(Color.Gray.copy(alpha = 0.3f), size.width / 2f - 4f,
                            style = Stroke(width = 4.dp.toPx()))
                        drawArc(
                            color = if (playerShip.shieldCharge >= 1f) Color(0xFF00ADB5)
                                    else Color(0xFFFF2E63),
                            startAngle = -90f,
                            sweepAngle = playerShip.shieldCharge * 360f,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx()))
                    }
                    Text(
                        text = if (playerShip.shieldCharge >= 1.0f) "SHIELD\nREADY"
                               else "🛡️\n${(playerShip.shieldCharge * 100).toInt()}%",
                        color = if (playerShip.shieldCharge >= 1.0f) Color.White else Color.Gray,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, lineHeight = 12.sp)
                }
            }
        }

        // ── PAUSE OVERLAY ────────────────────────────────────────
        if (isPaused) {
            Box(modifier = Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable { isPaused = false },
                contentAlignment = Alignment.Center) {
                NeonCard(borderColor = Color(0xFF00ADB5),
                    modifier = Modifier.fillMaxWidth(0.85f).clickable(enabled = false) {}) {
                    Text("⏸ PAUSED", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                        color = Color.White, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(20.dp))
                    NeonButton("RESUME", onClick = { isPaused = false },
                        buttonColor = Color(0xFF10B981), vibrationEnabled = vibrationEnabled,
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    NeonButton("RESTART", onClick = {
                        isPaused = false
                        viewModel.selectWorldAndStart(playState.worldIndex) },
                        buttonColor = Color(0xFF3B82F6), vibrationEnabled = vibrationEnabled,
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    NeonButton("MAIN MENU", onClick = {
                        isPaused = false
                        viewModel.changeScreen(GameScreen.WORLD_SELECT) },
                        buttonColor = Color(0xFFFF2E63), vibrationEnabled = vibrationEnabled,
                        modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // ── VICTORY SCREEN ───────────────────────────────────────
        if (playState.levelComplete) {
            Box(modifier = Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center) {
                NeonCard(borderColor = Color(0xFFFBBF24),
                    modifier = Modifier.fillMaxWidth(0.88f)) {
                    Text("🛰️ PORTAL CLEARED!", fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold, color = Color(0xFFFBBF24),
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFFBBF24).copy(alpha = 0.3f))
                    Spacer(Modifier.height(14.dp))
                    Text("${playState.score}", fontSize = 28.sp, fontWeight = FontWeight.Black,
                        color = Color.White, fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text("💎 ${playState.crystalsCollectedThisRun}", fontSize = 16.sp,
                        fontWeight = FontWeight.Bold, color = Color(0xFF00ADB5),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp))
                    val stars = when {
                        playState.wrongMatchesCount == 0 -> 3
                        playState.wrongMatchesCount < 3 -> 2
                        else -> 1
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
                        horizontalArrangement = Arrangement.Center) {
                        repeat(3) { i ->
                            Text("★", color = if (i < stars) Color(0xFFFBBF24) else Color.DarkGray,
                                fontSize = 32.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                    NeonButton("NEXT GALAXY", onClick = {
                        viewModel.changeScreen(GameScreen.WORLD_SELECT) },
                        buttonColor = Color(0xFF10B981), vibrationEnabled = vibrationEnabled,
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    NeonButton("MAIN MENU", onClick = {
                        viewModel.changeScreen(GameScreen.MAIN_MENU) },
                        buttonColor = Color.LightGray, vibrationEnabled = vibrationEnabled,
                        modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // ── GAME OVER SCREEN ─────────────────────────────────────
        if (playState.gameEnded) {
            Box(modifier = Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center) {
                NeonCard(borderColor = Color(0xFFFF2E63),
                    modifier = Modifier.fillMaxWidth(0.85f)) {
                    Text("GAME OVER", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFF2E63), textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFFF2E63).copy(alpha = 0.3f))
                    Spacer(Modifier.height(14.dp))
                    Text("FINAL SCORE", fontSize = 11.sp, color = Color.LightGray,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text("${playState.score}", fontSize = 32.sp, fontWeight = FontWeight.Black,
                        color = Color.White, fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                    Text("💎 ${playState.crystalsCollectedThisRun} crystals\n" +
                         "Wrong hits: ${playState.wrongMatchesCount}",
                        fontSize = 13.sp, color = Color.LightGray,
                        textAlign = TextAlign.Center, lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp))
                    NeonButton("RETRY", onClick = {
                        viewModel.selectWorldAndStart(playState.worldIndex) },
                        buttonColor = Color(0xFF00ADB5), vibrationEnabled = vibrationEnabled,
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    NeonButton("MAIN MENU", onClick = {
                        viewModel.changeScreen(GameScreen.MAIN_MENU) },
                        buttonColor = Color.LightGray, vibrationEnabled = vibrationEnabled,
                        modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// ── COLOR DOT BUTTON ─────────────────────────────────────────────
@Composable
fun ColorDotItem(colorObj: EnergyColor, isSelected: Boolean, onClick: () -> Unit) {
    val sizeVal by animateDpAsState(
        targetValue = if (isSelected) 54.dp else 44.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium), label = "wheel_scale")
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier.size(sizeVal).clip(CircleShape)
            .background(colorObj.composeColor)
            .border(width = if (isSelected) 3.1.dp else 0.dp,
                color = if (isSelected) Color.White else Color.Transparent,
                shape = CircleShape)
            .clickable { onClick() }) {
        if (isSelected) {
            val inf = rememberInfiniteTransition(label = "border_shift")
            val ratio by inf.animateFloat(0.5f, 1.0f,
                infiniteRepeatable(tween(1000, easing = LinearEasing),
                    RepeatMode.Reverse), label = "circle_ratio")
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color.White.copy(alpha = ratio * 0.4f),
                    size.width / 2f + 8.dp.toPx() * ratio,
                    style = Stroke(width = 1.5.dp.toPx()))
            }
        }
    }
}
