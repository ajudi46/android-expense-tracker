#!/bin/bash

# Expense Tracker Build and Test Script
echo "🚀 Building Expense Tracker Android App..."
echo "==========================================="

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 11 or newer."
    exit 1
fi

echo "✅ Java found: $(java -version 2>&1 | head -n 1)"

# Check if Android SDK is available
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "⚠️  Android SDK not found in environment variables."
    echo "   This is normal if you're using Android Studio's bundled SDK."
    echo "   Make sure Android Studio is installed and configured."
fi

# Make gradlew executable (just in case)
chmod +x ./gradlew

# Try to build the project
echo ""
echo "🔨 Building the project..."
echo "This may take a few minutes on first run..."

if ./gradlew build --no-daemon; then
    echo ""
    echo "✅ BUILD SUCCESSFUL!"
    echo ""
    echo "🎉 Your Expense Tracker app is ready!"
    echo ""
    echo "Next steps:"
    echo "1. Open Android Studio"
    echo "2. Open this project folder"
    echo "3. Set up an emulator or connect a device"
    echo "4. Click the green Run button"
    echo ""
    echo "Or check the TESTING_GUIDE.md for detailed instructions."
else
    echo ""
    echo "❌ BUILD FAILED"
    echo ""
    echo "Common solutions:"
    echo "1. Install Android Studio from: https://developer.android.com/studio"
    echo "2. Open Android Studio and install the Android SDK"
    echo "3. Open this project in Android Studio and let it sync"
    echo "4. Try building from Android Studio instead"
    echo ""
    echo "For detailed setup instructions, see TESTING_GUIDE.md"
fi
