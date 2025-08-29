#!/bin/bash

# Firebase Authentication Fix Script
# This script helps diagnose and fix common Firebase authentication issues

echo "🔥 Firebase Authentication Diagnostic Tool"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if we're on macOS or Linux
if [[ "$OSTYPE" == "darwin"* ]]; then
    DEBUG_KEYSTORE="$HOME/.android/debug.keystore"
else
    DEBUG_KEYSTORE="$HOME/.android/debug.keystore"
fi

echo -e "${BLUE}1. Checking Debug Keystore...${NC}"
if [ -f "$DEBUG_KEYSTORE" ]; then
    echo -e "${GREEN}✅ Debug keystore found at: $DEBUG_KEYSTORE${NC}"
    
    echo ""
    echo -e "${BLUE}2. Getting SHA-1 Fingerprints...${NC}"
    
    # Debug keystore SHA-1
    DEBUG_SHA1=$(keytool -list -v -keystore "$DEBUG_KEYSTORE" -alias androiddebugkey -storepass android -keypass android 2>/dev/null | grep "SHA1:" | sed 's/.*SHA1: //')
    
    if [ ! -z "$DEBUG_SHA1" ]; then
        echo -e "${GREEN}✅ Debug SHA-1 Fingerprint: $DEBUG_SHA1${NC}"
    else
        echo -e "${RED}❌ Could not extract debug SHA-1 fingerprint${NC}"
    fi
    
    # Release keystore SHA-1 (if exists)
    if [ -f "keystore.jks" ]; then
        echo -e "${BLUE}📱 Found release keystore: keystore.jks${NC}"
        echo -e "${YELLOW}⚠️  Release keystore requires password. Common aliases: 'key0', 'androidreleasekey', 'release'${NC}"
        echo ""
        echo -e "${BLUE}To get release SHA-1, run one of these commands:${NC}"
        echo "keytool -list -v -keystore keystore.jks -alias key0"
        echo "keytool -list -v -keystore keystore.jks -alias androidreleasekey"
        echo "keytool -list -v -keystore keystore.jks -alias release"
        echo ""
        echo -e "${YELLOW}💡 Add BOTH debug and release SHA-1 to Firebase Console!${NC}"
    fi
    
    if [ ! -z "$DEBUG_SHA1" ]; then
        echo ""
        echo -e "${YELLOW}📋 Debug SHA-1 fingerprint (add to Firebase Console):${NC}"
        echo -e "${BLUE}$DEBUG_SHA1${NC}"
        echo ""
        echo -e "${YELLOW}🔗 Firebase Console URL:${NC}"
        echo "https://console.firebase.google.com/project/logmoney-bb14e/settings/general"
        echo ""
    fi
else
    echo -e "${RED}❌ Debug keystore not found at: $DEBUG_KEYSTORE${NC}"
    echo -e "${YELLOW}💡 This usually means Android Studio hasn't been used yet.${NC}"
    echo -e "${YELLOW}   Please open Android Studio, create/load a project first.${NC}"
fi

