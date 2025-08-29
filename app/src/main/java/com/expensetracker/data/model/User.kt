package com.expensetracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val uid: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val isSignedIn: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val encryptionKey: String? = null // For E2E encryption
)

data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?
)
