package com.pocketledger.models;

import android.database.Cursor;

import com.pocketledger.utils.Constants;

public class MealSettings {
    public String id;
    public String userId;
    public long morningMinor = Constants.DEFAULT_MORNING_MINOR;
    public long lunchMinor = Constants.DEFAULT_LUNCH_MINOR;
    public long dinnerMinor = Constants.DEFAULT_DINNER_MINOR;
    public boolean autoGenerate = true;
    public String createdAt;
    public String updatedAt;
    public boolean deleted;

    public MealSettings() {
    }

    public long dailyTotalMinor() {
        return morningMinor + lunchMinor + dinnerMinor;
    }

    public long thirtyDayTotalMinor() {
        return dailyTotalMinor() * 30;
    }

    public long priceFor(String mealType) {
        switch (mealType) {
            case Constants.MEAL_MORNING:
                return morningMinor;
            case Constants.MEAL_LUNCH:
                return lunchMinor;
            case Constants.MEAL_DINNER:
                return dinnerMinor;
            default:
                return 0L;
        }
    }

    public static MealSettings fromCursor(Cursor c) {
        MealSettings settings = new MealSettings();
        settings.id = c.getString(c.getColumnIndexOrThrow("id"));
        settings.userId = c.getString(c.getColumnIndexOrThrow("user_id"));
        settings.morningMinor = c.getLong(c.getColumnIndexOrThrow("morning_minor"));
        settings.lunchMinor = c.getLong(c.getColumnIndexOrThrow("lunch_minor"));
        settings.dinnerMinor = c.getLong(c.getColumnIndexOrThrow("dinner_minor"));
        settings.autoGenerate = c.getInt(c.getColumnIndexOrThrow("auto_generate")) == 1;
        settings.createdAt = c.getString(c.getColumnIndexOrThrow("created_at"));
        settings.updatedAt = c.getString(c.getColumnIndexOrThrow("updated_at"));
        settings.deleted = c.getInt(c.getColumnIndexOrThrow("deleted")) == 1;
        return settings;
    }
}
