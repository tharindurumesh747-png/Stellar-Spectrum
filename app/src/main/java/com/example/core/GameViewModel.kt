package com.example.core

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.triggerVibration
import com.example.db.AppDatabase
import com.example.db.UserProgress
import com.example.db.UserProgressRepository
import com.example.entities.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.ceil

enum class GameScreen {
    SPLASH, MAIN_MENU, SHIP_SELECT, WORLD_SELECT, GAMEPLAY, SHOP, ACHIEVEMENTS, MISSIONS, LEADERBOARD, SETTINGS
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: UserProgressRepository

    val activeScreen = MutableStateFlow(GameScreen.SPLASH)
    val userProgress: StateFlow<UserProgress?>
    val gameplayState = MutableStateFlow(GameplayState())
    val playerShipState = MutableStateFlow(PlayerShip())
    val dailyRewardAvailable = MutableStateFlow(false)
    val showDailyRewardPopup = MutableStateFlow(false)
    val showLeaderboardComingSoon = MutableStateFlow(false)

    var soundEnabled = true
    var musicEnabled = true
    var vibrationEnabled = true

    // 3x enemy density: boss now appears after 60 kills (was 20), and up to
    // 20 enemies can be on screen at once (was 8).
    private val killsPerBoss = 60
    private val maxConcurrentEnemies = 20

    init {
        val database = AppDatabase.getDatabase(application)
        repository = UserProgressRepository(database.userProgressDao())
        viewModelScope.launch { repository.ensureInitialized() }

        userProgress = repository.progressFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        viewModelScope.launch {
            repository.progressFlow.collect { progress ->
                progress?.let {
                    soundEnabled = it.soundEnabled
                    musicEnabled = it.musicEnabled
                    vibrationEnabled = it.vibrationEnabled
                    SoundSynth.setEnabled(soundEnabled)
                    BgmEngine.setEnabled(musicEnabled)
                    checkDailyRewardStatus(it)
                }
            }
        }
    }

