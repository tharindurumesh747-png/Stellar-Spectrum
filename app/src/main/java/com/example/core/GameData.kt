package com.example.core

data class ShipDefinition(
    val id: String,
    val name: String,
    val hp: Int,
    val fireRateMs: Long,
    val specialAbility: String,
    val crystalCost: Int,
    val colorHex: Long,
    val unlockCondition: String
)

data class WorldDefinition(
    val index: Int,
    val name: String,
    val description: String,
    val difficulty: Int,
    val bgGradientHexes: List<Long>,
    val musicVibe: String,
    val enemiesDescription: String,
    val bossName: String,
    val unlockWorldIndexRequired: Int,
    // NEW: per-world difficulty scaling — these are what actually make
    // each world feel different to play, not just look different.
    val enemySpeedMultiplier: Float,
    val spawnRateMultiplier: Float,
    val bossHpMultiplier: Float,
    val killsRequiredForBoss: Int
)

data class MissionDefinition(
    val id: Int, val name: String, val description: String,
    val target: Int, val rewardCrystals: Int, val isWeekly: Boolean
)

data class AchievementDefinition(
    val id: String, val name: String, val description: String,
    val target: Int, val rewardCrystals: Int, val type: String
)

// NEW: real cosmetic data — trails follow the ship, explosions play on kill.
data class TrailDefinition(
    val id: String, val name: String, val cost: Int,
    val description: String, val visualHint: String, val colorHex: Long
)

data class ExplosionDefinition(
    val id: String, val name: String, val cost: Int,
    val description: String, val visualHint: String, val colorHex: Long
)

object GameData {
    val ships = listOf(
        ShipDefinition("solar_wing", "Solar Wing", 100, 350, "Starter Ship (No ability)", 0, 0xFFFF2E63, "Default Available"),
        ShipDefinition("quantum_falcon", "Quantum Falcon", 90, 260, "+20% firing speed", 500, 0xFF00ADB5, "Buy for 500 💎"),
        ShipDefinition("nova_phantom", "Nova Phantom", 80, 310, "Ghost Dodge (1s invincible on swipe)", 1000, 0xFF10B981, "Buy for 1,000 💎"),
        ShipDefinition("eclipse_runner", "Eclipse Runner", 110, 420, "Auto-Shield charges x2 faster", 1500, 0xFFFBBF24, "Buy for 1,500 💎"),
        ShipDefinition("void_hunter", "Void Hunter", 95, 340, "Purple Void bullets home harder", 2000, 0xFFBD00FF, "Buy for 2,000 💎"),
        ShipDefinition("stellar_dragon", "Stellar Dragon", 85, 220, "Glow trail does light damage", 3000, 0xFFF43F5E, "Buy for 3,000 💎"),
        ShipDefinition("prism_knight", "Prism Knight", 120, 330, "Wrong matches do -5 HP instead of -15", 4000, 0xFF3B82F6, "Buy for 4,000 💎"),
        ShipDefinition("nebula_wraith", "Nebula Wraith", 75, 170, "Rapid-fire quantum driver", 5000, 0xFFEC4899, "Buy for 5,000 💎"),
        ShipDefinition("crystal_titan", "Crystal Titan", 130, 480, "Heavy-impact shields default +15", 0, 0xFF06B6D4, "Clear World 6"),
        ShipDefinition("omega_specter", "Omega Specter", 150, 240, "All colors deal 1.5x damage (Endgame)", 0, 0xFFEEEEEE, "Unlock all achievements")
    )

