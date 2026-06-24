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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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

// Virtual design-space resolution that all game logic (enemy.x, bullet.x, etc.)
// is written against. We scale real touch/screen pixels to/from this space so
// gameplay behaves identically on every device.
private const val VW = 1080f
private const val VH = 1920f

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameplayScreen(viewModel: GameViewModel) {
    val progress by viewModel.userProgress.collectAsStateWithLifecycle()
    val playState by viewModel.gameplayState.collectAsStateWithLifecycle()
    val playerShip by viewModel.playerShipState.collectAsStateWithLifecycle()
    val vibrationEnabled = progress?.vibrationEnabled ?: true

    var isPaused by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var lastTouchX by remember { mutableStateOf(0f) }   // virtual-space coords
    var lastTouchY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val starCount = 35
    val starsList = remember {
        List(starCount) {
            Offset(
                x = (0..VW.toInt()).random().toFloat(),
                y = (0..VH.toInt()).random().toFloat()
            ) to (1..3).random()
        }
    }

    var lastTimeNanos by remember { mutableStateOf(System.nanoTime()) }
    LaunchedEffect(isPaused, playState.gameEnded, playState.levelComplete) {
        while (!isPaused && !playState.gameEnded && !playState.levelComplete) {
            delay(16)
            val now = System.nanoTime()
            val dt = ((now - lastTimeNanos) / 1_000_000_000f).coerceIn(0.01f, 0.033f)
            lastTimeNanos = now
            viewModel.updateGame(dt)
            if (isDragging) {
                viewModel.fireBulletAt(lastTouchX, lastTouchY - 200f)
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("gameplay_root")
            .background(Color(0xFF030308))
    ) {
        val density = LocalDensity.current
        val realWpx = with(density) { maxWidth.toPx() }
        val realHpx = with(density) { maxHeight.toPx() }
        val scaleX = if (realWpx > 0f) realWpx / VW else 1f
        val scaleY = if (realHpx > 0f) realHpx / VH else 1f

        // Exclusion zones (virtual-space rectangles) where the play-area touch
        // handler must NOT capture the touch, so the real Compose buttons
        // underneath (pause / shield / color wheel) still receive clicks.
        fun inPauseZone(vx: Float, vy: Float) = vx > VW - 160f && vy < 220f
        fun inShieldZone(vx: Float, vy: Float) = vx > VW - 220f && vy > VH - 260f
        fun inColorWheelZone(vx: Float, vy: Float) = vx < 220f && vy > VH - 260f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { event ->
                    if (playState.gameEnded || playState.levelComplete || isPaused) {
                        return@pointerInteropFilter false
                    }
                    val vx = (event.x / scaleX)
                    val vy = (event.y / scaleY)

                    if (inPauseZone(vx, vy) || inShieldZone(vx, vy) || inColorWheelZone(vx, vy)) {
                        isDragging = false
                        return@pointerInteropFilter false // let the real button handle it
                    }

                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            isDragging = true
                            lastTouchX = vx; lastTouchY = vy
                            viewModel.playerShipState.value.targetX = vx.coerceIn(80f, 1000f)
                            viewModel.playerShipState.value.targetY = vy.coerceIn(200f, 1600f)
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            lastTouchX = vx; lastTouchY = vy
                            viewModel.playerShipState.value.targetX = vx.coerceIn(80f, 1000f)
                            viewModel.playerShipState.value.targetY = vy.coerceIn(200f, 1600f)
                            viewModel.fireBulletAt(vx, vy - 300f)
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
            Canvas(modifier = Modifier
                .fillMaxSize()
                .testTag("gameplay_canvas")) {
                // Map the virtual 1080x1920 design space onto the real canvas size
                scale(scaleX, scaleY, pivot = Offset.Zero) {
                    val spaceWidth = VW
                    val spaceHeight = VH

                    // 1. Stars
                    starsList.forEach { (coord, speedTier) ->
                        val scrollFactor = 80f * speedTier
                        val timeFactor = (System.currentTimeMillis() % 1000000) / 1000f
                        val activeY = (coord.y + timeFactor * scrollFactor) % spaceHeight
                        val bubbleColor = when (speedTier) {
                            3 -> Color(0xFF00ADB5).copy(alpha = 0.55f)
                            2 -> Color(0xFFBD00FF).copy(alpha = 0.42f)
                            else -> Color.White.copy(alpha = 0.35f)
                        }
                        drawCircle(bubbleColor, speedTier.toFloat() * 1.2f, Offset(coord.x, activeY))
                    }

                    // 2. Enemies — filled bodies (not just outlines) so they read
                    // clearly as solid threats, with a bright contrasting rim.
                    playState.activeEnemies.forEach { enemy ->
                        val glowAlpha = 0.22f + 0.12f * sin(
                            (System.currentTimeMillis() - enemy.spawnTime).toDouble() / 150.0).toFloat()
                        val fillCol = enemy.auraColor.composeColor

                        when (enemy.type) {
                            EnemyType.PULSE -> {
                                val p = Path().apply {
                                    moveTo(enemy.x, enemy.y - 32f)
                                    lineTo(enemy.x + 32f, enemy.y)
                                    lineTo(enemy.x, enemy.y + 32f)
                                    lineTo(enemy.x - 32f, enemy.y)
                                    close()
                                }
                                drawPath(p, fillCol.copy(alpha = glowAlpha + 0.5f))
                                drawPath(p, fillCol, style = Stroke(width = 4.dp.toPx()))
                                drawPath(p, Color.White, style = Stroke(width = 1.5.dp.toPx()))
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
                                drawPath(hexPath, fillCol.copy(alpha = 0.55f))
                                drawPath(hexPath, fillCol, style = Stroke(width = 4.dp.toPx()))
                                drawPath(hexPath, Color.White, style = Stroke(width = 1.dp.toPx()))
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
                                drawPath(starPath, fillCol.copy(alpha = 0.5f))
                                drawPath(starPath, fillCol, style = Stroke(width = 4.dp.toPx()))
                                drawPath(starPath, Color.White, style = Stroke(width = 1.dp.toPx()))
                            }
                            EnemyType.SHIELD -> {
                                drawCircle(fillCol.copy(alpha = 0.45f), 48f, Offset(enemy.x, enemy.y))
                                drawCircle(fillCol, 48f, Offset(enemy.x, enemy.y),
                                    style = Stroke(width = 4.dp.toPx()))
                                val tPulse = (System.currentTimeMillis() % 1000).toFloat() / 1000f
                                drawCircle(fillCol.copy(alpha = 1f - tPulse), 32f + tPulse * 16f,
                                    Offset(enemy.x, enemy.y), style = Stroke(width = 1.dp.toPx()))
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
                                drawPath(triPath, fillCol.copy(alpha = 0.5f))
                                drawPath(triPath, fillCol, style = Stroke(width = 4.dp.toPx()))
                                drawPath(triPath, Color.White, style = Stroke(width = 1.dp.toPx()))
                            }
                            EnemyType.BOSS -> {
                                drawCircle(fillCol.copy(alpha = 0.1f), 130f, Offset(enemy.x, enemy.y))
                                drawCircle(fillCol.copy(alpha = 0.35f), 110f, Offset(enemy.x, enemy.y))
                                drawCircle(fillCol, 110f, Offset(enemy.x, enemy.y),
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
                                    drawLine(fillCol, Offset(px1, py1), Offset(px2, py2), 3.dp.toPx())
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

                    // 4. Player ship — bright halo ring so it's unmistakably "you"
                    drawCircle(Color.White.copy(alpha = 0.12f), 70f,
                        Offset(playerShip.x, playerShip.y))
                    drawProceduralShip(
                        shipId = playerShip.selectedShipId,
                        center = Offset(playerShip.x, playerShip.y),
                        size = 110f,
                        paintColor = playerShip.currentWeaponColor.composeColor
                    )

                    if (playerShip.isShieldActive) {
                        val tCycle = (System.currentTimeMillis() % 600) / 600f
                        val shieldColor = playerShip.currentWeaponColor.composeColor
                        drawCircle(shieldColor.copy(alpha = 0.2f), 90f, Offset(playerShip.x, playerShip.y))
                        drawCircle(shieldColor.copy(alpha = 1f - tCycle), 80f + tCycle * 18f,
                            Offset(playerShip.x, playerShip.y), style = Stroke(width = 2.dp.toPx()))
                    }

                    // 5. Particles — bigger & brighter on explosion for clear feedback
                    playState.activeParticles.forEach { part ->
                        when (part.type) {
                            ParticleType.EXPLOSION -> drawCircle(
                                part.color.copy(alpha = (part.life / part.maxLife).coerceIn(0f, 1f)),
                                part.size * 1.3f * (part.life / part.maxLife),
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
                                part.color.copy(alpha = (part.life / part.maxLife) * 0.3f),
                                size = Size(spaceWidth, spaceHeight))
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

                    if (isDragging) {
                        val col = playerShip.currentWeaponColor.composeColor
                        drawCircle(col.copy(alpha = 0.15f), 35f, Offset(lastTouchX, lastTouchY))
                        drawCircle(col.copy(alpha = 0.4f), 35f, Offset(lastTouchX, lastTouchY),
                            style = Stroke(width = 2f))
                    }
                }
            }

            // ── TOP HUD BAR ──────────────────────────────────────
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
                            .size(48.dp).clip(CircleShape)
                            .background(Color(0xFF1F1F35).copy(alpha = 0.9f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            .clickable { isPaused = true }
                    ) { Text("⏸", color = Color.White, fontSize = 16.sp) }
                }
            }

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

            // ── BOTTOM CONTROLS ──────────────────────────────────
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .padding(14.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {

                    Column(modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
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

                    Box(contentAlignment = Alignment.Center,
                        modifier = Modifier.testTag("shield_button").size(76.dp).clip(CircleShape)
                            .background(Color(0xFF10111F).copy(alpha = 0.92f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
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

            // ── PAUSE OVERLAY ─────────────────────────────────────
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

            // ── VICTORY SCREEN ────────────────────────────────────
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

            // ── GAME OVER SCREEN ──────────────────────────────────
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
}

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
