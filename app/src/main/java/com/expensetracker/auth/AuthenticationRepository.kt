package com.expensetracker.auth

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.expensetracker.data.dao.UserDao
import com.expensetracker.data.model.User
import com.expensetracker.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticationRepository @Inject constructor(
    private val context: Context,
    private val userDao: UserDao,
    private val firebaseAuth: FirebaseAuth
) {
    
    private val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("123456789012-abcdefghijklmnopqrstuvwxyz123456.apps.googleusercontent.com") // Replace with your actual Web Client ID
        .requestEmail()
        .build()
    
    private val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions)
    
    val currentUser: Flow<UserProfile?> = userDao.getCurrentUser().map { user ->
        user?.let {
            UserProfile(
                uid = it.uid,
                email = it.email,
                displayName = it.displayName,
                photoUrl = it.photoUrl
            )
        }
    }
    
    val isSignedIn: Flow<Boolean> = userDao.getCurrentUser().map { it?.isSignedIn == true }
    
    fun getGoogleSignInClient(): GoogleSignInClient = googleSignInClient
    
    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<UserProfile> {
        return try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("Firebase user is null")
            
            val user = createUserFromFirebaseUser(firebaseUser)
            userDao.signOutAllUsers() // Sign out any existing users
            userDao.insertUser(user.copy(isSignedIn = true))
            
            Result.success(UserProfile(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName,
                photoUrl = firebaseUser.photoUrl?.toString()
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signOut(): Result<Unit> {
        return try {
            // Sign out from Firebase
            firebaseAuth.signOut()
            
            // Sign out from Google
            googleSignInClient.signOut().await()
            
            // Update local database
            userDao.signOutAllUsers()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val currentFirebaseUser = firebaseAuth.currentUser
            currentFirebaseUser?.delete()?.await()
            
            // Clean up local data
            userDao.deleteAllUsers()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getCurrentFirebaseUser(): FirebaseUser? = firebaseAuth.currentUser
    
    private fun createUserFromFirebaseUser(firebaseUser: FirebaseUser): User {
        return User(
            uid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName,
            photoUrl = firebaseUser.photoUrl?.toString(),
            isSignedIn = true,
            lastSyncTimestamp = System.currentTimeMillis()
        )
    }
    
    suspend fun updateLastSyncTimestamp(uid: String, timestamp: Long) {
        userDao.updateLastSyncTimestamp(uid, timestamp)
    }
}
