package com.pocketledger.utils;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeUtils {

    private ThemeUtils() {
    }

    public static void applySavedTheme(Context context) {
        applyTheme(Prefs.getTheme(context));
    }

    public static void applyTheme(String theme) {
        if (Constants.THEME_LIGHT.equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (Constants.THEME_SYSTEM.equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    public static String label(String theme) {
        switch (theme) {
            case Constants.THEME_LIGHT:
                return "Light";
            case Constants.THEME_SYSTEM:
                return "System";
            default:
                return "Dark";
        }
    }
}
