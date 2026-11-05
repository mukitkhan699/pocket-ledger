-- Pocket Ledger cloud schema (Supabase / PostgreSQL)
-- Run this once in the Supabase SQL editor, or with `supabase db push`.
-- All money is stored as NUMERIC(12,2); the Android client converts to/from minor units.

create extension if not exists "pgcrypto";

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

-- ---------------------------------------------------------------------------
-- Tables
-- ---------------------------------------------------------------------------

create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    user_id uuid not null unique references auth.users(id) on delete cascade,
    email text,
    full_name text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted boolean not null default false,
    constraint profiles_id_user_id_match check (id = user_id)
);

create table if not exists public.categories (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    name text not null,
    type text not null check (type in ('income', 'expense')),
    icon text,
    color text,
    is_default boolean not null default false,
    is_food boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted boolean not null default false
);

create table if not exists public.meal_settings (
    id uuid primary key,
    user_id uuid not null unique references auth.users(id) on delete cascade,
    morning_price numeric(12,2) not null default 27.50 check (morning_price >= 0),
    lunch_price numeric(12,2) not null default 55.00 check (lunch_price >= 0),
    dinner_price numeric(12,2) not null default 55.00 check (dinner_price >= 0),
    auto_generate boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted boolean not null default false
);

create table if not exists public.meal_entries (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    meal_date date not null,
    meal_type text not null check (meal_type in ('MORNING', 'LUNCH', 'DINNER')),
    amount numeric(12,2) not null default 0 check (amount >= 0),
    state text not null default 'PLANNED' check (state in ('PLANNED', 'CONSUMED', 'SKIPPED')),
    notes text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted boolean not null default false
);

create table if not exists public.transactions (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    type text not null check (type in ('income', 'expense')),
    category_id uuid references public.categories(id) on delete set null,
    description text,
    notes text,
    amount numeric(12,2) not null check (amount > 0),
    occurred_at timestamptz not null default now(),
    meal_entry_id uuid references public.meal_entries(id) on delete set null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted boolean not null default false
);

create table if not exists public.loans (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    direction text not null check (direction in ('BORROWED', 'LENT')),
    person_name text not null,
    principal numeric(12,2) not null check (principal > 0),
    loan_date date not null,
    description text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted boolean not null default false
);

create table if not exists public.loan_repayments (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    loan_id uuid not null references public.loans(id) on delete cascade,
    amount numeric(12,2) not null check (amount > 0),
    repayment_date date not null,
    notes text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted boolean not null default false
);

create table if not exists public.user_settings (
    id uuid primary key,
    user_id uuid not null unique references auth.users(id) on delete cascade,
    theme text not null default 'dark' check (theme in ('system', 'light', 'dark')),
    notifications_enabled boolean not null default true,
    auto_sync_enabled boolean not null default true,
    last_sync_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted boolean not null default false
);

-- ---------------------------------------------------------------------------
-- Indexes
-- ---------------------------------------------------------------------------

create index if not exists ix_categories_user_type on public.categories(user_id, type, deleted);
create unique index if not exists ux_categories_user_name on public.categories(user_id, name) where deleted = false;

create index if not exists ix_meal_entries_user_date on public.meal_entries(user_id, meal_date, deleted);
create unique index if not exists ux_meal_user_date_type on public.meal_entries(user_id, meal_date, meal_type) where deleted = false;

create index if not exists ix_transactions_user_date on public.transactions(user_id, occurred_at, deleted);
create index if not exists ix_transactions_user_type on public.transactions(user_id, type, deleted);
create index if not exists ix_transactions_category on public.transactions(user_id, category_id, deleted);
create unique index if not exists ux_transactions_meal on public.transactions(meal_entry_id) where meal_entry_id is not null and deleted = false;

create index if not exists ix_loans_user on public.loans(user_id, direction, deleted);
create index if not exists ix_repayments_loan on public.loan_repayments(loan_id, deleted);
create index if not exists ix_repayments_user on public.loan_repayments(user_id, repayment_date, deleted);

