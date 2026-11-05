package com.pocketledger.models;

import android.database.Cursor;

import com.pocketledger.utils.Constants;

public class UserSettings {
    public String id;
    public String userId;
    public String theme = Constants.THEME_DARK;
    public boolean notificationsEnabled = true;
    public boolean autoSyncEnabled = true;
    public String lastSyncAt;
    public String createdAt;
    public String updatedAt;
    public boolean deleted;

    public UserSettings() {
    }

    public static UserSettings fromCursor(Cursor c) {
        UserSettings settings = new UserSettings();
        settings.id = c.getString(c.getColumnIndexOrThrow("id"));
        settings.userId = c.getString(c.getColumnIndexOrThrow("user_id"));
        settings.theme = c.getString(c.getColumnIndexOrThrow("theme"));
        settings.notificationsEnabled = c.getInt(c.getColumnIndexOrThrow("notifications_enabled")) == 1;
        settings.autoSyncEnabled = c.getInt(c.getColumnIndexOrThrow("auto_sync_enabled")) == 1;
        int lastSyncIdx = c.getColumnIndex("last_sync_at");
        settings.lastSyncAt = lastSyncIdx >= 0 ? c.getString(lastSyncIdx) : null;
        settings.createdAt = c.getString(c.getColumnIndexOrThrow("created_at"));
        settings.updatedAt = c.getString(c.getColumnIndexOrThrow("updated_at"));
        settings.deleted = c.getInt(c.getColumnIndexOrThrow("deleted")) == 1;
        return settings;
    }
}