echo -e "${BLUE}3. Checking Current Firebase Configuration...${NC}"
if [ -f "app/google-services.json" ]; then
    echo -e "${GREEN}✅ google-services.json found${NC}"
    
    # Extract project info
    PROJECT_ID=$(grep -o '"project_id": "[^"]*' app/google-services.json | sed 's/"project_id": "//')
    PACKAGE_NAME=$(grep -o '"package_name": "[^"]*' app/google-services.json | sed 's/"package_name": "//')
    CURRENT_SHA1=$(grep -o '"certificate_hash": "[^"]*' app/google-services.json | sed 's/"certificate_hash": "//')
    
    echo "   Project ID: $PROJECT_ID"
    echo "   Package Name: $PACKAGE_NAME"
    echo "   Configured SHA-1: $CURRENT_SHA1"
    
    if [ "$DEBUG_SHA1" = "$CURRENT_SHA1" ]; then
        echo -e "${GREEN}✅ Debug SHA-1 fingerprints match!${NC}"
    else
        echo -e "${RED}❌ Debug SHA-1 fingerprint mismatch!${NC}"
        echo -e "${YELLOW}   Your debug SHA-1: $DEBUG_SHA1${NC}"
        echo -e "${YELLOW}   Firebase config SHA-1: $CURRENT_SHA1${NC}"
        echo ""
        echo -e "${YELLOW}🔧 To fix this:${NC}"
        echo "1. Go to Firebase Console: https://console.firebase.google.com/project/$PROJECT_ID/settings/general"
        echo "2. Click on your Android app"
        echo -e "3. Add this debug SHA-1 fingerprint: ${BLUE}$DEBUG_SHA1${NC}"
        echo "4. If building release APK, also add release keystore SHA-1"
        echo "5. Download new google-services.json"
        echo "6. Replace app/google-services.json with the new file"
        echo "7. Run: ./gradlew clean build"
    fi
else
    echo -e "${RED}❌ google-services.json not found${NC}"
    echo -e "${YELLOW}💡 Please download it from Firebase Console${NC}"
fi

echo ""
echo -e "${BLUE}4. Testing Network Connectivity...${NC}"
if ping -c 1 google.com &> /dev/null; then
    echo -e "${GREEN}✅ Internet connection OK${NC}"
else
    echo -e "${RED}❌ No internet connection${NC}"
    echo -e "${YELLOW}💡 Check your network connection and try again${NC}"
fi

if ping -c 1 accounts.google.com &> /dev/null; then
    echo -e "${GREEN}✅ Google services reachable${NC}"
else
    echo -e "${RED}❌ Cannot reach Google services${NC}"
    echo -e "${YELLOW}💡 This might be due to firewall/VPN. Try different network.${NC}"
fi

echo ""
echo -e "${BLUE}5. Recommended Actions:${NC}"
echo ""

if [ "$SHA1" != "$CURRENT_SHA1" ]; then
    echo -e "${YELLOW}🔧 URGENT: Fix SHA-1 fingerprint mismatch${NC}"
    echo "   This is likely causing your authentication errors."
    echo ""
fi

echo -e "${YELLOW}🧹 Clean build (always recommended):${NC}"
echo "   ./gradlew clean build"
echo ""

echo -e "${YELLOW}📱 For device-specific issues:${NC}"
echo "   - Update Google Play Services on your device"
echo "   - Clear app data: Settings > Apps > Expense Tracker > Storage > Clear Data"
echo "   - Try different network (WiFi vs Mobile data)"
echo ""

echo -e "${YELLOW}📖 For detailed troubleshooting:${NC}"
echo "   Check DEVICE_TROUBLESHOOTING.md"
echo ""

echo -e "${GREEN}🎯 Quick Fix Summary:${NC}"
if [ ! -z "$DEBUG_SHA1" ]; then
    echo -e "1. Add debug SHA-1 to Firebase: ${BLUE}$DEBUG_SHA1${NC}"
    if [ -f "keystore.jks" ]; then
        echo -e "2. ${YELLOW}Get release keystore SHA-1 and add it to Firebase too${NC}"
        echo "   Run: keytool -list -v -keystore keystore.jks -alias [your_alias]"
    else
        echo "2. No release keystore found (debug builds only)"
    fi
    echo "3. Download new google-services.json"
    echo "4. Replace app/google-services.json"
    echo "5. Run: ./gradlew clean build"
    echo "6. Test on your device"
    echo ""
    echo -e "${RED}🚨 IMPORTANT for signed APKs:${NC}"
    echo -e "${YELLOW}Release builds require the RELEASE keystore SHA-1, not debug SHA-1!${NC}"
else
    echo "1. Set up Android Studio development environment"
    echo "2. Run this script again to get SHA-1 fingerprint"
fi

echo ""
echo -e "${BLUE}Done! 🚀${NC}"
