# JMODS — Private Android App Store

JMODS is a modern, privacy-focused Android app store. It enables you to search, discover, and install apps privately without tracking or the need for a Google account.

## Project Structure

This repository is a monorepo containing:

- **jmods-android/**: A modern Android application built with Jetpack Compose, Hilt, and Clean Architecture.
- **app/**: A high-performance web storefront built with Next.js 16 and React 19.

## Features

- **Privacy First**: No tracking, no ads, and no mandatory Google accounts.
- **Modern UI**: Clean Material 3 design on Android and a responsive Tailwind CSS 4 web interface.
- **Modular Architecture**: Built for maintainability and scalability.
- **Offline-First**: Metadata caching with Room persistence.
- **Privacy Audit**: Built-in tracker and permission analysis.

## Getting Started

### Android App
To build the Android application:
```bash
./gradlew :jmods:app:assembleDebug
```

### Web Storefront
To run the web storefront locally:
```bash
npm install
npm run dev # Runs on http://localhost:3001
```

## Architecture
See [ARCHITECTURE.md](ARCHITECTURE.md) for a detailed breakdown of the technical design.

## License
Licensed under the MIT License.
