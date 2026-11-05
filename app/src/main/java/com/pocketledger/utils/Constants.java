package com.pocketledger.utils;

public final class Constants {

    public static final String DB_NAME = "pocket_ledger.db";
    public static final int DB_VERSION = 1;

    public static final String PREF_FILE = "pocket_ledger_prefs";
    public static final String SECURE_PREF_FILE = "pocket_ledger_secure";

    public static final long DEFAULT_MORNING_MINOR = 2750L;
    public static final long DEFAULT_LUNCH_MINOR = 5500L;
    public static final long DEFAULT_DINNER_MINOR = 5500L;
    public static final long DEFAULT_DAILY_MEAL_MINOR = 13750L;
    public static final long DEFAULT_30_DAY_MEAL_MINOR = 412500L;

    public static final String TABLE_PROFILES = "profiles";
    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String TABLE_CATEGORIES = "categories";
    public static final String TABLE_MEAL_SETTINGS = "meal_settings";
    public static final String TABLE_MEAL_ENTRIES = "meal_entries";
    public static final String TABLE_LOANS = "loans";
    public static final String TABLE_LOAN_REPAYMENTS = "loan_repayments";
    public static final String TABLE_USER_SETTINGS = "user_settings";
    public static final String TABLE_SYNC_METADATA = "sync_metadata";

    public static final String TYPE_INCOME = "income";
    public static final String TYPE_EXPENSE = "expense";

    public static final String STATE_PLANNED = "PLANNED";
    public static final String STATE_CONSUMED = "CONSUMED";
    public static final String STATE_SKIPPED = "SKIPPED";

    public static final String MEAL_MORNING = "MORNING";
    public static final String MEAL_LUNCH = "LUNCH";
    public static final String MEAL_DINNER = "DINNER";

    public static final String LOAN_BORROWED = "BORROWED";
    public static final String LOAN_LENT = "LENT";

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    public static final String ACTION_SYNC_STATUS = "com.pocketledger.SYNC_STATUS";
    public static final String ACTION_AUTH_EXPIRED = "com.pocketledger.AUTH_EXPIRED";
    public static final String EXTRA_SYNC_MESSAGE = "sync_message";
    public static final String EXTRA_SYNC_SUCCESS = "sync_success";

    public static final String WORK_SYNC = "pocketledger-sync";
    public static final String WORK_MEALS = "pocketledger-meals";
    public static final String NOTIFICATION_CHANNEL = "pocketledger_notifications";

    public static final int REQUEST_EXPORT_JSON = 201;
    public static final int REQUEST_EXPORT_CSV = 202;
    public static final int REQUEST_RESTORE_JSON = 203;

    private Constants() {
    }
}
