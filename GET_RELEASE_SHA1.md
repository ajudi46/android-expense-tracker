# 🔑 Getting Release Keystore SHA-1 for Firebase

## 🚨 **Critical for Signed APK Authentication**

If your signed APK authentication is failing while debug builds work, you need to add the **release keystore SHA-1** to Firebase Console.

---

## 📋 **Step-by-Step Instructions**

### **Step 1: Extract SHA-1 from Release Keystore**

You have a `keystore.jks` file in your project. Run these commands to get the SHA-1:

```bash
# Try common alias names
keytool -list -v -keystore keystore.jks -alias key0
keytool -list -v -keystore keystore.jks -alias androidreleasekey  
keytool -list -v -keystore keystore.jks -alias release
keytool -list -v -keystore keystore.jks -alias upload
```

**If you don't know the alias name:**
```bash
# List all aliases in the keystore
keytool -list -keystore keystore.jks
```

### **Step 2: Find Your SHA-1**

Look for a line like this in the output:
```
SHA1: 12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78
```

Copy the entire SHA-1 fingerprint (including the colons).

### **Step 3: Add to Firebase Console**

1. **Go to [Firebase Console](https://console.firebase.google.com/project/logmoney-bb14e/settings/general)**
2. **Click on your Android app** (`com.expensetracker`)
3. **Click "Add fingerprint"**
4. **Paste your release SHA-1**
5. **Click "Save"**

### **Step 4: Download New Configuration**

1. **Download the updated `google-services.json`**
2. **Replace `app/google-services.json` with the new file**

### **Step 5: Clean Build**

```bash
./gradlew clean build
```

---

## 🔄 **Complete Firebase Setup for Both Builds**

For a complete setup, add **BOTH** SHA-1 fingerprints:

### **Debug SHA-1** (for development):
```
70:04:E6:19:35:22:72:44:1A:F5:00:76:2F:92:82:47:97:57:09:14
```

### **Release SHA-1** (get from your keystore):
```
[Your release keystore SHA-1 - get using commands above]
```

---

## 🛠️ **Quick Commands**

### **For macOS/Linux:**
```bash
# Get release SHA-1 (try each alias)
keytool -list -v -keystore keystore.jks -alias key0 | grep SHA1
keytool -list -v -keystore keystore.jks -alias androidreleasekey | grep SHA1
keytool -list -v -keystore keystore.jks -alias release | grep SHA1

# Get debug SHA-1 (for comparison)
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
```

### **For Windows:**
```cmd
keytool -list -v -keystore keystore.jks -alias key0 | findstr SHA1
keytool -list -v -keystore keystore.jks -alias androidreleasekey | findstr SHA1
keytool -list -v -keystore keystore.jks -alias release | findstr SHA1
```

---

## 🔍 **Troubleshooting**

### **"Keystore was tampered with" Error:**
- You entered the wrong password
- Try different passwords you might have used

### **"Alias does not exist" Error:**
- Run: `keytool -list -keystore keystore.jks` to see all aliases
- Use the correct alias name from the list

### **Still Getting Authentication Errors:**
1. **Verify both SHA-1s are in Firebase Console**
2. **Clear app data** on device: Settings > Apps > Expense Tracker > Storage > Clear Data
3. **Uninstall and reinstall** the signed APK
4. **Check internet connection** and try different network

---

## 📱 **Testing Process**

### **After Adding Release SHA-1:**

1. **Build signed APK:**
   ```bash
   ./gradlew assembleRelease
   ```

2. **Install on device:**
   ```bash
   adb install app/build/outputs/apk/release/app-release.apk
   ```

3. **Test authentication:**
   - Open app
   - Go to Profile
   - Tap "Sign In"
   - Should work without errors

---

## 🚨 **Important Notes**

- **Debug builds** use debug keystore SHA-1
- **Release builds** use release keystore SHA-1  
- **Both SHA-1s** can be added to the same Firebase project
- **Always rebuild** after updating `google-services.json`
- **Clear app data** if authentication still fails after adding SHA-1

---

**Remember:** The most common cause of signed APK authentication failure is missing the release keystore SHA-1 in Firebase Console!
