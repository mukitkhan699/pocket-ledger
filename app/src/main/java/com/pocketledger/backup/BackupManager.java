package com.pocketledger.backup;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.pocketledger.database.DatabaseHelper;
import com.pocketledger.database.SettingsDao;
import com.pocketledger.models.Session;
import com.pocketledger.utils.Constants;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Ids;
import com.pocketledger.utils.Money;
import com.pocketledger.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BackupManager {

    public static final int FORMAT_VERSION = 1;

    private static final List<String> EXPORT_TABLES = Arrays.asList(
            Constants.TABLE_PROFILES,
            Constants.TABLE_CATEGORIES,
            Constants.TABLE_MEAL_SETTINGS,
            Constants.TABLE_MEAL_ENTRIES,
            Constants.TABLE_TRANSACTIONS,
            Constants.TABLE_LOANS,
            Constants.TABLE_LOAN_REPAYMENTS,
            Constants.TABLE_USER_SETTINGS);

    private static final List<String> RESTORE_TABLES = Arrays.asList(
            Constants.TABLE_PROFILES,
            Constants.TABLE_CATEGORIES,
            Constants.TABLE_MEAL_SETTINGS,
            Constants.TABLE_USER_SETTINGS,
            Constants.TABLE_MEAL_ENTRIES,
            Constants.TABLE_TRANSACTIONS,
            Constants.TABLE_LOANS,
            Constants.TABLE_LOAN_REPAYMENTS);

    private BackupManager() {
    }

    public static String exportJson(Context context, String userId) throws Exception {
        SQLiteDatabase db = DatabaseHelper.get(context).getReadableDatabase();
        JSONObject root = new JSONObject();
        root.put("app", "Pocket Ledger");
        root.put("format", FORMAT_VERSION);
        root.put("exported_at", DateUtils.nowIso());
        root.put("user_id", userId);
        JSONObject tables = new JSONObject();
        for (String table : EXPORT_TABLES) {
            JSONArray rows = new JSONArray();
            try (Cursor cursor = db.rawQuery(
                    "SELECT * FROM " + table + " WHERE user_id=? AND deleted=0",
                    new String[]{userId})) {
                while (cursor.moveToNext()) {
                    rows.put(rowToJson(cursor));
                }
            }
            tables.put(table, rows);
        }
        root.put("tables", tables);
        return root.toString(2);
    }

    public static String exportTransactionsCsv(Context context, String userId) {
        SQLiteDatabase db = DatabaseHelper.get(context).getReadableDatabase();
        StringBuilder builder = new StringBuilder();
        builder.append("id,occurred_at,date_local,type,category,description,notes,amount_bdt,meal_entry_id\n");
        String sql = "SELECT t.id, t.occurred_at, t.type, t.description, t.notes, t.amount_minor, " +
                "t.meal_entry_id, c.name AS category_name " +
                "FROM transactions t LEFT JOIN categories c ON c.id=t.category_id " +
                "WHERE t.user_id=? AND t.deleted=0 ORDER BY t.occurred_at DESC";
        try (Cursor c = db.rawQuery(sql, new String[]{userId})) {
            while (c.moveToNext()) {
                builder.append(csv(c.getString(0))).append(',');
                builder.append(csv(c.getString(1))).append(',');
                builder.append(csv(DateUtils.localDate(c.getString(1)))).append(',');
                builder.append(csv(c.getString(2))).append(',');
                builder.append(csv(c.getString(7))).append(',');
                builder.append(csv(c.getString(3))).append(',');
                builder.append(csv(c.getString(4))).append(',');
                builder.append(Money.decimal(c.getLong(5))).append(',');
                builder.append(csv(c.getString(6)));
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    public static int restoreJson(Context context, String userId, String json) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONObject tables = root.optJSONObject("tables");
        if (tables == null) {
            throw new IllegalArgumentException("Backup file is missing its data tables.");
        }
        SQLiteDatabase db = DatabaseHelper.get(context).getWritableDatabase();
        SettingsDao settingsDao = new SettingsDao(context);
        Session session = SessionManager.get(context).getSession();
        boolean enqueue = session != null && !session.localOnly;
        Map<String, Set<String>> columns = new HashMap<>();
        String now = DateUtils.nowIso();
        int imported = 0;
        for (String table : RESTORE_TABLES) {
            JSONArray rows = tables.optJSONArray(table);
            if (rows == null) {
                continue;
            }
            Set<String> tableColumns = columns.get(table);
            if (tableColumns == null) {
                tableColumns = tableColumns(db, table);
                columns.put(table, tableColumns);
            }
            if (tableColumns.isEmpty()) {
                continue;
            }
            boolean singleton = Constants.TABLE_PROFILES.equals(table)
                    || Constants.TABLE_MEAL_SETTINGS.equals(table)
                    || Constants.TABLE_USER_SETTINGS.equals(table);
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row == null) {
                    continue;
                }
                ContentValues values = new ContentValues();
                Iterator<String> keys = row.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    if (!tableColumns.contains(key)) {
                        continue;
                    }
                    Object value = row.get(key);
                    if (value == null || value == JSONObject.NULL) {
                        values.putNull(key);
                    } else if (value instanceof Boolean) {
                        values.put(key, (Boolean) value ? 1 : 0);
                    } else if (value instanceof Number) {
                        values.put(key, ((Number) value).longValue());
                    } else {
                        values.put(key, String.valueOf(value));
                    }
                }
                if (tableColumns.contains("user_id")) {
                    values.put("user_id", userId);
                }
                if (Constants.TABLE_PROFILES.equals(table)) {
                    values.put("id", userId);
                }
                if (tableColumns.contains("created_at") && !values.containsKey("created_at")) {
                    values.put("created_at", now);
                }
                if (tableColumns.contains("updated_at") && !values.containsKey("updated_at")) {
                    values.put("updated_at", now);
                }
                String keyColumn = singleton ? "user_id" : "id";
                String keyValue = singleton ? userId : values.getAsString("id");
                if (keyValue == null || keyValue.isEmpty()) {
                    continue;
                }
                if (singleton && !values.containsKey("id")) {
                    if (Constants.TABLE_MEAL_SETTINGS.equals(table)) {
                        values.put("id", Ids.deterministic("meal-settings:" + userId));
                    } else if (Constants.TABLE_USER_SETTINGS.equals(table)) {
                        values.put("id", Ids.deterministic("user-settings:" + userId));
                    }
                }
                try {
                    int updated = db.update(table, values, keyColumn + "=?",
                            new String[]{keyValue});
                    if (updated == 0) {
                        long inserted = db.insertWithOnConflict(table, null, values,
                                SQLiteDatabase.CONFLICT_IGNORE);
                        if (inserted == -1) {
                            continue;
                        }
                    }
                    imported++;
                    if (enqueue) {
                        String recordId = values.getAsString("id");
                        if (recordId != null) {
                            settingsDao.enqueue(userId, table, recordId);
                        }
                    }
                } catch (SQLiteConstraintException ignored) {
                }
            }
        }
        return imported;
    }

    private static JSONObject rowToJson(Cursor cursor) throws Exception {
        JSONObject object = new JSONObject();
        String[] names = cursor.getColumnNames();
        for (int i = 0; i < names.length; i++) {
            if (cursor.isNull(i)) {
                object.put(names[i], JSONObject.NULL);
                continue;
            }
            switch (cursor.getType(i)) {
                case Cursor.FIELD_TYPE_INTEGER:
                    object.put(names[i], cursor.getLong(i));
                    break;
                case Cursor.FIELD_TYPE_FLOAT:
                    object.put(names[i], cursor.getDouble(i));
                    break;
                case Cursor.FIELD_TYPE_BLOB:
                    object.put(names[i], "");
                    break;
                default:
                    object.put(names[i], cursor.getString(i));
                    break;
            }
        }
        return object;
    }

    private static Set<String> tableColumns(SQLiteDatabase db, String table) {
        Set<String> columns = new HashSet<>();
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            int nameIndex = c.getColumnIndex("name");
            while (c.moveToNext()) {
                if (nameIndex >= 0) {
                    columns.add(c.getString(nameIndex));
                }
            }
        }
        return columns;
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
