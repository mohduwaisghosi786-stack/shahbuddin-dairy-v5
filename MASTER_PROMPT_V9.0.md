# ==============================================================================
# SHAHBUDDIN DAIRY V9.0 — ULTIMATE MASTER ARCHITECTURE & IMPLEMENTATION PROMPT
# ==============================================================================
# Project: Shahbuddin Dairy Android Application (Native Jetpack Compose / Kotlin)
# Target Hardware: Realme C55 & Modern Android Smartphones (Portrait Display Optimized)
# Architecture: Clean Architecture + MVVM + Room Database + Jetpack Compose Material 3
# Zero Data Loss Guarantee: Preserve 100% existing SQLite/Room schema and transactional records.
# ==============================================================================

### 🎯 OBJECTIVE & SUMMARY
Transform the Shahbuddin Dairy Android application into a high-speed, uncluttered, professional dairy distribution and ledger system. Solve critical UX pain points on the Daily Task screen (eliminate excessive scrolling, enlarge cash touch targets, allow inline editing of dues and advances), introduce a comprehensive Dashboard tab, add intuitive 4-way gesture navigation for field delivery, redesign the daily PDF report for smartphone portrait displays, and provide a secure, delayed 30-second factory reset mechanism for app sharing.

---

## 🏛️ MODULE 1: NEW FIRST TAB — "DASHBOARD" & BOTTOM NAVIGATION RESTRUCTURING

### 1.1 Bottom Navigation Bar
Update bottom navigation to 6 clear tabs:
1. **Dashboard** (`Icons.Default.Dashboard` / `GridView`) — Pehla tab (Home)
2. **Daily Task** (`Icons.Default.Today`) — Fast delivery view (clutter-free)
3. **Milk Sale** (`Icons.Default.LocalDrink`) — Daily counter sales
4. **Daily Report** (`Icons.Default.Assessment`) — Daily summary & reports
5. **Customer Khata** (`Icons.Default.People`) — Customer directory & ledger
6. **Total Hisab** (`Icons.Default.Calculate`) — Comprehensive accounting & settings

### 1.2 Dashboard Screen Design & Content
Create `DashboardScreen.kt` and `DashboardViewModel.kt` featuring:
- **Hero Metrics Strip:**
  - Today's Milk Dispatched (Total Litres)
  - Today's Total Billed Amount (₹)
  - Today's Cash Collected (₹)
- **Delivery Progress Gauge:**
  - Visual circular/linear progress indicator: Delivered vs Pending vs Naga (Skipped).
- **Financial Health Summary:**
  - Total Market Due (₹ Bazaar ka baaki udhar)
  - Total Advance Balance Held (₹)
  - Net Outstanding
- **Route Breakdown Cards:**
  - Progress card for each route (e.g. "Main Route: 35/45 Delivered").
- **Quick Action Triggers:**
  - "Start Delivery" (jumps to Daily Task)
  - "Record Cash Payment"
  - "Download Today's PDF"

---

## ⚡ MODULE 2: DAILY TASK SCREEN OVERHAUL (ZERO-SCROLL, FULL CUSTOMER VISIBILITY)

### 2.1 Screen Clutter Elimination (Reclaim 60% Screen Space)
- **Compact Top Bar:** Remove the heavy multi-line cards (`Today's Milk`, `Cash Collected`, `Route Progress`) from Daily Task (these are now on the Dashboard). Replace with an ultra-thin single line indicator (Date + Route + Remaining count).
- **Tool Overflow Menu:** Consolidate secondary buttons (`Order`, `Calc`, `+Quick`, `Routes`) into a single compact `⚡ Tools / Quick Actions` icon button.
- **Unified Filter Bar:** Merge search and status filters (`All`, `Pending`, `Done`, `Naga`, `Alternate`) into a single sleek row.

### 2.2 Customer Delivery Card Redesign
Ensure the entire customer card fits cleanly on the screen without scrolling:
1. **Prominent NAGAD CASH Input:**
   - Double-height input field with clear green accents and large typography.
   - Immediate keypad opening on tap for effortless field entry.
