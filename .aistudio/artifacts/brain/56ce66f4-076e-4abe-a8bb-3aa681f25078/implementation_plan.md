# Implementation Plan — CodePad Studio Creator & IDE Powerhouse

Comprehensive architectural plan to enhance **CodePad Studio** into an all-in-one developer IDE and viral coding video recording engine, incorporating all confirmed capabilities while retaining and optimizing existing modules.

---

## 1. Executive Summary & Goals

The user requested all proposed capabilities across both editor productivity and creator tooling:
1. **Developer Productivity Suite:** Quick Symbol Accessory Toolbar (`<`, `>`, `{`, `}`, `(`, `)`, `"`, `'`, `;`, `=`, `/`, `!`, Tab, Undo, Redo), Auto-Closing Tags & Quotes Engine, In-Code Find & Replace panel, and Interactive CSS Color Picker.
2. **Creator & Media Powerhouse:** Pre-built Component & Snippet Store, Interactive Audio Acoustic Customizer (pitch, volume, switch decay), Local Code Revision History with rollback snapshots, and In-App Video Capture helper via Web MediaRecorder.
3. **Architecture & Optimization:** Zero-bloat local SQLite Room persistence, memory-safe progressive rendering, maintaining Android API 24–36 full compatibility.

---

## 2. Proposed Architecture & Feature Breakdown

### A. Quick Symbol Keyboard Accessory Bar & Auto-Closing Engine
* **Location:** Directly above the Android virtual keyboard in `CodePadStudioScreen.kt`.
* **Symbols Provided:** `< > { } ( ) [ ] " ' ; : = / ! $ # . /* */ ->` plus dedicated `[Tab]` (2 spaces) and Quick Undo/Redo.
* **Auto-Closing Rules:**
  - Typing `{`, `(`, `[`, `"`, `'`, or `` ` `` automatically inserts the closing counterpart and places the cursor right in between.
  - Typing `<tag>` automatically pairs with `</tag>` for common HTML container tags.
  - Backspace immediately behind an auto-inserted closer removes both characters cleanly.

### B. In-Code Find & Replace Panel
* **UI Location:** Collapsible search bar toggled via search icon in the top toolbar or shortcut.
* **Features:**
  - Case-sensitive / Regex toggle.
  - Live count indicator (`Match 2 of 14`).
  - Next / Previous match navigation buttons with auto-scrolling to selection.
  - "Replace Current" and "Replace All" actions with atomic undo tracking.

### C. Interactive CSS Color Picker Dialog & Quick Swatches
* **UI Location:** Accessible from the editor toolbar or when tapping color values in CSS.
* **Features:**
  - Real-time HSV/RGB sliders and Hex input (`#38bdf8`, `#6366f1`, `#f43f5e`, `#22c55e`).
  - One-tap curated Cyberpunk Neon, Pastel Glow, and Material 3 palettes.
  - Injects formatted `#hex` or `rgba(...)` code directly at cursor.

### D. Pre-built Component Library & Snippet Store
* **New Dedicated Sheet:** Accessible from Editor & More Hub (`ComponentStoreSheet.kt`).
* **Ready-to-Insert Snippets:**
  1. *Cyberpunk Glitch & Neon Button* (HTML + CSS + click animation)
  2. *Glassmorphism Metric Card* with backdrop-filter blur
  3. *Fluid Canvas Particles Effect* (Canvas + requestAnimationFrame)
  4. *Responsive Mobile Navbar* with Hamburger Drawer
  5. *Confetti Explosion Trigger* on button tap
  6. *Minimal Pomodoro Timer* with digital display
  7. *Interactive Crypto Live Ticker* card
* **Injection Strategy:** Can either append into current tabs or replace with a complete working preset.

### E. Typing Sound Acoustic Customizer
* **Location:** Dedicated Section in `MoreStudioScreen.kt` and `TypingSoundEngine.kt`.
* **Controls:**
  - **Pitch Modulation Slider:** Tune mechanical pitch from deep bass hollow thock (0.80x) to high-pitch tactile click (1.30x).
  - **Volume Gain:** 10% to 100% fine control with instant preview test key.
  - **Switch Housing Acoustics:** Toggle spacebar stabilizer resonance and natural finger pressure variation.

### F. Local Code Revision History & Rollback Snapshots
* **Database Extension:** New `@Entity` `CodeRevisionSnapshot` in Room database (`codepad_studio.db` v9).
* **Automatic & Manual Triggers:**
  - Manual "Save Snapshot" with custom tag (e.g., "Before CSS refactor").
  - Auto-snapshot upon switching templates or formatting code.
* **Rollback UI:** Timeline sheet showing timestamp, character count, and single-tap "Restore Snapshot" or "Inspect Diff".

### G. In-App Video & Canvas Capture Engine
* **Integration:** Injected Web MediaRecorder script inside `WebSandboxEngine.kt`.
* **Functionality:**
  - Record the `<canvas>` or web view output directly to a `.webm` / `.mp4` video blob.
  - Floating trigger in Auto Write Studio to record high-framerate video clips of animations without external screen recorder artifacts.

---

## 3. Implementation Steps & Verification Plan

### Step 1: Database & Data Models
- Add `CodeRevisionSnapshot` entity, DAO queries (`observeSnapshots`, `insertSnapshot`, `deleteSnapshot`), and migration in `CodePadDatabase.kt`.
- Add state holders and event handlers in `StudioViewModel.kt` for search/replace, symbol bar, color picker, and snippet library.

### Step 2: Editor Productivity Suite (`CodePadStudioScreen.kt`)
- Build `SymbolAccessoryBar` composable placed above soft keyboard with haptic feedback.
- Build `FindAndReplaceBar` with instant regex text scanning.
- Build `CssColorPickerDialog` with live preview chip.

### Step 3: Snippet Store & Audio Customizer
- Create `ComponentStoreSheet` with tabs for UI Components, Layouts, and Interactive Scripts.
- Add audio pitch/volume sliders in `MoreStudioScreen.kt` wired to `TypingSoundEngine`.

### Step 4: Verification & Compilation
- Run `compile_applet` to ensure zero compilation or build errors.
- Run `gradle :app:testDebugUnitTest` to verify unit tests for search, symbol insertion, auto-closing, and Room migrations.
- Verify smooth responsive layout across Android 7.0 (API 24) to Android 15 (API 36).
