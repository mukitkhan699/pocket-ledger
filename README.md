# Pocket Ledger

A native Android (Java + XML) personal finance app for students, with offline-first SQLite
storage, optional Supabase auth/cloud sync, hostel meal tracking, loans, reports, and
JSON/CSV backup. Currency is BDT (`৳`) only.

## Highlights

- Offline-first: every write is local and queued for sync; the app works without an account.
- Dashboard with balance, monthly income/expense, meal totals, and recent transactions.
- Transactions with search, type/category/date filters, and daily net headers.
- Hostel meals: morning/lunch/dinner, planned/consumed/skipped states, automatic daily generation,
  and automatic linked expense transactions for consumed meals.
- Loans borrowed/lent with repayment tracking and outstanding balances.
- Reports by day/week/month/year with bar chart and category breakdown.
- Categories, themes, notifications, auto-sync, conflict log, and backup/restore.
- Supabase sync with deterministic IDs, tombstones, conflict detection, and WorkManager retries.

## Quick start

1. Install prerequisites from [docs/BUILD_INSTRUCTIONS.md](docs/BUILD_INSTRUCTIONS.md).
2. Build: `./gradlew assembleDebug` (or use the Gradle command in the build doc).
3. Install `app/build/outputs/apk/debug/app-debug.apk`.
4. The app starts in local-only mode. To enable cloud sync, follow
   [docs/SUPABASE_SETUP.md](docs/SUPABASE_SETUP.md).

## Documentation

- [Build instructions](docs/BUILD_INSTRUCTIONS.md)
- [Supabase setup](docs/SUPABASE_SETUP.md)
- [Database schema](docs/DATABASE_SCHEMA.md)
- [Sync architecture](docs/SYNC_ARCHITECTURE.md)
- [Backup and restore](docs/BACKUP_RESTORE.md)

## Tests

```bash
./gradlew testDebugUnitTest
```

Unit tests cover money parsing/formatting, Dhaka date boundaries, and deterministic ID generation.
