# Expense Tracker Android App

A modern Android expense tracking application built with Material 3 design language and Jetpack Compose.

## Features

### 🏦 Multiple Accounts
- Create and manage multiple accounts (Bank, Wallet, Credit Card, etc.)
- Set custom icons for each account
- Track individual account balances
- View total balance across all accounts

### 💰 Transaction Types
1. **Expense** - Track money going out
2. **Income** - Track money coming in  
3. **Transfer** - Move money between your accounts

### 🎨 Material 3 Design
- Modern Material 3 Expressive theme
- Dynamic colors support (Android 12+)
- Beautiful cards and surfaces
- Smooth animations and transitions

### 📱 Core Functionality
- Add/edit/delete accounts with custom icons
- Record transactions with categories
- View transaction history
- Dashboard with total balance overview
- Clean and intuitive navigation

## Tech Stack

- **Kotlin** - Primary programming language
- **Jetpack Compose** - Modern UI toolkit
- **Material 3** - Google's latest design system
- **Room Database** - Local data persistence
- **Hilt** - Dependency injection
- **Navigation Compose** - Navigation component
- **ViewModel** - MVVM architecture
- **Flow** - Reactive data streams

## Architecture

The app follows MVVM (Model-View-ViewModel) architecture with:

- **Data Layer**: Room database with DAOs and Repository pattern
- **Domain Layer**: ViewModels for business logic
- **UI Layer**: Composable screens with Material 3 components

## Project Structure

```
app/src/main/java/com/expensetracker/
├── data/
│   ├── dao/           # Database access objects
│   ├── database/      # Room database setup
│   ├── model/         # Data models (Account, Transaction, etc.)
│   └── repository/    # Repository pattern implementation
├── di/                # Hilt dependency injection modules
├── ui/
│   ├── navigation/    # Navigation setup
│   ├── screen/        # UI screens (Dashboard, Accounts, AddTransaction)
│   ├── theme/         # Material 3 theme and colors
│   └── viewmodel/     # ViewModels for each screen
├── MainActivity.kt    # Main activity
└── ExpenseTrackerApplication.kt  # Application class
```

## Getting Started

1. Clone the repository
2. Open in Android Studio
3. Sync the project to download dependencies
4. Build and run on your Android device or emulator

## Requirements

- Android Studio Electric Eel or newer
- Minimum SDK: API 24 (Android 7.0)
- Target SDK: API 34 (Android 14)
- Kotlin 1.9+

## Key Features Implementation

### Account Management
- Create accounts with custom names and icons
- Six predefined icon options (Wallet, Bank, Card, Savings, Cash, Business)
- Edit and delete accounts
- Real-time balance updates

### Transaction Recording
- Smart form validation
- Category selection based on transaction type
- Account selection with balance display
- Transfer between accounts with dual account selection

### Dashboard
- Beautiful Material 3 cards
- Total balance display with currency formatting
- Recent transactions list with type-specific icons and colors
- Quick action buttons for easy navigation

### Data Persistence
- Room database for offline storage
- Automatic balance calculations
- Transaction history preservation
- Account relationship management

## Material 3 Implementation

The app showcases Google's latest Material 3 design with:

- Dynamic color theming
- Expressive color tokens
- Modern typography scale
- Elevated surfaces and cards
- Intuitive navigation patterns
- Accessibility-first design

## Future Enhancements

- Categories management
- Transaction filtering and search
- Expense analytics and charts
- Export functionality
- Backup and restore
- Recurring transactions
- Budget tracking
