package com.example.entities

import androidx.compose.ui.graphics.Color
import com.example.core.GameData
import com.example.core.SoundSynth
import kotlin.math.cos
import kotlin.math.sin

enum class EnergyColor(val index: Int, val title: String, val composeColor: Color, val hex: Long) {
    RED(1, "Red Plasma", Color(0xFFFF2E63), 0xFFFF2E63),
    BLUE(2, "Blue Quantum", Color(0xFF00ADB5), 0xFF00ADB5),
    GREEN(3, "Green Nova", Color(0xFF10B981), 0xFF10B981),
    PURPLE(4, "Purple Void", Color(0xFFBD00FF), 0xFFBD00FF);

    companion object {
        fun fromIndex(idx: Int) = values().firstOrNull { it.index == idx } ?: RED
        fun random() = values().random()
    }
}

enum class EnemyType {
    PULSE, SPLIT, WARP, SHIELD, PRISM, BOSS
}

enum class PowerUpType {
    SHIELD, MULTI_SHOT, SLOW_TIME, SCORE_MULT
}

data class PlayerShip(
    var x: Float = 500f, // Scaled coordinate
    var y: Float = 1400f,
    var targetX: Float = 500f,
    var maxHp: Int = 100,
    var hp: Int = 100,
    var selectedShipId: String = "solar_wing",
    var currentWeaponColor: EnergyColor = EnergyColor.RED,
    var shieldCharge: Float = 0.5f,
    var shieldActiveTimeRemaining: Float = 0f, // In seconds
    var lastShootTimeMs: Long = 0,
    var invincibilityTimeRemaining: Float = 0f // Ghost dodge 
) {
    val isShieldActive: Boolean get() = shieldActiveTimeRemaining > 0f
    val isInvincible: Boolean get() = invincibilityTimeRemaining > 0f || isShieldActive

    fun update(deltaTime: Float, fireCooldownMultiplier: Float) {
        // Interpolate smoothly toward target x coordinate (dodge swipe)
        x += (targetX - x) * 12f * deltaTime
        
        // Cooldowns
        if (shieldActiveTimeRemaining > 0f) {
            shieldActiveTimeRemaining = maxOf(0f, shieldActiveTimeRemaining - deltaTime)
        } else {
            // Charge shield over time slower / faster based on upgrade
            val chargeSpeed = if (selectedShipId == "eclipse_runner") 0.08f else 0.04f
            shieldCharge = minOf(1.0f, shieldCharge + chargeSpeed * deltaTime)
        }
        
        if (invincibilityTimeRemaining > 0f) {
            invincibilityTimeRemaining = maxOf(0f, invincibilityTimeRemaining - deltaTime)
        }
    }
}

data class Projectile(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: EnergyColor,
    val isFromPlayer: Boolean,
    var hasSplit: Boolean = false, // Special effect on Crystal Titan
    val id: Long = System.nanoTime()
) {
    fun update(deltaTime: Float) {
        x += vx * deltaTime
        y += vy * deltaTime
    }
    
    val isOutOfBounds: Boolean get() = y < -50f || y > 2000f || x < -50f || x > 1150f
}

