package com.pocketledger.models;

import android.database.Cursor;

public class SyncQueueItem {
    public String id;
    public String userId;
    public String tableName;
    public String recordId;
    public String operation;
    public int attempts;
    public String lastError;
    public String createdAt;

    public static SyncQueueItem fromCursor(Cursor c) {
        SyncQueueItem item = new SyncQueueItem();
        item.id = c.getString(c.getColumnIndexOrThrow("id"));
        item.userId = c.getString(c.getColumnIndexOrThrow("user_id"));
        item.tableName = c.getString(c.getColumnIndexOrThrow("table_name"));
        item.recordId = c.getString(c.getColumnIndexOrThrow("record_id"));
        item.operation = c.getString(c.getColumnIndexOrThrow("operation"));
        item.attempts = c.getInt(c.getColumnIndexOrThrow("attempts"));
        item.lastError = c.getString(c.getColumnIndexOrThrow("last_error"));
        item.createdAt = c.getString(c.getColumnIndexOrThrow("created_at"));
        return item;
    }
}
