package com.example.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class UserProgressRepository(private val dao: UserProgressDao) {
    val progressFlow: Flow<UserProgress?> = dao.getProgressFlow()

    // FIX: every "read row -> modify -> write row" sequence in this app now
    // goes through this single Mutex. Previously, completeRun() (saving your
    // high score) and several independent mission/achievement saves could
    // run concurrently, each reading a slightly different snapshot of the
    // row and writing it back — whichever finished LAST won, even if it was
    // overwriting a newer high score with a stale one. This is exactly why
    // the recorded score sometimes silently reverted to 0 / didn't stick
    // until a later run "happened" to land last. The Mutex makes every
    // read+write atomic relative to all the others.
    private val mutex = Mutex()

    suspend fun getProgressDirect(): UserProgress = withContext(Dispatchers.IO) {
        mutex.withLock { getOrCreateLocked() }
    }

    // Generic helper: read the current row, apply `transform`, save the
    // result — all under the same lock used everywhere else in this file.
    suspend fun updateAtomic(transform: (UserProgress) -> UserProgress): UserProgress =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val current = getOrCreateLocked()
                val updated = transform(current)
                dao.saveProgress(updated)
                updated
            }
        }

    private suspend fun getOrCreateLocked(): UserProgress {
        val existing = dao.getProgressDirect()
        if (existing != null) return existing
        val fresh = UserProgress()
        dao.saveProgress(fresh)
        return fresh
    }

    suspend fun ensureInitialized() = withContext(Dispatchers.IO) {
        mutex.withLock { getOrCreateLocked() }
    }

    suspend fun updateProgress(progress: UserProgress) = withContext(Dispatchers.IO) {
        mutex.withLock { dao.saveProgress(progress) }
    }

    suspend fun addCrystals(amount: Int) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = getOrCreateLocked()
            dao.saveProgress(current.copy(crystals = current.crystals + amount))
        }
    }

    suspend fun unlockShip(shipId: String, cost: Int): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = getOrCreateLocked()
            if (current.crystals >= cost) {
                val unlocked = current.getUnlockedShips().toMutableList()
                if (!unlocked.contains(shipId)) unlocked.add(shipId)
                dao.saveProgress(
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
    }

    suspend fun selectShip(shipId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = getOrCreateLocked()
            if (current.getUnlockedShips().contains(shipId)) {
                dao.saveProgress(current.copy(selectedShip = shipId))
            }
        }
    }

    suspend fun registerHighScore(worldId: Int, score: Int) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = getOrCreateLocked()
            val scores = current.getHighScores().toMutableMap()
            val currentHigh = scores[worldId] ?: 0
            if (score > currentHigh) {
                scores[worldId] = score
                dao.saveProgress(current.copy(highScoresStr = UserProgress.buildHighScoresStr(scores)))
            }
        }
    }

    suspend fun unlockWorld(worldId: Int) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = getOrCreateLocked()
            val unlocked = current.getUnlockedWorlds().toMutableList()
            if (!unlocked.contains(worldId)) {
                unlocked.add(worldId)
                dao.saveProgress(current.copy(unlockedWorldsStr = unlocked.joinToString(",")))
            }
        }
    }

    // FIX: this is THE critical one — saving the run result (including the
    // high score) is now fully atomic. No other save can interleave between
    // "read current high score" and "write new high score" anymore.
    suspend fun completeRun(worldId: Int, score: Int, crystalsEarned: Int, rawEnemies: Int, bosssKilled: Int) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val current = getOrCreateLocked()

                val scores = current.getHighScores().toMutableMap()
                val oldHigh = scores[worldId] ?: 0
                if (score > oldHigh) {
                    scores[worldId] = score
                }

                val newRuns = current.totalRuns + 1
                val newEnemies = current.totalEnemiesKilled + rawEnemies
                val newBosses = current.totalBossesKilled + bosssKilled
                val newCrystals = current.crystals + crystalsEarned

                dao.saveProgress(
                    current.copy(
                        crystals = newCrystals,
                        highScoresStr = UserProgress.buildHighScoresStr(scores),
                        totalRuns = newRuns,
                        totalEnemiesKilled = newEnemies,
                        totalBossesKilled = newBosses
                    )
                )
            }
        }

    suspend fun updateSettings(sound: Boolean, music: Boolean, vibration: Boolean) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = getOrCreateLocked()
            dao.saveProgress(current.copy(soundEnabled = sound, musicEnabled = music, vibrationEnabled = vibration))
        }
    }

    suspend fun resetAllData() = withContext(Dispatchers.IO) {
        mutex.withLock { dao.saveProgress(UserProgress()) }
    }
}
