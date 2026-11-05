package com.pocketledger.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.pocketledger.utils.Constants;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static volatile DatabaseHelper instance;

    public static DatabaseHelper get(Context context) {
        if (instance == null) {
            synchronized (DatabaseHelper.class) {
                if (instance == null) {
                    instance = new DatabaseHelper(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, Constants.DB_NAME, null, Constants.DB_VERSION);
        setWriteAheadLoggingEnabled(true);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE profiles (" +
                "id TEXT PRIMARY KEY," +
                "user_id TEXT NOT NULL UNIQUE," +
                "email TEXT," +
                "full_name TEXT," +
                "created_at TEXT NOT NULL," +
                "updated_at TEXT NOT NULL," +
                "deleted INTEGER NOT NULL DEFAULT 0" +
                ")");

        db.execSQL("CREATE TABLE categories (" +
                "id TEXT PRIMARY KEY," +
                "user_id TEXT NOT NULL," +
                "name TEXT NOT NULL," +
                "type TEXT NOT NULL CHECK(type IN ('income','expense'))," +
                "icon TEXT," +
                "color TEXT," +
                "is_default INTEGER NOT NULL DEFAULT 0," +
                "is_food INTEGER NOT NULL DEFAULT 0," +
                "created_at TEXT NOT NULL," +
                "updated_at TEXT NOT NULL," +
                "deleted INTEGER NOT NULL DEFAULT 0," +
                "FOREIGN KEY(user_id) REFERENCES profiles(id) ON DELETE CASCADE" +
                ")");
        db.execSQL("CREATE UNIQUE INDEX ux_categories_user_name ON categories(user_id, name) WHERE deleted = 0");
        db.execSQL("CREATE INDEX ix_categories_user_type ON categories(user_id, type, deleted)");

        db.execSQL("CREATE TABLE meal_settings (" +
                "id TEXT PRIMARY KEY," +
                "user_id TEXT NOT NULL UNIQUE," +
                "morning_minor INTEGER NOT NULL DEFAULT 2750," +
                "lunch_minor INTEGER NOT NULL DEFAULT 5500," +
                "dinner_minor INTEGER NOT NULL DEFAULT 5500," +
                "auto_generate INTEGER NOT NULL DEFAULT 1," +
                "created_at TEXT NOT NULL," +
                "updated_at TEXT NOT NULL," +
                "deleted INTEGER NOT NULL DEFAULT 0," +
                "FOREIGN KEY(user_id) REFERENCES profiles(id) ON DELETE CASCADE" +
                ")");

        db.execSQL("CREATE TABLE meal_entries (" +
                "id TEXT PRIMARY KEY," +
                "user_id TEXT NOT NULL," +
                "meal_date TEXT NOT NULL," +
                "meal_type TEXT NOT NULL CHECK(meal_type IN ('MORNING','LUNCH','DINNER'))," +
                "amount_minor INTEGER NOT NULL CHECK(amount_minor >= 0)," +
                "state TEXT NOT NULL CHECK(state IN ('PLANNED','CONSUMED','SKIPPED'))," +
                "notes TEXT," +
                "created_at TEXT NOT NULL," +
                "updated_at TEXT NOT NULL," +
                "deleted INTEGER NOT NULL DEFAULT 0," +
                "FOREIGN KEY(user_id) REFERENCES profiles(id) ON DELETE CASCADE" +
                ")");
        db.execSQL("CREATE UNIQUE INDEX ux_meal_user_date_type ON meal_entries(user_id, meal_date, meal_type) WHERE deleted = 0");
        db.execSQL("CREATE INDEX ix_meal_user_date ON meal_entries(user_id, meal_date, deleted)");

        db.execSQL("CREATE TABLE transactions (" +
                "id TEXT PRIMARY KEY," +
                "user_id TEXT NOT NULL," +
                "type TEXT NOT NULL CHECK(type IN ('income','expense'))," +
                "category_id TEXT," +
                "description TEXT," +
                "notes TEXT," +
                "amount_minor INTEGER NOT NULL CHECK(amount_minor > 0)," +
                "occurred_at TEXT NOT NULL," +
                "meal_entry_id TEXT," +
                "created_at TEXT NOT NULL," +
                "updated_at TEXT NOT NULL," +
                "deleted INTEGER NOT NULL DEFAULT 0," +
                "FOREIGN KEY(user_id) REFERENCES profiles(id) ON DELETE CASCADE," +
                "FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE SET NULL," +
                "FOREIGN KEY(meal_entry_id) REFERENCES meal_entries(id) ON DELETE SET NULL" +
                ")");
        db.execSQL("CREATE INDEX ix_tx_user_date ON transactions(user_id, occurred_at, deleted)");
        db.execSQL("CREATE INDEX ix_tx_user_type ON transactions(user_id, type, deleted)");
        db.execSQL("CREATE INDEX ix_tx_category ON transactions(user_id, category_id, deleted)");
        db.execSQL("CREATE UNIQUE INDEX ux_tx_meal ON transactions(meal_entry_id) WHERE meal_entry_id IS NOT NULL AND deleted = 0");

        db.execSQL("CREATE TABLE loans (" +
                "id TEXT PRIMARY KEY," +
                "user_id TEXT NOT NULL," +
                "direction TEXT NOT NULL CHECK(direction IN ('BORROWED','LENT'))," +
                "person_name TEXT NOT NULL," +
                "principal_minor INTEGER NOT NULL CHECK(principal_minor > 0)," +
                "loan_date TEXT NOT NULL," +
                "description TEXT," +
                "created_at TEXT NOT NULL," +
                "updated_at TEXT NOT NULL," +
                "deleted INTEGER NOT NULL DEFAULT 0," +
                "FOREIGN KEY(user_id) REFERENCES profiles(id) ON DELETE CASCADE" +
                ")");
        db.execSQL("CREATE INDEX ix_loans_user ON loans(user_id, direction, deleted)");

        db.execSQL("CREATE TABLE loan_repayments (" +
                "id TEXT PRIMARY KEY," +
                "user_id TEXT NOT NULL," +
                "loan_id TEXT NOT NULL," +
                "amount_minor INTEGER NOT NULL CHECK(amount_minor > 0)," +
                "repayment_date TEXT NOT NULL," +
                "notes TEXT," +
                "created_at TEXT NOT NULL," +
                "updated_at TEXT NOT NULL," +
                "deleted INTEGER NOT NULL DEFAULT 0," +
                "FOREIGN KEY(user_id) REFERENCES profiles(id) ON DELETE CASCADE," +
                "FOREIGN KEY(loan_id) REFERENCES loans(id) ON DELETE CASCADE" +
                ")");
        db.execSQL("CREATE INDEX ix_repayments_loan ON loan_repayments(loan_id, deleted)");
        db.execSQL("CREATE INDEX ix_repayments_user ON loan_repayments(user_id, repayment_date, deleted)");

        db.execSQL("CREATE TABLE user_settings (" +
                "id TEXT PRIMARY KEY," +
                "user_id TEXT NOT NULL UNIQUE," +
                "theme TEXT NOT NULL DEFAULT 'dark'," +
                "notifications_enabled INTEGER NOT NULL DEFAULT 1," +
                "auto_sync_enabled INTEGER NOT NULL DEFAULT 1," +
                "last_sync_at TEXT," +
                "created_at TEXT NOT NULL," +
                "updated_at TEXT NOT NULL," +
                "deleted INTEGER NOT NULL DEFAULT 0," +
                "FOREIGN KEY(user_id) REFERENCES profiles(id) ON DELETE CASCADE" +
                ")");

        db.execSQL("CREATE TABLE sync_metadata (" +
                "id TEXT PRIMARY KEY," +
                "user_id TEXT NOT NULL," +
                "table_name TEXT NOT NULL," +
                "last_pulled_at TEXT," +
                "updated_at TEXT NOT NULL," +
                "UNIQUE(user_id, table_name)" +
                ")");

        db.execSQL("CREATE TABLE sync_queue (" +
                "id TEXT PRIMARY KEY," +
                "user_id TEXT NOT NULL," +
                "table_name TEXT NOT NULL," +
                "record_id TEXT NOT NULL," +
                "operation TEXT NOT NULL DEFAULT 'UPSERT'," +
                "payload TEXT," +
                "attempts INTEGER NOT NULL DEFAULT 0," +
                "last_error TEXT," +
                "created_at TEXT NOT NULL," +
                "UNIQUE(table_name, record_id)" +
                ")");
        db.execSQL("CREATE INDEX ix_queue_user ON sync_queue(user_id, created_at)");

        db.execSQL("CREATE TABLE sync_conflicts (" +
                "id TEXT PRIMARY KEY," +
                "user_id TEXT NOT NULL," +
                "table_name TEXT NOT NULL," +
                "record_id TEXT NOT NULL," +
                "local_payload TEXT," +
                "server_payload TEXT," +
                "created_at TEXT NOT NULL" +
                ")");
        db.execSQL("CREATE INDEX ix_conflicts_user ON sync_conflicts(user_id, created_at)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Future migration from version 1 to 2
        }
    }
}
