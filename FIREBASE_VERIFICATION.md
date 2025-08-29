# 🔥 Firebase Setup Verification & Troubleshooting

## ✅ **Current Status**
- ✅ `google-services.json` file is properly configured
- ✅ Web client ID is being generated: `476900064653-ba9ufup43it6r3vhc8k7qofs2o42v207.apps.googleusercontent.com`
- ✅ Project ID: `logmoney-bb14e`

## 🚨 **Required Actions in Firebase Console**

### 1. **Enable Authentication**
1. Go to [Firebase Console](https://console.firebase.google.com/project/logmoney-bb14e)
2. Click on "Authentication" in the left sidebar
3. Click "Get started" if not already enabled
4. Go to "Sign-in method" tab
5. **Enable Google provider:**
   - Click on "Google"
   - Toggle "Enable"
   - Add support email: `your-email@gmail.com`
   - Click "Save"

### 2. **Enable Firestore Database**
1. In Firebase Console, click "Firestore Database"
2. Click "Create database"
3. **Choose "Start in test mode"** (for development)
4. Select a location (choose closest to you)
5. Click "Done"

### 3. **Configure Firestore Security Rules**
1. Go to Firestore → Rules tab
2. Replace the default rules with:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // User-specific data isolation with E2E encryption
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```
3. Click "Publish"

### 4. **Verify SHA-1 Fingerprint**
Your current SHA-1: `7004e619352272441af500762f92824797570914`

To verify this matches your debug keystore:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
```

If the SHA-1 doesn't match, you need to:
1. Go to Project Settings → Your Android App
2. Add the correct SHA-1 fingerprint
3. Download new `google-services.json`
4. Replace the current file

## 🔧 **Common Error Solutions**

### Error: "Sign in failed 10"
- **Cause:** Firebase Authentication not enabled or SHA-1 mismatch
- **Fix:** Enable Google Sign-In in Firebase Console + verify SHA-1

### Error: "Default app has not been initialized"
- **Cause:** `google-services.json` not processed correctly
- **Fix:** Clean build → `./gradlew clean build`

### Error: "FirebaseFirestore component is not present"
- **Cause:** Firestore not enabled in Firebase project
- **Fix:** Enable Firestore Database in Firebase Console

### Error: "Permission denied"
- **Cause:** Firestore security rules too restrictive
- **Fix:** Update security rules as shown above

## 🧪 **Quick Test Steps**

1. **Clean Build:**
   ```bash
   ./gradlew clean build
   ```

2. **Run the app and test:**
   - Should see sign-in screen
   - Tap "Continue with Google"
   - Complete Google authentication
   - Should navigate to dashboard

3. **Verify in Firebase Console:**
   - Go to Authentication → Users (should see your account)
   - Go to Firestore → Data (should see user data after adding transactions)

## 📱 **Manual Verification Commands**

Check if your project is properly configured:
```bash
# Verify google-services.json is valid
cat app/google-services.json | grep "project_id"

# Check generated resources
cat app/build/generated/res/processDebugGoogleServices/values/values.xml | grep default_web_client_id
```

## 🆘 **Still Having Issues?**

If you're still getting errors:

1. **Copy your exact error message** and share it
2. **Check Firebase Console logs:**
   - Go to Analytics → DebugView (if Analytics enabled)
   - Check for any error messages

3. **Enable verbose logging** in your app for debugging:
   ```kotlin
   FirebaseApp.setLogLevel(Log.VERBOSE)
   ```

---

**Next Steps:** Complete the Firebase Console setup above, then test the app again!
