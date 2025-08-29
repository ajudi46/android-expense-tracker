# 🧪 Testing Guide for Expense Tracker App

## Prerequisites

### 1. Install Android Studio
- Download from: https://developer.android.com/studio
- Make sure you have the latest version (Electric Eel or newer)

### 2. Install Java/JDK
- Android Studio includes OpenJDK, but you can also install:
- Oracle JDK 11+ or OpenJDK 11+

### 3. Set up Android SDK
- Android Studio will prompt you to install the Android SDK
- Make sure you have:
  - Android SDK Platform 34 (Android 14)
  - Android SDK Build-Tools 34+
  - Android Emulator

## 🚀 Running the App

### Option 1: Using Android Studio (Recommended)

1. **Open the Project**
   ```bash
   # Navigate to the project folder
   cd "/Users/ajinkya.pingale/Documents/Custom Full Stack Projects/Android App"
   
   # Open Android Studio and select "Open an existing project"
   # Or from terminal:
   open -a "Android Studio" .
   ```

2. **Sync the Project**
   - Android Studio will automatically detect the Gradle build files
   - Click "Sync Now" when prompted
   - Wait for dependencies to download (first time may take 5-10 minutes)

3. **Set up Device/Emulator**
   
   **For Physical Device:**
   - Enable Developer Options on your Android device
   - Enable USB Debugging
   - Connect via USB
   
   **For Emulator:**
   - Open AVD Manager (Tools > AVD Manager)
   - Create a new Virtual Device
   - Choose Pixel 6 or similar with API 34 (Android 14)
   - Download the system image if needed

4. **Build and Run**
   - Click the green "Run" button (▶️) in Android Studio
   - Or use keyboard shortcut: `Shift + F10`
   - Select your target device/emulator
   - Wait for build to complete and app to launch

### Option 2: Using Command Line

1. **Build the APK**
   ```bash
   cd "/Users/ajinkya.pingale/Documents/Custom Full Stack Projects/Android App"
   ./gradlew assembleDebug
   ```

2. **Install on Device**
   ```bash
   # Make sure device is connected and USB debugging is enabled
   ./gradlew installDebug
   ```

3. **Build and Install in One Command**
   ```bash
   ./gradlew installDebug
   ```

## 🧪 Testing the App Features

### 1. First Launch - Setting Up Accounts

1. **App Opens to Dashboard**
   - Should see "Expense Tracker" title
   - Total balance should show $0.00
   - Should see "No transactions yet" message

2. **Add Your First Account**
   - Tap the "Accounts" quick action button OR
   - Tap the floating action button → then navigate to Accounts
   - Tap the (+) button to add account
   - Test the form:
     - Enter account name: "My Wallet"
     - Set initial balance: 1000
     - Select an icon (try the wallet icon)
     - Tap "Save"

3. **Verify Account Creation**
   - Should see the new account in the list
   - Check that balance displays correctly
   - Try editing the account (tap edit icon)
   - Verify total balance updates on dashboard

### 2. Adding Multiple Accounts

1. **Create Different Account Types**
   - Bank Account: "Chase Checking" with $5000
   - Credit Card: "Visa Card" with $-500 (negative balance)
   - Savings: "Emergency Fund" with $10000
   - Cash: "Pocket Money" with $200

2. **Test Icon Selection**
   - Use different icons for each account type
   - Verify icons display correctly in the list

### 3. Testing Transactions

#### A. Expense Transactions
1. **Navigate to Add Transaction**
   - From dashboard, tap the (+) floating button
   - Should open Add Transaction screen

2. **Create an Expense**
   - Select "Expense" chip (should be red)
   - Enter amount: 25.50
   - Description: "Lunch at cafe"
   - Category: Select "Food"
   - Account: Select "My Wallet"
   - Tap "Save"

3. **Verify Expense**
   - Return to dashboard
   - Check that wallet balance decreased by $25.50
   - Verify transaction appears in recent transactions
   - Should show red down arrow and negative amount

#### B. Income Transactions
1. **Add Income**
   - Tap (+) button again
   - Select "Income" chip (should be green)
   - Amount: 2000
   - Description: "Monthly salary"
   - Category: "Salary"
   - Account: "Chase Checking"
   - Save

