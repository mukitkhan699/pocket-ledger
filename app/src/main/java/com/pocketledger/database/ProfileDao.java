package com.pocketledger.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pocketledger.models.Profile;

public class ProfileDao {

    private final DatabaseHelper helper;

    public ProfileDao(Context context) {
        this.helper = DatabaseHelper.get(context);
    }

    public Profile find(String userId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM profiles WHERE user_id=? AND deleted=0", new String[]{userId})) {
            if (c.moveToFirst()) {
                return Profile.fromCursor(c);
            }
        }
        return null;
    }

    public void upsert(Profile profile) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", profile.id);
        values.put("user_id", profile.userId);
        values.put("email", profile.email);
        values.put("full_name", profile.fullName);
        values.put("created_at", profile.createdAt);
        values.put("updated_at", profile.updatedAt);
        values.put("deleted", profile.deleted ? 1 : 0);
        int updated = db.update("profiles", values, "id=?", new String[]{values.getAsString("id")});
        if (updated == 0) {
            db.insertOrThrow("profiles", null, values);
        }
    }

    public void applyRemote(Profile profile) {
        upsert(profile);
    }
}
