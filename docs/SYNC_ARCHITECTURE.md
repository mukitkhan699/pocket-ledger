# Sync Architecture

## Goals

- Never block the UI on the network.
- Preserve all local changes when offline.
- Converge deterministically across devices without duplicating rows.
- Keep the local database usable without an account.

## Local write path

1. A repository writes the row to SQLite.
2. It enqueues `(table_name, record_id)` in `sync_queue` with a deterministic queue ID
   (`Ids.deterministic("queue:" + table + ":" + recordId)`), so repeated edits collapse into one
   queue entry.
3. The UI observes the local database immediately.
4. A sync is requested: immediate one-off work after mutations, plus a periodic 15-minute worker.

Deletes are **tombstones**: `deleted = 1` and a fresh `updated_at`. Rows are never hard-deleted
during normal use, so other devices can learn about the deletion.

## Deterministic IDs

| Entity | Seed |
| --- | --- |
| Category | `category:<userId>:<lowercase name>` |
| Meal entry | `meal:<userId>:<date>:<mealType>` |
| Meal-linked transaction | `meal-transaction:<mealEntryId>` |
| Meal settings | `meal-settings:<userId>` |
| User settings | `user-settings:<userId>` |
| Queue entry | `queue:<table>:<recordId>` |

Deterministic IDs make retries and cross-device creation idempotent.

## Push

`SyncManager.pushAll` processes `sync_queue` in creation order:

1. Read the **latest** local row for the queued record (not the payload captured at edit time).
2. Fetch the server row by `id`.
3. If the server `updated_at` is newer than local, record a conflict, apply the server row
   locally, dequeue, and continue. The server wins.
4. Otherwise upsert the local row with `Prefer: resolution=merge-duplicates,return=representation`.
5. Store the server-returned `updated_at` locally (the server trigger is authoritative) and dequeue.
6. On `401`, refresh the session once and retry. On `IOException`, stop and mark offline.
   On `5xx`/`429`, stop and retry on the next run. Other API errors are recorded on the queue item.

## Pull

Pull runs before push, in dependency order:

`profiles → categories → meal_settings → user_settings → meal_entries → transactions → loans → loan_repayments`

For each table:

1. Read the per-table cursor from `sync_metadata.last_pulled_at`.
2. Request `user_id = auth.uid()`, ordered by `updated_at.asc`, in pages of 500, with
   `updated_at > cursor` applied to **every** page.
3. For each row:
   - If the record is queued locally and the server row is not newer, keep the local edit.
   - If the record is queued and the server is newer, log a conflict, apply the server row,
     dequeue, and count the conflict.
   - Otherwise apply the row locally.
4. Apply with `UPDATE` first and `INSERT` only when the row is absent. This preserves foreign-key
   dependents (for example, updating a category does not null out transactions).
5. If an apply fails a foreign-key constraint (for example a transaction arrives before its
   category), defer it and retry after all tables are pulled. The cursor only advances when no
   rows were deferred, so nothing is skipped.

## Conflict policy

- **Server newer wins.** The local payload and server payload are stored in `sync_conflicts`.
- The UI exposes the conflict count and log under **Manage → Conflicts**.
- Push conflicts are also logged when a queued local row is older than the server row.

## Workers and lifecycle

- `SyncWorker`: periodic 15-minute sync, only when a non-local session exists and auto-sync is on.
  Retries on network/server failures.
- `MealGenerationWorker`: daily at 00:05 Asia/Dhaka. Creates the next day's planned meals and posts
  a notification when notifications are enabled.
- `WorkerScheduler` schedules both with WorkManager and can cancel sync on logout.
- Sync status is broadcast in-process (`ACTION_SYNC_STATUS`) and auth expiry in
  (`ACTION_AUTH_EXPIRED`) using `setPackage` to avoid leaking outside the app.

## Local-only mode

If `SUPABASE_URL`/`SUPABASE_ANON_KEY` are missing, the user can continue with a local account.
`SyncManager` returns `localOnly` or `notConfigured`, no network calls are made, and queued rows
remain available if the app is later configured and signed in.