2. **Verify Income**
   - Check that checking account balance increased
   - Transaction should show green up arrow and positive amount

#### C. Transfer Transactions
1. **Transfer Between Accounts**
   - Select "Transfer" chip (should be blue)
   - Amount: 500
   - Description: "Emergency fund deposit"
   - From Account: "Chase Checking"
   - To Account: "Emergency Fund"
   - Save

2. **Verify Transfer**
   - Checking should decrease by $500
   - Emergency Fund should increase by $500
   - Total balance should remain the same
   - Transaction should show blue transfer icon

### 4. Dashboard Testing

1. **Total Balance Verification**
   - Should accurately reflect sum of all account balances
   - Updates in real-time after transactions

2. **Recent Transactions**
   - Should show most recent transactions first
   - Each transaction should display:
     - Correct icon and color for type
     - Description and category
     - Date formatted nicely
     - Amount with proper +/- signs

3. **Quick Actions**
   - Test "Accounts" button navigation
   - Test "Add Transaction" button navigation

### 5. Account Management Testing

1. **Account List**
   - All accounts should display with correct icons
   - Balances should show with proper formatting
   - Positive balances in primary color
   - Negative balances in red

2. **Edit Account**
   - Tap edit button on any account
   - Modify name, balance, or icon
   - Verify changes persist

3. **Delete Account**
   - Try deleting an account (be careful - this is permanent)
   - Verify it disappears from list
   - Check that total balance updates

## 🐛 Common Issues and Troubleshooting

### Build Issues

1. **Gradle Sync Failed**
   - Check internet connection
   - Try: Tools > Sync Project with Gradle Files
   - Clear cache: File > Invalidate Caches and Restart

2. **Missing SDK**
   - Go to Tools > SDK Manager
   - Install missing SDK platforms or build tools

3. **Java/Kotlin Version Issues**
   - Check that you're using JDK 11 or newer
   - File > Project Structure > SDK Location

### Runtime Issues

1. **App Crashes on Launch**
   - Check Logcat in Android Studio for error messages
   - Common issue: Database migration problems

2. **Navigation Not Working**
   - Check if all screens are properly defined in navigation graph

3. **UI Issues**
   - Clear app data and restart
   - Check if Material 3 theme is properly applied

### Testing on Different Devices

1. **Test on Various Screen Sizes**
   - Try different emulator configurations
   - Phone, tablet, foldable

2. **Test Dark/Light Theme**
   - Change system theme and verify app adapts
   - Check Material 3 dynamic colors (Android 12+)

3. **Test Different Android Versions**
   - Minimum: API 24 (Android 7.0)
   - Recommended: API 34 (Android 14)

## 📱 Device Recommendations for Testing

### Emulators
- **Pixel 6** (API 34) - Latest features, Material 3
- **Pixel 4** (API 30) - Good middle ground
- **Nexus 5X** (API 24) - Minimum supported version

### Physical Devices
- Any Android device running Android 7.0+
- Better performance than emulator
- Real touch experience

## 🔧 Developer Tools

### Useful Android Studio Tools
1. **Logcat** - View app logs and crash reports
2. **Layout Inspector** - Debug UI layouts
3. **Database Inspector** - View Room database contents
4. **Network Inspector** - Monitor network calls (if added later)

### ADB Commands
```bash
# View connected devices
adb devices

# Clear app data
adb shell pm clear com.expensetracker

# View app logs
adb logcat | grep ExpenseTracker
```

## ✅ Success Criteria

Your app is working correctly if:

1. ✅ App launches without crashes
2. ✅ Can create accounts with different icons
3. ✅ Can add expenses, income, and transfers
4. ✅ Balances update correctly after transactions
5. ✅ Navigation works between all screens
6. ✅ UI follows Material 3 design principles
7. ✅ Data persists after app restart
8. ✅ Total balance calculation is accurate

## 🎯 Next Steps

Once basic testing is complete, consider:

1. **Stress Testing** - Add many accounts and transactions
2. **Edge Cases** - Test with very large numbers, special characters
3. **Performance** - Test with hundreds of transactions
4. **Accessibility** - Test with TalkBack enabled
5. **Localization** - Test in different languages/regions

Happy testing! 🚀
