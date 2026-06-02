package com.example.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class UserProgressRepository(private val dao: UserProgressDao) {
    val progressFlow: Flow<UserProgress?> = dao.getProgressFlow()

    suspend fun getProgressDirect(): UserProgress = withContext(Dispatchers.IO) {
        dao.getProgressDirect() ?: UserProgress()
    }

    suspend fun updateProgress(progress: UserProgress) = withContext(Dispatchers.IO) {
        dao.saveProgress(progress)
    }

    suspend fun addCrystals(amount: Int) = withContext(Dispatchers.IO) {
        val current = getProgressDirect()
        updateProgress(current.copy(crystals = current.crystals + amount))
    }

    suspend fun unlockShip(shipId: String, cost: Int) = withContext(Dispatchers.IO) {
        val current = getProgressDirect()
        if (current.crystals >= cost) {
            val unlocked = current.getUnlockedShips().toMutableList()
            if (!unlocked.contains(shipId)) {
                unlocked.add(shipId)
            }
            updateProgress(
                current.copy(
                    crystals = current.crystals - cost,
                    unlockedShipsStr = unlocked.joinToString(",")
                )
            )
            true
        } else {
            false
        }
    }

    suspend fun selectShip(shipId: String) = withContext(Dispatchers.IO) {
        val current = getProgressDirect()
        if (current.getUnlockedShips().contains(shipId)) {
            updateProgress(current.copy(selectedShip = shipId))
        }
    }

    suspend fun registerHighScore(worldId: Int, score: Int) = withContext(Dispatchers.IO) {
        val current = getProgressDirect()
        val scores = current.getHighScores().toMutableMap()
        val currentHigh = scores[worldId] ?: 0
        if (score > currentHigh) {
            scores[worldId] = score
            updateProgress(current.copy(highScoresStr = UserProgress.buildHighScoresStr(scores)))
        }
    }

    suspend fun unlockWorld(worldId: Int) = withContext(Dispatchers.IO) {
        val current = getProgressDirect()
        val unlocked = current.getUnlockedWorlds().toMutableList()
        if (!unlocked.contains(worldId)) {
            unlocked.add(worldId)
            updateProgress(current.copy(unlockedWorldsStr = unlocked.joinToString(",")))
        }
    }

    suspend fun completeRun(worldId: Int, score: Int, crystalsEarned: Int, rawEnemies: Int, bosssKilled: Int) = withContext(Dispatchers.IO) {
        val current = getProgressDirect()
        
        // Update high scores
        val scores = current.getHighScores().toMutableMap()
        val oldHigh = scores[worldId] ?: 0
        if (score > oldHigh) {
            scores[worldId] = score
        }
        
        // Increment other statistics
        val newRuns = current.totalRuns + 1
        val newEnemies = current.totalEnemiesKilled + rawEnemies
        val newBosses = current.totalBossesKilled + bosssKilled
        val newCrystals = current.crystals + crystalsEarned
        
        updateProgress(
            current.copy(
                crystals = newCrystals,
                highScoresStr = UserProgress.buildHighScoresStr(scores),
                totalRuns = newRuns,
                totalEnemiesKilled = newEnemies,
                totalBossesKilled = newBosses
            )
        )
    }

    suspend fun updateSettings(sound: Boolean, music: Boolean, vibration: Boolean) = withContext(Dispatchers.IO) {
        val current = getProgressDirect()
        updateProgress(current.copy(
            soundEnabled = sound,
            musicEnabled = music,
            vibrationEnabled = vibration
        ))
    }

    suspend fun resetAllData() = withContext(Dispatchers.IO) {
        updateProgress(UserProgress()) // Saves default blank progress
    }
}
