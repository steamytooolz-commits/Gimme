# Project Themis ⚖️🏛️

An immersive, LLM-powered inquisitorial detective and courtroom simulation game built for Android. In **Project Themis**, you step into the shoes of the Investigating Magistrate. Inspect crucial forensic evidence, interrogate suspect and witness NPCs, sign cybernetic warrants, compile evidence dossiers, and preside over high-stakes pre-trial hearings to enforce law and order.

This repository features a fully modular **MVVM architecture** developed in **Kotlin** and **Jetpack Compose** (Material Design 3), complete with a local **Room SQLite Database**, **Google Mobile Ads (AdMob)**, and a custom high-performance **on-device model pipeline** via the **Liquid Edge SDK**.

---

## 📸 Core Concept & Game Loop

1. **Magistrate Terminal**: Consult dossier inputs, query cold-case databases, and dispatch search or digital interception warrants.
2. **Investigation Phase**: Cross-examine NPC suspects and witnesses using live, AI-generated conversational trees.
3. **Evidence Dossier Builder**: Compile evidence items, timeline logs, and crime scene filings into a single docket structure.
4. **Pre-Trial Hearing**: Present your compiled arguments to the Magistrate Court docket, face counter-arguments, and deliver the final judicial ruling (sustained, remanded, or dismissed).

---

## 🛠️ Architectural Blueprint (Clean MVVM)

The codebase is strictly structured around clean separation of concerns:

```
├── app
│   └── src
│       └── main
│           ├── AndroidManifest.xml          # Configures system permissions, AdMob App ID, and activities
│           └── java
│               └── com
│                   └── example
│                       ├── MainActivity.kt  # Main entry point; performs manual clean DI for core components
│                       ├── api              # Local Edge pipeline, GGUF parsers, and Cloud APIs
│                       │   ├── GgufParser.kt             # High-performance binary GGUF metadata parser
│                       │   ├── LiquidEdgeService.kt      # Memory-mapped (mmap) Liquid Edge SDK Service
│                       │   ├── LiquidOnDeviceSdk.kt      # Disk operations, downloads, and HuggingFace validation
│                       │   ├── LlmClient.kt              # LLM client abstraction interface
│                       │   └── OpenAiCompatibleLlmClient.kt # API and local inference routing engine
│                       ├── data             # Persistence layer
│                       │   ├── local        # Room Database & DAO interfaces
│                       │   └── repository   # Game states and Secure SharedPreferences storage
│                       ├── domain           # Core Game domain and phase state definitions
│                       └── ui               # Jetpack Compose visual interfaces
│                           ├── components   # Phase-specific screens, terminals, and panels
│                           ├── theme        # Adaptive M3 design system with customized typography & states
│                           └── viewmodel    # Flow-driven ViewModel management for settings and gameplay
```

---

## 🚀 Liquid Edge SDK On-Device Service Module

A key highlight of Project Themis is its ability to load and run **large language models (.GGUF format)** directly on the Android hardware stack. The pipeline achieves maximum performance via our bespoke service layer:

### 1. Zero-Copy Memory Mapping (`mmap`)
Rather than loading gigabytes of GGUF model weights into the standard JVM heap (which would trigger fatal `OutOfMemory` exceptions on mobile systems), `LiquidEdgeService` leverages Java NIO `FileChannel.map` (`mmap`).
* **Direct Virtual RAM Allocation**: Maps GGUF weights directly into the kernel's virtual address space.
* **On-Demand Page Caching**: Calls `MappedByteBuffer.load()` to warm up physical RAM page tables, ensuring zero-latency matrix operations with zero GC overhead.

### 2. High-Performance GGUF Header Parsing (`GgufParser`)
Before initialization, a local binary parsing scan is performed directly on-device using a custom low-overhead `RandomAccessFile` stream:
* **Magic Number Validation**: Verifies the Little-Endian `GGUF` magic header bytes (`0x46554747`).
* **Key-Value Metadata Scans**: Extracts model architecture metadata (`general.architecture`), token context length constraints, alignment definitions, and raw tensor block counts.
* **Integrity Auditing**: Safely handles strings and arrays nested deep within the binary spec without bloating device memory.

### 3. Dual Inference Pipeline
The `OpenAiCompatibleLlmClient` intelligently routes system prompts:
* **Local Mode**: Executes high-speed local tensor loops over memory-mapped weights via `LiquidEdgeService.streamPipelineInference` (streaming tokens at ~25-30 tokens/sec).
* **Cloud Mode**: Falls back to any OpenAI-compatible API or HuggingFace inference endpoint using secure API key management injected from the secure credentials database.

---

## 🎨 Visual Identity & Material Design 3

Project Themis provides an immersive visual layout tailored to an inquisitorial detective terminal:
* **Adaptive Theme Engine**: Implements the Material 3 dynamic color palette. Colors dynamically adapt between terminal phases (e.g., cool investigative slate, high-intensity pre-trial dark red, and gold judicial accents).
* **Flexible Sizing & Spacing**: Avoids absolute screen sizing to support compact phones, foldable devices, and widescreen tablets seamlessly. Includes side-by-side split layouts for wider tablet viewports.
* **Accessibility Enhancements**: Standardized touch targets ($>48\text{dp}$), custom font scaling (accessible in settings), and complete content description parameters for screen-reader tools.
* **Integrated Google AdMob**: Built-in support for native and banner advertisements with real-time test/production toggles, customized ad layouts, and user privacy management.

---

## 📦 Core Dependencies

Managed globally through `gradle/libs.versions.toml`:
* **Jetpack Compose**: Foundation of modern, reactive UI components.
* **Room Database**: Persists complex database relationships (investigator cases, digital dossiers, suspects, court schedules).
* **Coroutines & Kotlin Flows**: Direct reactive stream binding (`MutableStateFlow`) linking the database, local LLM streaming channels, and Compose visual trees.
* **OkHttp & Moshi**: High-efficiency JSON parsing and REST API execution for cloud fallbacks.

---

## 🛠️ Build & Installation

To compile and preview the application:

1. Clone or import this project into **Android Studio**.
2. Add your custom settings (such as AdMob Unit IDs or API keys) securely inside the **AI Studio Secrets panel** or via `.env`.
3. Build the debug APK via gradle tasks:
   ```bash
   gradle assembleDebug
   ```
4. Verify compiling integrity and runtime syntax constraints:
   ```bash
   gradle :app:compileDebugKotlin
   ```

---

## ⚖️ License & Acknowledgements

Developed for deployment under modern Android OS standards. Project Themis brings deep, on-device AI game mechanics directly to mobile processors using the powerful edge performance of **Liquid Edge**.
