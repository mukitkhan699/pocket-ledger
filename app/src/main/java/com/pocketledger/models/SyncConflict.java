package com.pocketledger.models;

import android.database.Cursor;

public class SyncConflict {
    public String id;
    public String userId;
    public String tableName;
    public String recordId;
    public String localPayload;
    public String serverPayload;
    public String createdAt;

    public static SyncConflict fromCursor(Cursor c) {
        SyncConflict conflict = new SyncConflict();
        conflict.id = c.getString(c.getColumnIndexOrThrow("id"));
        conflict.userId = c.getString(c.getColumnIndexOrThrow("user_id"));
        conflict.tableName = c.getString(c.getColumnIndexOrThrow("table_name"));
        conflict.recordId = c.getString(c.getColumnIndexOrThrow("record_id"));
        conflict.localPayload = c.getString(c.getColumnIndexOrThrow("local_payload"));
        conflict.serverPayload = c.getString(c.getColumnIndexOrThrow("server_payload"));
        conflict.createdAt = c.getString(c.getColumnIndexOrThrow("created_at"));
        return conflict;
    }
}
