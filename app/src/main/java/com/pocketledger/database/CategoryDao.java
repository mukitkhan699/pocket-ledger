package com.pocketledger.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pocketledger.models.Category;
import com.pocketledger.utils.Constants;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Ids;

import java.util.ArrayList;
import java.util.List;

public class CategoryDao {

    private final DatabaseHelper helper;

    public CategoryDao(Context context) {
        this.helper = DatabaseHelper.get(context);
    }

    public void seedDefaults(String userId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        String now = DateUtils.nowIso();
        db.beginTransaction();
        try {
            insertDefault(db, userId, "Morning Food", Constants.TYPE_EXPENSE, "sunrise", "#F5A623", true, now);
            insertDefault(db, userId, "Lunch", Constants.TYPE_EXPENSE, "food", "#16C784", true, now);
            insertDefault(db, userId, "Dinner", Constants.TYPE_EXPENSE, "moon", "#4A9DFF", true, now);
            insertDefault(db, userId, "Transportation", Constants.TYPE_EXPENSE, "transport", "#22D3EE", false, now);
            insertDefault(db, userId, "Shopping", Constants.TYPE_EXPENSE, "shopping", "#A78BFA", false, now);
            insertDefault(db, userId, "Mobile Recharge", Constants.TYPE_EXPENSE, "phone", "#F472B6", false, now);
            insertDefault(db, userId, "Education", Constants.TYPE_EXPENSE, "education", "#FBBF24", false, now);
            insertDefault(db, userId, "Personal", Constants.TYPE_EXPENSE, "person", "#FB7185", false, now);
            insertDefault(db, userId, "Medical", Constants.TYPE_EXPENSE, "medical", "#FF6B6B", false, now);
            insertDefault(db, userId, "Other", Constants.TYPE_EXPENSE, "other", "#94A3B8", false, now);
            insertDefault(db, userId, "Allowance", Constants.TYPE_INCOME, "income", "#16C784", false, now);
            insertDefault(db, userId, "Salary", Constants.TYPE_INCOME, "income", "#22D3EE", false, now);
            insertDefault(db, userId, "Money Received", Constants.TYPE_INCOME, "repayment", "#A78BFA", false, now);
            insertDefault(db, userId, "Other Income", Constants.TYPE_INCOME, "other", "#94A3B8", false, now);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void insertDefault(SQLiteDatabase db, String userId, String name, String type,
                               String icon, String color, boolean food, String now) {
        ContentValues values = new ContentValues();
        values.put("id", Ids.categoryId(userId, name));
        values.put("user_id", userId);
        values.put("name", name);
        values.put("type", type);
        values.put("icon", icon);
        values.put("color", color);
        values.put("is_default", 1);
        values.put("is_food", food ? 1 : 0);
        values.put("created_at", now);
        values.put("updated_at", now);
        values.put("deleted", 0);
        db.insertWithOnConflict("categories", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public List<Category> list(String userId, String type) {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT * FROM categories WHERE user_id=? AND deleted=0";
        String[] args;
        if (type != null) {
            sql += " AND type=?";
            args = new String[]{userId, type};
        } else {
            args = new String[]{userId};
        }
        sql += " ORDER BY is_default DESC, name COLLATE NOCASE ASC";
        try (Cursor c = db.rawQuery(sql, args)) {
            while (c.moveToNext()) {
                categories.add(Category.fromCursor(c));
            }
        }
        return categories;
    }

    public Category findById(String userId, String id) {
        if (id == null) {
            return null;
        }
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM categories WHERE user_id=? AND id=? AND deleted=0",
                new String[]{userId, id})) {
            if (c.moveToFirst()) {
                return Category.fromCursor(c);
            }
        }
        return null;
    }

    public Category findByName(String userId, String name) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM categories WHERE user_id=? AND name=? COLLATE NOCASE AND deleted=0",
                new String[]{userId, name})) {
            if (c.moveToFirst()) {
                return Category.fromCursor(c);
            }
        }
        return null;
    }

    public void upsert(Category category) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", category.id);
        values.put("user_id", category.userId);
        values.put("name", category.name);
        values.put("type", category.type);
        values.put("icon", category.icon);
        values.put("color", category.color);
        values.put("is_default", category.isDefault ? 1 : 0);
        values.put("is_food", category.isFood ? 1 : 0);
        values.put("created_at", category.createdAt);
        values.put("updated_at", category.updatedAt);
        values.put("deleted", category.deleted ? 1 : 0);
        int updated = db.update("categories", values, "id=?", new String[]{values.getAsString("id")});
        if (updated == 0) {
            db.insertOrThrow("categories", null, values);
        }
    }

    public boolean nameExists(String userId, String name, String excludeId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT 1 FROM categories WHERE user_id=? AND name=? COLLATE NOCASE AND deleted=0";
        List<String> args = new ArrayList<>();
        args.add(userId);
        args.add(name);
        if (excludeId != null) {
            sql += " AND id<>?";
            args.add(excludeId);
        }
        try (Cursor c = db.rawQuery(sql, args.toArray(new String[0]))) {
            return c.moveToFirst();
        }
    }

    public void softDelete(String userId, String id, String updatedAt) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("deleted", 1);
        values.put("updated_at", updatedAt);
        db.update("categories", values, "user_id=? AND id=?", new String[]{userId, id});
    }

    public void applyRemote(Category category) {
        upsert(category);
    }

    public int countActive(String userId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM categories WHERE user_id=? AND deleted=0",
                new String[]{userId})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }
}
