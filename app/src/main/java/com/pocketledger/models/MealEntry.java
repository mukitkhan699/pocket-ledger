package com.pocketledger.models;

import android.database.Cursor;

import com.pocketledger.utils.Constants;

public class MealEntry {
    public String id;
    public String userId;
    public String mealDate;
    public String mealType;
    public long amountMinor;
    public String state;
    public String notes;
    public String createdAt;
    public String updatedAt;
    public boolean deleted;

    public MealEntry() {
    }

    public boolean isConsumed() {
        return Constants.STATE_CONSUMED.equals(state);
    }

    public boolean isSkipped() {
        return Constants.STATE_SKIPPED.equals(state);
    }

    public boolean isPlanned() {
        return Constants.STATE_PLANNED.equals(state);
    }

    public String label() {
        switch (mealType) {
            case Constants.MEAL_MORNING:
                return "Morning";
            case Constants.MEAL_LUNCH:
                return "Lunch";
            case Constants.MEAL_DINNER:
                return "Dinner";
            default:
                return mealType;
        }
    }

    public String iconKey() {
        switch (mealType) {
            case Constants.MEAL_MORNING:
                return "sunrise";
            case Constants.MEAL_LUNCH:
                return "food";
            case Constants.MEAL_DINNER:
                return "moon";
            default:
                return "food";
        }
    }

    public static MealEntry fromCursor(Cursor c) {
        MealEntry entry = new MealEntry();
        entry.id = c.getString(c.getColumnIndexOrThrow("id"));
        entry.userId = c.getString(c.getColumnIndexOrThrow("user_id"));
        entry.mealDate = c.getString(c.getColumnIndexOrThrow("meal_date"));
        entry.mealType = c.getString(c.getColumnIndexOrThrow("meal_type"));
        entry.amountMinor = c.getLong(c.getColumnIndexOrThrow("amount_minor"));
        entry.state = c.getString(c.getColumnIndexOrThrow("state"));
        entry.notes = c.getString(c.getColumnIndexOrThrow("notes"));
        entry.createdAt = c.getString(c.getColumnIndexOrThrow("created_at"));
        entry.updatedAt = c.getString(c.getColumnIndexOrThrow("updated_at"));
        entry.deleted = c.getInt(c.getColumnIndexOrThrow("deleted")) == 1;
        return entry;
    }
}
