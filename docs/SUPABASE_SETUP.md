# Supabase Setup

Pocket Ledger works fully offline. Cloud sync is optional and is enabled only when both
`SUPABASE_URL` and `SUPABASE_ANON_KEY` are configured.

## 1. Create the project

1. Create a project at <https://supabase.com>.
2. Open **Project Settings → API** and copy:
   - Project URL (for example `https://abcd1234.supabase.co`)
   - `anon` public key

Never use the `service_role` key in the Android app. The anon key is safe to ship only because
row-level security (RLS) restricts every table to the signed-in user.

## 2. Apply the schema

Open **SQL Editor**, paste the contents of
[`supabase/migrations/0001_init.sql`](../supabase/migrations/0001_init.sql), and run it.

The migration creates:

- `profiles`, `categories`, `meal_settings`, `meal_entries`, `transactions`, `loans`,
  `loan_repayments`, `user_settings`
- indexes used by the sync cursor and per-user lookups
- `updated_at` triggers (the server clock is authoritative)
- RLS policies where `auth.uid() = user_id`
- grants for the `authenticated` role only

With the Supabase CLI, the same migration can be applied with:

```bash
supabase link --project-ref YOUR_PROJECT_REF
supabase db push
```

## 3. Configure auth

1. **Authentication → Providers → Email**: enable Email.
2. For quick testing, disable **Confirm email**; otherwise confirm the address before logging in.
3. Optionally customise the confirmation/recovery email templates.

The app calls the Supabase Auth REST endpoints directly:

- sign up: `POST /auth/v1/signup`
- login: `POST /auth/v1/token?grant_type=password`
- refresh: `POST /auth/v1/token?grant_type=refresh_token`
- recovery: `POST /auth/v1/recover`
- logout: `POST /auth/v1/logout`

## 4. Configure the Android build

Add the values to `local.properties` (not committed):

```properties
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_ANON_KEY=YOUR_ANON_KEY
```

They are read at build time into `BuildConfig.SUPABASE_URL` and `BuildConfig.SUPABASE_ANON_KEY`.
Environment variables and Gradle properties with the same names are also supported.

Rebuild after changing them:

```bash
./gradlew assembleDebug
```

## 5. Verify RLS

After signing in, the app upserts a `profiles` row whose `id` and `user_id` both equal the auth
user ID, then syncs the remaining tables. In the Supabase table editor:

- each row's `user_id` should equal the signed-in user's ID
- a second test user must not be able to read the first user's rows

RLS is enabled on all eight tables. If a query returns `permission denied`, confirm the migration
completed and that the request carries the user's access token.

## 6. Realtime (optional)

The client currently uses periodic WorkManager sync and manual pull-to-refresh/sync actions.
Realtime is not required. To enable it later:

1. **Database → Replication** and add the eight tables to the `supabase_realtime` publication.
2. Add a `SupabaseRealtimeClient` that subscribes to postgres changes and calls
   `SyncManager.syncBlocking()` (or the async variant) after a change.

## Troubleshooting

| Symptom | Likely cause |
| --- | --- |
| `Sync is not configured` | URL/key missing from the build |
| `401` after login | Email not confirmed, or session expired and refresh failed |
| `permission denied for table ...` | Migration/grants not applied |
| `new row violates row-level security policy` | `user_id` does not match `auth.uid()` |
| Duplicate category/meal rows | A unique partial index rejected a second active row |
| Conflicts keep appearing | Two devices edited the same row; the newer server row wins and the local payload is logged under Manage → Conflicts |
