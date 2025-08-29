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
        val keyString = encryptedPrefs.getString("user_encryption_key", null)
        
        return if (keyString != null) {
            val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
            SecretKeySpec(keyBytes, "AES")
        } else {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            val secretKey = keyGenerator.generateKey()
            
            val keyString = Base64.encodeToString(secretKey.encoded, Base64.DEFAULT)
            encryptedPrefs.edit().putString("user_encryption_key", keyString).apply()
            
            secretKey
        }
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
        return gson.fromJson(decryptedString, Account::class.java)
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
    }
}