    private fun checkDailyRewardStatus(progress: UserProgress) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        dailyRewardAvailable.value = progress.lastDailyReward != todayStr
    }

    fun claimDailyReward() {
        val progress = userProgress.value ?: return
        if (!dailyRewardAvailable.value) return
        viewModelScope.launch {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val nextDay = (progress.dailyRewardDay % 7) + 1
            val rewardCrystals = nextDay * 50
            repository.updateProgress(
                progress.copy(
                    crystals = progress.crystals + rewardCrystals,
                    lastDailyReward = todayStr,
                    dailyRewardDay = nextDay
                )
            )
            dailyRewardAvailable.value = false
            showDailyRewardPopup.value = false
            SoundSynth.playPowerup()
            triggerAchievementProgress("crystals", progress.crystals + rewardCrystals)
        }
    }

    fun changeScreen(screen: GameScreen) {
        activeScreen.value = screen
        if (screen == GameScreen.GAMEPLAY) {
            BgmEngine.start(gameplayState.value.worldIndex)
        } else {
            BgmEngine.stop()
        }
    }

    fun selectWorldAndStart(worldIndex: Int) {
        val progress = userProgress.value ?: return
        val worldDef = GameData.worlds.first { it.index == worldIndex }
        val shipDef = GameData.ships.first { it.id == progress.selectedShip }

        val player = PlayerShip(
            x = 540f, y = 1400f, targetX = 540f, targetY = 1400f,
            maxHp = shipDef.hp, hp = shipDef.hp,
            selectedShipId = shipDef.id
        )
        playerShipState.value = player

        gameplayState.value = GameplayState(
            worldIndex = worldIndex,
            difficultyStars = worldDef.difficulty,
            totalWaves = 5,
            levelComplete = false,
            gameEnded = false,
            spawnTimer = 0.6f,
            totalKilledThisRun = 0,
            currentWave = 1,
            isBossFight = false,
            ultimateCharge = 0f
        )

        changeScreen(GameScreen.GAMEPLAY)
    }

    private fun spawnSingleEnemy(world: Int, wave: Int): EnemyDrone {
        val roll = (0..99).random()
        val type = when (world) {
            1 -> if (roll < 20) EnemyType.WARP else EnemyType.PULSE
            2 -> if (roll < 35) EnemyType.SPLIT else EnemyType.PULSE
            3 -> when { roll < 25 -> EnemyType.WARP; roll < 50 -> EnemyType.SPLIT; else -> EnemyType.PULSE }
            4 -> when { roll < 25 -> EnemyType.PRISM; roll < 50 -> EnemyType.SHIELD; else -> EnemyType.PULSE }
            else -> EnemyType.values().filter { it != EnemyType.BOSS }.random()
        }
        val spawnX = 100f + kotlin.random.Random.nextFloat() * 880f
        val aura = EnergyColor.random()
        val speedY = 130f + wave * 14f
        val speedX = when (type) { EnemyType.SPLIT -> 180f; EnemyType.PRISM -> 160f; else -> 0f }

        return EnemyDrone(
            type = type, x = spawnX, y = -60f, vx = speedX, vy = speedY,
            hp = if (type == EnemyType.SHIELD) 3 else 1,
            maxHp = if (type == EnemyType.SHIELD) 3 else 1,
            auraColor = aura
        )
    }

    private fun spawnBoss(world: Int): EnemyDrone {
        val bossHp = when (world) { 1 -> 50; 2 -> 100; 3 -> 120; 4 -> 150; 5 -> 200; else -> 300 }
        val aura = when (world) {
            1 -> EnergyColor.RED; 2 -> EnergyColor.RED; 3 -> EnergyColor.BLUE
            4 -> EnergyColor.RED; 5 -> EnergyColor.PURPLE; else -> EnergyColor.RED
        }
        return EnemyDrone(
            type = EnemyType.BOSS, x = 540f, y = -200f, vx = 0f, vy = 0f,
            hp = bossHp, maxHp = bossHp, auraColor = aura,
            ultimateTimer = 7f
        )
    }

    fun switchWeaponColor(color: EnergyColor) {
        val player = playerShipState.value
        if (player.currentWeaponColor != color) {
            val updated = player.copy(currentWeaponColor = color)
            SoundSynth.playUiClick()
            registerMissionProgress(4, 1)
            playerShipState.value = updated
        }
    }

    fun fireBulletAt(targetTapX: Float, targetTapY: Float) {
        val player = playerShipState.value
        val now = System.currentTimeMillis()
        val shipDef = GameData.ships.first { it.id == player.selectedShipId }
        val fRate = if (player.selectedShipId == "quantum_falcon") (shipDef.fireRateMs * 0.82f).toLong() else shipDef.fireRateMs
        if (now - player.lastShootTimeMs < fRate) return

        val bulletColor = player.currentWeaponColor
        val px = player.x
        val py = player.y - 40f
        val speed = 1000f

        when (bulletColor) {
            EnergyColor.RED -> SoundSynth.playShootRed()
            EnergyColor.BLUE -> SoundSynth.playShootBlue()
            EnergyColor.GREEN -> SoundSynth.playShootGreen()
            EnergyColor.PURPLE -> SoundSynth.playShootPurple()
        }

        val list = gameplayState.value.activeBullets.toMutableList()
        when (bulletColor) {
            EnergyColor.RED -> list.add(Projectile(px, py, 0f, -speed * 1.2f, bulletColor, true))
            EnergyColor.BLUE -> list.add(Projectile(px, py, 0f, -speed * 0.8f, bulletColor, true))
            EnergyColor.GREEN -> {
                list.add(Projectile(px, py - 10f, 0f, -speed, bulletColor, true))
                list.add(Projectile(px, py, -220f, -speed * 0.95f, bulletColor, true))
                list.add(Projectile(px, py, 220f, -speed * 0.95f, bulletColor, true))
            }
            EnergyColor.PURPLE -> list.add(Projectile(px, py, 0f, -speed * 0.9f, bulletColor, true))
        }
        gameplayState.value = gameplayState.value.copy(activeBullets = list)
        // FIX: .copy() guarantees a genuinely new instance so Compose/StateFlow
        // always detects the change (lastShootTimeMs updated here).
        playerShipState.value = player.copy(lastShootTimeMs = now)
    }

    fun triggerSwipeDodge(swipeDirectionLeft: Boolean) {
        val player = playerShipState.value
        val boundsMove = if (swipeDirectionLeft) -250f else 250f
        val newTargetX = (player.x + boundsMove).coerceIn(100f, 980f)
        var updated = player.copy(targetX = newTargetX)
        if (player.selectedShipId == "nova_phantom") {
            updated = updated.copy(invincibilityTimeRemaining = 1.0f)
            spawnCustomParticles(player.x, player.y, Color(0xFF10B981), ParticleType.COMBO_FLASH, "GHOST!")
        }
        playerShipState.value = updated
    }

    fun activateShield() {
        val player = playerShipState.value
        if (player.shieldCharge >= 1.0f) {
            SoundSynth.playPowerup()
            spawnCustomParticles(player.x, player.y, Color.White, ParticleType.SHOCKWAVE)
            playerShipState.value = player.copy(shieldCharge = 0f, shieldActiveTimeRemaining = 3.0f)
        }
    }

    // ── ELECTRIC WAVE ULTIMATE ──────────────────────────────────────────
    // Charges +1/12 per kill. When full, destroys ~50% of on-screen enemies
    // (non-boss) and deals a heavy chunk of damage to a boss if present.
    fun activateUltimate() {
        val currentPlay = gameplayState.value
        if (currentPlay.ultimateCharge < 1.0f) return

        val enemies = currentPlay.activeEnemies.toMutableList()
        val particles = currentPlay.activeParticles.toMutableList()
        val nonBoss = enemies.filter { it.type != EnemyType.BOSS }
        val killCount = ceil(nonBoss.size * 0.5f).toInt()
        val toKill = nonBoss.shuffled().take(killCount)

        var bonusScore = 0
        var bonusCrystals = 0
        toKill.forEach { e ->
            enemies.remove(e)
            bonusScore += 80
            bonusCrystals += 1
            for (i in 0..8) {
                particles.add(Particle(
                    type = ParticleType.EXPLOSION, x = e.x, y = e.y,
                    vx = -200f + kotlin.random.Random.nextFloat()*400f,
                    vy = -200f + kotlin.random.Random.nextFloat()*400f,
                    size = 16f, color = Color(0xFF00E5FF), maxLife = 0.5f
                ))
            }
        }
        // Boss takes a big chunk of damage too
        val boss = enemies.firstOrNull { it.type == EnemyType.BOSS }
        if (boss != null) {
            boss.hp = (boss.hp - (boss.maxHp * 0.2f).toInt()).coerceAtLeast(1)
        }

        // Big screen-wide electric flash
        particles.add(Particle(ParticleType.SCREEN_FLASH, 0f, 0f, 0f, 0f, 0f,
            Color(0xFF00E5FF).copy(alpha = 0.5f), 0.4f))

        SoundSynth.playPowerup()
        gameplayState.value = currentPlay.copy(
            activeEnemies = enemies,
            activeParticles = particles,
            score = currentPlay.score + bonusScore,
            crystalsCollectedThisRun = currentPlay.crystalsCollectedThisRun + bonusCrystals,
            ultimateCharge = 0f
        )
    }

    // ── MAIN GAME LOOP ────────────────────────────────────────────────
    fun updateGame(deltaTime: Float) {
        val currentPlay = gameplayState.value
        if (currentPlay.gameEnded || currentPlay.levelComplete) return

        var player = playerShipState.value
        player.update(deltaTime, 1.0f)
        if (player.shieldActiveTimeRemaining <= 0f) {
            val chargeSpeed = if (player.selectedShipId == "eclipse_runner") 0.30f else 0.18f
            player.shieldCharge = minOf(1.0f, player.shieldCharge + chargeSpeed * deltaTime)
        }

        val bullets = currentPlay.activeBullets.toMutableList()
        val enemies = currentPlay.activeEnemies.toMutableList()
        // FIX (crash bug): SPLIT enemies used to be added directly into
        // `enemies` WHILE an iterator over that same list was still active —
        // this threw ConcurrentModificationException and crashed the app the
        // moment a piercing BLUE bullet split an enemy mid-loop. New enemies
        // are now queued here and merged in only AFTER all loops finish.
        val pendingSpawns = mutableListOf<EnemyDrone>()
        val powerups = currentPlay.activePowerUps.toMutableList()
        val particles = currentPlay.activeParticles.toMutableList()

        var currentScore = currentPlay.score
        var currentCombo = currentPlay.comboMultiplier
        var comboTimer = maxOf(0f, currentPlay.comboTimerRemaining - deltaTime)
        var crystalsCollected = currentPlay.crystalsCollectedThisRun
        var totalKilled = currentPlay.totalKilledThisRun
        var wave = currentPlay.currentWave
        var bossActive = currentPlay.isBossFight
        var spawnTimer = currentPlay.spawnTimer - deltaTime
        var ultimateCharge = currentPlay.ultimateCharge

        if (comboTimer <= 0f) currentCombo = 1

        val pIter = particles.iterator()
        while (pIter.hasNext()) { val part = pIter.next(); part.update(deltaTime); if (part.runOut) pIter.remove() }

        if ((0..10).random() < 3) {
            val trailColor = when (player.currentWeaponColor) {
                EnergyColor.RED -> Color(0xFFFF2E63); EnergyColor.BLUE -> Color(0xFF00ADB5)
                EnergyColor.GREEN -> Color(0xFF10B981); EnergyColor.PURPLE -> Color(0xFFBD00FF)
            }
            particles.add(Particle(
                type = ParticleType.TRAIL,
                x = player.x + (-20f + kotlin.random.Random.nextFloat() * 40f),
                y = player.y + 35f,
                vx = -40f + kotlin.random.Random.nextFloat() * 80f,
                vy = 150f + kotlin.random.Random.nextFloat() * 150f,
                size = (6..12).random().toFloat(),
                color = trailColor.copy(alpha = 0.65f), maxLife = 0.4f
            ))
        }

        val bulletIter = bullets.iterator()
        while (bulletIter.hasNext()) {
            val bullet = bulletIter.next()
            bullet.update(deltaTime)
            if (bullet.color == EnergyColor.PURPLE && bullet.isFromPlayer) {
                val target = enemies.minByOrNull { sqrt((it.x - bullet.x)*(it.x - bullet.x) + (it.y - bullet.y)*(it.y - bullet.y)) }
                if (target != null) {
                    val angle = atan2(target.y - bullet.y, target.x - bullet.x)
                    val factor = if (player.selectedShipId == "void_hunter") 12f else 6f
                    bullet.vx += cos(angle) * 800f * factor * deltaTime
                    bullet.vy += sin(angle) * 800f * factor * deltaTime
                    val mag = sqrt(bullet.vx*bullet.vx + bullet.vy*bullet.vy)
                    if (mag > 0f) { bullet.vx = (bullet.vx/mag)*950f; bullet.vy = (bullet.vy/mag)*950f }
                }
            }
            if (bullet.isOutOfBounds) bulletIter.remove()
        }

        // Continuous spawning — 3x density via killsPerBoss & maxConcurrentEnemies
        wave = (1 + totalKilled / 8).coerceAtMost(6)

        if (!bossActive && totalKilled > 0 && totalKilled % killsPerBoss == 0 && enemies.none { it.type == EnemyType.BOSS }) {
            enemies.add(spawnBoss(currentPlay.worldIndex))
            bossActive = true
        } else if (!bossActive && spawnTimer <= 0f && enemies.size < maxConcurrentEnemies) {
            enemies.add(spawnSingleEnemy(currentPlay.worldIndex, wave))
            spawnTimer = (1.5f - wave * 0.16f).coerceAtLeast(0.4f)
        }

        val enemyIter = enemies.iterator()
        while (enemyIter.hasNext()) {
            val enemy = enemyIter.next()
            enemy.update(deltaTime, 1080f, player.x, currentPlay.worldIndex)

            if (enemy.type == EnemyType.BOSS) {
                enemy.bossShootingTimer += deltaTime
                val shootRate = when (enemy.bossPhase) { 4 -> 0.8f; 3 -> 1.2f; 2 -> 1.6f; else -> 2.0f }
                if (enemy.bossShootingTimer >= shootRate) {
                    enemy.bossShootingTimer = 0f
                    val count = if (enemy.bossPhase == 4) 8 else 4
                    for (i in 0 until count) {
                        val ang = (i * (2 * Math.PI / count)) + (System.currentTimeMillis() / 400.0)
                        bullets.add(Projectile(enemy.x, enemy.y + 50f,
                            cos(ang).toFloat()*350f, sin(ang).toFloat()*350f, enemy.auraColor, false))
                    }
                }

                // ── BOSS FULL-SCREEN ULTIMATE vs PLAYER SHIELD ──────────
                // Every ~7-10s the boss unleashes a screen-wide blast. If the
                // player's shield is active at that moment, it's fully
                // absorbed (no damage, big cyan flash + ultimate-charge
                // bonus). Otherwise it deals heavy damage.
                enemy.ultimateTimer -= deltaTime
                if (enemy.ultimateTimer <= 0f) {
                    enemy.ultimateTimer = 8f
                    if (player.isShieldActive) {
                        particles.add(Particle(ParticleType.SCREEN_FLASH, 0f,0f,0f,0f,0f,
                            Color(0xFF00ADB5).copy(alpha = 0.5f), 0.5f))
                        ultimateCharge = minOf(1f, ultimateCharge + 0.25f)
                        SoundSynth.playPowerup()
                    } else {
                        particles.add(Particle(ParticleType.SCREEN_FLASH, 0f,0f,0f,0f,0f,
                            Color.Red.copy(alpha = 0.45f), 0.45f))
                        player.hp = maxOf(0, player.hp - 30)
                        triggerVibration(getApplication(), 150)
                    }
                }
            } else if ((0..1000).random() < 2 + wave) {
                bullets.add(Projectile(enemy.x, enemy.y + 35f, 0f, 380f, enemy.auraColor, false))
            }

            if (enemy.y > 1750f) {
                if (enemy.type != EnemyType.BOSS) {
                    enemyIter.remove()
                    if (!player.isInvincible) {
                        player.hp = maxOf(0, player.hp - 6)
                        player.invincibilityTimeRemaining = maxOf(player.invincibilityTimeRemaining, 0.2f)
                        triggerVibration(getApplication(), 50)
                        particles.add(Particle(ParticleType.SCREEN_FLASH, 0f,0f,0f,0f,0f,
                            Color.Red.copy(alpha=0.18f), 0.2f))
                    }
                } else {
                    enemy.y = 200f
                }
            }
        }

        // Any hit destroys any enemy — color no longer required.
        val bIter = bullets.iterator()
        while (bIter.hasNext()) {
            val bullet = bIter.next()
            if (!bullet.isFromPlayer) continue
            var bulletConsumed = false
            val eIter = enemies.iterator()
            while (eIter.hasNext()) {
                val enemy = eIter.next()
                val dx = bullet.x - enemy.x; val dy = bullet.y - enemy.y
                val dist = sqrt(dx*dx + dy*dy)
                val rangeRadius = if (enemy.type == EnemyType.BOSS) 150f else 50f
                if (dist < rangeRadius) {
                    enemy.hp--
                    SoundSynth.playHitCorrect()
                    for (i in 0..12) {
                        particles.add(Particle(
                            type = ParticleType.EXPLOSION, x = enemy.x, y = enemy.y,
                            vx = -250f + kotlin.random.Random.nextFloat()*500f,
                            vy = -250f + kotlin.random.Random.nextFloat()*500f,
                            size = (8..22).random().toFloat(),
                            color = enemy.auraColor.composeColor, maxLife = 0.45f
                        ))
                    }
                    if (enemy.hp <= 0) {
                        eIter.remove()
                        totalKilled++
                        ultimateCharge = minOf(1f, ultimateCharge + 1f / 12f)

                        if (enemy.type == EnemyType.SPLIT) {
                            // FIX: queue instead of adding directly to `enemies`
                            pendingSpawns.add(EnemyDrone(type = EnemyType.PULSE, x = enemy.x-40f, y = enemy.y,
                                vx = -120f, vy = enemy.vy*1.1f, hp=1, maxHp=1, auraColor = enemy.auraColor))
                            pendingSpawns.add(EnemyDrone(type = EnemyType.PULSE, x = enemy.x+40f, y = enemy.y,
                                vx = 120f, vy = enemy.vy*1.1f, hp=1, maxHp=1, auraColor = enemy.auraColor))
                        } else if (enemy.type == EnemyType.BOSS) {
                            bossActive = false
                            SoundSynth.playBossDie()
                            viewModelScope.launch { triggerAchievementProgress("boss", 1) }
                            registerMissionProgress(3, 1)
                            crystalsCollected += 200
                            currentScore += 5000
                        }

                        val rewardBase = when (enemy.type) {
                            EnemyType.PULSE -> 100; EnemyType.SPLIT -> 150; EnemyType.WARP -> 200
                            EnemyType.SHIELD -> 300; EnemyType.PRISM -> 400; EnemyType.BOSS -> 5000
                        }
                        currentScore += rewardBase * currentCombo
                        crystalsCollected += 2 * currentCombo
                        currentCombo = minOf(5, currentCombo + 1)
                        comboTimer = 2.5f
                        viewModelScope.launch { triggerAchievementProgress("combo", currentCombo) }

                        if (currentCombo > 1) {
                            particles.add(Particle(type = ParticleType.TEXT, x = enemy.x, y = enemy.y-30f,
                                vx = 0f, vy = -180f, size = 32f, color = Color(0xFFFBBF24),
                                maxLife = 0.55f, text = "${currentCombo}x COMBO!"))
                        }
                    }
                    bulletConsumed = true
                    if (bullet.color != EnergyColor.BLUE) break
                }
            }
            if (bulletConsumed && bullet.color != EnergyColor.BLUE) bIter.remove()
        }

        // FIX: merge queued SPLIT spawns now that all iteration is finished
        enemies.addAll(pendingSpawns)

        val enemyBIter = bullets.iterator()
        while (enemyBIter.hasNext()) {
            val b = enemyBIter.next()
            if (b.isFromPlayer) continue
            val dx = b.x - player.x; val dy = b.y - player.y
            if (sqrt(dx*dx+dy*dy) < 60f) {
                enemyBIter.remove()
                if (!player.isInvincible) {
                    player.hp = maxOf(0, player.hp - 8)
                    triggerVibration(getApplication(), 70)
                    particles.add(Particle(ParticleType.SCREEN_FLASH, 0f,0f,0f,0f,0f,
                        Color.Red.copy(alpha=0.2f), 0.2f))
                }
            }
        }

        val puIter = powerups.iterator()
        while (puIter.hasNext()) {
            val pu = puIter.next()
            pu.update(deltaTime)
            if (pu.y > 2000f) { puIter.remove(); continue }
            val dx = pu.x - player.x; val dy = pu.y - player.y
            if (sqrt(dx*dx+dy*dy) < 60f) {
                puIter.remove()
                crystalsCollected += 20
                SoundSynth.playPowerup()
            }
        }

        // FIX: always reassign via .copy() so the PlayerShip StateFlow
        // genuinely emits a new value every single frame — this is what was
        // making the shield arc/charge UI look frozen or inconsistent.
        player = player.copy()

        if (player.hp <= 0) {
            gameplayState.value = currentPlay.copy(gameEnded = true)
            playerShipState.value = player
            saveGameStatsAndEnd(currentPlay.worldIndex, currentScore, crystalsCollected, totalKilled, 0, 0, false)
            return
        }

        if (!bossActive && currentPlay.isBossFight) {
            gameplayState.value = currentPlay.copy(levelComplete = true)
            playerShipState.value = player
            saveGameStatsAndEnd(currentPlay.worldIndex, currentScore, crystalsCollected, totalKilled, 1, 0, true)
            return
        }

        playerShipState.value = player
        gameplayState.value = currentPlay.copy(
            score = currentScore,
            comboMultiplier = currentCombo,
            comboTimerRemaining = comboTimer,
            crystalsCollectedThisRun = crystalsCollected,
            activeBullets = bullets,
            activeEnemies = enemies,
            activePowerUps = powerups,
            activeParticles = particles,
            totalKilledThisRun = totalKilled,
            currentWave = wave,
            isBossFight = bossActive,
            spawnTimer = spawnTimer,
            ultimateCharge = ultimateCharge
        )
    }

    private fun saveGameStatsAndEnd(worldIndex: Int, score: Int, crystals: Int, rawKilled: Int, bossesKilled: Int, strikes: Int, complete: Boolean) {
        viewModelScope.launch {
            if (complete) {
                if (worldIndex < 6) repository.unlockWorld(worldIndex + 1)
                if (worldIndex == 6) triggerAchievementProgress("world", 6)
                triggerAchievementProgress("hits", 1)
                registerMissionProgress(5, 1)
            }
            // completeRun() internally compares against the saved high score
            // and only overwrites it if this run's score is higher — true
            // "best score" semantics, not a running total.
            repository.completeRun(worldId = worldIndex, score = score, crystalsEarned = crystals,
                rawEnemies = rawKilled, bosssKilled = bossesKilled)
            registerMissionProgress(2, score)
            registerMissionProgress(6, rawKilled)
            registerMissionProgress(7, score)
            if (complete && worldIndex == 6) unlockSpecialShip("crystal_titan")
        }
    }

    private suspend fun unlockSpecialShip(shipId: String) {
        val progress = repository.getProgressDirect()
        val unlockedList = progress.getUnlockedShips().toMutableList()
        if (!unlockedList.contains(shipId)) {
            unlockedList.add(shipId)
            repository.updateProgress(progress.copy(unlockedShipsStr = unlockedList.joinToString(",")))
            SoundSynth.playPowerup()
        }
    }

    fun buyShip(shipId: String, cost: Int) {
        viewModelScope.launch {
            val progress = userProgress.value ?: return@launch
            if (progress.crystals >= cost) {
                if (repository.unlockShip(shipId, cost)) SoundSynth.playPowerup()
            } else SoundSynth.playHitWrong()
        }
    }

    fun selectShip(shipId: String) { viewModelScope.launch { repository.selectShip(shipId) } }

    fun toggleSounds(sound: Boolean, music: Boolean, vibration: Boolean) {
        viewModelScope.launch { repository.updateSettings(sound, music, vibration) }
    }

    fun resetAllProfileData() {
        viewModelScope.launch { repository.resetAllData(); SoundSynth.playHitWrong() }
    }

    private fun spawnCustomParticles(px: Float, py: Float, col: Color, type: ParticleType, message: String = "") {
        val state = gameplayState.value
        val list = state.activeParticles.toMutableList()
        list.add(Particle(type, px, py, 0f, -120f, 26f, col, 0.7f, text = message))
        gameplayState.value = state.copy(activeParticles = list)
    }

    private suspend fun triggerAchievementProgress(type: String, currentValue: Int) {
        val progress = repository.getProgressDirect()
        val unlocked = progress.getUnlockedAchievements().toMutableList()
        GameData.achievements.forEach { ach ->
            if (!unlocked.contains(ach.id) && ach.type == type && currentValue >= ach.target) {
                unlocked.add(ach.id)
                val addedCrystals = progress.crystals + ach.rewardCrystals
                repository.updateProgress(progress.copy(crystals = addedCrystals,
                    achievementsUnlockedStr = unlocked.joinToString(",")))
                SoundSynth.playPowerup()
                if (unlocked.size >= GameData.achievements.size - 1) unlockSpecialShip("omega_specter")
            }
        }
    }

    private fun registerMissionProgress(missionId: Int, amt: Int) {
        viewModelScope.launch {
            val progress = repository.getProgressDirect()
            val progMap = progress.getMissionsProgress().toMutableMap()
            val originalVal = progMap[missionId] ?: 0
            val targetMission = GameData.missions.firstOrNull { it.id == missionId } ?: return@launch
            if (originalVal < targetMission.target) {
                val newVal = if (missionId == 2) maxOf(originalVal, amt) else originalVal + amt
                progMap[missionId] = minOf(targetMission.target, newVal)
                repository.updateProgress(progress.copy(missionsProgressStr = UserProgress.buildMissionsProgressStr(progMap)))
            }
        }
    }

    fun claimMissionReward(missionId: Int) {
        viewModelScope.launch {
            val progress = userProgress.value ?: return@launch
            val progressMap = progress.getMissionsProgress()
            val currentProgress = progressMap[missionId] ?: 0
            val targetMission = GameData.missions.firstOrNull { it.id == missionId } ?: return@launch
            if (currentProgress >= targetMission.target) {
                val mutableProgress = progressMap.toMutableMap()
                mutableProgress[missionId] = 0
                repository.updateProgress(progress.copy(
                    crystals = progress.crystals + targetMission.rewardCrystals,
                    missionsProgressStr = UserProgress.buildMissionsProgressStr(mutableProgress)
                ))
                SoundSynth.playPowerup()
            }
        }
    }
}

data class GameplayState(
    val worldIndex: Int = 1,
    val difficultyStars: Int = 1,
    val score: Int = 0,
    val comboMultiplier: Int = 1,
    val comboTimerRemaining: Float = 0f,
    val crystalsCollectedThisRun: Int = 0,
    val activeBullets: List<Projectile> = emptyList(),
    val activeEnemies: List<EnemyDrone> = emptyList(),
    val activePowerUps: List<PowerUp> = emptyList(),
    val activeParticles: List<Particle> = emptyList(),
    val currentWave: Int = 1,
    val totalWaves: Int = 5,
    val isBossFight: Boolean = false,
    val gameEnded: Boolean = false,
    val levelComplete: Boolean = false,
    val wrongMatchesCount: Int = 0,
    val spawnTimer: Float = 0.6f,
    val totalKilledThisRun: Int = 0,
    val ultimateCharge: Float = 0f
)
