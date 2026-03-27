# GOODOK Messenger

A modern messenger application with real-time messaging, voice/video calls, and premium features.

## Features

- **Real-time Messaging**: Send and receive messages instantly
- **4 Design Themes**: Classic, Modern, Neon, Drawn by a child
- **9 Languages**: English (US/UK), French, Spanish, Portuguese, Chinese, Belarusian, Ukrainian, Russian, German
- **Calls History**: View your call history with Minsk timezone
- **Channels**: Create and subscribe to channels
- **Contacts**: Import and manage contacts
- **Premium Plans**:
  - BASIC ($2/month or $6 forever): No ads, larger file uploads, priority support
  - GOODPLAN ($5/month or $15 forever): All BASIC features + call recording, auto-translate, VPN detection

## Building

1. Clone the repository
2. Add your `google-services.json` file to `app/` directory
3. Run `./gradlew assembleDebug`

## Tech Stack

- Kotlin
- Firebase Auth & Realtime Database
- Room Database
- Material Design Components
- Coroutines & Flow
