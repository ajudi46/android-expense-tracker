#!/bin/bash

echo "🚀 Pushing Android Expense Tracker to GitHub..."
echo "=============================================="

# Check if repository name is provided
REPO_NAME=${1:-android-expense-tracker}

echo "📋 Repository name: $REPO_NAME"
echo ""
echo "⚠️  Make sure you've created the repository on GitHub first!"
echo "   Go to: https://github.com/new"
echo "   Repository name: $REPO_NAME"
echo "   Don't initialize with README, .gitignore, or license"
echo ""

read -p "Have you created the repository on GitHub? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]
then
    echo "📡 Adding remote origin..."
    git remote add origin https://github.com/ajudi46/$REPO_NAME.git
    
    echo "🚀 Pushing to GitHub..."
    git push -u origin main
    
    echo ""
    echo "✅ SUCCESS! Your code is now on GitHub!"
    echo "🌐 View your repository: https://github.com/ajudi46/$REPO_NAME"
    echo ""
    echo "📱 Repository includes:"
    echo "  ✓ Complete Android Expense Tracker app"
    echo "  ✓ Material 3 design implementation"
    echo "  ✓ Comprehensive README.md"
    echo "  ✓ Detailed TESTING_GUIDE.md"
    echo "  ✓ Build scripts and configuration"
    echo ""
    echo "🎉 Share your project: https://github.com/ajudi46/$REPO_NAME"
else
    echo ""
    echo "📝 Please create the repository first:"
    echo "1. Go to https://github.com/new"
    echo "2. Repository name: $REPO_NAME"
    echo "3. Add description: 'Modern Android expense tracking app with Material 3 design'"
    echo "4. Choose Public or Private"
    echo "5. DON'T initialize with README, .gitignore, or license"
    echo "6. Click 'Create repository'"
    echo "7. Run this script again"
fi
