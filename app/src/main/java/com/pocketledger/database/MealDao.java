package com.pocketledger.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pocketledger.models.MealEntry;
import com.pocketledger.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class MealDao {

    private final DatabaseHelper helper;

    public MealDao(Context context) {
        this.helper = DatabaseHelper.get(context);
    }

    public List<MealEntry> getForDate(String userId, String date) {
        List<MealEntry> entries = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT * FROM meal_entries WHERE user_id=? AND meal_date=? AND deleted=0 " +
                        "ORDER BY CASE meal_type WHEN 'MORNING' THEN 1 WHEN 'LUNCH' THEN 2 ELSE 3 END",
                new String[]{userId, date})) {
            while (c.moveToNext()) {
                entries.add(MealEntry.fromCursor(c));
            }
        }
        return entries;
    }

    public List<MealEntry> getForRange(String userId, String fromDate, String toDate) {
        List<MealEntry> entries = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT * FROM meal_entries WHERE user_id=? AND meal_date>=? AND meal_date<=? AND deleted=0 " +
                        "ORDER BY meal_date ASC, CASE meal_type WHEN 'MORNING' THEN 1 WHEN 'LUNCH' THEN 2 ELSE 3 END",
                new String[]{userId, fromDate, toDate})) {
            while (c.moveToNext()) {
                entries.add(MealEntry.fromCursor(c));
            }
        }
        return entries;
    }

    public MealEntry findById(String userId, String id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM meal_entries WHERE user_id=? AND id=? AND deleted=0",
                new String[]{userId, id})) {
            if (c.moveToFirst()) {
                return MealEntry.fromCursor(c);
            }
        }
        return null;
    }

    public MealEntry findAny(String userId, String date, String mealType) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT * FROM meal_entries WHERE user_id=? AND meal_date=? AND meal_type=?",
                new String[]{userId, date, mealType})) {
            if (c.moveToFirst()) {
                return MealEntry.fromCursor(c);
            }
        }
        return null;
    }

    public boolean existsIncludingDeleted(String userId, String date, String mealType) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT 1 FROM meal_entries WHERE user_id=? AND meal_date=? AND meal_type=?",
                new String[]{userId, date, mealType})) {
            return c.moveToFirst();
        }
    }

    public boolean existsActive(String userId, String date, String mealType) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT 1 FROM meal_entries WHERE user_id=? AND meal_date=? AND meal_type=? AND deleted=0",
                new String[]{userId, date, mealType})) {
            return c.moveToFirst();
        }
    }

    public void insertIfMissing(MealEntry entry) {
        if (existsIncludingDeleted(entry.userId, entry.mealDate, entry.mealType)) {
            return;
        }
        upsert(entry);
    }

    public void upsert(MealEntry entry) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", entry.id);
        values.put("user_id", entry.userId);
        values.put("meal_date", entry.mealDate);
        values.put("meal_type", entry.mealType);
        values.put("amount_minor", entry.amountMinor);
        values.put("state", entry.state);
        values.put("notes", entry.notes);
        values.put("created_at", entry.createdAt);
        values.put("updated_at", entry.updatedAt);
        values.put("deleted", entry.deleted ? 1 : 0);
        int updated = db.update("meal_entries", values, "id=?", new String[]{values.getAsString("id")});
        if (updated == 0) {
            db.insertOrThrow("meal_entries", null, values);
        }
    }

    public void softDelete(String userId, String id, String updatedAt) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("deleted", 1);
        values.put("updated_at", updatedAt);
        db.update("meal_entries", values, "user_id=? AND id=?", new String[]{userId, id});
    }

    public long sumState(String userId, String fromDate, String toDate, String state) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT COALESCE(SUM(amount_minor),0) FROM meal_entries " +
                "WHERE user_id=? AND meal_date>=? AND meal_date<=? AND deleted=0";
        List<String> args = new ArrayList<>();
        args.add(userId);
        args.add(fromDate);
        args.add(toDate);
        if (state != null) {
            sql += " AND state=?";
            args.add(state);
        }
        try (Cursor c = db.rawQuery(sql, args.toArray(new String[0]))) {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        }
    }

    public long sumNonSkipped(String userId, String fromDate, String toDate) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT COALESCE(SUM(amount_minor),0) FROM meal_entries " +
                        "WHERE user_id=? AND meal_date>=? AND meal_date<=? AND deleted=0 AND state<>'SKIPPED'",
                new String[]{userId, fromDate, toDate})) {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        }
    }

    public long consumedForDate(String userId, String date) {
        return sumState(userId, date, date, Constants.STATE_CONSUMED);
    }

    public void applyRemote(MealEntry entry) {
        upsert(entry);
    }

    public int countForDate(String userId, String date) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM meal_entries WHERE user_id=? AND meal_date=? AND deleted=0",
                new String[]{userId, date})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }
}
