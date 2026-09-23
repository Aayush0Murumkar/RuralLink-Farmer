# 🌾 RuralLink — Farmer App

> **AI-powered rural logistics for smarter, simpler agricultural transportation.**

RuralLink connects **farmers with suitable transporters** for moving agricultural produce and materials across rural India.

The platform is designed as a **two-application ecosystem**:

**👨‍🌾 Farmer App** → creates transport requests
**🚛 Transporter App** → receives and responds to requests

Both applications communicate through a shared backend architecture.

---

## ✨ What is RuralLink?

Farmers often need reliable transportation for goods such as:

🌾 Grains
🧅 Onions
🥬 Vegetables
🍎 Fruits
🌱 Fertilizers
📦 Other agricultural materials

RuralLink simplifies this process by allowing farmers to create a transport request, receive transporter responses, compare available options, and make the final decision.

### Core Principle

> **🤖 AI recommends. The farmer decides.**

AI is used to assist the farmer, not replace the farmer's decision.

---

# 🚀 Key Features

### 👨‍🌾 Farmer Experience

* 📱 Farmer registration and authentication
* 👤 Farmer profile management
* 📍 Pickup and drop location entry
* ⚖️ Material weight and unit selection
* 🌾 Produce/material type selection
* 🗓️ Required date and time
* 📝 Transport request creation
* 🎙️ Voice-assisted booking
* 📋 Request history and status tracking
* 🚛 Transporter response comparison
* 🤖 AI-assisted transporter recommendation
* 💰 Transparent pricing information
* 🔐 QR/payment verification workflow

---

## 🎙️ Voice-Assisted Booking

RuralLink supports farmers who may find traditional form-based input difficult.

Instead of filling every field manually, a farmer can speak a request such as:

> “I want to send 20 quintals of onion from Kopargaon to Nashik tomorrow at 9 AM.”

The system processes the speech and extracts structured information:

```text
Material      → Onion
Weight        → 20
Unit          → Quintal
Pickup        → Kopargaon
Destination   → Nashik
Date          → Tomorrow
Time          → 09:00 AM
```

The farmer reviews the extracted information before the request is submitted.

---

# 🏗️ System Architecture

```text
                    RURAL LINK
                        │
          ┌─────────────┴─────────────┐
          │                           │
          ▼                           ▼
     👨‍🌾 FARMER APP             🚛 TRANSPORTER APP
     Jetpack Compose             Android / Kotlin
          │                           │
          └─────────────┬─────────────┘
                        ▼
                 SERVICE LAYER
          ┌─────────────┼─────────────┐
          │             │             │
          ▼             ▼             ▼
      AuthService   RequestService   AI Services
          │             │             │
          └─────────────┼─────────────┘
                        ▼
                  SUPABASE BACKEND
             ┌──────────┼──────────┐
             │          │          │
             ▼          ▼          ▼
           Auth     PostgreSQL   Realtime
             │          │
             │      ┌───┼──────────────┐
             │      │   │      │       │
             ▼      ▼   ▼      ▼       ▼
          Users  Farmers Requests Responses
                              │
                              ▼
                           Bookings
```

---

# 🧠 Technology Stack

| Layer                       | Technology                   |
| --------------------------- | ---------------------------- |
| **Platform**                | Android                      |
| **Language**                | Kotlin 2.0.21                |
| **UI**                      | Jetpack Compose + Material 3 |
| **Local Storage**           | Room Database                |
| **Primary Backend**         | Supabase                     |
| **Database**                | PostgreSQL                   |
| **Authentication**          | Supabase Auth                |
| **Realtime**                | Supabase Realtime            |
| **AI Processing**           | Gemini API                   |
| **Build System**            | Gradle                       |
| **Minimum Android Version** | API 26                       |
| **Target SDK**              | Android SDK 35               |
| **JDK**                     | JDK 17+                      |

---

# 🗄️ Backend Structure

RuralLink uses a relational PostgreSQL model designed around the relationship between farmers, transport requests, transporters and vehicles.

