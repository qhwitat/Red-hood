# Ren ai 🔴

**Ren ai** is a high-performance, multi-provider AI assistant Android application built with Kotlin and Jetpack Compose. It features a striking "Deep Red" cyberpunk-inspired user interface, supporting ultra-low latency inference via multiple API backends.

## Features

- **Multi-Provider AI Matrix**: Connect effortlessly to models across Gemini, Groq, OpenRouter, or your own Custom API nodes.
- **Deep Red Immersive UI**: A bold, edge-to-edge dark theme (`#140505` surface, `#F44336` neon accents) designed for high contrast and developer ergonomics without distractions. 
- **System Persona Injection**: Granular control over the system prompt context, allowing you to fine-tune AI behavior and directives.
- **Dynamic Memory Bounding**: Choose between "Fixed Window" memory to save context tokens or "Infinite" memory for long context window streams.
- **Real-Time Data Streaming**: Rapid streaming responses mapped efficiently to UI components, optimized specifically for fast-inference backends like Groq.
- **Diagnostics & Telemetry**: Toggleable tactile feedback, synthetic operation pacing, and terminal-style telemetry metrics. 

## Technology Stack

- **Platform**: Android (Min SDK 24, Target SDK 36)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design Customization)
- **Networking**: Ktor / Retrofit & Coroutines Flow for continuous stream processing
- **Build System**: Gradle Kotlin DSL
- **CI/CD**: GitHub Actions workflow included for automated APK releases

## Getting Started

1. Clone or download the repository.
2. Open with Android Studio.
3. Sync the Gradle files.
4. Run `gradle assembleDebug` or install onto a connected device/emulator.

> **Note**: To utilize the inference backends, you must provide your own API Keys within the application's Connect panel (Groq API, OpenRouter API etc).

## License

This project is licensed under the MIT License.