data class EnemyDrone(
    val id: Long = System.nanoTime(),
    val type: EnemyType,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var hp: Int,
    var maxHp: Int,
    var auraColor: EnergyColor,
    var spawnTime: Long = System.currentTimeMillis(),
    
    // Type specific indicators
    var warpTimer: Float = 2.0f,         // Warp teleport countdown
    var rotationAngle: Float = 0f,       // Triangular Prism animation rotation
    var colorCycleTimer: Float = 1.5f,   // Prism color toggle duration
    var shieldHitsRemaining: Int = 3,    // Shield drone hit marker
    var bossShootingTimer: Float = 0f,   // Boss firing delays
    var bossPhase: Int = 1,              // Boss state phases
    var width: Float = 80f,
    var height: Float = 80f
) {
    fun update(deltaTime: Float, screenWidth: Float, playerX: Float, currentWorld: Int) {
        rotationAngle = (rotationAngle + 120f * deltaTime) % 360f

        when (type) {
            EnemyType.PULSE -> {
                // Sine wave movement straight down
                val t = (System.currentTimeMillis() - spawnTime).toFloat() / 1000f
                x += sin(t * 4.0f) * 150f * deltaTime
                y += vy * deltaTime
            }
            EnemyType.SPLIT -> {
                // Standard block zigzag
                x += vx * deltaTime
                if (x < 50f || x > screenWidth - 50f) {
                    vx = -vx
                }
                y += vy * deltaTime
            }
            EnemyType.WARP -> {
                // Teleport randomly after 2s
                warpTimer -= deltaTime
                if (warpTimer <= 0f) {
                    warpTimer = 2.0f
                    // Teleport left or right randomly safely in view
                    val randomFloatX = kotlin.random.Random.nextFloat()
                    val randomFloatY = kotlin.random.Random.nextFloat()
                    x = 100f + randomFloatX * (screenWidth - 200f)
                    y = 150f + randomFloatY * 450f
                    // Re-trigger visual flash
                    SoundSynth.playUiClick()
                }
                y += vy * 0.3f * deltaTime // Drifts slowly down between warps
            }
            EnemyType.SHIELD -> {
                // Slow straight descend
                y += vy * deltaTime
            }
            EnemyType.PRISM -> {
                // Erratic bounce movement
                x += vx * deltaTime
                y += vy * deltaTime
                if (x < 40f || x > screenWidth - 40f) vx = -vx
                if (y < 40f || y > 1000f) vy = -vy
                
                // Cycles color every 1.5s
                colorCycleTimer -= deltaTime
                if (colorCycleTimer <= 0f) {
                    colorCycleTimer = 1.5f
                    val nextIndex = (auraColor.ordinal + 1) % EnergyColor.values().size
                    auraColor = EnergyColor.values()[nextIndex]
                }
            }
            EnemyType.BOSS -> {
                width = 320f
                height = 320f
                
                // Slow floating entry toward vertical center (y = 400)
                if (y < 350f) {
                    y += 100f * deltaTime
                } else {
                    // Sway left and right
                    val t = (System.currentTimeMillis() - spawnTime).toFloat() / 1000f
                    x = 540f + sin(t * 1.5f) * 350f
                }

                // Handle Boss Phasing on Health markers
                val hpRatio = hp.toFloat() / maxHp.toFloat()
                val targetPhase = when {
                    hpRatio <= 0.25f -> 4
                    hpRatio <= 0.5f -> 3
                    hpRatio <= 0.75f -> 2
                    else -> 1
                }
                if (targetPhase > bossPhase) {
                    bossPhase = targetPhase
                    // Color transitions based on World specifications
                    when (currentWorld) {
                        2 -> auraColor = if (bossPhase % 2 == 0) EnergyColor.BLUE else EnergyColor.RED
                        3 -> auraColor = if (bossPhase % 2 == 0) EnergyColor.PURPLE else EnergyColor.BLUE
                        4 -> auraColor = EnergyColor.values()[bossPhase - 1] // Cycles fast
                        5 -> auraColor = if (bossPhase % 2 == 0) EnergyColor.PURPLE else EnergyColor.RED
                        6 -> auraColor = EnergyColor.values()[bossPhase - 1] // Omega cycles through sequentially
                    }
                }
                
                // Dynamic shifting aura for Omega Boss
                if (currentWorld == 6) {
                    val tCycle = (System.currentTimeMillis() - spawnTime).toFloat() / 1500f
                    auraColor = EnergyColor.values()[(tCycle.toInt() % 4)]
                }
            }
        }
    }
}

data class PowerUp(
    val type: PowerUpType,
    var x: Float,
    var y: Float,
    var vy: Float = 250f,
    val width: Float = 60f,
    val height: Float = 60f
) {
    fun update(deltaTime: Float) {
        y += vy * deltaTime
    }
}

enum class ParticleType {
    EXPLOSION, TRAIL, TEXT, SHOCKWAVE, COMBO_FLASH, SCREEN_FLASH
}

data class Particle(
    val type: ParticleType,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    val color: Color,
    val maxLife: Float, // seconds
    var life: Float = maxLife,
    val text: String = ""
) {
    fun update(deltaTime: Float) {
        life = maxOf(0f, life - deltaTime)
        x += vx * deltaTime
        y += vy * deltaTime
        
        if (type == ParticleType.EXPLOSION) {
            vx *= 0.95f // slows down
            vy *= 0.95f
        }
    }
    
    val runOut: Boolean get() = life <= 0f
}