    // Difficulty escalates per world: faster enemies, denser spawns, tankier
    // bosses, and more kills required to summon the boss — so World 6 takes
    // meaningfully longer and feels harder than World 1, not just re-skinned.
    val worlds = listOf(
        WorldDefinition(1, "Neon Orbit", "Deep orbit surrounding grid-energy grids. Floating particle lanes.", 1,
            listOf(0xFF060913, 0xFF0D1B2A), "Intro Synthwave", "Pulse Drones", "Mini Titan", 0,
            enemySpeedMultiplier = 1.0f, spawnRateMultiplier = 1.0f, bossHpMultiplier = 1.0f, killsRequiredForBoss = 40),
        WorldDefinition(2, "Cyber Nebula", "Atmospheric layers of teal and violet gas fields. Digital data lines.", 2,
            listOf(0xFF0F0A1C, 0xFF2A0845), "Dark Cyberpunk Bass", "Pulse & Split Drones", "Dual-Phase Titan", 1,
            enemySpeedMultiplier = 1.15f, spawnRateMultiplier = 1.1f, bossHpMultiplier = 1.3f, killsRequiredForBoss = 55),
        WorldDefinition(3, "Quantum Gateway", "Highly energized gravitational gateway portals warping spacetime.", 3,
            listOf(0xFF050B14, 0xFF1B053A), "Gateway Ambient Dub", "Pulse & Warp Drones", "Warp Titan", 2,
            enemySpeedMultiplier = 1.3f, spawnRateMultiplier = 1.2f, bossHpMultiplier = 1.6f, killsRequiredForBoss = 70),
        WorldDefinition(4, "Crystal Galaxy", "A cluster of polyhedral crystal nodes reflecting star energy beams.", 4,
            listOf(0xFF0A192F, 0xFF0E0E32), "Electro Prisms", "Pulse, Shield & Prism Drones", "Prism Titan", 3,
            enemySpeedMultiplier = 1.45f, spawnRateMultiplier = 1.3f, bossHpMultiplier = 2.0f, killsRequiredForBoss = 85),
        WorldDefinition(5, "Dark Matter Realm", "Invisible gravitational rifts distorting star lanes.", 5,
            listOf(0xFF020205, 0xFF140D24), "Cinematic Void Bass", "All Drone types", "Grav-Void Titan", 4,
            enemySpeedMultiplier = 1.6f, spawnRateMultiplier = 1.45f, bossHpMultiplier = 2.5f, killsRequiredForBoss = 100),
        WorldDefinition(6, "Singularity Core", "The reality engine core fracturing space time grids.", 6,
            listOf(0xFF05000A, 0xFF1F0015), "Orchestral Final Synth", "High Intensity Drone Waves", "OMEGA TITAN", 5,
            enemySpeedMultiplier = 1.8f, spawnRateMultiplier = 1.6f, bossHpMultiplier = 3.2f, killsRequiredForBoss = 120)
    )

    val missions = listOf(
        MissionDefinition(1, "Red Nebula Purge", "Destroy 30 Red aura enemies", 30, 100, false),
        MissionDefinition(2, "Survival Streak", "Reach a score of 3,000 in a run", 3000, 150, false),
        MissionDefinition(3, "Titan Vanquished", "Defeat any World Boss", 1, 200, false),
        MissionDefinition(4, "Spectrum Shifts", "Switch colors 20 times in a single game", 20, 100, false),
        MissionDefinition(5, "Clean Driver", "Complete any World with < 3 wrong-color strikes", 3, 250, false),
        MissionDefinition(6, "Elite Hunter", "Defeat 150 total enemies", 150, 400, true),
        MissionDefinition(7, "Cosmic Sovereign", "Accumulate 10,000 total score points", 10000, 500, true)
    )

    val achievements = listOf(
        AchievementDefinition("first_blood", "First Blood", "Eliminate your first enemy drone", 1, 50, "enemy"),
        AchievementDefinition("color_master", "Color Master", "Defeat 100 total colored enemies", 100, 200, "enemy"),
        AchievementDefinition("galaxy_guardian", "Galaxy Guardian", "Unlock and clear the Singularity Core (World 6)", 6, 500, "world"),
        AchievementDefinition("cosmic_legend", "Cosmic Legend", "Score over 15,000 in a single run", 15000, 400, "score"),
        AchievementDefinition("untouchable", "Untouchable", "Complete a world run with zero wrong matches", 1, 350, "hits"),
        AchievementDefinition("boss_slayer_ach", "Boss Master", "Defeat a total of 10 world bosses", 10, 300, "boss"),
        AchievementDefinition("combo_king", "Combo Monarch", "Reach a massive 5x score combo multiplier", 5, 250, "combo")
    )

    // ── COSMETICS ──────────────────────────────────────────────────────
    val trails = listOf(
        TrailDefinition("default", "Standard Ion Trail", 0, "Basic glowing trail matching weapon color.", "🔹", 0xFF00ADB5),
        TrailDefinition("cyan_fume", "Cyan Fume Exhaust", 150, "Glowing cyan exhaust smoke trail.", "☄️", 0xFF00E5FF),
        TrailDefinition("purple_warp", "Wormhole Quantum", 350, "Pulsing gravitational purple rings.", "🌀", 0xFFBD00FF),
        TrailDefinition("gold_dust", "Crystalline Sparkle", 600, "Sparkling gold dust shed behind wings.", "✨", 0xFFFFD700),
        TrailDefinition("omega_clon", "Omega Spectral Echo", 1200, "Glowing white static silhouette tail.", "👥", 0xFFEEEEEE)
    )

    val explosions = listOf(
        ExplosionDefinition("default", "Standard Burst", 0, "Basic radial particle burst.", "💥", 0xFFFF2E63),
        ExplosionDefinition("cosmic_ring", "Radial Ring Burst", 200, "A massive expanding ring on impact.", "⭕", 0xFF00ADB5),
        ExplosionDefinition("pixel_shrapnel", "Retro Cube Shrapnel", 450, "Retro square shards on drone deaths.", "⏹️", 0xFFFBBF24),
        ExplosionDefinition("chroma_flicker", "Chromatic Aberration Flash", 800, "Flickers full-screen chromatic tints.", "🌈", 0xFFBD00FF)
    )
}
