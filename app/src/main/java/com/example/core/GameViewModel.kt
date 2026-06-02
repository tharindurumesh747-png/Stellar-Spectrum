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

enum class GameScreen {
    SPLASH, MAIN_MENU, SHIP_SELECT, WORLD_SELECT, GAMEPLAY, SHOP, ACHIEVEMENTS, MISSIONS, LEADERBOARD, SETTINGS
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: UserProgressRepository
    
    // UI Screen state machine
    val activeScreen = MutableStateFlow(GameScreen.SPLASH)
    
    // Save-state flow from DB
    val userProgress: StateFlow<UserProgress?>
    
    // Gameplay states
    val gameplayState = MutableStateFlow(GameplayState())
    val playerShipState = MutableStateFlow(PlayerShip())
    
    // Daily reward tracking state
    val dailyRewardAvailable = MutableStateFlow(false)
    val showDailyRewardPopup = MutableStateFlow(false)

    // Leaderboard coming soon status
    val showLeaderboardComingSoon = MutableStateFlow(false)
    
    // Sound & Vibration local caches from Settings
    var soundEnabled = true
    var musicEnabled = true
    var vibrationEnabled = true

    init {
        val database = AppDatabase.getDatabase(application)
        repository = UserProgressRepository(database.userProgressDao())
        userProgress = repository.progressFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        // Observe progress updates to cache sound/vibe & check daily rewards
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

    // Daily Rewards checks
    private fun checkDailyRewardStatus(progress: UserProgress) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val isClaimable = progress.lastDailyReward != todayStr
        dailyRewardAvailable.value = isClaimable
    }

    fun claimDailyReward() {
        val progress = userProgress.value ?: return
        if (!dailyRewardAvailable.value) return

        viewModelScope.launch {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val nextDay = (progress.dailyRewardDay % 7) + 1
            // Escalating reward crystals
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
            
            // Check Achievements: Unlock milestones
            triggerAchievementProgress("crystals", progress.crystals + rewardCrystals)
        }
    }

    fun changeScreen(screen: GameScreen) {
        activeScreen.value = screen
        
        // Custom world sound trigger on Screen change
        if (screen == GameScreen.GAMEPLAY) {
            val selectedWorld = gameplayState.value.worldIndex
            BgmEngine.start(selectedWorld)
        } else {
            BgmEngine.stop()
        }
    }

    // Setup active Level details
    fun selectWorldAndStart(worldIndex: Int) {
        val progress = userProgress.value ?: return
        val worldDef = GameData.worlds.first { it.index == worldIndex }
        
        // Instantiate Player parameters
        val shipDef = GameData.ships.first { it.id == progress.selectedShip }
        val player = PlayerShip(
            x = 540f, // Center horizontally
            y = 1500f,
            targetX = 540f,
            maxHp = shipDef.hp,
            hp = shipDef.hp,
            selectedShipId = shipDef.id
        )
        playerShipState.value = player
        
        // Initialize gameplay values
        gameplayState.value = GameplayState(
            worldIndex = worldIndex,
            difficultyStars = worldDef.difficulty,
            totalWaves = 5,
            levelComplete = false,
            gameEnded = false
        )

        triggerWaveSpawn(1)
        changeScreen(GameScreen.GAMEPLAY)
    }

