package com.pocketledger.models;

import android.database.Cursor;

public class Transaction {
    public String id;
    public String userId;
    public String type;
    public String categoryId;
    public String categoryName;
    public String categoryIcon;
    public String categoryColor;
    public String description;
    public String notes;
    public long amountMinor;
    public String occurredAt;
    public String mealEntryId;
    public String createdAt;
    public String updatedAt;
    public boolean deleted;

    public Transaction() {
    }

    public boolean isIncome() {
        return "income".equals(type);
    }

    public static Transaction fromCursor(Cursor c) {
        Transaction t = new Transaction();
        t.id = c.getString(c.getColumnIndexOrThrow("id"));
        t.userId = c.getString(c.getColumnIndexOrThrow("user_id"));
        t.type = c.getString(c.getColumnIndexOrThrow("type"));
        t.categoryId = getNullable(c, "category_id");
        t.description = getNullable(c, "description");
        t.notes = getNullable(c, "notes");
        t.amountMinor = c.getLong(c.getColumnIndexOrThrow("amount_minor"));
        t.occurredAt = c.getString(c.getColumnIndexOrThrow("occurred_at"));
        t.mealEntryId = getNullable(c, "meal_entry_id");
        t.createdAt = c.getString(c.getColumnIndexOrThrow("created_at"));
        t.updatedAt = c.getString(c.getColumnIndexOrThrow("updated_at"));
        t.deleted = c.getInt(c.getColumnIndexOrThrow("deleted")) == 1;
        int nameIndex = c.getColumnIndex("category_name");
        if (nameIndex >= 0) {
            t.categoryName = c.getString(nameIndex);
        }
        int iconIndex = c.getColumnIndex("category_icon");
        if (iconIndex >= 0) {
            t.categoryIcon = c.getString(iconIndex);
        }
        int colorIndex = c.getColumnIndex("category_color");
        if (colorIndex >= 0) {
            t.categoryColor = c.getString(colorIndex);
        }
        return t;
    }

    private static String getNullable(Cursor c, String column) {
        int index = c.getColumnIndex(column);
        if (index < 0 || c.isNull(index)) {
            return null;
        }
        return c.getString(index);
    }
}
