package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val id: Int = 1,
    val crystals: Int = 0,
    val selectedShip: String = "solar_wing",
    val unlockedShipsStr: String = "solar_wing",
    val unlockedWorldsStr: String = "1",
    val highScoresStr: String = "1:0,2:0,3:0,4:0,5:0,6:0",
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val lastDailyReward: String = "",
    val dailyRewardDay: Int = 0,
    val totalRuns: Int = 0,
    val totalEnemiesKilled: Int = 0,
    val totalBossesKilled: Int = 0,
    val missionsProgressStr: String = "1:0,2:0,3:0,4:0,5:0",
    val achievementsUnlockedStr: String = "",
    // NEW: cosmetic trails & explosions — these were previously purchased
    // via a fake "aesthetic_dummy" id that deducted crystals but never
    // recorded WHICH item was bought or equipped. Now properly tracked.
    val unlockedTrailsStr: String = "",
    val unlockedExplosionsStr: String = "",
    val selectedTrail: String = "default",
    val selectedExplosion: String = "default"
) {
    fun getHighScores(): Map<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        if (highScoresStr.isEmpty()) return map
        highScoresStr.split(",").forEach { pair ->
            val parts = pair.split(":")
            if (parts.size == 2) {
                val world = parts[0].toIntOrNull() ?: return@forEach
                val score = parts[1].toIntOrNull() ?: 0
                map[world] = score
            }
        }
        return map
    }

    companion object {
        fun buildHighScoresStr(scores: Map<Int, Int>): String =
            scores.entries.joinToString(",") { "${it.key}:${it.value}" }

        fun buildMissionsProgressStr(progress: Map<Int, Int>): String =
            progress.entries.joinToString(",") { "${it.key}:${it.value}" }
    }

    fun getMissionsProgress(): Map<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        if (missionsProgressStr.isEmpty()) return map
        missionsProgressStr.split(",").forEach { pair ->
            val parts = pair.split(":")
            if (parts.size == 2) {
                val missionId = parts[0].toIntOrNull() ?: return@forEach
                val progress = parts[1].toIntOrNull() ?: 0
                map[missionId] = progress
            }
        }
        return map
    }

    fun getUnlockedShips(): List<String> =
        if (unlockedShipsStr.isEmpty()) emptyList() else unlockedShipsStr.split(",")

    fun getUnlockedWorlds(): List<Int> =
        if (unlockedWorldsStr.isEmpty()) emptyList() else unlockedWorldsStr.split(",").mapNotNull { it.toIntOrNull() }

    fun getUnlockedAchievements(): List<String> =
        if (achievementsUnlockedStr.isEmpty()) emptyList() else achievementsUnlockedStr.split(",")

    fun getUnlockedTrails(): List<String> =
        if (unlockedTrailsStr.isEmpty()) emptyList() else unlockedTrailsStr.split(",")

    fun getUnlockedExplosions(): List<String> =
        if (unlockedExplosionsStr.isEmpty()) emptyList() else unlockedExplosionsStr.split(",")
}
