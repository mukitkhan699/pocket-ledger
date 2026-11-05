package com.pocketledger.utils;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {

    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_BALANCE_HIDDEN = "balance_hidden";
    private static final String KEY_LAST_BACKUP = "last_backup_at";
    private static final String KEY_REALTIME = "realtime_enabled";

    private Prefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(Constants.PREF_FILE, Context.MODE_PRIVATE);
    }

    public static String getTheme(Context context) {
        return prefs(context).getString(KEY_THEME, Constants.THEME_DARK);
    }

    public static void setTheme(Context context, String theme) {
        prefs(context).edit().putString(KEY_THEME, theme).apply();
    }

    public static boolean isBalanceHidden(Context context) {
        return prefs(context).getBoolean(KEY_BALANCE_HIDDEN, false);
    }

    public static void setBalanceHidden(Context context, boolean hidden) {
        prefs(context).edit().putBoolean(KEY_BALANCE_HIDDEN, hidden).apply();
    }

    public static String getLastBackupAt(Context context) {
        return prefs(context).getString(KEY_LAST_BACKUP, null);
    }

    public static void setLastBackupAt(Context context, String value) {
        prefs(context).edit().putString(KEY_LAST_BACKUP, value).apply();
    }

    public static boolean isRealtimeEnabled(Context context) {
        return prefs(context).getBoolean(KEY_REALTIME, true);
    }

    public static void setRealtimeEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_REALTIME, enabled).apply();
    }
}
