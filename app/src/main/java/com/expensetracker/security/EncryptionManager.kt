package com.expensetracker.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.expensetracker.data.model.*
import com.google.gson.Gson
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import java.security.MessageDigest
import com.google.firebase.auth.FirebaseAuth

@Singleton
class EncryptionManager @Inject constructor(
    private val context: Context
) {
    private val gson = Gson()
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "encrypted_expense_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private fun getOrCreateEncryptionKey(): SecretKey {
        // Try to get existing key from local storage first
        val localKeyString = encryptedPrefs.getString("user_encryption_key", null)
        
        // Get current Firebase user
        val currentUser = FirebaseAuth.getInstance().currentUser
        
        return if (currentUser != null) {
            // Use Firebase UID to derive consistent encryption key across devices
            val userBasedKey = deriveKeyFromUserId(currentUser.uid)
            
            // Store the key locally for offline access
            val keyString = Base64.encodeToString(userBasedKey.encoded, Base64.DEFAULT)
            encryptedPrefs.edit().putString("user_encryption_key", keyString).apply()
            
            android.util.Log.d("EncryptionManager", "Using user-derived encryption key for UID: ${currentUser.uid}")
            userBasedKey
        } else if (localKeyString != null) {
            // Fallback to local key if user not signed in
            android.util.Log.d("EncryptionManager", "Using local encryption key (user not signed in)")
            val keyBytes = Base64.decode(localKeyString, Base64.DEFAULT)
            SecretKeySpec(keyBytes, "AES")
        } else {
            // Last resort: generate random key
            android.util.Log.w("EncryptionManager", "Generating random encryption key (no user, no local key)")
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            val secretKey = keyGenerator.generateKey()
            
            val keyString = Base64.encodeToString(secretKey.encoded, Base64.DEFAULT)
            encryptedPrefs.edit().putString("user_encryption_key", keyString).apply()
            
            secretKey
        }
    }
    
    private fun deriveKeyFromUserId(userId: String): SecretKey {
        // Derive a consistent 256-bit key from the user ID
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest("ExpenseTracker_$userId".toByteArray(Charsets.UTF_8))
        return SecretKeySpec(hash, "AES")
    }
    
    private fun encrypt(data: String): String {
        val secretKey = getOrCreateEncryptionKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val encryptedData = cipher.doFinal(data.toByteArray())
        val iv = cipher.iv
        
        val combined = iv + encryptedData
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }
    
    private fun decrypt(encryptedData: String): String {
        val secretKey = getOrCreateEncryptionKey()
        val combined = Base64.decode(encryptedData, Base64.DEFAULT)
        
        val iv = combined.sliceArray(0..11) // GCM IV is 12 bytes
        val cipherText = combined.sliceArray(12 until combined.size)
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, javax.crypto.spec.GCMParameterSpec(128, iv))
        
        val decryptedData = cipher.doFinal(cipherText)
        return String(decryptedData)
    }
    
    // Account encryption/decryption
    fun encryptAccount(account: Account): Map<String, Any> {
        val jsonString = gson.toJson(account)
        android.util.Log.d("EncryptionManager", "Encrypting account: ${account.name}, balance: ${account.balance}")
        android.util.Log.d("EncryptionManager", "Account JSON: $jsonString")
        val encryptedString = encrypt(jsonString)
        
        return mapOf(
            "id" to account.id,
            "encryptedData" to encryptedString,
            "timestamp" to System.currentTimeMillis()
        )
    }
    
    fun decryptAccount(data: Map<String, Any>): Account {
        val encryptedData = data["encryptedData"] as String
        val decryptedString = decrypt(encryptedData)
        android.util.Log.d("EncryptionManager", "Decrypted account JSON: $decryptedString")
        val account = gson.fromJson(decryptedString, Account::class.java)
        android.util.Log.d("EncryptionManager", "Decrypted account: ${account.name}, balance: ${account.balance}")
        return account
    }
    
    // Transaction encryption/decryption
    fun encryptTransaction(transaction: Transaction): Map<String, Any> {
        val jsonString = gson.toJson(transaction)
        val encryptedString = encrypt(jsonString)
        
        return mapOf(
            "id" to transaction.id,
            "encryptedData" to encryptedString,
            "createdAt" to transaction.createdAt, // Keep timestamp unencrypted for querying
            "timestamp" to System.currentTimeMillis()
        )
    }
    
    fun decryptTransaction(data: Map<String, Any>): Transaction {
        val encryptedData = data["encryptedData"] as String
        val decryptedString = decrypt(encryptedData)
        return gson.fromJson(decryptedString, Transaction::class.java)
    }
    
    // Category encryption/decryption
    fun encryptCategory(category: Category): Map<String, Any> {
        val jsonString = gson.toJson(category)
        val encryptedString = encrypt(jsonString)
        
        return mapOf(
            "id" to category.id,
            "encryptedData" to encryptedString,
            "timestamp" to System.currentTimeMillis()
        )
    }
    
    fun decryptCategory(data: Map<String, Any>): Category {
        val encryptedData = data["encryptedData"] as String
        val decryptedString = decrypt(encryptedData)
        return gson.fromJson(decryptedString, Category::class.java)
    }
    
    // Budget encryption/decryption
    fun encryptBudget(budget: Budget): Map<String, Any> {
        val jsonString = gson.toJson(budget)
        val encryptedString = encrypt(jsonString)
        
        return mapOf(
            "id" to budget.id,
            "encryptedData" to encryptedString,
            "createdAt" to budget.createdAt, // Keep timestamp unencrypted for querying
            "timestamp" to System.currentTimeMillis()
        )
    }
    
    fun decryptBudget(data: Map<String, Any>): Budget {
        val encryptedData = data["encryptedData"] as String
        val decryptedString = decrypt(encryptedData)
        return gson.fromJson(decryptedString, Budget::class.java)
    }
    
    fun clearEncryptionKey() {
        encryptedPrefs.edit().remove("user_encryption_key").apply()
        android.util.Log.d("EncryptionManager", "Encryption key cleared")
    }
    
    fun refreshEncryptionKey() {
        // Clear existing key and regenerate based on current user
        clearEncryptionKey()
        getOrCreateEncryptionKey() // This will regenerate the key
        android.util.Log.d("EncryptionManager", "Encryption key refreshed")
    }
}