    private fun triggerWaveSpawn(wave: Int) {
        val currentPlay = gameplayState.value
        val isBoss = wave >= currentPlay.totalWaves
        
        val world = currentPlay.worldIndex
        val count = 4 + wave * 2
        val dronesList = mutableListOf<EnemyDrone>()

        if (isBoss) {
            val bossHp = when (world) {
                1 -> 50
                2 -> 100
                3 -> 120
                4 -> 150
                5 -> 200
                else -> 300
            }
            val firstAura = when (world) {
                1 -> EnergyColor.RED
                2 -> EnergyColor.RED
                3 -> EnergyColor.BLUE
                4 -> EnergyColor.RED
                5 -> EnergyColor.PURPLE
                else -> EnergyColor.RED // Will cycle
            }
            dronesList.add(
                EnemyDrone(
                    type = EnemyType.BOSS,
                    x = 540f,
                    y = -200f, // Drops in smoothly from sky
                    vx = 0f,
                    vy = 0f,
                    hp = bossHp,
                    maxHp = bossHp,
                    auraColor = firstAura
                )
            )
        } else {
            // Spawn drones dynamically by World
            for (i in 0 until count) {
                val spawnX = 120f + i * (800f / maxOf(1, count - 1))
                val spawnY = -50f - (i * 180f)
                
                // Select Drone type based on World Index
                val type = when (world) {
                    1 -> EnemyType.PULSE
                    2 -> if (i % 2 == 0) EnemyType.SPLIT else EnemyType.PULSE
                    3 -> if (i % 3 == 0) EnemyType.WARP else EnemyType.PULSE
                    4 -> if (i % 3 == 0) EnemyType.PRISM else if (i % 3 == 1) EnemyType.SHIELD else EnemyType.PULSE
                    else -> EnemyType.values().filter { it != EnemyType.BOSS }.random() // Heavy chaotic worlds
                }

                val aura = if (type == EnemyType.PRISM) EnergyColor.RED else EnergyColor.values().random()
                val speedY = 160f + wave * 15f
                val speedX = if (type == EnemyType.SPLIT) 200f else 0f
                
                dronesList.add(
                    EnemyDrone(
                        type = type,
                        x = spawnX,
                        y = spawnY,
                        vx = speedX,
                        vy = speedY,
                        hp = if (type == EnemyType.SHIELD) 3 else 1,
                        maxHp = if (type == EnemyType.SHIELD) 3 else 1,
                        auraColor = aura
                    )
                )
            }
        }

        gameplayState.value = currentPlay.copy(
            activeEnemies = dronesList,
            currentWave = wave,
            isBossFight = isBoss
        )
    }

    // Switch ship energy alignment color
    fun switchWeaponColor(color: EnergyColor) {
        val player = playerShipState.value
        if (player.currentWeaponColor != color) {
            player.currentWeaponColor = color
            SoundSynth.playUiClick()
            
            // Register Mission Progress: Color triggers
            registerMissionProgress(4, 1)
        }
    }

    // Fire bullet from player ship
    fun fireBulletAt(targetTapX: Float, targetTapY: Float) {
        val player = playerShipState.value
        val now = System.currentTimeMillis()
        
        // Verify weapon cooldown rate
        val shipDef = GameData.ships.first { it.id == player.selectedShipId }
        val fRate = if (player.selectedShipId == "quantum_falcon") (shipDef.fireRateMs * 0.82f).toLong() else shipDef.fireRateMs
        
        if (now - player.lastShootTimeMs < fRate) return
        player.lastShootTimeMs = now

        val bulletColor = player.currentWeaponColor
        val px = player.x
        val py = player.y - 40f

        val angle = atan2(targetTapY - py, targetTapX - px)
        val speed = 1000f

        // Play corresponding procedural laser sound
        when (bulletColor) {
            EnergyColor.RED -> SoundSynth.playShootRed()
            EnergyColor.BLUE -> SoundSynth.playShootBlue()
            EnergyColor.GREEN -> SoundSynth.playShootGreen()
            EnergyColor.PURPLE -> SoundSynth.playShootPurple()
        }

        val list = gameplayState.value.activeBullets.toMutableList()

        when (bulletColor) {
            EnergyColor.RED -> {
                // Red plasma: straight fast lasers
                list.add(Projectile(px, py, 0f, -speed * 1.2f, bulletColor, isFromPlayer = true))
            }
            EnergyColor.BLUE -> {
                // Blue Quantum: heavier pulsing piercing laser
                list.add(Projectile(px, py, 0f, -speed * 0.8f, bulletColor, isFromPlayer = true))
            }
            EnergyColor.GREEN -> {
                // Green Nova: Spread shot 3 directions
                list.add(Projectile(px, py - 10f, 0f, -speed, bulletColor, isFromPlayer = true))
                list.add(Projectile(px, py, -220f, -speed * 0.95f, bulletColor, isFromPlayer = true))
                list.add(Projectile(px, py, 220f, -speed * 0.95f, bulletColor, isFromPlayer = true))
            }
            EnergyColor.PURPLE -> {
                // Purple Void: Homes onto the closest enemy (dx dy drift calculated dynamically in loop)
                list.add(Projectile(px, py, 0f, -speed * 0.9f, bulletColor, isFromPlayer = true))
            }
        }
        
        gameplayState.value = gameplayState.value.copy(activeBullets = list)
    }

