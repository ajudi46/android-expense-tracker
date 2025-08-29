# 🔍 Firestore Debug Checklist

## Issue: Backup/Restore/Delete operations not working

### Step 1: Test Firestore Connection
1. **Install the updated app**
2. **Sign in** to your account  
3. **Go to Profile screen**
4. **Tap "Test Firestore Connection"**
5. **Check the error message** - it will show either:
   - ✅ "Firestore connection successful! User: your-email@gmail.com"
   - ❌ "Firestore test failed: [specific error message]"

### Step 2: Check Android Studio Logs
Open Android Studio → Logcat → Filter by "CloudSync" and "ExpenseTracker"

**Look for these log messages:**
```
CloudSync: Testing Firestore connection for user: [user-id]
CloudSync: User email: your-email@gmail.com
CloudSync: Writing test data to: users/[user-id]/test
CloudSync: Test write successful!
CloudSync: Test read successful!
```

**If you see errors like:**
- `PERMISSION_DENIED` → Firestore rules issue
- `UNAUTHENTICATED` → Authentication issue  
- `UNAVAILABLE` → Network/connectivity issue

### Step 3: Check Firestore Security Rules
1. **Go to Firebase Console**: https://console.firebase.google.com/
2. **Select your project**: `logmoney-bb14e`
3. **Navigate to**: Firestore Database → Rules
4. **Current rules should allow authenticated users:**

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow authenticated users to read/write their own data
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### Step 4: Check Firebase Authentication
1. **Firebase Console** → Authentication → Users
2. **Verify your user account exists and is active**
3. **Check if email is verified** (may be required)

### Step 5: Check Network Connectivity
1. **Ensure device has internet**
2. **Try both WiFi and mobile data**
3. **Check if Firebase services are blocked by firewall**

### Step 6: Check SHA-1 Fingerprints (for release builds)
1. **Firebase Console** → Project Settings → Your apps
2. **Verify SHA-1 fingerprints are added for both debug and release**
3. **Debug SHA-1**: `DA:39:A3:EE:5E:6B:4B:0D:32:55:BF:EF:95:60:18:90:AF:D8:07:09`
4. **Release SHA-1**: `0E:B6:6A:87:EB:D5:05:38:B8:03:62:35:C6:DA:EE:86:F9:60:0B:B2`

### Common Issues & Solutions:

#### ❌ "PERMISSION_DENIED"
**Cause**: Firestore security rules don't allow the operation
**Solution**: Update Firestore rules to allow authenticated users

#### ❌ "UNAUTHENTICATED" 
**Cause**: User is not properly authenticated
**Solution**: 
- Sign out and sign in again
- Check if Google Sign-In is working
- Verify SHA-1 fingerprints

#### ❌ "UNAVAILABLE"
**Cause**: Network or Firebase service issues
**Solution**:
- Check internet connection
- Try different network (WiFi/mobile)
- Wait and retry (temporary Firebase issues)

#### ❌ "INVALID_ARGUMENT"
**Cause**: Data format or path issues
**Solution**: Check app logs for specific field errors

### Step 7: Manual Firestore Test
1. **Firebase Console** → Firestore Database
2. **Try creating a test document manually**:
   - Collection: `users`
   - Document ID: `test`
   - Field: `test = "manual"`
3. **If this fails** → Firebase project configuration issue
4. **If this works** → App configuration issue

### Step 8: Re-download google-services.json
1. **Firebase Console** → Project Settings → Your apps
2. **Download latest google-services.json**
3. **Replace** `app/google-services.json` with new file
4. **Clean and rebuild** the app

---

## 🔧 Quick Fix Commands:

```bash
# Clean and rebuild
./gradlew clean build

# Check SHA-1 fingerprints
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

## 📞 If Still Having Issues:

Run the **"Test Firestore Connection"** button and share:
1. The exact error message from the app
2. Android Studio logs (CloudSync and ExpenseTracker filters)
3. Your Firebase project URL: https://console.firebase.google.com/project/logmoney-bb14e