2. **Inline Editable PREV DUE (₹):**
   - Tapping `PREV DUE` opens an inline quick-edit modal to reconcile/adjust previous dues directly, persisting changes to Room DB immediately.
3. **Inline Editable ADVANCE (₹):**
   - Tapping `ADVANCE` opens an adjustment dialog to credit/debit customer advance balances.
4. **Delivery State Permanence:**
   - When "Deliver" is tapped, the card visually updates to a clear "✔ DELIVERED" state and remains visible instead of disappearing unexpectedly.
5. **Alternate Day Indicator:**
   - Prominent badge for customers marked for alternate-day delivery.
6. **Clutter Removal:**
   - Completely remove unused placeholders: "Additional book", "Previous Pending 0", and monthly rent clutter.

---

## 👆 MODULE 3: 4-WAY GESTURE DELIVERY NAVIGATION

Implement fluid swipe gestures on the active customer card with haptic feedback:
- **Swipe RIGHT (👉):** **Quick Deliver & Next** — marks milk delivered with entered/default quantity, saves cash, triggers light haptic vibration, and smoothly scrolls to the next pending customer.
- **Swipe LEFT (👈):** **Quick Naga (Skip)** — marks customer as skipped/naga for the day and advances.
- **Swipe UP (👆):** Move to **Next Customer** without modifying status.
- **Swipe DOWN (👇):** Move to **Previous Customer** to review/edit earlier entries.

---

## 📱 MODULE 4: REALME C55 PORTRAIT-OPTIMIZED DAILY REPORT PDF

Redesign `PdfReportGenerator.kt` for mobile screen aspect ratios:
- **Page Dimensions & Aspect Ratio:**
  - Switch from standard horizontal/A4 print dimensions to smartphone portrait proportions (optimized for viewing on mobile screens like Realme C55 without horizontal panning or pinch-zoom).
- **Table Column Hierarchy:**
  1. `# ID / Customer Name` (Bold, prominent)
  2. `Mohalla / Area`
  3. `Milk (L)` & `Rate (₹)`
  4. `Today Bill (₹)`
  5. `Cash Paid (₹)`
  6. **`PREV DUE (₹)`** *(Dedicated new column)*
  7. **`ADVANCE BALANCE (₹)`** *(Dedicated new column)*
  8. `Status` (Delivered / Naga / Pending)
- **Header & Footer:**
  - Compact top header summarizing Date, Total Dispatched Litres, Total Bill, Total Cash Collected, and Pending Deliveries count.

---

## 🛡️ MODULE 5: 30-SECOND DELAYED FACTORY RESET (SAFE APP SHARING)

Provide a bulletproof data wipe feature in Total Hisab / Settings:
- **Target Function:** "Format / Reset All App Data" (for handover or fresh deployment).
- **Safety Protocol:**
  1. **Pre-Reset Auto Backup:** Automatically dumps full SQLite/Room database to a JSON backup file in device Downloads (`ShahbuddinDairy_AutoBackup_PreFormat_<Timestamp>.json`).
  2. **Confirmation Modal:** Asks user to explicitly type `FORMAT` or `CONFIRM`.
  3. **30-Second Countdown Enforcer:** The "Permanently Erase Everything" button remains completely disabled (greyed out) for **30 seconds**, displaying a real-time countdown `(Wait 30s... 29s... 0s)`.
  4. **Post-Reset Reinitialization:** Wipes transactional tables, resets IDs, and restarts app into clean state with demo routes.

---

## 🔒 DATA INTEGRITY CONSTRAINTS
- **Zero Data Loss:** Existing tables (`customers`, `deliveries`, `payments`, `routes`, `app_settings`) must retain all records without dropping tables.
- **Room Migration:** Increment database version cleanly with structured migration SQL scripts (`ALTER TABLE` / default values).
