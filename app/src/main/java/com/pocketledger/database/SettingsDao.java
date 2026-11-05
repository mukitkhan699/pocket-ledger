package com.pocketledger.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pocketledger.models.MealSettings;
import com.pocketledger.models.SyncConflict;
import com.pocketledger.models.SyncQueueItem;
import com.pocketledger.models.UserSettings;
import com.pocketledger.utils.Constants;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Ids;

import java.util.ArrayList;
import java.util.List;

public class SettingsDao {

    private final DatabaseHelper helper;

    public SettingsDao(Context context) {
        this.helper = DatabaseHelper.get(context);
    }

    public UserSettings getUserSettings(String userId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM user_settings WHERE user_id=? AND deleted=0", new String[]{userId})) {
            if (c.moveToFirst()) {
                return UserSettings.fromCursor(c);
            }
        }
        UserSettings settings = new UserSettings();
        settings.id = Ids.deterministic("user-settings:" + userId);
        settings.userId = userId;
        settings.theme = Constants.THEME_DARK;
        settings.notificationsEnabled = true;
        settings.autoSyncEnabled = true;
        settings.createdAt = DateUtils.nowIso();
        settings.updatedAt = settings.createdAt;
        saveUserSettings(settings);
        return settings;
    }

    public void saveUserSettings(UserSettings settings) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", settings.id);
        values.put("user_id", settings.userId);
        values.put("theme", settings.theme);
        values.put("notifications_enabled", settings.notificationsEnabled ? 1 : 0);
        values.put("auto_sync_enabled", settings.autoSyncEnabled ? 1 : 0);
        values.put("last_sync_at", settings.lastSyncAt);
        values.put("created_at", settings.createdAt);
        values.put("updated_at", settings.updatedAt);
        values.put("deleted", settings.deleted ? 1 : 0);
        db.insertWithOnConflict("user_settings", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public MealSettings getMealSettings(String userId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM meal_settings WHERE user_id=? AND deleted=0", new String[]{userId})) {
            if (c.moveToFirst()) {
                return MealSettings.fromCursor(c);
            }
        }
        MealSettings settings = new MealSettings();
        settings.id = Ids.deterministic("meal-settings:" + userId);
        settings.userId = userId;
        settings.morningMinor = Constants.DEFAULT_MORNING_MINOR;
        settings.lunchMinor = Constants.DEFAULT_LUNCH_MINOR;
        settings.dinnerMinor = Constants.DEFAULT_DINNER_MINOR;
        settings.autoGenerate = true;
        settings.createdAt = DateUtils.nowIso();
        settings.updatedAt = settings.createdAt;
        saveMealSettings(settings);
        return settings;
    }

    public void saveMealSettings(MealSettings settings) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", settings.id);
        values.put("user_id", settings.userId);
        values.put("morning_minor", settings.morningMinor);
        values.put("lunch_minor", settings.lunchMinor);
        values.put("dinner_minor", settings.dinnerMinor);
        values.put("auto_generate", settings.autoGenerate ? 1 : 0);
        values.put("created_at", settings.createdAt);
        values.put("updated_at", settings.updatedAt);
        values.put("deleted", settings.deleted ? 1 : 0);
        db.insertWithOnConflict("meal_settings", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String getLastPulledAt(String userId, String tableName) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT last_pulled_at FROM sync_metadata WHERE user_id=? AND table_name=?",
                new String[]{userId, tableName})) {
            if (c.moveToFirst()) {
                return c.isNull(0) ? null : c.getString(0);
            }
        }
        return null;
    }

    public void setLastPulledAt(String userId, String tableName, String value) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", Ids.deterministic("sync-meta:" + userId + ":" + tableName));
        values.put("user_id", userId);
        values.put("table_name", tableName);
        values.put("last_pulled_at", value);
        values.put("updated_at", DateUtils.nowIso());
        db.insertWithOnConflict("sync_metadata", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void enqueue(String userId, String tableName, String recordId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", Ids.deterministic("queue:" + tableName + ":" + recordId));
        values.put("user_id", userId);
        values.put("table_name", tableName);
        values.put("record_id", recordId);
        values.put("operation", "UPSERT");
        values.put("attempts", 0);
        values.put("created_at", DateUtils.nowIso());
        db.insertWithOnConflict("sync_queue", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<SyncQueueItem> queue(String userId, int limit) {
        List<SyncQueueItem> items = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT * FROM sync_queue WHERE user_id=? ORDER BY created_at ASC";
        if (limit > 0) {
            sql += " LIMIT " + limit;
        }
        try (Cursor c = db.rawQuery(sql, new String[]{userId})) {
            while (c.moveToNext()) {
                items.add(SyncQueueItem.fromCursor(c));
            }
        }
        return items;
    }

    public int queueCount(String userId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM sync_queue WHERE user_id=?", new String[]{userId})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public void dequeue(String id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete("sync_queue", "id=?", new String[]{id});
    }

    public void recordQueueFailure(String id, String error) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL("UPDATE sync_queue SET attempts=attempts+1, last_error=? WHERE id=?", new String[]{error, id});
    }

    public void logConflict(String userId, String tableName, String recordId,
                            String localPayload, String serverPayload) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", Ids.newId());
        values.put("user_id", userId);
        values.put("table_name", tableName);
        values.put("record_id", recordId);
        values.put("local_payload", localPayload);
        values.put("server_payload", serverPayload);
        values.put("created_at", DateUtils.nowIso());
        db.insert("sync_conflicts", null, values);
    }

    public List<SyncConflict> conflicts(String userId, int limit) {
        List<SyncConflict> conflicts = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT * FROM sync_conflicts WHERE user_id=? ORDER BY created_at DESC LIMIT " + limit;
        try (Cursor c = db.rawQuery(sql, new String[]{userId})) {
            while (c.moveToNext()) {
                conflicts.add(SyncConflict.fromCursor(c));
            }
        }
        return conflicts;
    }

    public int conflictCount(String userId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM sync_conflicts WHERE user_id=?", new String[]{userId})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public void clearConflicts(String userId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete("sync_conflicts", "user_id=?", new String[]{userId});
    }

    public void setLastSync(String userId, String iso) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("last_sync_at", iso);
        db.update("user_settings", values, "user_id=?", new String[]{userId});
    }
}
