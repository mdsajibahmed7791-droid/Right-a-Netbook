# Notebook — Café Free-Item & Delivery Ledger

**Notebook** is a native Android application built with **Kotlin**, **Jetpack Compose (Material 3)**, **MVVM**, **Firebase Authentication**, and **Cloud Firestore** (with offline persistence enabled).

## Features

1. **Authentication & Roles**
   - Uses **Firebase Auth** (Google Sign-In via Jetpack Credential Manager) with reactive auth gating in `MainActivity`.
   - Role check: a signed-in user is **Admin** if their UID exists in `admins/{uid}` (or matches the verified owner email in `firestore.rules`). Everyone else is **Staff**.
   - Staff queries always filter by `uid` and only expose their own records; Admins can read across all staff.

2. **5 Staff Tabs + Admin Dashboard**
   - **Free (POS)**: 3-column category grid (`Food`, `Coffee`, `Tea`, `Juice`, `Cold Drinks`, `Mojito`, `Cake`), cross-category search, 2-column item cards with ~200x200 JPEG Base64 thumbnails or letter avatars, multi-select saved notes pop-up (per item, `scope = "free"`), floating pill bar above the bottom navigation, review sheet with quantity steppers, required `"Taken by"` dropdown / new-name auto-add, editable date & time, optional note, and recent entries with delete confirmation.
   - **Items**: Category chips, add item form with price validation & camera/gallery photo picker, item list with quick camera photo update and delete confirmation.
   - **People**: Add and delete regular recipients.
   - **Report**: Month picker, person filter (`Everyone` or specific person), summary tiles, category tiles, daily bar chart, sorted breakdowns by person and by item, and multi-page A4 PDF export.
   - **Delivery ("Other Shop Delivery")**: Completely separate price-free POS using `items + dlItems`, `scope = "delivery"` notes, date/time review sheet, delivery report (month pieces, today, delivery days, daily chart, per-day list, per-item totals), delivery-only items manager, and delivery PDF export.
   - **Admin (Admin only)**: Month picker, staff email filter, cross-staff summary tiles, per-staff table, loss-per-day & loss-per-category charts, read-only all-staff entries list, cross-staff delivery summary, admin master PDF export, and Staff/Role setup card.

3. **Multi-Language & RTL**
   - Instant in-app switcher for **English (`en`)**, **Bangla (`bn`)**, and **Arabic (`ar` with full RTL)**, persisted via Jetpack DataStore.
   - Currency formatted as `"25 SR"` with locale digits.

4. **A4 Multi-Page PDF Reports (`PdfDocument` + `Canvas`)**
   - Uses `StaticLayout` for accurate Bengali and Arabic glyph shaping and RTL alignment.
   - Includes a dark navy-to-teal glowing-wave header banner, 3 teal-accented summary boxes, rounded daily bar chart with values above bars, "By item" rows with rounded item thumbnails and proportional teal bars, zebra tables with repeated headers across page breaks, and `"Notebook • Page n / N"` footers on every page.
   - Exported through the Android system Save dialog (`CreateDocument`) or Share sheet (`FileProvider`).

## Firestore Collections

- `items/{id}`: `{ uid, cat, name, price, photo?, createdAt, updatedAt }`
- `people/{id}`: `{ uid, name, createdAt }`
- `entries/{id}`: `{ uid, item, base, cat, price, qty, total, person, ts, note?, createdAt }`
- `notes/{id}`: `{ uid, scope ("free"|"delivery"), itemId, text, createdAt }`
- `dlItems/{id}`: `{ uid, name, photo?, createdAt, updatedAt }`
- `dlEntries/{id}`: `{ uid, item, base, qty, ts, note?, createdAt }`
- `staff/{uid}`: `{ uid, email, displayName, updatedAt }`
- `admins/{uid}`: console-managed admin marker documents

## Instructions to Build an APK

### Prerequisites
- Android SDK (API 36, `minSdk = 26`), JDK 17+, and Gradle (or Android Studio).
- Valid `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml` (already provisioned in AI Studio).

### 1. Build Debug APK (Command Line)
```bash
gradle :app:assembleDebug
```
Output APK path:
`app/build/outputs/apk/debug/app-debug.apk`

### 2. Build Release APK / App Bundle
Configure your upload keystore environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) or use AI Studio's APK/AAB export menu, then run:
```bash
gradle :app:assembleRelease
# or for Play Store bundle:
gradle :app:bundleRelease
```
Output APK path:
`app/build/outputs/apk/release/app-release.apk`

### 3. Run Tests
- Firestore Security Rules unit tests (with Firestore Emulator on port 8085):
  ```bash
  FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
  ```
- Android JVM unit tests:
  ```bash
  gradle :app:testDebugUnitTest
  ```

## Firebase Console Notes
- **Granting Admin Access**: Create a document in Firestore at `admins/<USER_UID>` (e.g. `{ "role": "admin" }`). The user can copy their UID directly from the Admin tab or sign-in profile.
- **Optional Email/Password Auth for Standalone Deployments**: In AI Studio environments, Google Sign-In is provisioned automatically. If exporting to your own Firebase project and you prefer Email/Password login, enable the **Email/Password** sign-in provider in Firebase Console → Authentication → Sign-in method and create staff accounts there (or via a secondary `FirebaseApp` instance in an admin-only build).
