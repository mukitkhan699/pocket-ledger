package com.pocketledger.sync;

import android.content.ContentValues;
import android.database.Cursor;

import com.pocketledger.utils.Constants;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Money;

import org.json.JSONObject;

public final class SyncMapper {

    private SyncMapper() {
    }

    public static JSONObject toCloud(String table, Cursor c) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", string(c, "id"));
        o.put("user_id", string(c, "user_id"));
        o.put("created_at", string(c, "created_at"));
        o.put("updated_at", string(c, "updated_at"));
        o.put("deleted", integer(c, "deleted") != 0);

        switch (table) {
            case Constants.TABLE_PROFILES:
                putString(o, "email", string(c, "email"));
                putString(o, "full_name", string(c, "full_name"));
                break;
            case Constants.TABLE_CATEGORIES:
                o.put("name", string(c, "name"));
                o.put("type", string(c, "type"));
                putString(o, "icon", string(c, "icon"));
                putString(o, "color", string(c, "color"));
                o.put("is_default", integer(c, "is_default") != 0);
                o.put("is_food", integer(c, "is_food") != 0);
                break;
            case Constants.TABLE_MEAL_SETTINGS:
                o.put("morning_price", Money.decimal(integer(c, "morning_minor")));
                o.put("lunch_price", Money.decimal(integer(c, "lunch_minor")));
                o.put("dinner_price", Money.decimal(integer(c, "dinner_minor")));
                o.put("auto_generate", integer(c, "auto_generate") != 0);
                break;
            case Constants.TABLE_MEAL_ENTRIES:
                o.put("meal_date", string(c, "meal_date"));
                o.put("meal_type", string(c, "meal_type"));
                o.put("amount", Money.decimal(integer(c, "amount_minor")));
                o.put("state", string(c, "state"));
                putString(o, "notes", string(c, "notes"));
                break;
            case Constants.TABLE_TRANSACTIONS:
                o.put("type", string(c, "type"));
                putString(o, "category_id", string(c, "category_id"));
                putString(o, "description", string(c, "description"));
                putString(o, "notes", string(c, "notes"));
                o.put("amount", Money.decimal(integer(c, "amount_minor")));
                o.put("occurred_at", string(c, "occurred_at"));
                putString(o, "meal_entry_id", string(c, "meal_entry_id"));
                break;
            case Constants.TABLE_LOANS:
                o.put("direction", string(c, "direction"));
                o.put("person_name", string(c, "person_name"));
                o.put("principal", Money.decimal(integer(c, "principal_minor")));
                o.put("loan_date", string(c, "loan_date"));
                putString(o, "description", string(c, "description"));
                break;
            case Constants.TABLE_LOAN_REPAYMENTS:
                o.put("loan_id", string(c, "loan_id"));
                o.put("amount", Money.decimal(integer(c, "amount_minor")));
                o.put("repayment_date", string(c, "repayment_date"));
                putString(o, "notes", string(c, "notes"));
                break;
            case Constants.TABLE_USER_SETTINGS:
                o.put("theme", string(c, "theme"));
                o.put("notifications_enabled", integer(c, "notifications_enabled") != 0);
                o.put("auto_sync_enabled", integer(c, "auto_sync_enabled") != 0);
                putString(o, "last_sync_at", string(c, "last_sync_at"));
                break;
            default:
                break;
        }
        return o;
    }

    public static ContentValues fromCloud(String table, JSONObject o) throws Exception {
        ContentValues v = new ContentValues();
        v.put("id", o.optString("id"));
        v.put("user_id", o.optString("user_id"));
        v.put("created_at", text(o, "created_at", DateUtils.nowIso()));
        v.put("updated_at", text(o, "updated_at", DateUtils.nowIso()));
        v.put("deleted", o.optBoolean("deleted", false) ? 1 : 0);

        switch (table) {
            case Constants.TABLE_PROFILES:
                putString(v, "email", nullable(o, "email"));
                putString(v, "full_name", nullable(o, "full_name"));
                break;
            case Constants.TABLE_CATEGORIES:
                v.put("name", text(o, "name", ""));
                v.put("type", text(o, "type", Constants.TYPE_EXPENSE));
                putString(v, "icon", nullable(o, "icon"));
                putString(v, "color", nullable(o, "color"));
                v.put("is_default", o.optBoolean("is_default", false) ? 1 : 0);
                v.put("is_food", o.optBoolean("is_food", false) ? 1 : 0);
                break;
            case Constants.TABLE_MEAL_SETTINGS:
                v.put("morning_minor", amount(o, "morning_price"));
                v.put("lunch_minor", amount(o, "lunch_price"));
                v.put("dinner_minor", amount(o, "dinner_price"));
                v.put("auto_generate", o.optBoolean("auto_generate", true) ? 1 : 0);
                break;
            case Constants.TABLE_MEAL_ENTRIES:
                v.put("meal_date", text(o, "meal_date", DateUtils.todayDate()));
                v.put("meal_type", text(o, "meal_type", Constants.MEAL_MORNING));
                v.put("amount_minor", amount(o, "amount"));
                v.put("state", text(o, "state", Constants.STATE_PLANNED));
                putString(v, "notes", nullable(o, "notes"));
                break;
            case Constants.TABLE_TRANSACTIONS:
                v.put("type", text(o, "type", Constants.TYPE_EXPENSE));
                putString(v, "category_id", nullable(o, "category_id"));
                putString(v, "description", nullable(o, "description"));
                putString(v, "notes", nullable(o, "notes"));
                v.put("amount_minor", amount(o, "amount"));
                v.put("occurred_at", text(o, "occurred_at", DateUtils.nowIso()));
                putString(v, "meal_entry_id", nullable(o, "meal_entry_id"));
                break;
            case Constants.TABLE_LOANS:
                v.put("direction", text(o, "direction", Constants.LOAN_BORROWED));
                v.put("person_name", text(o, "person_name", ""));
                v.put("principal_minor", amount(o, "principal"));
                v.put("loan_date", text(o, "loan_date", DateUtils.todayDate()));
                putString(v, "description", nullable(o, "description"));
                break;
            case Constants.TABLE_LOAN_REPAYMENTS:
                v.put("loan_id", text(o, "loan_id", ""));
                v.put("amount_minor", amount(o, "amount"));
                v.put("repayment_date", text(o, "repayment_date", DateUtils.todayDate()));
                putString(v, "notes", nullable(o, "notes"));
                break;
            case Constants.TABLE_USER_SETTINGS:
                v.put("theme", text(o, "theme", Constants.THEME_DARK));
                v.put("notifications_enabled", o.optBoolean("notifications_enabled", true) ? 1 : 0);
                v.put("auto_sync_enabled", o.optBoolean("auto_sync_enabled", true) ? 1 : 0);
                putString(v, "last_sync_at", nullable(o, "last_sync_at"));
                break;
            default:
                break;
        }
        return v;
    }

    private static long amount(JSONObject o, String key) {
        if (!o.has(key) || o.isNull(key)) {
            return 0L;
        }
        try {
            return Money.parseMinor(o.optString(key));
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String nullable(JSONObject o, String key) {
        if (!o.has(key) || o.isNull(key)) {
            return null;
        }
        String value = o.optString(key, null);
        return value == null || value.isEmpty() ? null : value;
    }

    private static String text(JSONObject o, String key, String fallback) {
        String value = nullable(o, key);
        return value == null ? fallback : value;
    }

    private static void putString(JSONObject o, String key, String value) throws Exception {
        if (value == null) {
            o.put(key, JSONObject.NULL);
        } else {
            o.put(key, value);
        }
    }

    private static void putString(ContentValues v, String key, String value) {
        if (value == null) {
            v.putNull(key);
        } else {
            v.put(key, value);
        }
    }

    private static String string(Cursor c, String column) {
        int index = c.getColumnIndex(column);
        if (index < 0 || c.isNull(index)) {
            return null;
        }
        return c.getString(index);
    }

    private static long integer(Cursor c, String column) {
        int index = c.getColumnIndex(column);
        if (index < 0 || c.isNull(index)) {
            return 0L;
        }
        return c.getLong(index);
    }
}
