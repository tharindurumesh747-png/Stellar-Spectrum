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

enum class EnemyType { PULSE, SPLIT, WARP, SHIELD, PRISM, BOSS }
enum class PowerUpType { SHIELD, MULTI_SHOT, SLOW_TIME, SCORE_MULT }

data class PlayerShip(
    var x: Float = 500f,
    var y: Float = 1400f,
    var targetX: Float = 500f,
    var targetY: Float = 1400f,
    var maxHp: Int = 100,
    var hp: Int = 100,
    var selectedShipId: String = "solar_wing",
    var currentWeaponColor: EnergyColor = EnergyColor.RED,
    var shieldCharge: Float = 0.5f,
    var shieldActiveTimeRemaining: Float = 0f,
    var lastShootTimeMs: Long = 0,
    var invincibilityTimeRemaining: Float = 0f
) {
    val isShieldActive: Boolean get() = shieldActiveTimeRemaining > 0f
    val isInvincible: Boolean get() = invincibilityTimeRemaining > 0f || isShieldActive

    fun update(deltaTime: Float, fireCooldownMultiplier: Float) {
        x += (targetX - x) * 12f * deltaTime
        y += (targetY - y) * 12f * deltaTime
        x = x.coerceIn(80f, 1000f)
        y = y.coerceIn(200f, 1600f)

        if (shieldActiveTimeRemaining > 0f) {
            shieldActiveTimeRemaining = maxOf(0f, shieldActiveTimeRemaining - deltaTime)
        }
        if (invincibilityTimeRemaining > 0f) {
            invincibilityTimeRemaining = maxOf(0f, invincibilityTimeRemaining - deltaTime)
        }
    }
}

data class Projectile(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    val color: EnergyColor, val isFromPlayer: Boolean,
    var hasSplit: Boolean = false, val id: Long = System.nanoTime()
) {
    fun update(deltaTime: Float) { x += vx * deltaTime; y += vy * deltaTime }
    val isOutOfBounds: Boolean get() = y < -50f || y > 2000f || x < -50f || x > 1150f
}

data class EnemyDrone(
    val id: Long = System.nanoTime(),
    val type: EnemyType,
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    var hp: Int, var maxHp: Int, var auraColor: EnergyColor,
    var spawnTime: Long = System.currentTimeMillis(),
    var warpTimer: Float = 1.6f,
    var rotationAngle: Float = 0f,
    var colorCycleTimer: Float = 1.5f,
    var shieldHitsRemaining: Int = 3,
    var bossShootingTimer: Float = 0f,
    var bossPhase: Int = 1,
    var width: Float = 80f,
    var height: Float = 80f,
    // ── BOSS LIGHTNING STRIKE state machine ──────────────────────────
    // 0 = idle, 1 = charging (telegraph warning), 2 = actively striking
    var lightningState: Int = 0,
    // Counts down during charging/firing phases (repurposes the old field)
    var ultimateTimer: Float = 7f,
    // Each fires exactly once per boss fight, at that HP% threshold
    var hit70: Boolean = false,
    var hit50: Boolean = false,
    var hit20: Boolean = false
) {
    fun update(deltaTime: Float, screenWidth: Float, playerX: Float, currentWorld: Int) {
        rotationAngle = (rotationAngle + 120f * deltaTime) % 360f

        when (type) {
            EnemyType.PULSE -> {
                val t = (System.currentTimeMillis() - spawnTime).toFloat() / 1000f
                x += sin(t * 4.0f) * 150f * deltaTime
                y += vy * deltaTime
            }
            EnemyType.SPLIT -> {
                x += vx * deltaTime
                if (x < 50f || x > screenWidth - 50f) vx = -vx
                y += vy * deltaTime
            }
            EnemyType.WARP -> {
                warpTimer -= deltaTime
                if (warpTimer <= 0f) {
                    warpTimer = 1.6f
                    x = 100f + kotlin.random.Random.nextFloat() * (screenWidth - 200f)
                    SoundSynth.playUiClick()
                }
                y += vy * deltaTime
            }
            EnemyType.SHIELD -> { y += vy * deltaTime }
            EnemyType.PRISM -> {
                x += vx * deltaTime
                y += vy * deltaTime
                if (x < 40f || x > screenWidth - 40f) vx = -vx
                if (y < 40f || y > 1000f) vy = -vy
                colorCycleTimer -= deltaTime
                if (colorCycleTimer <= 0f) {
                    colorCycleTimer = 1.5f
                    auraColor = EnergyColor.values()[(auraColor.ordinal + 1) % EnergyColor.values().size]
                }
            }
            EnemyType.BOSS -> {
                width = 320f; height = 320f
                if (y < 350f) { y += 100f * deltaTime }
                else {
                    val t = (System.currentTimeMillis() - spawnTime).toFloat() / 1000f
                    x = 540f + sin(t * 1.5f) * 350f
                }
                val hpRatio = hp.toFloat() / maxHp.toFloat()
                val targetPhase = when {
                    hpRatio <= 0.25f -> 4; hpRatio <= 0.5f -> 3
                    hpRatio <= 0.75f -> 2; else -> 1
                }
                if (targetPhase > bossPhase) {
                    bossPhase = targetPhase
                    when (currentWorld) {
                        2 -> auraColor = if (bossPhase % 2 == 0) EnergyColor.BLUE else EnergyColor.RED
                        3 -> auraColor = if (bossPhase % 2 == 0) EnergyColor.PURPLE else EnergyColor.BLUE
                        4 -> auraColor = EnergyColor.values()[bossPhase - 1]
                        5 -> auraColor = if (bossPhase % 2 == 0) EnergyColor.PURPLE else EnergyColor.RED
                        6 -> auraColor = EnergyColor.values()[bossPhase - 1]
                    }
                }
                if (currentWorld == 6) {
                    val tCycle = (System.currentTimeMillis() - spawnTime).toFloat() / 1500f
                    auraColor = EnergyColor.values()[(tCycle.toInt() % 4)]
                }
            }
        }
    }
}

data class PowerUp(
    val type: PowerUpType, var x: Float, var y: Float, var vy: Float = 220f,
    val width: Float = 60f, val height: Float = 60f, var angle: Float = 0f
) { fun update(deltaTime: Float) { y += vy * deltaTime; angle += 90f * deltaTime } }

enum class ParticleType { EXPLOSION, TRAIL, TEXT, SHOCKWAVE, COMBO_FLASH, SCREEN_FLASH, LIGHTNING_LINK }

data class Particle(
    val type: ParticleType, var x: Float, var y: Float, var vx: Float, var vy: Float,
    var size: Float, val color: Color, val maxLife: Float,
    var life: Float = maxLife, val text: String = "",
    // Used only by LIGHTNING_LINK to draw a jagged bolt from (x,y) to (targetX,targetY)
    var targetX: Float = 0f, var targetY: Float = 0f
) {
    fun update(deltaTime: Float) {
        life = maxOf(0f, life - deltaTime)
        x += vx * deltaTime; y += vy * deltaTime
        if (type == ParticleType.EXPLOSION) { vx *= 0.95f; vy *= 0.95f }
    }
    val runOut: Boolean get() = life <= 0f
}
