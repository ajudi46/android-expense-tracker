package com.expensetracker.data.dao

import androidx.room.*
import com.expensetracker.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isSignedIn = 1 LIMIT 1")
    fun getCurrentUser(): Flow<User?>

    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getUserById(uid: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("UPDATE users SET isSignedIn = 0")
    suspend fun signOutAllUsers()

    @Query("UPDATE users SET lastSyncTimestamp = :timestamp WHERE uid = :uid")
    suspend fun updateLastSyncTimestamp(uid: String, timestamp: Long)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}
