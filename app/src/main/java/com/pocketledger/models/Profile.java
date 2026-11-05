package com.pocketledger.models;

import android.database.Cursor;

public class Profile {
    public String id;
    public String userId;
    public String email;
    public String fullName;
    public String createdAt;
    public String updatedAt;
    public boolean deleted;

    public Profile() {
    }

    public static Profile fromCursor(Cursor c) {
        Profile profile = new Profile();
        profile.id = c.getString(c.getColumnIndexOrThrow("id"));
        profile.userId = c.getString(c.getColumnIndexOrThrow("user_id"));
        int emailIdx = c.getColumnIndex("email");
        profile.email = emailIdx >= 0 ? c.getString(emailIdx) : null;
        int fullNameIdx = c.getColumnIndex("full_name");
        profile.fullName = fullNameIdx >= 0 ? c.getString(fullNameIdx) : null;
        profile.createdAt = c.getString(c.getColumnIndexOrThrow("created_at"));
        profile.updatedAt = c.getString(c.getColumnIndexOrThrow("updated_at"));
        profile.deleted = c.getInt(c.getColumnIndexOrThrow("deleted")) == 1;
        return profile;
    }
}
