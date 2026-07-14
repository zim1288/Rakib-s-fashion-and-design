# Rakib Silk & Fashion - Tally Khata App

A comprehensive inventory and production management system built with **Jetpack Compose** (Android) and **Node.js/MongoDB** (Backend). This app helps manage sarees, stock, production tracking, and transaction history.

## 🚀 Features

- **Inventory Management**: Track saree stock, categories, and unit prices.
- **Production Tracking**: Monitor ongoing weaving and production statuses.
- **Transaction Logs**: Automatic logging of purchases, sales, and stock movements.
- **Secure Authentication**: Email-based login with OTP verification via **EmailJS**.
- **Image Management**: Integrated with **Cloudinary** for cloud storage of product images.
- **Data Export**: Export inventory and transaction history to **CSV** files.
- **Modern UI**: Built with Material 3 and Jetpack Compose for a smooth user experience.
- **Cloud Backend**: Node.js server connected to MongoDB Atlas for persistent data storage.

---

## 🛠️ Project Structure

- `/app`: Android application source code (Kotlin, Jetpack Compose, Room, KSP, Retrofit).
- `/backend`: Node.js API server (Express, Mongoose, Cloudinary, Multer).

---

## 🏗️ Getting Started

### Prerequisites

- [Android Studio Ladybug+](https://developer.android.com/studio)
- [Node.js v18+](https://nodejs.org/)
- [MongoDB Atlas Account](https://www.mongodb.com/cloud/atlas)
- [Cloudinary Account](https://cloudinary.com/)
- [EmailJS Account](https://www.emailjs.com/)

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
   CLOUDINARY_CLOUD_NAME=your_cloud_name
   CLOUDINARY_API_KEY=your_api_key
   CLOUDINARY_API_SECRET=your_api_secret
   EMAILJS_PRIVATE_KEY=your_emailjs_private_key
   BACKEND_PORT=5000
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
   *Note: Gemini integration is managed via Firebase Vertex AI. Ensure you have a `google-services.json` file if needed for Firebase services.*
4. Update API Base URL:
   In `app/src/main/java/com/example/api/SareeApiService.kt`, the `DEFAULT_BASE_URL` is configured to:
   `https://rakib-fashion-backend.onrender.com/v1/`
   Modify this if you are running the backend locally (e.g., `http://10.0.2.2:5000/v1/`).
5. Run the app on an emulator or physical device.

---

## 📦 Dependencies

### Android
- **Compose BOM**: UI framework
- **Room**: Local caching & offline synchronization
- **KSP**: Kotlin Symbol Processing
- **Retrofit/OkHttp**: Networking and API interaction
- **Coil**: Image loading from Cloudinary
- **Firebase Vertex AI**: Gemini AI integration
- **WorkManager**: Background sync and database backups

### Backend
- **Express**: Web framework
- **Mongoose**: MongoDB object modeling
- **Cloudinary**: Cloud image storage and management
- **Multer**: Middleware for handling multipart/form-data (image uploads)
- **Cors**: Cross-Origin Resource Sharing
- **Dotenv**: Environment variable management

---

## 📄 License

This project is for private use and development purposes.