    // Swipe dodge action activates special ghost dodging
    fun triggerSwipeDodge(swipeDirectionLeft: Boolean) {
        val player = playerShipState.value
        val boundsMove = if (swipeDirectionLeft) -250f else 250f
        player.targetX = (player.x + boundsMove).coerceIn(100f, 980f)
        
        if (player.selectedShipId == "nova_phantom") {
            // 1s invincibility
            player.invincibilityTimeRemaining = 1.0f
            spawnCustomParticles(player.x, player.y, Color(0xFF10B981), ParticleType.COMBO_FLASH, "GHOST!")
        }
    }

    // Trigger energy shield
    fun activateShield() {
        val player = playerShipState.value
        if (player.shieldCharge >= 1.0f) {
            player.shieldCharge = 0f
            player.shieldActiveTimeRemaining = 3.0f // 3 seconds immunity
            SoundSynth.playPowerup()
            spawnCustomParticles(player.x, player.y, Color.White, ParticleType.SHOCKWAVE)
        }
    }

    // Update coordinates, spawn stars, check collision triggers
    fun updateGame(deltaTime: Float) {
        val currentPlay = gameplayState.value
        if (currentPlay.gameEnded || currentPlay.levelComplete) return

        val player = playerShipState.value
        val fireCooldownMultiplier = if (player.selectedShipId == "nebula_wraith") 0.65f else 1.0f
        player.update(deltaTime, fireCooldownMultiplier)
        
        val bullets = currentPlay.activeBullets.toMutableList()
        val enemies = currentPlay.activeEnemies.toMutableList()
        val powerups = currentPlay.activePowerUps.toMutableList()
        val particles = currentPlay.activeParticles.toMutableList()
        
        var currentScore = currentPlay.score
        var currentCombo = currentPlay.comboMultiplier
        var comboTimer = maxOf(0f, currentPlay.comboTimerRemaining - deltaTime)
        var crystalsCollected = currentPlay.crystalsCollectedThisRun
        var rawKilled = 0
        var bossesKilled = 0
        var strikes = currentPlay.wrongMatchesCount

        if (comboTimer <= 0f) {
            currentCombo = 1 // reset combo multiplier
        }

        // Particle dynamics fade life
        val iteratorParticles = particles.iterator()
        while (iteratorParticles.hasNext()) {
            val part = iteratorParticles.next()
            part.update(deltaTime)
            if (part.runOut) {
                iteratorParticles.remove()
            }
        }

        // Add standard ship thrust particles
        if ((0..10).random() < 3) {
            val trailColor = when (player.currentWeaponColor) {
                EnergyColor.RED -> Color(0xFFFF2E63)
                EnergyColor.BLUE -> Color(0xFF00ADB5)
                EnergyColor.GREEN -> Color(0xFF10B981)
                EnergyColor.PURPLE -> Color(0xFFBD00FF)
            }
            val randomXOffset = -20f + kotlin.random.Random.nextFloat() * 40f
            val randomVx = -40f + kotlin.random.Random.nextFloat() * 80f
            val randomVy = 150f + kotlin.random.Random.nextFloat() * 150f
            particles.add(
                Particle(
                    type = ParticleType.TRAIL,
                    x = player.x + randomXOffset,
                    y = player.y + 35f,
                    vx = randomVx,
                    vy = randomVy,
                    size = (6..12).random().toFloat(),
                    color = trailColor.copy(alpha = 0.65f),
                    maxLife = 0.4f
                )
            )
        }

        // Update bullets positions
        val bulletIter = bullets.iterator()
        while (bulletIter.hasNext()) {
            val bullet = bulletIter.next()
            bullet.update(deltaTime)
            
            // Homing tracking for Purple voids
            if (bullet.color == EnergyColor.PURPLE && bullet.isFromPlayer) {
                val target = enemies.minByOrNull { sqrt((it.x - bullet.x)*(it.x - bullet.x) + (it.y - bullet.y)*(it.y - bullet.y)) }
                if (target != null) {
                    val angle = atan2(target.y - bullet.y, target.x - bullet.x)
                    val factor = if (player.selectedShipId == "void_hunter") 12f else 6f
                    bullet.vx += cos(angle) * 800f * factor * deltaTime
                    bullet.vy += sin(angle) * 800f * factor * deltaTime
                    // Speed lock
                    val mag = sqrt(bullet.vx*bullet.vx + bullet.vy*bullet.vy)
                    if (mag > 0f) {
                        bullet.vx = (bullet.vx / mag) * 950f
                        bullet.vy = (bullet.vy / mag) * 950f
                    }
                }
            }
            
            if (bullet.isOutOfBounds) {
                bulletIter.remove()
            }
        }

        // Update enemies list mechanics
        val enemyIter = enemies.iterator()
        while (enemyIter.hasNext()) {
            val enemy = enemyIter.next()
            enemy.update(deltaTime, 1080f, player.x, currentPlay.worldIndex)
            
            // Enemies firing patterns
            if (enemy.type == EnemyType.BOSS) {
                enemy.bossShootingTimer += deltaTime
                val shootRate = when (enemy.bossPhase) {
                    4 -> 0.8f
                    3 -> 1.2f
                    2 -> 1.6f
                    else -> 2.0f
                }
                if (enemy.bossShootingTimer >= shootRate) {
                    enemy.bossShootingTimer = 0f
                    // Fires color burst
                    val count = if (enemy.bossPhase == 4) 8 else 4
                    for (i in 0 until count) {
                        val ang = (i * (2 * Math.PI / count)) + (System.currentTimeMillis() / 400.0)
                        bullets.add(
                            Projectile(
                                x = enemy.x,
                                y = enemy.y + 50f,
                                vx = cos(ang).toFloat() * 350f,
                                vy = sin(ang).toFloat() * 350f,
                                color = enemy.auraColor,
                                isFromPlayer = false
                            )
                        )
                    }
                }
            } else if ((0..1000).random() < 3 + currentPlay.currentWave) {
                // Occasional bullet from normal drones
                bullets.add(
                    Projectile(
                        x = enemy.x,
                        y = enemy.y + 35f,
                        vx = 0f,
                        vy = 400f,
                        color = enemy.auraColor,
                        isFromPlayer = false
                    )
                )
            }

            // Check if bounds breached bottom limit
            if (enemy.y > 1750f) {
                if (enemy.type != EnemyType.BOSS) {
                    enemyIter.remove()
                    // Escape causes player HP strike if unshielded
                    if (!player.isInvincible) {
                        player.hp = maxOf(0, player.hp - 10)
                        triggerVibration(getApplication(), 80)
                        SoundSynth.playHitWrong()
                        particles.add(Particle(ParticleType.SCREEN_FLASH, 0f,0f,0f,0f,0f,Color.Red.copy(alpha=0.3f),0.25f))
                    }
                } else {
                    enemy.y = 200f // bounce back safe
                }
            }
        }

        // Collision Check: Player bullets vs Enemies
        val bIter = bullets.iterator()
        while (bIter.hasNext()) {
            val bullet = bIter.next()
            if (!bullet.isFromPlayer) continue

            val eIter = enemies.iterator()
            var bulletConsumed = false
            while (eIter.hasNext()) {
                val enemy = eIter.next()
                
                // standard radial bounding box collision estimate
                val dx = bullet.x - enemy.x
                val dy = bullet.y - enemy.y
                val dist = sqrt(dx*dx + dy*dy)
                val rangeRadius = if (enemy.type == EnemyType.BOSS) 150f else 50f

                if (dist < rangeRadius) {
                    // Match check!
                    if (bullet.color == enemy.auraColor) {
                        // Success matching!
                        enemy.hp--
                        SoundSynth.playHitCorrect()
                        
                        // Spawn explosion bursts
                        for (i in 0..12) {
                            particles.add(
                                Particle(
                                    type = ParticleType.EXPLOSION,
                                    x = enemy.x,
                                    y = enemy.y,
                                    vx = -250f + kotlin.random.Random.nextFloat() * 500f,
                                    vy = -250f + kotlin.random.Random.nextFloat() * 500f,
                                    size = (8..22).random().toFloat(),
                                    color = bullet.color.composeColor,
                                    maxLife = 0.45f
                                )
                            )
                        }

                        if (enemy.hp <= 0) {
                            // Enemy destroyed!
                            eIter.remove()
                            rawKilled++
                            
                            // Color stats milestones
                            if (bullet.color == EnergyColor.RED) registerMissionProgress(1, 1)

                            // Check special behaviors: SPLIT Drone breaks in halves
                            if (enemy.type == EnemyType.SPLIT) {
                                enemies.add(
                                    EnemyDrone(
                                        type = EnemyType.PULSE,
                                        x = enemy.x - 40f,
                                        y = enemy.y,
                                        vx = -120f,
                                        vy = enemy.vy * 1.1f,
                                        hp = 1,
                                        maxHp = 1,
                                        auraColor = enemy.auraColor
                                    )
                                )
                                enemies.add(
                                    EnemyDrone(
                                        type = EnemyType.PULSE,
                                        x = enemy.x + 40f,
                                        y = enemy.y,
                                        vx = 120f,
                                        vy = enemy.vy * 1.1f,
                                        hp = 1,
                                        maxHp = 1,
                                        auraColor = enemy.auraColor
                                    )
                                )
                            } else if (enemy.type == EnemyType.BOSS) {
                                bossesKilled++
                                SoundSynth.playBossDie()
                                viewModelScope.launch { triggerAchievementProgress("boss", 1) }
                                registerMissionProgress(3, 1)
                                
                                // massive reward
                                crystalsCollected += 200
                                currentScore += 5000
                            }

                            // Compute points
                            val rewardBase = when (enemy.type) {
                                EnemyType.PULSE -> 100
                                EnemyType.SPLIT -> 150
                                EnemyType.WARP -> 200
                                EnemyType.SHIELD -> 300
                                EnemyType.PRISM -> 400
                                EnemyType.BOSS -> 5000
                            }
                            
                            // Scale score with current active combo multiplier
                            currentScore += rewardBase * currentCombo
                            crystalsCollected += 2 * currentCombo
                            
                            // Advance combo
                            currentCombo = minOf(5, currentCombo + 1)
                            comboTimer = 2.5f // 2.5 seconds window link
                            
                            viewModelScope.launch { triggerAchievementProgress("combo", currentCombo) }
                            
                            // Spark combo flash particle of text
                            if (currentCombo > 1) {
                                particles.add(
                                    Particle(
                                        type = ParticleType.TEXT,
                                        x = enemy.x,
                                        y = enemy.y - 30f,
                                        vx = 0f,
                                        vy = -180f,
                                        size = 32f,
                                        color = Color(0xFFFBBF24),
                                        maxLife = 0.55f,
                                        text = "${currentCombo}x COMBO!"
                                    )
                                )
                            }
                        }
                    } else {
                        // Color mismatch failure! ENERGY BOUNCE STRIKE
                        strikes++
                        bulletConsumed = true
                        
                        // Shield consumes wrong matches with no damage penalty
                        if (!player.isInvincible) {
                            val damageDone = if (player.selectedShipId == "prism_knight") 5 else 15
                            player.hp = maxOf(0, player.hp - damageDone)
                            triggerVibration(getApplication(), 95)
                            SoundSynth.playHitWrong()
                        }
                        
                        // Spawn erratic error sparks
                        for (i in 0..6) {
                            particles.add(
                                Particle(
                                    type = ParticleType.EXPLOSION,
                                    x = bullet.x,
                                    y = bullet.y,
                                    vx = -200f + kotlin.random.Random.nextFloat() * 400f,
                                    vy = -200f + kotlin.random.Random.nextFloat() * 400f,
                                    size = 12f,
                                    color = Color.Red,
                                    maxLife = 0.35f
                                )
                            )
                        }
                        
                        // Screen flash red damage indicator
                        particles.add(
                            Particle(
                                type = ParticleType.SCREEN_FLASH,
                                x = 0f, y = 0f, vx = 0f, vy = 0f, size = 0f,
                                color = Color.Red.copy(alpha = 0.28f),
                                maxLife = 0.28f
                            )
                        )
                        
                        // break combo
                        currentCombo = 1
                    }
                    
                    // Consume bullet unless Blue piercing quantum weapon is selected
                    if (bullet.color != EnergyColor.BLUE || bulletConsumed) {
                        bulletConsumed = true
                        break
                    }
                }
            }
            if (bulletConsumed) {
                bIter.remove()
            }
        }

        // Collision Check: Enemy bullets/bodies hits Player Ship
        val enemyBIter = bullets.iterator()
        while (enemyBIter.hasNext()) {
            val b = enemyBIter.next()
            if (b.isFromPlayer) continue

            val dx = b.x - player.x
            val dy = b.y - player.y
            val dist = sqrt(dx*dx + dy*dy)
            
            if (dist < 60f) {
                enemyBIter.remove()
                // Take hit
                if (!player.isInvincible) {
                    player.hp = maxOf(0, player.hp - 12)
                    triggerVibration(getApplication(), 100)
                    SoundSynth.playHitWrong()
                    
                    particles.add(
                        Particle(
                            type = ParticleType.SCREEN_FLASH,
                            x = 0f, y = 0f, vx = 0f, vy = 0f, size = 0f,
                            color = Color.Red.copy(alpha = 0.25f),
                            maxLife = 0.22f
                        )
                    )
                }
            }
        }

        // Game over conditions
        if (player.hp <= 0) {
            SoundSynth.playHitWrong()
            gameplayState.value = currentPlay.copy(gameEnded = true)
            saveGameStatsAndEnd(worldIndex = currentPlay.worldIndex, score = currentScore, crystals = crystalsCollected, rawKilled = rawKilled, bossesKilled = bossesKilled, strikes = strikes, complete = false)
            return
        }

        // Level Wave Complete transition checks
        if (enemies.isEmpty()) {
            val nextWave = currentPlay.currentWave + 1
            if (nextWave <= currentPlay.totalWaves) {
                triggerWaveSpawn(nextWave)
            } else {
                // Victory! Clear screen
                gameplayState.value = currentPlay.copy(levelComplete = true)
                saveGameStatsAndEnd(worldIndex = currentPlay.worldIndex, score = currentScore, crystals = crystalsCollected, rawKilled = rawKilled, bossesKilled = bossesKilled, strikes = strikes, complete = true)
                return
            }
        }

        // Sync local states back to state flows
        gameplayState.value = currentPlay.copy(
            score = currentScore,
            comboMultiplier = currentCombo,
            comboTimerRemaining = comboTimer,
            crystalsCollectedThisRun = crystalsCollected,
            activeBullets = bullets,
            activeEnemies = enemies,
            activePowerUps = powerups,
            activeParticles = particles,
            wrongMatchesCount = strikes
        )
    }

