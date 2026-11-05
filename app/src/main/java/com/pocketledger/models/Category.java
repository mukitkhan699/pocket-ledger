package com.pocketledger.models;

import android.database.Cursor;

public class Category {
    public String id;
    public String userId;
    public String name;
    public String type;
    public String icon;
    public String color;
    public boolean isDefault;
    public boolean isFood;
    public String createdAt;
    public String updatedAt;
    public boolean deleted;

    public Category() {
    }

    public static Category fromCursor(Cursor c) {
        Category category = new Category();
        category.id = c.getString(c.getColumnIndexOrThrow("id"));
        category.userId = c.getString(c.getColumnIndexOrThrow("user_id"));
        category.name = c.getString(c.getColumnIndexOrThrow("name"));
        category.type = c.getString(c.getColumnIndexOrThrow("type"));
        category.icon = c.getString(c.getColumnIndexOrThrow("icon"));
        category.color = c.getString(c.getColumnIndexOrThrow("color"));
        category.isDefault = c.getInt(c.getColumnIndexOrThrow("is_default")) == 1;
        category.isFood = c.getInt(c.getColumnIndexOrThrow("is_food")) == 1;
        category.createdAt = c.getString(c.getColumnIndexOrThrow("created_at"));
        category.updatedAt = c.getString(c.getColumnIndexOrThrow("updated_at"));
        category.deleted = c.getInt(c.getColumnIndexOrThrow("deleted")) == 1;
        return category;
    }

    @Override
    public String toString() {
        return name;
    }
}