create index if not exists ix_profiles_updated on public.profiles(user_id, updated_at);
create index if not exists ix_categories_updated on public.categories(user_id, updated_at);
create index if not exists ix_meal_settings_updated on public.meal_settings(user_id, updated_at);
create index if not exists ix_meal_entries_updated on public.meal_entries(user_id, updated_at);
create index if not exists ix_transactions_updated on public.transactions(user_id, updated_at);
create index if not exists ix_loans_updated on public.loans(user_id, updated_at);
create index if not exists ix_repayments_updated on public.loan_repayments(user_id, updated_at);
create index if not exists ix_user_settings_updated on public.user_settings(user_id, updated_at);

-- ---------------------------------------------------------------------------
-- updated_at triggers (server clock is authoritative for conflict detection)
-- ---------------------------------------------------------------------------

drop trigger if exists trg_profiles_updated on public.profiles;
create trigger trg_profiles_updated before insert or update on public.profiles
for each row execute function public.set_updated_at();

drop trigger if exists trg_categories_updated on public.categories;
create trigger trg_categories_updated before insert or update on public.categories
for each row execute function public.set_updated_at();

drop trigger if exists trg_meal_settings_updated on public.meal_settings;
create trigger trg_meal_settings_updated before insert or update on public.meal_settings
for each row execute function public.set_updated_at();

drop trigger if exists trg_meal_entries_updated on public.meal_entries;
create trigger trg_meal_entries_updated before insert or update on public.meal_entries
for each row execute function public.set_updated_at();

drop trigger if exists trg_transactions_updated on public.transactions;
create trigger trg_transactions_updated before insert or update on public.transactions
for each row execute function public.set_updated_at();

drop trigger if exists trg_loans_updated on public.loans;
create trigger trg_loans_updated before insert or update on public.loans
for each row execute function public.set_updated_at();

drop trigger if exists trg_loan_repayments_updated on public.loan_repayments;
create trigger trg_loan_repayments_updated before insert or update on public.loan_repayments
for each row execute function public.set_updated_at();

drop trigger if exists trg_user_settings_updated on public.user_settings;
create trigger trg_user_settings_updated before insert or update on public.user_settings
for each row execute function public.set_updated_at();

-- ---------------------------------------------------------------------------
-- Row Level Security: every row belongs to auth.uid()
-- ---------------------------------------------------------------------------

alter table public.profiles enable row level security;
alter table public.categories enable row level security;
alter table public.meal_settings enable row level security;
alter table public.meal_entries enable row level security;
alter table public.transactions enable row level security;
alter table public.loans enable row level security;
alter table public.loan_repayments enable row level security;
alter table public.user_settings enable row level security;

drop policy if exists profiles_select_own on public.profiles;
create policy profiles_select_own on public.profiles for select using (auth.uid() = user_id);
drop policy if exists profiles_insert_own on public.profiles;
create policy profiles_insert_own on public.profiles for insert with check (auth.uid() = user_id and id = auth.uid());
drop policy if exists profiles_update_own on public.profiles;
create policy profiles_update_own on public.profiles for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
drop policy if exists profiles_delete_own on public.profiles;
create policy profiles_delete_own on public.profiles for delete using (auth.uid() = user_id);

drop policy if exists categories_select_own on public.categories;
create policy categories_select_own on public.categories for select using (auth.uid() = user_id);
drop policy if exists categories_insert_own on public.categories;
create policy categories_insert_own on public.categories for insert with check (auth.uid() = user_id);
drop policy if exists categories_update_own on public.categories;
create policy categories_update_own on public.categories for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
drop policy if exists categories_delete_own on public.categories;
create policy categories_delete_own on public.categories for delete using (auth.uid() = user_id);

