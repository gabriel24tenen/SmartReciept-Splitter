# SmartReceipet-Splitter

Android app that scans receipts with on-device OCR, extracts totals, dates, items and VAT, and splits costs — including tax and tips — among multiple people. All OCR processing happens entirely on-device; receipt images are never uploaded to a server.

## Features

- **Receipt scanning** — Integrated camera interface with automatic paper edge detection and perspective correction
- **On-device OCR extraction** — Automatically recognizes total amount, date, line items, and VAT/tax using Google ML Kit, with no image ever leaving the device
- **Multi-currency support** — Converts amounts to your home currency using the last saved exchange rates, fully offline
- **Bill & tax splitting** — Split a receipt among multiple people, with control over who pays which share of tax or tip
- **Report export** — Generate PDF and CSV expense reports ready for bookkeeping
- **Receipt history** — Store, search, filter, edit, and delete past receipts
- **Manual entry & correction** — Fix OCR mistakes or add a receipt entirely by hand
- **Expense categories** — Tag receipts with categories (food, travel, accommodation, fuel) for reporting
- **Spending analytics** — Visual breakdown of expenses by category and time period
- **People & groups** — Save frequent contacts, create groups, and track running balances/debts
- **Security** — Optional PIN lock to protect financial data
- **Backup & restore** — Export the database and receipt images to a file for easy migration to a new device

## Tech Stack

| Layer | Technology |
|---|---|
| Language & UI | Kotlin, Jetpack Compose (declarative UI) |
| Camera & OCR | CameraX, Google ML Kit Text Recognition |
| Local storage | Room Database with TypeConverters (currency, date) |
| Async | Kotlin Coroutines & Flow |
| DI | Hilt |
| Architecture | Clean Architecture (Data / Domain / UI) + MVVM |

## Architecture

The app follows Clean Architecture with a standard MVVM presentation layer:
data/ → Room entities, DAOs, repositories, OCR & exchange-rate data sources
domain/ → Use cases, domain models, business logic
ui/ → Jetpack Compose screens, ViewModels, navigation


State is exposed from ViewModels via Kotlin `Flow`, with Hilt providing dependency injection across all layers.

## Getting Started

### Prerequisites

- Android Studio (latest stable)
- JDK 17
- `minSdk 26`

### Setup

```bash
git clone https://github.com/<your-username>/SmartReciept-Splitter.git
```

Open the project in Android Studio and let Gradle sync. No API keys are required — OCR and camera processing run entirely on-device.

## Git Strategy

| Branch | Purpose |
|---|---|
| `master` | Stable releases |
| `dev` | Integration branch |
| `feature/*` | New functionality |
| `bugfix/*` | Bug fixes |
| `hotfix/*` | Urgent fixes on `master` |
| `release/*` | Release preparation |

## Roadmap

| Version | Scope |
|---|---|
| **1.0 (MVP)** | Camera scanning, OCR extraction, manual entry/correction, receipt storage, bill & tax splitting |
| **1.1** | Exchange rate management, multi-currency conversion, categories, settings, summary sharing, receipt photo storage |
| **1.5** | PIN security, backup/restore, improved scanning UI (flash, haptic feedback) |
| **2.0** | PDF/CSV export, spending analytics, people & group debt management |

See [Issues](../../issues) and [Milestones](../../milestones) for detailed task breakdown per version.

## License

_TBD_
