# RuralLink Project Context

## 1. Project Overview

**RuralLink** is an AI-assisted rural logistics Android platform designed to connect farmers in rural India with suitable, verified transporters for agricultural produce (grains, onions, vegetables, fruits, fertilizers, and equipment).

The platform operates on a guiding principle:
> **"AI recommends. The farmer decides."**

Farmers can create transport requests through traditional form entry or voice-driven input, compare quotes from multiple transporters, receive intelligent match scores based on capacity and backload routes, select the best carrier, and complete booking verification.

---

## 2. Current Applications

The RuralLink ecosystem is architected around two complementary applications:

1. **RuralLink Farmer (This Repository)**
   - **Target User:** Farmers and agricultural producers.
   - **Platform:** Native Android (Kotlin, Jetpack Compose, Material 3).
   - **Key Responsibilities:**
     - Farmer registration and profile management with Aadhaar masking.
     - Authentication (Supabase GoTrue, Firebase Phone OTP, and demo mock bypass).
     - Transport request creation via manual input or speech-to-text voice assistant.
     - Offline-first local data persistence with Room Database.
     - Multi-quote comparison and AI recommendation scoring.
     - Payment simulation with QR code confirmation.
     - Diagnostics and health monitoring for backend connectivity.

2. **RuralLink Transporter / Partner (Companion Repository)**
   - **Target User:** Truck owners, fleet managers, and independent rural drivers.
   - **Repository:** [ruralfarmerlinkpartner](https://github.com/Aayush0Murumkar/ruralfarmerlinkpartner/tree/main)
   - **Key Responsibilities:**
     - Transporter registration and vehicle capacity registration.
     - Viewing available transport requests submitted by farmers.
     - Submitting quotes, estimated arrival times, and backload availability.
     - Updating trip milestones (Accepted, In-Transit, Delivered).

---

## 3. Current Architecture

```text
[Farmer User]
      │
      ▼
[Jetpack Compose UI & Screens]
      │
      ▼
[Service Layer: FarmerServices / TransporterResponseService / AgriMatchAiService]
      │
      ├───────────────────────────────┐
      ▼                               ▼
[Local Room Database (SQLite)]   [Remote Backend Layer]
 - FarmerDao                      ├── Supabase (Primary Active Backend)
 - RequestDao                     │    ├─ GoTrue Auth
 - NotificationDao                │    ├─ PostgREST (PostgreSQL)
                                  │    └─ Realtime / Storage Client SDK
                                  │
                                  └── Firebase (Transitional / Legacy)
                                       ├─ Phone Auth (SMS OTP)
                                       └─ Cloud Firestore Collections
```

### Backend Transition Strategy
- **Supabase** is the primary target backend for PostgreSQL relational storage, authentication, and future realtime sync.
- **Firebase** is currently maintained as a transitional layer to avoid breaking existing prototypes and shared Firestore dependencies between the two applications. Firebase code and `google-services.json` must be preserved until formal migration is complete.

---

## 4. Farmer App Structure

The codebase is organized under `app/src/main/java/com/example/`:

```text
app/src/main/java/com/example/
├── MainActivity.kt                      # Main Compose entrypoint and navigation host
├── RuralLinkChatActivity.kt             # Chat activity integration
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt               # Room database configuration
│   │   └── FarmerDaos.kt                # DAOs for Farmer, Request, Notification
│   └── model/
│       ├── Farmer.kt                    # Farmer entity & data model
│       ├── TransportRequest.kt          # Booking request entity & data model
│       ├── Transporter.kt               # Transporter profile data model
│       ├── TransporterResponse.kt       # Transporter quote and pricing model
│       ├── Notification.kt              # NotificationItem entity & model
│       └── AiRecommendation.kt         # AI scoring breakdown model
├── domain/
│   └── PricingEngine.kt                 # Fare calculation, backload discounts, savings
├── services/
│   ├── FarmerServices.kt                # AuthService, RequestService, ProfileService
│   ├── TransporterResponseService.kt    # Response fetching, demo seeding, Firestore/Supabase sync
│   ├── SupabaseClientConfig.kt          # URL, Publishable Key, headers, rate limiter
│   ├── SupabaseClient.kt                # Official Jan Supabase SDK client instance
│   ├── SupabaseService.kt               # HTTP/PostgREST client, retries, session persistence
│   ├── FirebaseService.kt               # Firebase Phone Auth & Firestore collections
│   ├── GeminiApiService.kt              # Gemini API REST client
│   ├── AgriMatchAiService.kt            # AI scoring & ranking algorithms
│   ├── AiExtractionService.kt           # Natural language voice parsing into booking fields
│   ├── SpeechRecognitionService.kt      # Android speech recognizer wrapper
│   └── RecommendationService.kt         # Heuristic recommendation engine
└── ui/
    ├── components/
    │   ├── CommonComponents.kt          # Reusable buttons, OTP inputs, status badges
    │   ├── VoiceBookingOrb.kt           # Animated mic orb for voice input
    │   ├── VoiceBookingDialog.kt        # Voice booking confirmation dialog
    │   ├── TransporterFeatures.kt       # Transporter feature list components
    │   └── AgriMatchRecommendationCard.kt # AI matching badge and breakdown card
    ├── navigation/
    │   └── NavGraph.kt                  # App navigation routes and screen transitions
    ├── screens/
    │   ├── WelcomeScreen.kt             # Landing screen
    │   ├── FarmerRegisterScreen.kt      # Registration form with demo quick-fills
    │   ├── FarmerVerifyScreen.kt        # OTP verification screen
    │   ├── TwoFactorAuthScreen.kt       # 2FA security verification screen (demo: 999999)
    │   ├── FarmerDashboardScreen.kt     # Dashboard with metrics, active bookings, profile switch
    │   ├── BookTransportScreen.kt       # Transport booking screen (manual and voice)
    │   ├── RequestDetailScreen.kt       # Booking details, quotes, QR payment simulation
    │   ├── RequestsListScreen.kt        # All past and ongoing requests
    │   ├── NotificationsScreen.kt       # User notification feed
    │   ├── ProfileScreen.kt             # Farmer profile view and edit
    │   ├── AgriMatchScreen.kt           # AI transporter comparison screen
    │   └── SupabaseDiagnosticsScreen.kt # Live Supabase health and connectivity test
    └── theme/
        ├── Color.kt                     # Emerald Green and brand palette
        ├── Theme.kt                     # Material 3 dynamic theme definition
        └── Type.kt                      # Typography scales
```

---

## 5. Transporter App Structure

The companion Transporter App is maintained at:
`https://github.com/Aayush0Murumkar/ruralfarmerlinkpartner/tree/main`

Both apps interact through the same backend data models:
- When a farmer creates a booking in the Farmer App, a row is inserted into `transport_requests`.
- The Transporter App displays these requests to available drivers in the relevant geographic cluster.
- When drivers respond with a quote, records are created in `transporter_responses`, which then appear in the Farmer App for comparison and AI scoring.

---

## 6. Supabase Backend

### Active PostgreSQL Tables

| Table Name | Purpose | Important Fields | Accessed By |
|---|---|---|---|
| `farmers` | Farmer profiles | `id` (UUID), `name`, `address`, `mobile_number`, `email`, `aadhaar_masked`, `created_at` | Farmer App (Write/Read) |
| `transport_requests` | Logistics booking requests | `id` (Text/UUID), `farmer_id` (UUID FK), `pickup_location`, `drop_location`, `material_type`, `weight_quintals`, `required_date`, `required_time`, `status`, `payment_status`, `selected_transporter_id`, `created_at` | Farmer App (Write/Read), Transporter App (Read/Update) |
| `transporters` | Registered vehicle operators | `id`, `name`, `mobile_number`, `vehicle_type`, `capacity_tons`, `rating`, `verified`, `created_at` | Transporter App (Write), Farmer App (Read) |
| `transporter_responses` | Quotes submitted by transporters | `id`, `request_id`, `transporter_id`, `transporter_name`, `price_quote`, `vehicle_type`, `status`, `matching_score`, `created_at` | Transporter App (Write), Farmer App (Read) |
| `notifications` | Alerts and status updates | `id`, `farmer_id`, `title`, `message`, `related_request_id`, `read`, `created_at` | Farmer App (Write/Read) |

### Row Level Security (RLS) Policies
In `SupabaseService.kt`, diagnostic SQL scripts ensure tables have RLS enabled with permissive policies for authenticated and anon users during prototype testing:
```sql
ALTER TABLE public.transport_requests ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow public inserts to transport_requests"
ON public.transport_requests FOR INSERT TO authenticated, anon WITH CHECK (true);

CREATE POLICY "Allow public select on transport_requests"
ON public.transport_requests FOR SELECT TO authenticated, anon USING (true);
```

---

## 7. Authentication

The application currently supports a hybrid authentication workflow:

1. **Supabase GoTrue (Email / Password):**
   - Registered through `SupabaseService.signUp` and `signIn`.
   - Access and refresh tokens are persisted in `SharedPreferences`.
2. **Firebase Phone Authentication (SMS OTP):**
   - Initiates SMS delivery via `PhoneAuthProvider.verifyPhoneNumber`.
   - Verified via `signInWithCredential`.
3. **Demo & Mock Authentication (Local Resilience):**
   - Pre-configured demo farmers (e.g., Ramesh Patil, Suresh Deshmukh, Anand Shinde) allow instant testing without live SMS quotas.
   - Initial application startup seeds a default session if no local profile exists.
4. **Two-Factor Authentication (2FA Mock):**
   - Dedicated `TwoFactorAuthScreen` simulates multi-factor authenticator verification.
   - For demo purposes, verification code `999999` is configured as the valid input.

---

## 8. Farmer → Transporter Workflow

1. **Request Creation:**
   - Farmer enters booking details via form or records voice prompt via `VoiceBookingOrb`.
   - Voice transcript is parsed into fields (`pickup`, `drop`, `material`, `weight`, `date`, `time`).
2. **Persistence & Dual-Sync:**
   - Saved immediately to local Room Database (`RequestDao`) with status `SEARCHING`.
   - Asynchronously dispatched to Supabase table `transport_requests` and Firebase collection `transport_requests`.
3. **Transporter Quotes:**
   - Transporter responses are received from `transporter_responses` (or seeded with realistic local demo transporters for offline testing).
4. **AI-Assisted Evaluation:**
   - `AgriMatchAiService` computes suitability scores based on vehicle capacity, distance, route compatibility, and backload availability.
   - Price calculations via `PricingEngine` show transparent breakdowns of base price, shared capacity rate, backload discount, platform fee, and farmer savings.
5. **Selection & Payment:**
   - Farmer selects the preferred transporter.
   - Request transitions to `TRANSPORTER_SELECTED`.
   - Farmer completes UPI QR code scan simulation, updating payment status to `PAID`.
   - Ride status advances to `IN_TRANSIT` and ultimately `DELIVERED`.

---

## 9. Realtime

- **SDK Configuration:** `SupabaseClient.kt` installs the official Realtime plugin (`install(Realtime)`), and `SupabaseClientConfig.kt` resolves the `wss://` WebSocket endpoint.
- **Current UI Connection State:** Realtime streaming is currently configured at the service/client layer. The UI layer currently relies on Room Kotlin Flow observables (`collectAsStateWithLifecycle`) and manual sync calls rather than open Realtime broadcast subscriptions. Wiring live Postgres change listeners directly into Compose ViewModels is an upcoming roadmap milestone.

---

## 10. Current Development Status

| Feature / Module | Status | Notes |
|---|---|---|
| **Farmer App UI & Navigation** | `DONE` | Complete Jetpack Compose implementation with Material 3 theming. |
| **Local Room Database** | `DONE` | Offline-first persistence for farmers, requests, and notifications. |
| **Manual Booking Form** | `DONE` | Validated input with pickup/drop locations, weight, and date picker. |
| **Voice-Assisted Booking** | `DONE` | Speech recognition + AI field extraction with review dialog. |
| **AI Recommendation Engine** | `DONE` | Multi-parameter scoring (route, vehicle type, rating, backload). |
| **Pricing Engine & Transparency** | `DONE` | Prorated shared load, backload discount, and platform fee breakdown. |
| **QR Code Payment Simulation** | `DONE` | Scan-to-pay mock flow with confirmation screen. |
| **Supabase PostgREST Sync** | `DONE` | HTTP client with retries, exponential backoff, and rate-limit tracking. |
| **Supabase Client SDK Setup** | `DONE` | Configured with Postgrest, Auth, Realtime, Storage plugins. |
| **Supabase Diagnostics Screen** | `DONE` | UI tool to inspect latency, headers, and test table inserts. |
| **Two-Factor Authentication (2FA)** | `DEMO/TEMPORARY` | Mock authenticator verification screen (accepts code 999999). |
| **Phone OTP (SMS)** | `PARTIAL` | Firebase Phone Auth implemented; demo fallback bypass included for quota resilience. |
| **Supabase Realtime UI Sync** | `IN PROGRESS` | SDK configured; WebSocket channels not yet wired to Compose UI. |
| **Firebase Migration to Supabase** | `IN PROGRESS` | Supabase is active; Firebase retained transitionally to prevent breaking partner app. |
| **Transporter App** | `IN PROGRESS` | Maintained in separate repository by team member. |

---

## 11. Important Files

- `app/src/main/java/com/example/services/FarmerServices.kt`: Core service facade orchestrating authentication, profile management, and transport request creation across Room, Supabase, and Firebase.
- `app/src/main/java/com/example/services/SupabaseService.kt`: Comprehensive HTTP PostgREST and GoTrue client with rate limiting, exponential backoff, and session persistence.
- `app/src/main/java/com/example/services/SupabaseClientConfig.kt`: Resolves credentials from `BuildConfig`, `.env`, or fallbacks; manages API endpoints and headers.
- `app/src/main/java/com/example/services/SupabaseClient.kt`: Official Jan Supabase SDK client singleton.
- `app/src/main/java/com/example/services/FirebaseService.kt`: Handles Firebase Phone Auth and Cloud Firestore data synchronization.
- `app/src/main/java/com/example/domain/PricingEngine.kt`: Business logic calculating fares, backload discounts, platform fees, and savings.
- `app/src/main/java/com/example/ui/navigation/NavGraph.kt`: Central routing graph managing screen navigation and deep linking.
- `app/src/main/java/com/example/ui/screens/RequestDetailScreen.kt`: Primary quote comparison, pricing breakdown, and QR payment screen.
- `app/src/main/java/com/example/ui/screens/BookTransportScreen.kt`: Booking screen integrating manual and voice input flows.
- `app/src/main/java/com/example/data/local/AppDatabase.kt`: Room database definition.

---

## 12. Setup Instructions

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17 or higher
- Android SDK 35 (compileSdk 35, minSdk 26)

### Steps
1. **Clone the Repository:**
   ```bash
   git clone <repository_url>
   cd <repository_directory>
   ```

2. **Configure Environment Variables:**
   Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```
   Add your credentials to `.env`:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key
   SUPABASE_URL=https://your-project-id.supabase.co
   SUPABASE_PUBLISHABLE_KEY=your-supabase-publishable-anon-key
   ```
   *Note: If `.env` is unpopulated, fallback credentials in `SupabaseClientConfig.kt` allow immediate compilation and demo testing.*

3. **Build the Project:**
   ```bash
   gradle assembleDebug
   ```

4. **Run on Device or Emulator:**
   Deploy the generated APK or run directly through Android Studio.

---

## 13. Development Rules

1. **Protect Credentials:** Never commit `.env`, private keys, Firebase service account keys, or keystore files to source control.
2. **Prioritize Supabase:** New backend features, tables, or database schema additions must be implemented using Supabase.
3. **Preserve Firebase Safely:** Do not delete Firebase dependencies, `FirebaseService.kt`, or `google-services.json` until the partner app migration is fully validated.
4. **Preserve Offline Resilience:** Keep Room Database as the immediate data source for UI reads; sync remotely in the background to ensure offline operability in rural areas.
5. **Verify Before Committing:** Run `gradle assembleDebug` or `compile_applet` to confirm builds succeed before pushing.

---

## 14. Known Limitations

1. **Supabase Realtime Not Connected to UI:** While the Realtime SDK plugin is installed, UI screens currently poll Room Flow queries rather than subscribing directly to Postgres Realtime WebSocket change events.
2. **Firebase Phone Auth SMS Quota:** Real SMS delivery depends on Firebase Spark/Blaze plan SMS quotas. For offline or local testing, use the built-in mock OTP flow.
3. **Companion Transporter App Sync:** True end-to-end integration between Farmer and Transporter requires both apps to point to the identical Supabase project instance with active RLS permissions.