### Core Tables

```text
organizations
      │
      ├── farmers
      │
      └── transporters
                │
                └── vehicles

farmers
   │
   └── transport_requests
              │
              └── transporter_responses
                         │
                         └── transporters

transport_requests
        │
        └── bookings

notifications
```

### Main Entities

| Table                   | Purpose                                        |
| ----------------------- | ---------------------------------------------- |
| `farmers`               | Farmer profiles                                |
| `transporters`          | Transporter profiles                           |
| `vehicles`              | Transporter vehicle information                |
| `organizations`         | FPOs, cooperatives and logistics organizations |
| `transport_requests`    | Farmer-created transport requests              |
| `transporter_responses` | Transporter quotes/responses                   |
| `bookings`              | Confirmed farmer-transporter bookings          |
| `notifications`         | User notifications                             |

---

# 🔄 Core Workflow

```text
1. Farmer registers
        ↓
2. Authentication / verification
        ↓
3. Farmer opens Book Transport
        ↓
4. Enters transport details
        ↓
5. Reviews request
        ↓
6. Request stored in backend
        ↓
7. Suitable transporters receive request
        ↓
8. Transporters respond / quote
        ↓
9. Farmer receives multiple responses
        ↓
10. AI compares available options
        ↓
11. AI recommends a suitable transporter
        ↓
12. Farmer makes final selection
        ↓
13. Booking is confirmed
```

---

# 🤖 AI Recommendation

When multiple transporters respond, RuralLink can compare several factors.

### Matching Factors

* 📍 Pickup distance
* 🚛 Vehicle capacity
* 🛣️ Route compatibility
* 🕐 Time compatibility
* ✅ Availability
* 💰 Quote information

The recommendation is designed to be **explainable**, so the farmer can understand why an option is suggested.

> **AI recommends. The farmer decides.**

---

# 🔐 Security

The Supabase backend uses **Row Level Security (RLS)** to control access to application data.

The design is based on the authenticated Supabase user ID.

### Farmer

Can access:

* Own profile
* Own transport requests
* Relevant transporter responses
* Own bookings

### Transporter

Can access:

* Own profile
* Own vehicles
* Eligible transport requests
* Own responses
* Own bookings

Sensitive verification fields and administrative information are intended to remain backend/admin controlled.

---

# 📱 Two-App Ecosystem

RuralLink is designed as two separate applications sharing the same backend.

| Application                         | Role                                      | Repository                                                                                    |
| ----------------------------------- | ----------------------------------------- | --------------------------------------------------------------------------------------------- |
| **RuralLink Farmer**                | Creates requests and selects transporters | This repository                                                                               |
| **RuralLink Transporter / Partner** | Receives requests and responds            | [ruralfarmerlinkpartner](https://github.com/Aayush0Murumkar/ruralfarmerlinkpartner/tree/main) |

### Communication Architecture

```text
             SUPABASE
                 │
       ┌─────────┴─────────┐
       │                   │
       ▼                   ▼
  FARMER APP          TRANSPORTER APP
       │                   │
       │ Create Request    │
       ├──────────────────►│
       │                   │
       │◄──────────────────┤
       │   Quote/Response  │
       │                   │
       ├──────────────────►│
       │ Booking Selected  │
       │                   │
```

---

# 💾 Offline-First Design

RuralLink uses **Room Database** for local persistence.

This allows the application to:

* Cache important farmer data
* Maintain responsive UI behaviour
* Store local request state
* Support offline-first interaction patterns
* Synchronize with the remote backend when connectivity is available

```text
             FARMER APP
                 │
       ┌─────────┴─────────┐
       ▼                   ▼
   ROOM DATABASE       SUPABASE
   Local Cache        Remote Backend
       │                   │
       └────────Sync───────┘
```

---

# 🔑 Authentication Status

The application is being migrated toward **Supabase Auth** as the primary authentication system.

Current repository code still contains transitional Firebase authentication/Firestore components, particularly around the existing phone-auth workflow.

##