    private fun saveGameStatsAndEnd(worldIndex: Int, score: Int, crystals: Int, rawKilled: Int, bossesKilled: Int, strikes: Int, complete: Boolean) {
        viewModelScope.launch {
            if (complete) {
                // Unlock and advance next World index progression securely
                if (worldIndex < 6) {
                    repository.unlockWorld(worldIndex + 1)
                }
                
                // Clear conditions achievement triggers
                if (worldIndex == 6) {
                    triggerAchievementProgress("world", 6)
                }
                if (strikes == 0) {
                    triggerAchievementProgress("hits", 1)
                    registerMissionProgress(5, 1) // Clean driver
                }
            }
            
            // Core save DB integration
            repository.completeRun(
                worldId = worldIndex,
                score = score,
                crystalsEarned = crystals,
                rawEnemies = rawKilled,
                bosssKilled = bossesKilled
            )

            // Mission analytics progress updates
            registerMissionProgress(2, score) // Score survivor
            registerMissionProgress(6, rawKilled) // Elite hunter
            registerMissionProgress(7, score) // Total cosmic score accumulation

            // Unlock Crystal Titan ship definition on World 6 clear
            if (complete && worldIndex == 6) {
                unlockSpecialShip("crystal_titan")
            }
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

    // Purchase shop listings using crystal balances
    fun buyShip(shipId: String, cost: Int) {
        viewModelScope.launch {
            val progress = userProgress.value ?: return@launch
            if (progress.crystals >= cost) {
                val success = repository.unlockShip(shipId, cost)
                if (success) {
                    SoundSynth.playPowerup()
                }
            } else {
                SoundSynth.playHitWrong()
            }
        }
    }

    fun selectShip(shipId: String) {
        viewModelScope.launch {
            repository.selectShip(shipId)
        }
    }

    fun toggleSounds(sound: Boolean, music: Boolean, vibration: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(sound, music, vibration)
        }
    }

    fun resetAllProfileData() {
        viewModelScope.launch {
            repository.resetAllData()
            SoundSynth.playHitWrong()
        }
    }

    // Helper to spin particle shapes on HUD coordinates
    private fun spawnCustomParticles(px: Float, py: Float, col: Color, type: ParticleType, message: String = "") {
        val state = gameplayState.value
        val list = state.activeParticles.toMutableList()
        list.add(Particle(type, px, py, 0f, -120f, 26f, col, 0.7f, text = message))
        gameplayState.value = state.copy(activeParticles = list)
    }

    // Achievement state check loops
    private suspend fun triggerAchievementProgress(type: String, currentValue: Int) {
        val progress = repository.getProgressDirect()
        val unlocked = progress.getUnlockedAchievements().toMutableList()
        
        GameData.achievements.forEach { ach ->
            if (!unlocked.contains(ach.id) && ach.type == type && currentValue >= ach.target) {
                unlocked.add(ach.id)
                // Award crystals instantly to profile
                val addedCrystals = progress.crystals + ach.rewardCrystals
                repository.updateProgress(
                    progress.copy(
                        crystals = addedCrystals,
                        achievementsUnlockedStr = unlocked.joinToString(",")
                    )
                )
                SoundSynth.playPowerup()
                
                // If all achievements unlocked milestone, unlock legendary Omega Specter
                if (unlocked.size >= GameData.achievements.size - 1) { // Omega lock exception
                    unlockSpecialShip("omega_specter")
                }
            }
        }
    }

    // Database tracking mission counts
    private fun registerMissionProgress(missionId: Int, amt: Int) {
        viewModelScope.launch {
            val progress = repository.getProgressDirect()
            val progMap = progress.getMissionsProgress().toMutableMap()
            val originalVal = progMap[missionId] ?: 0
            val targetMission = GameData.missions.firstOrNull { it.id == missionId } ?: return@launch
            
            if (originalVal < targetMission.target) {
                val newVal = if (missionId == 2) maxOf(originalVal, amt) else originalVal + amt
                progMap[missionId] = minOf(targetMission.target, newVal)
                repository.updateProgress(
                    progress.copy(
                        missionsProgressStr = UserProgress.buildMissionsProgressStr(progMap)
                    )
                )
            }
        }
    }

    // Claim crystals rewards for finished missions
    fun claimMissionReward(missionId: Int) {
        viewModelScope.launch {
            val progress = userProgress.value ?: return@launch
            val progressMap = progress.getMissionsProgress()
            val currentProgress = progressMap[missionId] ?: 0
            val targetMission = GameData.missions.firstOrNull { it.id == missionId } ?: return@launch

            if (currentProgress >= targetMission.target) {
                // Claim reward! Add crystals and reset mission progress to 0
                val mutableProgress = progressMap.toMutableMap()
                mutableProgress[missionId] = 0 // resets after claiming
                
                repository.updateProgress(
                    progress.copy(
                        crystals = progress.crystals + targetMission.rewardCrystals,
                        missionsProgressStr = UserProgress.buildMissionsProgressStr(mutableProgress)
                    )
                )
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
    val wrongMatchesCount: Int = 0
)
