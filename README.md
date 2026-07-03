# Rakib Silk & Fashion - Tally Khata App

A comprehensive inventory and production management system built with **Jetpack Compose** (Android) and **Node.js/MongoDB** (Backend). This app helps manage sarees, stock, production tracking, and transaction history.

## 🚀 Features

- **Inventory Management**: Track saree stock, categories, and unit prices.
- **Production Tracking**: Monitor ongoing weaving and production statuses.
- **Transaction Logs**: Automatic logging of purchases, sales, and stock movements.
- **Secure Authentication**: Email-based login with security questions for recovery.
- **Modern UI**: Built with Material 3 and Jetpack Compose for a smooth user experience.
- **Cloud Backend**: Node.js server connected to MongoDB Atlas for persistent data storage.

---

## 🛠️ Project Structure

- `/app`: Android application source code (Kotlin, Jetpack Compose, Room, KSP).
- `/backend`: Node.js API server (Express, Mongoose).

---

## 🏗️ Getting Started

### Prerequisites

- [Android Studio Koala+](https://developer.android.com/studio)
- [Node.js v18+](https://nodejs.org/)
- [MongoDB Atlas Account](https://www.mongodb.com/cloud/atlas)

### 1. Backend Setup

1. Navigate to the `backend` directory:
   ```bash
   cd backend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Configure environment variables:
   Create a `.env` file in the `backend` folder and add your connection strings:
   ```env
   MONGODB_URI=your_mongodb_connection_string
   SMTP_USER=your_gmail_address
   SMTP_PASS=your_app_specific_password
   PORT=5000
   ```
4. Start the server:
   ```bash
   npm start
   ```

### 2. Android App Setup

1. Open Android Studio and select **Open** -> choose the root directory of this project.
2. Allow Gradle to sync and download dependencies.
3. Configure API Key:
   Create a `.env` file in the root directory and add your Gemini API key:
   ```env
   GEMINI_API_KEY=your_actual_api_key
   ```
4. Update API Base URL (if necessary):
   In `app/src/main/java/com/example/api/SareeApiService.kt`, ensure the `DEFAULT_BASE_URL` points to your running backend (e.g., `http://10.0.2.2:3000/v1/` for local emulator).
5. Run the app on an emulator or physical device.

---

## 📦 Dependencies

### Android
- **Compose BOM**: UI framework
- **Room**: Local caching
- **KSP**: Kotlin Symbol Processing
- **Retrofit/OkHttp**: Networking
- **Coil**: Image loading

### Backend
- **Express**: Web framework
- **Mongoose**: MongoDB object modeling
- **Cors**: Cross-Origin Resource Sharing
- **Dotenv**: Environment variable management

---

## 📄 License

This project is for private use and development purposes.
