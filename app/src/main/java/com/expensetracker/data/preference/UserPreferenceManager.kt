package com.expensetracker.data.preference

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    private val _hasSeenLogin = MutableStateFlow(getHasSeenLogin())
    val hasSeenLogin: Flow<Boolean> = _hasSeenLogin.asStateFlow()
    
    private val _hasSkippedLogin = MutableStateFlow(getHasSkippedLogin())
    val hasSkippedLogin: Flow<Boolean> = _hasSkippedLogin.asStateFlow()
    
    fun setHasSeenLogin(seen: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_HAS_SEEN_LOGIN, seen)
            .apply()
        _hasSeenLogin.value = seen
    }
    
    fun setHasSkippedLogin(skipped: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_HAS_SKIPPED_LOGIN, skipped)
            .apply()
        _hasSkippedLogin.value = skipped
    }
    
    fun clearAllPreferences() {
        sharedPreferences.edit().clear().apply()
        _hasSeenLogin.value = false
        _hasSkippedLogin.value = false
    }
    
    private fun getHasSeenLogin(): Boolean {
        return sharedPreferences.getBoolean(KEY_HAS_SEEN_LOGIN, false)
    }
    
    private fun getHasSkippedLogin(): Boolean {
        return sharedPreferences.getBoolean(KEY_HAS_SKIPPED_LOGIN, false)
    }
    
    companion object {
        private const val PREF_NAME = "expense_tracker_prefs"
        private const val KEY_HAS_SEEN_LOGIN = "has_seen_login"
        private const val KEY_HAS_SKIPPED_LOGIN = "has_skipped_login"
    }
}
