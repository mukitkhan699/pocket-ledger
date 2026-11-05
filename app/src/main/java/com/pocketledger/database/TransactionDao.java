package com.pocketledger.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pocketledger.models.ReportSummary;
import com.pocketledger.models.Transaction;
import com.pocketledger.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class TransactionDao {

    private final DatabaseHelper helper;

    public TransactionDao(Context context) {
        this.helper = DatabaseHelper.get(context);
    }

    public void upsert(Transaction transaction) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", transaction.id);
        values.put("user_id", transaction.userId);
        values.put("type", transaction.type);
        values.put("category_id", transaction.categoryId);
        values.put("description", transaction.description);
        values.put("notes", transaction.notes);
        values.put("amount_minor", transaction.amountMinor);
        values.put("occurred_at", transaction.occurredAt);
        values.put("meal_entry_id", transaction.mealEntryId);
        values.put("created_at", transaction.createdAt);
        values.put("updated_at", transaction.updatedAt);
        values.put("deleted", transaction.deleted ? 1 : 0);
        int updated = db.update("transactions", values, "id=?", new String[]{values.getAsString("id")});
        if (updated == 0) {
            db.insertOrThrow("transactions", null, values);
        }
    }

    public Transaction findById(String userId, String id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(baseSelect() + " AND t.id=?", new String[]{userId, id})) {
            if (c.moveToFirst()) {
                return Transaction.fromCursor(c);
            }
        }
        return null;
    }

    public Transaction findByMealEntryId(String userId, String mealEntryId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(baseSelect() + " AND t.meal_entry_id=?", new String[]{userId, mealEntryId})) {
            if (c.moveToFirst()) {
                return Transaction.fromCursor(c);
            }
        }
        return null;
    }

    public List<Transaction> query(String userId, String search, String categoryId, String type,
                                   String fromIso, String toIso, String sort, int limit) {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        StringBuilder sql = new StringBuilder(baseSelect());
        List<String> args = new ArrayList<>();
        args.add(userId);
        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (t.description LIKE ? OR t.notes LIKE ? OR c.name LIKE ?)");
            String like = "%" + search.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (categoryId != null && !categoryId.isEmpty()) {
            sql.append(" AND t.category_id=?");
            args.add(categoryId);
        }
        if (type != null && !type.isEmpty()) {
            sql.append(" AND t.type=?");
            args.add(type);
        }
        if (fromIso != null) {
            sql.append(" AND t.occurred_at>=?");
            args.add(fromIso);
        }
        if (toIso != null) {
            sql.append(" AND t.occurred_at<=?");
            args.add(toIso);
        }
        if ("oldest".equals(sort)) {
            sql.append(" ORDER BY t.occurred_at ASC, t.created_at ASC");
        } else if ("amount_high".equals(sort)) {
            sql.append(" ORDER BY t.amount_minor DESC, t.occurred_at DESC");
        } else if ("amount_low".equals(sort)) {
            sql.append(" ORDER BY t.amount_minor ASC, t.occurred_at DESC");
        } else {
            sql.append(" ORDER BY t.occurred_at DESC, t.created_at DESC");
        }
        if (limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }
        try (Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]))) {
            while (c.moveToNext()) {
                transactions.add(Transaction.fromCursor(c));
            }
        }
        return transactions;
    }

    public List<Transaction> recent(String userId, int limit) {
        return query(userId, null, null, null, null, null, "newest", limit);
    }

    public List<Transaction> listForRange(String userId, String fromIso, String toIso) {
        return query(userId, null, null, null, fromIso, toIso, "newest", 0);
    }

    public long sumByType(String userId, String type, String fromIso, String toIso) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT COALESCE(SUM(amount_minor),0) FROM transactions WHERE user_id=? AND type=? AND deleted=0";
        List<String> args = new ArrayList<>();
        args.add(userId);
        args.add(type);
        if (fromIso != null) {
            sql += " AND occurred_at>=?";
            args.add(fromIso);
        }
        if (toIso != null) {
            sql += " AND occurred_at<=?";
            args.add(toIso);
        }
        try (Cursor c = db.rawQuery(sql, args.toArray(new String[0]))) {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        }
    }

    public long sumFood(String userId, String fromIso, String toIso) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT COALESCE(SUM(t.amount_minor),0) FROM transactions t " +
                "LEFT JOIN categories c ON c.id=t.category_id " +
                "WHERE t.user_id=? AND t.deleted=0 AND t.type='expense' " +
                "AND (t.meal_entry_id IS NOT NULL OR c.is_food=1)";
        List<String> args = new ArrayList<>();
        args.add(userId);
        if (fromIso != null) {
            sql += " AND t.occurred_at>=?";
            args.add(fromIso);
        }
        if (toIso != null) {
            sql += " AND t.occurred_at<=?";
            args.add(toIso);
        }
        try (Cursor c = db.rawQuery(sql, args.toArray(new String[0]))) {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        }
    }

    public long totalIncome(String userId) {
        return sumByType(userId, Constants.TYPE_INCOME, null, null);
    }

    public long totalExpense(String userId) {
        return sumByType(userId, Constants.TYPE_EXPENSE, null, null);
    }

    public List<ReportSummary.CategoryTotal> sumByCategory(String userId, String fromIso, String toIso) {
        List<ReportSummary.CategoryTotal> totals = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT t.category_id, COALESCE(c.name,'Uncategorised') AS name, " +
                "COALESCE(c.color,'#94A3B8') AS color, COALESCE(c.icon,'other') AS icon, " +
                "SUM(t.amount_minor) AS total FROM transactions t " +
                "LEFT JOIN categories c ON c.id=t.category_id " +
                "WHERE t.user_id=? AND t.deleted=0 AND t.type='expense'";
        List<String> args = new ArrayList<>();
        args.add(userId);
        if (fromIso != null) {
            sql += " AND t.occurred_at>=?";
            args.add(fromIso);
        }
        if (toIso != null) {
            sql += " AND t.occurred_at<=?";
            args.add(toIso);
        }
        sql += " GROUP BY t.category_id ORDER BY total DESC";
        try (Cursor c = db.rawQuery(sql, args.toArray(new String[0]))) {
            while (c.moveToNext()) {
                totals.add(new ReportSummary.CategoryTotal(
                        c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4)));
            }
        }
        return totals;
    }

    public void softDelete(String userId, String id, String updatedAt) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("deleted", 1);
        values.put("updated_at", updatedAt);
        db.update("transactions", values, "user_id=? AND id=?", new String[]{userId, id});
    }

    public void markSynced(String userId, String id, String serverUpdatedAt) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("updated_at", serverUpdatedAt);
        db.update("transactions", values, "user_id=? AND id=?", new String[]{userId, id});
    }

    public void applyRemote(Transaction transaction) {
        upsert(transaction);
    }

    public int countActive(String userId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM transactions WHERE user_id=? AND deleted=0",
                new String[]{userId})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    private String baseSelect() {
        return "SELECT t.*, c.name AS category_name, c.icon AS category_icon, c.color AS category_color " +
                "FROM transactions t LEFT JOIN categories c ON c.id=t.category_id " +
                "WHERE t.user_id=? AND t.deleted=0";
    }
}
