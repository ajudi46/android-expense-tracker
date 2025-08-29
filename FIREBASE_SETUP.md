# Firebase Setup Instructions

## 🔥 **Firebase Configuration Required**

To enable Google Sign-In and cloud backup features, you need to set up Firebase for this app.

## ⚙️ **Step-by-Step Setup**

### 1. **Create Firebase Project**
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Create a project" or "Add project"
3. Enter project name: `expense-tracker` (or your preferred name)
4. Enable Google Analytics (optional)
5. Create the project

### 2. **Add Android App**
1. In your Firebase project, click "Add app" → Android icon
2. Enter the package name: `com.expensetracker`
3. Enter app nickname: `Expense Tracker`
4. Get SHA-1 fingerprint:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
5. Copy the SHA-1 and paste it in Firebase
6. Download `google-services.json`

### 3. **Replace Configuration File**
1. Replace the dummy `app/google-services.json` with your downloaded file
2. Make sure the file is in the `app/` directory

### 4. **Enable Authentication**
1. In Firebase Console, go to "Authentication"
2. Click "Get started"
3. Go to "Sign-in method" tab
4. Enable "Google" provider
5. Add your support email
6. Save

### 5. **Setup Firestore Database**
1. In Firebase Console, go to "Firestore Database"
2. Click "Create database"
3. Choose "Start in test mode" (for development)
4. Select location closest to your users
5. Create database

### 6. **Configure Security Rules**
1. Go to Firestore → Rules
2. Replace the rules with:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // User-specific data isolation
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```
3. Publish the rules

### 7. **Update Web Client ID**
1. In Firebase Console, go to Project Settings → General
2. Scroll down to "Your apps" section
3. Click on your Android app
4. Go to "SDK setup and configuration"
5. Copy the "Web client ID"
6. Replace the client ID in `AuthenticationRepository.kt`:
   ```kotlin
   .requestIdToken("YOUR_ACTUAL_WEB_CLIENT_ID_HERE")
   ```

## 🔐 **Security Features**

### **What's Protected:**
- ✅ All transaction data encrypted end-to-end
- ✅ User data isolated (users can only access their own data)
- ✅ Firebase can't read your financial data
- ✅ Secure key management using Android Keystore

### **Data Structure in Firestore:**
```
users/
  {userId}/
    accounts/
      {accountId}: { encryptedData: "..." }
    transactions/
      {transactionId}: { encryptedData: "...", createdAt: timestamp }
    categories/
      {categoryId}: { encryptedData: "..." }
    budgets/
      {budgetId}: { encryptedData: "...", createdAt: timestamp }
```

## 🧪 **Testing the Setup**

1. Build and run the app
2. You should see the sign-in screen
3. Tap "Continue with Google"
4. Complete Google sign-in
5. Create a test transaction
6. Check Firestore Console - you should see encrypted data under `users/{your-uid}/transactions/`

## 🚨 **Important Notes**

- **Never commit** your real `google-services.json` to version control
- **Always use** the test/debug keystore SHA-1 for development
- **Set up production** keystore SHA-1 for release builds
- **Enable App Check** in production for additional security
- **Monitor usage** in Firebase Console to stay within free tier limits

## 📱 **App Features After Setup**

Once Firebase is configured, users get:
- ✅ **Secure Google Sign-In**
- ✅ **Automatic cloud backup** of all transactions
- ✅ **Cross-device sync** - same data on all devices
- ✅ **End-to-end encryption** - Google can't read financial data
- ✅ **Offline support** - works without internet, syncs when online
- ✅ **Data recovery** - never lose data even if device is lost

---

**Need Help?** Check the [Firebase Documentation](https://firebase.google.com/docs/android/setup) for detailed setup instructions.
