# JMODS Architecture

This document outlines the modern, modular Clean Architecture implemented for the JMODS app store.

## Core Principles

- **Clean Architecture**: Strict separation of concerns into Data, Domain, and Presentation layers.
- **Modularization**: High-cohesion, low-coupling modules to improve build times and maintainability.
- **Offline-First**: Reliable access to app metadata using local persistence (Room).
- **Reactive UI**: State-driven UI built with Jetpack Compose and Kotlin Flow.

## Tech Stack

- **UI**: Jetpack Compose, Material 3
- **Dependency Injection**: Hilt
- **Persistence**: Room (SQL), DataStore (Preferences)
- **Network**: OkHttp, Kotlinx Serialization
- **Image Loading**: Coil 3
- **Navigation**: Type-Safe Navigation (Compose)
- **Background Tasks**: WorkManager

## Module Structure

The project is divided into several logical layers and modules within `jmods-android/`:

### 1. Presentation Layer (Features & UI)

- **:jmods:core-ui**: Shared design system. Contains the `JMODSTheme`, common Compose components (e.g., `AppCard`, `JModsTopBar`), and UI utilities.
- **:jmods:feature-home**: Discovery screen. Displays categorized app lists and hero sections.
- **:jmods:feature-details**: App details screen. Shows descriptions, versions, and installation options.
- **:jmods:feature-categories**: Category browsing screen.

### 2. Domain Layer (Business Logic)

- **:jmods:core-domain**: The heart of the app. Contains:
    - **Models**: Plain Kotlin classes representing the business entities (e.g., `App`).
    - **UseCases**: Granular business logic (e.g., `GetAppsUseCase`, `GetAppDetailsUseCase`).
    - **Repository Interfaces**: Definitions for data operations.

### 3. Data Layer (Implementation)

- **:jmods:core-data**: Implements Repository interfaces. Orchestrates between local and remote sources using a "cache-then-network" strategy.
- **:jmods:core-database**: Room-based local persistence. Stores app metadata for offline access and performance.
- **:jmods:core-network**: Remote data source.
- **:jmods:core-auth**: Authentication management.
- **:jmods:core-installer**: Handles APK installation and uninstallation logic.

### 4. Infrastructure

- **:jmods:core-navigation**: Defines type-safe routes and destination models.
- **:jmods:app**: The main entry point for the Android application.

## Web Storefront

The repository also includes a high-performance web storefront built with **Next.js 16**, **React 19**, and **Tailwind CSS 4**, located in the `app/` directory.
