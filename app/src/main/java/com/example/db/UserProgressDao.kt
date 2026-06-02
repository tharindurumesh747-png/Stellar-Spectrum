package com.example.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {
    @Query("SELECT * FROM user_progress WHERE id = 1")
    fun getProgressFlow(): Flow<UserProgress?>

    @Query("SELECT * FROM user_progress WHERE id = 1")
    suspend fun getProgressDirect(): UserProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: UserProgress)
}