drop policy if exists meal_settings_select_own on public.meal_settings;
create policy meal_settings_select_own on public.meal_settings for select using (auth.uid() = user_id);
drop policy if exists meal_settings_insert_own on public.meal_settings;
create policy meal_settings_insert_own on public.meal_settings for insert with check (auth.uid() = user_id);
drop policy if exists meal_settings_update_own on public.meal_settings;
create policy meal_settings_update_own on public.meal_settings for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
drop policy if exists meal_settings_delete_own on public.meal_settings;
create policy meal_settings_delete_own on public.meal_settings for delete using (auth.uid() = user_id);

drop policy if exists meal_entries_select_own on public.meal_entries;
create policy meal_entries_select_own on public.meal_entries for select using (auth.uid() = user_id);
drop policy if exists meal_entries_insert_own on public.meal_entries;
create policy meal_entries_insert_own on public.meal_entries for insert with check (auth.uid() = user_id);
drop policy if exists meal_entries_update_own on public.meal_entries;
create policy meal_entries_update_own on public.meal_entries for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
drop policy if exists meal_entries_delete_own on public.meal_entries;
create policy meal_entries_delete_own on public.meal_entries for delete using (auth.uid() = user_id);

drop policy if exists transactions_select_own on public.transactions;
create policy transactions_select_own on public.transactions for select using (auth.uid() = user_id);
drop policy if exists transactions_insert_own on public.transactions;
create policy transactions_insert_own on public.transactions for insert with check (auth.uid() = user_id);
drop policy if exists transactions_update_own on public.transactions;
create policy transactions_update_own on public.transactions for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
drop policy if exists transactions_delete_own on public.transactions;
create policy transactions_delete_own on public.transactions for delete using (auth.uid() = user_id);

drop policy if exists loans_select_own on public.loans;
create policy loans_select_own on public.loans for select using (auth.uid() = user_id);
drop policy if exists loans_insert_own on public.loans;
create policy loans_insert_own on public.loans for insert with check (auth.uid() = user_id);
drop policy if exists loans_update_own on public.loans;
create policy loans_update_own on public.loans for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
drop policy if exists loans_delete_own on public.loans;
create policy loans_delete_own on public.loans for delete using (auth.uid() = user_id);

drop policy if exists loan_repayments_select_own on public.loan_repayments;
create policy loan_repayments_select_own on public.loan_repayments for select using (auth.uid() = user_id);
drop policy if exists loan_repayments_insert_own on public.loan_repayments;
create policy loan_repayments_insert_own on public.loan_repayments for insert with check (auth.uid() = user_id);
drop policy if exists loan_repayments_update_own on public.loan_repayments;
create policy loan_repayments_update_own on public.loan_repayments for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
drop policy if exists loan_repayments_delete_own on public.loan_repayments;
create policy loan_repayments_delete_own on public.loan_repayments for delete using (auth.uid() = user_id);

drop policy if exists user_settings_select_own on public.user_settings;
create policy user_settings_select_own on public.user_settings for select using (auth.uid() = user_id);
drop policy if exists user_settings_insert_own on public.user_settings;
create policy user_settings_insert_own on public.user_settings for insert with check (auth.uid() = user_id);
drop policy if exists user_settings_update_own on public.user_settings;
create policy user_settings_update_own on public.user_settings for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
drop policy if exists user_settings_delete_own on public.user_settings;
create policy user_settings_delete_own on public.user_settings for delete using (auth.uid() = user_id);

-- ---------------------------------------------------------------------------
-- API grants (PostgREST uses the authenticated role; anon has no table access)
-- ---------------------------------------------------------------------------

revoke all on public.profiles, public.categories, public.meal_settings,
    public.meal_entries, public.transactions, public.loans,
    public.loan_repayments, public.user_settings from anon;

grant select, insert, update, delete on public.profiles, public.categories,
    public.meal_settings, public.meal_entries, public.transactions,
    public.loans, public.loan_repayments, public.user_settings to authenticated;
