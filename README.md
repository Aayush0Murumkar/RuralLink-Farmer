# RuralLink Farmer App

> AI-powered rural logistics Android application connecting farmers with suitable transporters for agricultural produce.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android%20(API%2026+)-green.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-blue.svg)](https://developer.android.com/jetpack/compose)
[![Supabase](https://img.shields.io/badge/Backend-Supabase%20%7C%20PostgreSQL-3ECF8E.svg)](https://supabase.com)
[![Room Database](https://img.shields.io/badge/Storage-Room%20(Offline--First)-orange.svg)](https://developer.android.com/training/data-storage/room)

---

## 📌 Project Overview

**RuralLink** is a digital logistics platform designed to simplify transport booking for Indian farmers moving produce (onions, grains, vegetables, fruits, and fertilizers).

Guided by the principle:
> **"AI recommends. The farmer decides."**

Farmers can create transport bookings using a manual form or voice commands, compare real-time quotes, evaluate AI suitability scores based on capacity and backhaul routes, review transparent fare breakdowns, and confirm bookings with QR-code payment verification.

---

## 🏛 Architecture

```text
[Farmer UI (Jetpack Compose & M3)]
                 │
                 ▼
     [Service & Domain Layer]
 (AuthService, RequestService, PricingEngine, AgriMatchAiService)
                 │
        ┌────────┴────────┐
        ▼                 ▼
[Room Database]     [Remote Backend]
 (Offline SQLite)    ├── Supabase (Primary PostgreSQL / GoTrue / SDK)
                     └── Firebase (Transitional Phone Auth & Firestore)
```

- **Local Persistence:** Room Database cache ensures instant UI feedback and offline access.
- **Primary Backend:** Supabase (PostgreSQL tables: `farmers`, `transport_requests`, `transporters`, `transporter_responses`, `notifications`).
- **Transitional Backend:** Firebase Phone Auth and Cloud Firestore maintained for cross-application compatibility.
- **AI Processing:** Gemini API & AgriMatch engine for voice transcript parsing and quote matching.

---

## 🚀 Quick Setup

### Prerequisites
- Android Studio Ladybug (2024.2+) or newer
- JDK 17+
- Android SDK 35 (minSdk 26)

### Installation
1. **Clone the repository:**
   ```bash
   git clone https://github.com/Aayush0Murumkar/ruralfarmerlink.git
   cd ruralfarmerlink
   ```

2. **Configure environment credentials:**
   ```bash
   cp .env.example .env
   ```
   Provide your values in `.env`:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_PUBLISHABLE_KEY=your_supabase_publishable_anon_key
   ```
   *(Note: The app contains built-in demo profiles and fallbacks allowing immediate offline compilation without external keys).*

3. **Build the project:**
   ```bash
   gradle assembleDebug
   ```

---

## 🚚 Companion Application

RuralLink operates as a two-application ecosystem:

| Application | Repository | Status |
|---|---|---|
| **RuralLink Farmer** (This repo) | Current Repository | Active |
| **RuralLink Transporter / Partner** | [ruralfarmerlinkpartner](https://github.com/Aayush0Murumkar/ruralfarmerlinkpartner/tree/main) | Active |

---

## 📊 Current Status

- **Farmer UI & Navigation:** Complete (Jetpack Compose, Material 3)
- **Voice Booking Assistant:** Complete (Speech recognition + Gemini NLP extraction)
- **AI Recommendation Engine:** Complete (Multi-factor scoring & transparent pricing breakdown)
- **Local Persistence:** Complete (Room Database offline-first)
- **Supabase Integration:** Complete (PostgREST HTTP engine & Official SDK Client)
- **Two-Factor Authentication:** Demo mock flow (verification code: `999999`)
- **Phone Auth (SMS):** Firebase Phone Auth with demo bypass fallback
- **Realtime UI Updates:** In progress (SDK installed, UI currently polls Room Flow)

---

## 📖 Complete Technical Architecture

For detailed architectural diagrams, table schemas, authentication mechanisms, and workflow specifications, consult:

👉 **[PROJECT_CONTEXT.md](./PROJECT_CONTEXT.md)**
