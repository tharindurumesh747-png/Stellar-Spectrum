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
import androidx.compose.ui.graphics.drawscope.rotate
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

// Virtual design-space resolution all game logic is written against.
private const val VW = 1080f
private const val VH = 1920f

// Fixed height of the bottom control bar (color wheel + shield). Kept small
// on purpose so most of the screen stays dedicated to actual gameplay.
private val CONTROL_BAR_HEIGHT = 118.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameplayScreen(viewModel: GameViewModel) {
    val progress by viewModel.userProgress.collectAsStateWithLifecycle()
    val playState by viewModel.gameplayState.collectAsStateWithLifecycle()
    val playerShip by viewModel.playerShipState.collectAsStateWithLifecycle()
    val vibrationEnabled = progress?.vibrationEnabled ?: true

    var isPaused by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var lastTouchX by remember { mutableStateOf(0f) }
    var lastTouchY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val starCount = 35
    val starsList = remember {
        List(starCount) {
            Offset(x = (0..VW.toInt()).random().toFloat(), y = (0..VH.toInt()).random().toFloat()) to (1..3).random()
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
            if (isDragging) viewModel.fireBulletAt(lastTouchX, lastTouchY - 200f)
        }
    }

    // ── ROOT LAYOUT: gameplay area on top, dedicated control bar below ────
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF030308))) {

        // ════════════════════ GAMEPLAY AREA (touch = move + fire) ════════════════════
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("gameplay_root")
        ) {
            val density = LocalDensity.current
            val realWpx = with(density) { maxWidth.toPx() }
            val realHpx = with(density) { maxHeight.toPx() }
            val scaleX = if (realWpx > 0f) realWpx / VW else 1f
            val scaleY = if (realHpx > 0f) realHpx / VH else 1f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInteropFilter { event ->
                        if (playState.gameEnded || playState.levelComplete || isPaused) {
                            return@pointerInteropFilter false
                        }
                        val vx = (event.x / scaleX)
                        val vy = (event.y / scaleY)
                        // Only the pause icon (top-right) needs an exclusion zone here —
                        // color wheel & shield now live in their own bar, fully separate.
                        val inPauseZone = vx > VW - 170f && vy < 220f
                        if (inPauseZone) { isDragging = false; return@pointerInteropFilter false }

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
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { isDragging = false; true }
                            else -> false
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize().testTag("gameplay_canvas")) {
                    scale(scaleX, scaleY, pivot = Offset.Zero) {
                        val spaceWidth = VW
                        val spaceHeight = VH

                        // Stars
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

                        // Enemies — solid fill so they read clearly
                        playState.activeEnemies.forEach { enemy ->
                            val fillCol = enemy.auraColor.composeColor
                            when (enemy.type) {
                                EnemyType.PULSE -> {
                                    val p = Path().apply {
                                        moveTo(enemy.x, enemy.y - 32f); lineTo(enemy.x + 32f, enemy.y)
                                        lineTo(enemy.x, enemy.y + 32f); lineTo(enemy.x - 32f, enemy.y); close()
                                    }
                                    drawPath(p, fillCol.copy(alpha = 0.75f))
                                    drawPath(p, fillCol, style = Stroke(width = 4.dp.toPx()))
                                    drawPath(p, Color.White, style = Stroke(width = 1.5.dp.toPx()))
                                }
                                EnemyType.SPLIT -> {
                                    val hexPath = Path().apply {
                                        for (i in 0..5) {
                                            val angle = i * Math.PI / 3.0
                                            val px = enemy.x + cos(angle).toFloat()*36f
                                            val py = enemy.y + sin(angle).toFloat()*36f
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
                                            val px = enemy.x + cos(angle).toFloat()*radius
                                            val py = enemy.y + sin(angle).toFloat()*radius
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
                                    drawCircle(fillCol, 48f, Offset(enemy.x, enemy.y), style = Stroke(width = 4.dp.toPx()))
                                    val tPulse = (System.currentTimeMillis() % 1000).toFloat() / 1000f
                                    drawCircle(fillCol.copy(alpha = 1f - tPulse), 32f + tPulse*16f,
                                        Offset(enemy.x, enemy.y), style = Stroke(width = 1.dp.toPx()))
                                }
                                EnemyType.PRISM -> {
                                    val triPath = Path().apply {
                                        for (i in 0..2) {
                                            val angle = (i * 2.0 * Math.PI / 3.0) + Math.toRadians(enemy.rotationAngle.toDouble())
                                            val px = enemy.x + cos(angle).toFloat()*34f
                                            val py = enemy.y + sin(angle).toFloat()*34f
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
                                    drawCircle(fillCol, 110f, Offset(enemy.x, enemy.y), style = Stroke(width = 5.dp.toPx()))
                                    drawCircle(Color.White, 80f, Offset(enemy.x, enemy.y), style = Stroke(width = 1.dp.toPx()))
                                    for (i in 0..7) {
                                        val angle = (i * Math.PI / 4.0) + Math.toRadians(enemy.rotationAngle.toDouble())
                                        val px1 = enemy.x + cos(angle).toFloat()*80f
                                        val py1 = enemy.y + sin(angle).toFloat()*80f
                                        val px2 = enemy.x + cos(angle).toFloat()*110f
                                        val py2 = enemy.y + sin(angle).toFloat()*110f
                                        drawLine(fillCol, Offset(px1, py1), Offset(px2, py2), 3.dp.toPx())
                                    }

                                    // ── LIGHTNING STRIKE telegraph + bolt ──
                                    // Vertical (top-to-bottom), anchored to
                                    // the PLAYER's tracked x position (not
                                    // the boss's own x) — red-mixed electric
                                    // look so it's unmistakably "boss attack".
                                    if (enemy.lightningState == 1) {
                                        val tx = enemy.lightningTargetX
                                        val flicker = if ((System.currentTimeMillis() / 80) % 2 == 0L) 0.75f else 0.3f
                                        drawLine(Color(0xFFFF3344).copy(alpha = flicker),
                                            Offset(tx, enemy.y + 110f), Offset(tx, spaceHeight),
                                            7f)
                                        // Small warning marker at the bottom showing exactly where it'll land
                                        drawCircle(Color(0xFFFF3344).copy(alpha = flicker), 22f, Offset(tx, spaceHeight - 40f))
                                    } else if (enemy.lightningState == 2) {
                                        val tx = enemy.lightningTargetX
                                        val segs = 14
                                        val path = Path()
                                        var cx = tx; var cy = enemy.y + 110f
                                        path.moveTo(cx, cy)
                                        val segLen = (spaceHeight - cy) / segs
                                        for (s in 1..segs) {
                                            cx = tx + (kotlin.random.Random.nextFloat() - 0.5f) * 60f
                                            cy = enemy.y + 110f + segLen * s
                                            path.lineTo(cx, cy)
                                        }
                                        // Red-mixed: outer red glow, mid orange, white-hot core
                                        drawPath(path, Color(0xFFFF3344).copy(alpha = 0.45f), style = Stroke(width = 24.dp.toPx()))
                                        drawPath(path, Color(0xFFFF6600), style = Stroke(width = 11.dp.toPx()))
                                        drawPath(path, Color.White, style = Stroke(width = 4.dp.toPx()))
                                    }
                                }
                            }
                        }

                        // Bullets
                        playState.activeBullets.forEach { bullet ->
                            val beamColor = bullet.color.composeColor
                            if (bullet.color == EnergyColor.BLUE) {
                                // Lightning Pierce: crackling jagged bolt instead of a clean line
                                val jx1 = bullet.x + (kotlin.random.Random.nextFloat()-0.5f)*10f
                                val jx2 = bullet.x + (kotlin.random.Random.nextFloat()-0.5f)*10f
                                val path = Path().apply {
                                    moveTo(bullet.x, bullet.y + 26f)
                                    lineTo(jx1, bullet.y + 8f)
                                    lineTo(jx2, bullet.y - 8f)
                                    lineTo(bullet.x, bullet.y - 26f)
                                }
                                drawPath(path, beamColor.copy(alpha = 0.4f), style = Stroke(width = 14f))
                                drawPath(path, beamColor, style = Stroke(width = 6f))
                                drawPath(path, Color.White, style = Stroke(width = 2.5f))
                            } else if (bullet.color == EnergyColor.PURPLE) {
                                // Void Bomb: pulsing orb
                                drawCircle(beamColor.copy(alpha = 0.3f), 16f, Offset(bullet.x, bullet.y))
                                drawCircle(beamColor, 9f, Offset(bullet.x, bullet.y))
                                drawCircle(Color.White, 4f, Offset(bullet.x, bullet.y))
                            } else {
                                val beamSize = 6f
                                drawLine(beamColor.copy(alpha = 0.35f), Offset(bullet.x, bullet.y+15f), Offset(bullet.x, bullet.y-15f), beamSize*2.5f)
                                drawLine(beamColor, Offset(bullet.x, bullet.y+12f), Offset(bullet.x, bullet.y-12f), beamSize)
                                drawLine(Color.White, Offset(bullet.x, bullet.y+6f), Offset(bullet.x, bullet.y-6f), beamSize*0.4f)
                            }
                        }

                        // Shield bonus boxes
                        playState.activePowerUps.forEach { pu ->
                            val pulse = (sin(System.currentTimeMillis() % 1000 / 1000f * 2 * Math.PI).toFloat() * 0.15f + 0.85f)
                            rotate(pu.angle, Offset(pu.x, pu.y)) {
                                drawRect(Color(0xFF00ADB5).copy(alpha = 0.25f * pulse),
                                    Offset(pu.x - 24f, pu.y - 24f), Size(48f, 48f))
                                drawRect(Color(0xFF00ADB5), Offset(pu.x - 24f, pu.y - 24f), Size(48f, 48f),
                                    style = Stroke(width = 3.dp.toPx()))
                            }
                            drawContext.canvas.nativeCanvas.drawText("🛡", pu.x, pu.y + 10f,
                                android.graphics.Paint().apply {
                                    textSize = 30f; textAlign = android.graphics.Paint.Align.CENTER
                                })
                        }

                        // Player ship
                        drawCircle(Color.White.copy(alpha = 0.12f), 70f, Offset(playerShip.x, playerShip.y))
                        drawProceduralShip(playerShip.selectedShipId, Offset(playerShip.x, playerShip.y), 110f,
                            playerShip.currentWeaponColor.composeColor)

                        if (playerShip.isShieldActive) {
                            val tCycle = (System.currentTimeMillis() % 600) / 600f
                            val shieldColor = playerShip.currentWeaponColor.composeColor
                            drawCircle(shieldColor.copy(alpha = 0.2f), 90f, Offset(playerShip.x, playerShip.y))
                            drawCircle(shieldColor.copy(alpha = 1f - tCycle), 80f + tCycle*18f,
                                Offset(playerShip.x, playerShip.y), style = Stroke(width = 2.dp.toPx()))
                        }

                        // Particles
                        playState.activeParticles.forEach { part ->
                            when (part.type) {
                                ParticleType.EXPLOSION -> drawCircle(
                                    part.color.copy(alpha = (part.life/part.maxLife).coerceIn(0f,1f)),
                                    part.size*1.3f*(part.life/part.maxLife), Offset(part.x, part.y))
                                ParticleType.TRAIL -> drawCircle(
                                    part.color.copy(alpha = (part.life/part.maxLife)*0.5f),
                                    part.size*(part.life/part.maxLife)*0.8f, Offset(part.x, part.y))
                                ParticleType.SHOCKWAVE -> {
                                    val fraction = 1.0f - (part.life/part.maxLife)
                                    drawCircle(part.color.copy(alpha=(part.life/part.maxLife)*0.8f),
                                        40f+fraction*180f, Offset(part.x, part.y), style = Stroke(width = 3.dp.toPx()))
                                }
                                ParticleType.SCREEN_FLASH -> drawRect(
                                    part.color.copy(alpha=(part.life/part.maxLife)*0.3f), size = Size(spaceWidth, spaceHeight))
                                ParticleType.LIGHTNING_LINK -> {
                                    val a = (part.life / part.maxLife).coerceIn(0f, 1f)
                                    val midX = (part.x + part.targetX) / 2f + (kotlin.random.Random.nextFloat()-0.5f)*30f
                                    val midY = (part.y + part.targetY) / 2f + (kotlin.random.Random.nextFloat()-0.5f)*30f
                                    val path = Path().apply {
                                        moveTo(part.x, part.y)
                                        lineTo(midX, midY)
                                        lineTo(part.targetX, part.targetY)
                                    }
                                    drawPath(path, part.color.copy(alpha = a * 0.4f), style = Stroke(width = 14f))
                                    drawPath(path, part.color.copy(alpha = a), style = Stroke(width = 5f))
                                    drawPath(path, Color.White.copy(alpha = a), style = Stroke(width = 2f))
                                }
                                else -> drawContext.canvas.nativeCanvas.apply {
                                    val paint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.YELLOW; textSize = part.size
                                        style = android.graphics.Paint.Style.FILL
                                        textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true
                                    }
                                    drawText(part.text, part.x, part.y, paint)
                                }
                            }
                        }

                        if (isDragging) {
                            val col = playerShip.currentWeaponColor.composeColor
                            drawCircle(col.copy(alpha = 0.15f), 35f, Offset(lastTouchX, lastTouchY))
                            drawCircle(col.copy(alpha = 0.4f), 35f, Offset(lastTouchX, lastTouchY), style = Stroke(width = 2f))
                        }
                    }
                }

                // ── TOP HUD ──
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 6.dp).testTag("gameplay_hud"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.width(130.dp)) {
                        val ratio = playerShip.hp.toFloat() / playerShip.maxHp.toFloat()
                        val progressColor = when { ratio>0.6f -> Color(0xFF10B981); ratio>0.3f -> Color(0xFFFBBF24); else -> Color(0xFFFF2E63) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("❤️ ", fontSize = 11.sp)
                            Text("HP: ${playerShip.hp}/${playerShip.maxHp}", color = Color.White, fontSize = 11.sp,
                                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF10101C)).border(1.dp, Color.Gray.copy(alpha=0.2f), RoundedCornerShape(4.dp))) {
                            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(ratio.coerceIn(0f,1f)).background(progressColor))
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${playState.score}", fontSize = 24.sp, fontWeight = FontWeight.Black,
                            color = Color.White, fontFamily = FontFamily.Monospace)
                        Text("WAVE ${playState.currentWave}  •  KILLS ${playState.totalKilledThisRun}",
                            fontSize = 10.sp, color = Color.LightGray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        if (playState.comboMultiplier > 1) {
                            Text("${playState.comboMultiplier}X COMBO", color = Color(0xFFFBBF24), fontSize = 13.sp,
                                fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("💎 ${playState.crystalsCollectedThisRun}", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFF00ADB5), fontFamily = FontFamily.Monospace)
                        Box(contentAlignment = Alignment.Center,
                            modifier = Modifier.testTag("pause_game_button").size(48.dp).clip(CircleShape)
                                .background(Color(0xFF1F1F35).copy(alpha=0.9f))
                                .border(1.5.dp, Color.White.copy(alpha=0.4f), CircleShape)
                                .clickable { isPaused = true }
                        ) { Text("⏸", color = Color.White, fontSize = 16.sp) }
                    }
                }

                if (playState.isBossFight && playState.activeEnemies.any { it.type == EnemyType.BOSS }) {
                    val bossObj = playState.activeEnemies.first { it.type == EnemyType.BOSS }
                    val hpRatio = bossObj.hp.toFloat() / bossObj.maxHp.toFloat()
                    Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                        .padding(top = 100.dp, start = 20.dp, end = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️ BOSS ACTIVE - PHASE ${bossObj.bossPhase}", color = Color(0xFFFF2E63),
                            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
                            .background(Color.Black.copy(alpha=0.6f)).border(1.5.dp, Color(0xFFFF2E63), RoundedCornerShape(5.dp))) {
                            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(hpRatio.coerceIn(0f,1f)).background(Color(0xFFFF2E63)))
                        }
                    }
                }

                // ── OVERLAYS ──
                if (isPaused) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.75f))
                        .clickable { isPaused = false }, contentAlignment = Alignment.Center) {
                        NeonCard(borderColor = Color(0xFF00ADB5), modifier = Modifier.fillMaxWidth(0.85f).clickable(enabled=false){}) {
                            Text("⏸ PAUSED", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(20.dp))
                            NeonButton("RESUME", onClick = { isPaused = false }, buttonColor = Color(0xFF10B981),
                                vibrationEnabled = vibrationEnabled, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(10.dp))
                            NeonButton("RESTART", onClick = { isPaused = false; viewModel.selectWorldAndStart(playState.worldIndex) },
                                buttonColor = Color(0xFF3B82F6), vibrationEnabled = vibrationEnabled, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(10.dp))
                            NeonButton("MAIN MENU", onClick = { isPaused = false; viewModel.changeScreen(GameScreen.WORLD_SELECT) },
                                buttonColor = Color(0xFFFF2E63), vibrationEnabled = vibrationEnabled, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                if (playState.levelComplete) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.85f)), contentAlignment = Alignment.Center) {
                        NeonCard(borderColor = Color(0xFFFBBF24), modifier = Modifier.fillMaxWidth(0.88f)) {
                            Text("🛰️ PORTAL CLEARED!", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFBBF24),
                                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(14.dp)); HorizontalDivider(color = Color(0xFFFBBF24).copy(alpha=0.3f)); Spacer(Modifier.height(14.dp))
                            Text("${playState.score}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White,
                                fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(6.dp))
                            Text("💎 ${playState.crystalsCollectedThisRun}", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                color = Color(0xFF00ADB5), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom=14.dp))
                            NeonButton("NEXT GALAXY", onClick = { viewModel.changeScreen(GameScreen.WORLD_SELECT) },
                                buttonColor = Color(0xFF10B981), vibrationEnabled = vibrationEnabled, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            NeonButton("MAIN MENU", onClick = { viewModel.changeScreen(GameScreen.MAIN_MENU) },
                                buttonColor = Color.LightGray, vibrationEnabled = vibrationEnabled, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                if (playState.gameEnded) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.85f)), contentAlignment = Alignment.Center) {
                        NeonCard(borderColor = Color(0xFFFF2E63), modifier = Modifier.fillMaxWidth(0.85f)) {
                            Text("GAME OVER", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF2E63),
                                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(14.dp)); HorizontalDivider(color = Color(0xFFFF2E63).copy(alpha=0.3f)); Spacer(Modifier.height(14.dp))
                            Text("FINAL SCORE", fontSize = 11.sp, color = Color.LightGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            Text("${playState.score}", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White,
                                fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom=12.dp))
                            Text("💎 ${playState.crystalsCollectedThisRun} crystals\nKills: ${playState.totalKilledThisRun}",
                                fontSize = 13.sp, color = Color.LightGray, textAlign = TextAlign.Center, lineHeight = 18.sp,
                                modifier = Modifier.fillMaxWidth().padding(bottom=20.dp))
                            NeonButton("RETRY", onClick = { viewModel.selectWorldAndStart(playState.worldIndex) },
                                buttonColor = Color(0xFF00ADB5), vibrationEnabled = vibrationEnabled, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(10.dp))
                            NeonButton("MAIN MENU", onClick = { viewModel.changeScreen(GameScreen.MAIN_MENU) },
                                buttonColor = Color.LightGray, vibrationEnabled = vibrationEnabled, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }

        // ════════════════════ DEDICATED CONTROL BAR (separate from gameplay touch) ════════════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(CONTROL_BAR_HEIGHT)
                .background(Color(0xFF0A0A14))
                .border(width = 1.dp, color = Color.White.copy(alpha = 0.08f))
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color wheel — 4 dots, bigger reliable targets
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ColorDotItem(EnergyColor.RED, playerShip.currentWeaponColor == EnergyColor.RED) { viewModel.switchWeaponColor(EnergyColor.RED) }
                ColorDotItem(EnergyColor.BLUE, playerShip.currentWeaponColor == EnergyColor.BLUE) { viewModel.switchWeaponColor(EnergyColor.BLUE) }
                ColorDotItem(EnergyColor.GREEN, playerShip.currentWeaponColor == EnergyColor.GREEN) { viewModel.switchWeaponColor(EnergyColor.GREEN) }
                ColorDotItem(EnergyColor.PURPLE, playerShip.currentWeaponColor == EnergyColor.PURPLE) { viewModel.switchWeaponColor(EnergyColor.PURPLE) }
            }

            // ⚡ ELECTRIC WAVE ultimate — charges from kills, destroys ~50%
            // of on-screen enemies when full.
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier.testTag("ultimate_button").size(56.dp).clip(CircleShape)
                    .background(Color(0xFF10111F).copy(alpha = 0.95f))
                    .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), CircleShape)
                    .clickable(enabled = playState.ultimateCharge >= 1.0f) { viewModel.activateUltimate() }) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(Color.Gray.copy(alpha = 0.3f), size.width / 2f - 4f, style = Stroke(width = 4.dp.toPx()))
                    drawArc(
                        color = if (playState.ultimateCharge >= 1f) Color(0xFF00E5FF) else Color(0xFFBD00FF),
                        startAngle = -90f, sweepAngle = playState.ultimateCharge * 360f,
                        useCenter = false, style = Stroke(width = 4.dp.toPx())
                    )
                }
                Text(
                    text = if (playState.ultimateCharge >= 1.0f) "⚡" else "${(playState.ultimateCharge*100).toInt()}%",
                    color = if (playState.ultimateCharge >= 1.0f) Color(0xFF00E5FF) else Color.Gray,
                    fontSize = if (playState.ultimateCharge >= 1.0f) 20.sp else 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Shield button — icon always stays once ready; a number badge
            // above it shows total stacked charges (2, 3, 4...) collected
            // from bonus boxes. Only the count changes; the icon never
            // disappears once you have at least one charge ready.
            run {
                val baseReady = playerShip.shieldCharge >= 1.0f
                val totalCharges = playerShip.shieldStock + (if (baseReady) 1 else 0)
                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier.testTag("shield_button").size(56.dp).clip(CircleShape)
                        .background(Color(0xFF10111F).copy(alpha = 0.95f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .clickable(enabled = totalCharges >= 1) { viewModel.activateShield() }) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(Color.Gray.copy(alpha = 0.3f), size.width / 2f - 4f, style = Stroke(width = 4.dp.toPx()))
                        drawArc(
                            color = if (totalCharges >= 1) Color(0xFF00ADB5) else Color(0xFFFF2E63),
                            startAngle = -90f, sweepAngle = playerShip.shieldCharge.coerceIn(0f,1f) * 360f,
                            useCenter = false, style = Stroke(width = 4.dp.toPx())
                        )
                    }
                    if (totalCharges >= 1) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            if (totalCharges >= 2) {
                                Text("$totalCharges", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                            Text("🛡️", fontSize = 16.sp)
                        }
                    } else {
                        Text("${(playerShip.shieldCharge*100).toInt()}%", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ColorDotItem(colorObj: EnergyColor, isSelected: Boolean, onClick: () -> Unit) {
    val sizeVal by animateDpAsState(
        targetValue = if (isSelected) 50.dp else 40.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "wheel_scale"
    )
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier.size(sizeVal).clip(CircleShape).background(colorObj.composeColor)
            .border(width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) Color.White else Color.Transparent, shape = CircleShape)
            .clickable { onClick() }) {
        if (isSelected) {
            val inf = rememberInfiniteTransition(label = "border_shift")
            val ratio by inf.animateFloat(0.5f, 1.0f,
                infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse), label = "circle_ratio")
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color.White.copy(alpha = ratio * 0.4f), size.width/2f + 6.dp.toPx()*ratio, style = Stroke(width = 1.5.dp.toPx()))
            }
        }
    }
}
