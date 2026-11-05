# Database Schema

## Local SQLite

Database: `pocket_ledger.db` (schema version 1). Money is stored as integer **minor units**
(poisha) in `*_minor` columns. Timestamps are UTC ISO-8601 strings with millisecond precision.
Soft deletes use `deleted = 0/1`.

| Table | Purpose | Key columns |
| --- | --- | --- |
| `profiles` | One row per local account | `id` (= user ID), `user_id` unique, `email`, `full_name` |
| `categories` | Income/expense categories | `id`, `user_id`, `name`, `type`, `icon`, `color`, `is_default`, `is_food` |
| `meal_settings` | Per-user meal prices | `morning_minor`, `lunch_minor`, `dinner_minor`, `auto_generate` |
| `meal_entries` | Daily meal plan | `meal_date`, `meal_type`, `amount_minor`, `state` |
| `transactions` | Income/expense records | `type`, `category_id`, `amount_minor`, `occurred_at`, `meal_entry_id` |
| `loans` | Money borrowed or lent | `direction`, `person_name`, `principal_minor`, `loan_date` |
| `loan_repayments` | Repayments against a loan | `loan_id`, `amount_minor`, `repayment_date` |
| `user_settings` | Theme and preferences | `theme`, `notifications_enabled`, `auto_sync_enabled`, `last_sync_at` |
| `sync_metadata` | Pull cursor per table | `user_id`, `table_name`, `last_pulled_at` |
| `sync_queue` | Pending local changes | `table_name`, `record_id`, `attempts`, `last_error` |
| `sync_conflicts` | Conflict audit log | `table_name`, `record_id`, `local_payload`, `server_payload` |

Constraints and indexes:

- Foreign keys are enabled; `profiles` is the local account root and all user tables cascade from it.
- `transactions.category_id` and `transactions.meal_entry_id` are `ON DELETE SET NULL`.
- `loan_repayments.loan_id` is `ON DELETE CASCADE`.
- Unique active rows: category name per user, meal type per user/date, and transaction per meal entry.
- Query indexes cover user/date, user/type, category, loan, and repayment lookups.
- DAO upserts update the existing row and only insert when absent. This avoids SQLite
  `INSERT OR REPLACE` deleting and re-inserting rows, which would trigger cascades.

## Cloud PostgreSQL (Supabase)

The cloud schema mirrors the local tables with these differences:

- IDs are `uuid`; `user_id` references `auth.users(id)`.
- Money is `numeric(12,2)` (`morning_price`, `lunch_price`, `dinner_price`, `amount`, `principal`).
- Booleans are real `boolean` columns (`deleted`, `is_default`, `is_food`, `auto_generate`, ...).
- Dates are `date`; timestamps are `timestamptz`.
- `updated_at` is set by a server trigger, so the server clock wins during conflict detection.
- Partial unique indexes match the local active-row uniqueness rules.
- RLS restricts every row to `auth.uid() = user_id`.

The full DDL is in [`supabase/migrations/0001_init.sql`](../supabase/migrations/0001_init.sql).

## Money and dates

- `Money` parses and formats BDT, stores minor units, and uses `BigDecimal` with `HALF_UP`.
- `DateUtils` uses `Asia/Dhaka` for local day boundaries and `en-BD` for display.
- A transaction's `occurred_at` is an instant; reports group by its Dhaka local date.
- Meal `meal_date` and loan dates are stored as `yyyy-MM-dd` local dates.

## Default seed data

Default categories are inserted per user with deterministic IDs. Default meal prices are
`৳27.50` (morning), `৳55.00` (lunch), `৳55.00` (dinner) — `৳137.50` per day and `৳4,125.00`
for 30 days.
