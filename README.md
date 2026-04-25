# BudgetControl

Personal finance Android app for tracking expenses, incomes, and multi-currency accounts with live FX rates.

## Screenshots

Screenshots are available in [`docs/`](docs/).

## Tech Stack

- Kotlin + Jetpack Compose + Material 3
- Room v16 (local persistence, schema migrations)
- Hilt (dependency injection)
- Retrofit + OkHttp + Gson (CERPS & Gemini APIs)
- Clean Architecture + MVVM (domain / data / feature layers)

## Features

- Track expenses and incomes across multiple accounts
- Multi-currency accounts with EUR-based local conversion
- Account groups with atomic member management
- Live exchange rates via CERPS backend (in-memory → DataStore → API cache)
- Historical rate charts with LTTB downsampling
- Analytics dashboard (`1D` / `7D` / `30D` / `90D` / `180D`)
- Categories with custom colors and icons
- Bank commission applied at the ViewModel layer
- AI-assisted bank commission lookup via Gemini
- Onboarding flow with default banks and categories seeded on first launch

## Getting Started

### Prerequisites

- Android Studio (Hedgehog or newer)
- Android device or emulator running API 26+
- JDK 11

### Setup

1. Clone the repository:
   ```bash
   git clone <repo-url>
   cd BudgetControl
   ```

2. Create `local.properties` in the project root with the CERPS endpoints:
   ```properties
   # Gemini API key is not needed — handled by CERPS backend
   cerps.base.url=https://supportive-vision-production.up.railway.app/
   cerps.analytics.url=https://sparkling-curiosity-production-9ffd.up.railway.app/
   ```

3. Open the project in Android Studio, let Gradle sync, and run on a device or emulator.

## Related Repositories

- **CERPS backend** — currency & analytics services: https://github.com/MikgasH/CERPS
- **Documentation** — diploma write-up and API specs: https://github.com/MikgasH/BudgetControl-docs

