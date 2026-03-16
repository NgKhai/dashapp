# DashApp Delivery

DashApp is a comprehensive delivery platform composed of two native Android applications and a Node.js backend. The platform provides a seamless experience for both customers requesting deliveries and drivers fulfilling them, complete with real-time tracking, secure authentication, and advanced AI features.

## 🚀 Project Architecture

The project is divided into three main components:

- **CustomerDashApp**: The customer-facing Android application.
- **DriverDashApp**: The driver-facing Android application.
- **Backend API**: The central API built with Node.js and Express.

---

### 1. Backend API (`/backend`)
A robust Node.js backend built with Express, providing real-time data synchronization via Supabase.

**Tech Stack:**
- **Runtime:** Node.js (>= 18.0)
- **Framework:** Express.js
- **Database/BaaS:** Supabase (PostgreSQL)
- **Authentication:** JWT (JSON Web Tokens) & bcrypt
- **Security & Config:** CORS, dotenv

**Core Routes:**
- `/auth`: User and driver authentication.
- `/admin`: Administration panel routes.
- `/customers`: Customer profile and management.
- `/drivers`: Driver onboarding and status management.
- `/deliveries`: Delivery creation and tracking.
- `/pricing`: Dynamic pricing calculation engine.

---

### 2. Customer Dash App (`/CustomerDashApp`)
The native Android app for customers to book and track deliveries in real-time.

**Tech Stack & Libraries:**
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Architecture:** MVVM with Clean Architecture principles
- **Dependency Injection:** Dagger Hilt
- **Network:** Retrofit + OkHttp
- **Local Storage:** DataStore Preferences
- **Image Loading:** Coil
- **Maps:** OSMDroid (OpenStreetMap)
- **Real-time Engine:** Supabase Realtime via Ktor Client
- **AI/Camera Integration:** 
  - **CameraX:** For seamless camera integration.
  - **ML Kit:** Machine learning image labeling to help users categorize their packages quickly.

---

### 3. Driver Dash App (`/DriverDashApp`)
The native Android app designed for drivers to accept, navigate, and complete delivery requests efficiently.

**Tech Stack & Libraries:**
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Architecture:** MVVM with Clean Architecture
- **Dependency Injection:** Dagger Hilt
- **Network:** Retrofit + OkHttp
- **Local Storage:** DataStore Preferences
- **Maps for Navigation:** OSMDroid
- **Real-time Order Updates:** Supabase Realtime

---

## 🛠 Prerequisites

To run these applications locally, ensure you have the following installed:
- [Android Studio](https://developer.android.com/studio) (latest version supporting Jetpack Compose)
- Node.js `v18+`
- A [Supabase](https://supabase.com) project

## ⚙️ Environment Setup

### Backend Setup
1. Navigate to the backend folder:
   ```bash
   cd backend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Set up your `.env` file based on `.env.example` (ensure `SUPABASE_URL` and `SUPABASE_ANON_KEY` are provided).
4. Run the development server:
   ```bash
   npm run dev
   ```

### Android Apps Setup
Create a `local.properties` file in the root directory (`/dashapp`) and define the following variables:
```properties
API_BASE_URL="http://your-local-ip:your-port/api"
VERCEL_BYPASS_SECRET="your-vercel-bypass-secret"
SUPABASE_URL="your-supabase-url"
SUPABASE_ANON_KEY="your-supabase-anon-key"
```

Then, open either `CustomerDashApp` or `DriverDashApp` in Android Studio and run the application on an emulator or physical device.

---

## 📈 Key Features

- **Real-Time Data Sync:** Uses Supabase's Realtime broadcast channels to keep the customer and driver seamlessly connected during an active delivery.
- **Smart Package Labeling:** The customer app uses Google ML Kit to automatically label and categorize images taken using CameraX.
- **No Google Maps Billing:** Instead of relying on expensive Google Maps APIs, the app incorporates OSMDroid, an open-source mapping solution, reducing overall operational costs.
- **Role-Based Access:** Clean separation of concerns with distinct APIs for admins, drivers, and customers within the Express backend.

## 📄 License
This project is licensed under the MIT License.
