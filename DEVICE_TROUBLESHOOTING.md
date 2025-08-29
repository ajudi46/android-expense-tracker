# 📱 Device-Specific Firebase Authentication Troubleshooting

## 🚨 **Common Issues & Solutions**

### **Error 7: NETWORK_ERROR**
**Symptoms:** Sign in fails with "Network Error" message
**Devices Affected:** Physical devices, especially on mobile networks

**Solutions:**

#### **1. Network Connection Issues**
```bash
# Test internet connectivity
ping google.com
```
- **Switch from mobile data to WiFi** or vice versa
- **Disable VPN** if active during sign-in
- **Check if corporate/school network blocks Google services**
- **Try different WiFi network** (public networks sometimes block OAuth)

#### **2. Google Play Services Issues**
- **Update Google Play Services:**
  1. Open Google Play Store
  2. Search "Google Play services"
  3. Update if available
  4. Restart device

- **Clear Google Play Services Cache:**
  1. Settings → Apps → Google Play services
  2. Storage → Clear Cache
  3. Restart app

#### **3. Device Time/Date Issues**
- **Sync device time automatically:**
  1. Settings → Date & time
  2. Enable "Automatic date & time"
  3. Enable "Automatic time zone"

#### **4. Firewall/Network Security**
- **Corporate/School Networks:** May block OAuth domains
- **Solution:** Use personal hotspot or different network

---

### **Error 10: DEVELOPER_ERROR (Configuration Error)**
**Symptoms:** "Configuration Error" or "Incorrect configuration"
**Devices Affected:** Physical devices with different SHA-1 fingerprints

**Solutions:**

#### **1. SHA-1 Fingerprint Mismatch**
This is the **most common cause** when working between emulator and physical devices.

**Check your device's current SHA-1:**
```bash
# For debug builds (development)
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1

# For release builds (if you have a release keystore)
keytool -list -v -keystore /path/to/your/release.keystore -alias your_alias -storepass your_store_password | grep SHA1
```

**Your current SHA-1 in Firebase:** `7004e619352272441af500762f92824797570914`

**If SHA-1 doesn't match:**
1. **Go to [Firebase Console](https://console.firebase.google.com/project/logmoney-bb14e/settings/general)**
2. **Click on your Android app**
3. **Add the correct SHA-1 fingerprint:**
   - Click "Add fingerprint"
   - Paste the SHA-1 from your device
   - Save
4. **Download new `google-services.json`**
5. **Replace the existing file in `app/google-services.json`**
6. **Clean and rebuild:**
   ```bash
   ./gradlew clean build
   ```

#### **2. Multiple SHA-1 Fingerprints (Recommended)**
Add SHA-1 fingerprints for all your development scenarios:

**Common fingerprints to add:**
- **Debug keystore SHA-1** (for development)
- **Release keystore SHA-1** (for production)
- **Android Studio emulator SHA-1** (if different)
- **Additional developer machines** (if team development)

#### **3. Google Services Plugin Issues**
```bash
# Clean build completely
./gradlew clean
rm -rf app/build/
./gradlew build
```

---

### **Working on Emulator but Not Physical Device**

**This is typically a SHA-1 fingerprint mismatch issue.**

**Quick Fix:**
1. **Get SHA-1 from your physical device development setup**
2. **Add it to Firebase Console** (don't remove existing ones)
3. **Download and replace `google-services.json`**
4. **Clean build and test**

---

## 🔧 **Debugging Steps**

### **Step 1: Verify Firebase Project Setup**
1. **Go to [Firebase Console](https://console.firebase.google.com/project/logmoney-bb14e)**
2. **Check Authentication is enabled:**
   - Authentication → Sign-in method
   - Google provider should be **enabled**
3. **Check your Android app configuration:**
   - Project Settings → Your apps
   - Verify package name: `com.expensetracker`
   - Check SHA-1 fingerprints

### **Step 2: Device-Specific Checks**

#### **On Physical Device:**
```bash
# Check Google Play Services version
adb shell dumpsys package com.google.android.gms | grep versionName

# Check if Google Play Services is running
adb shell dumpsys activity services | grep gms

# View detailed error logs
adb logcat | grep -E "(GoogleSignIn|Firebase|Auth)"
```

#### **On Emulator:**
- **Use Google Play version** of emulator (not Google APIs)
- **Sign in with Google account** in emulator settings
- **Update Google Play Services** in emulator

### **Step 3: App-Specific Debugging**

#### **Enable Verbose Logging** (Temporary for debugging)
Add this to your `MainActivity.onCreate()`:
```kotlin
// Add to imports
import android.util.Log
import com.google.firebase.FirebaseApp

// Add after setContent in MainActivity
if (BuildConfig.DEBUG) {
    Log.d("ExpenseTracker", "Firebase Debug Mode Enabled")
}
```

#### **Clear App Data**
```bash
# Clear all app data
adb shell pm clear com.expensetracker

# Or manually: Settings → Apps → Expense Tracker → Storage → Clear Data
```

---

## 📋 **Device Testing Checklist**

### **Before Testing on New Device:**
- [ ] Add device's SHA-1 to Firebase Console
- [ ] Download fresh `google-services.json`
- [ ] Clean build: `./gradlew clean build`
- [ ] Install fresh APK (not existing build)

### **When Switching Between Devices:**
- [ ] Verify Google Play Services is up to date
- [ ] Check internet connectivity
- [ ] Clear app data if issues persist
- [ ] Test on WiFi and mobile data separately

### **Network Environment Testing:**
- [ ] Home WiFi
- [ ] Mobile data (4G/5G)
- [ ] Public WiFi
- [ ] Corporate/School network (if applicable)

---

## 🆘 **Still Having Issues?**

### **Collect Debug Information:**
1. **Error code and exact message**
2. **Device model and Android version**
3. **Network type** (WiFi/Mobile data)
4. **Google Play Services version**
5. **Screenshots of error**

### **Advanced Debugging:**
```bash
# Comprehensive log collection
adb logcat -c  # Clear logs
# Reproduce the error
adb logcat > error_logs.txt  # Save logs to file

# Look for specific errors
grep -E "(ERROR|FATAL)" error_logs.txt
```

### **Last Resort Solutions:**
1. **Create new Firebase project** with fresh configuration
2. **Use different Google account** for testing
3. **Test on different device** to isolate device-specific issues
4. **Factory reset emulator** or create new emulator instance

---

## 📱 **Device-Specific Notes**

### **Samsung Devices:**
- May have additional security restrictions
- Check Samsung Account settings
- Disable Samsung's built-in VPN if active

### **Huawei/Honor Devices (without Google Play):**
- Google services may not be available
- Consider alternative authentication methods

### **Xiaomi/MIUI Devices:**
- Check MIUI's app permissions
- Disable MIUI optimizations for your app
- Allow auto-start for your app

### **Corporate/Enterprise Devices:**
- May have restricted Google services
- Contact IT department for whitelist requirements
- Use personal device for testing if possible

---

**Remember:** The most common issue is SHA-1 fingerprint mismatch between development environments. Always add multiple SHA-1 fingerprints to Firebase for different scenarios!
