package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val id: Int = 1,
    val crystals: Int = 0,
    val selectedShip: String = "solar_wing",
    val unlockedShipsStr: String = "solar_wing", // Comma-separated list of unlocked ships
    val unlockedWorldsStr: String = "1", // Comma-separated list of unlocked world indices, e.g. "1"
    val highScoresStr: String = "1:0,2:0,3:0,4:0,5:0,6:0", // Comma-separated level_index:score
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val lastDailyReward: String = "", // Date string like "2026-06-02"
    val dailyRewardDay: Int = 0,
    val totalRuns: Int = 0,
    val totalEnemiesKilled: Int = 0,
    val totalBossesKilled: Int = 0,
    val missionsProgressStr: String = "1:0,2:0,3:0,4:0,5:0", // mission_id:progress
    val achievementsUnlockedStr: String = "" // Comma-separated list of unlocked achievement IDs
) {
    // Helper to get high scores as a Map
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

    // Helper to create high scores string from Map
    companion object {
        fun buildHighScoresStr(scores: Map<Int, Int>): String {
            return scores.entries.joinToString(",") { "${it.key}:${it.value}" }
        }

        fun buildMissionsProgressStr(progress: Map<Int, Int>): String {
            return progress.entries.joinToString(",") { "${it.key}:${it.value}" }
        }
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

    fun getUnlockedShips(): List<String> {
        return if (unlockedShipsStr.isEmpty()) emptyList() else unlockedShipsStr.split(",")
    }

    fun getUnlockedWorlds(): List<Int> {
        if (unlockedWorldsStr.isEmpty()) return emptyList()
        return unlockedWorldsStr.split(",").mapNotNull { it.toIntOrNull() }
    }

    fun getUnlockedAchievements(): List<String> {
        return if (achievementsUnlockedStr.isEmpty()) emptyList() else achievementsUnlockedStr.split(",")
    }
}
