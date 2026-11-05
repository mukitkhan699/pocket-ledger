package com.pocketledger.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pocketledger.models.Loan;
import com.pocketledger.models.LoanRepayment;
import com.pocketledger.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class LoanDao {

    private final DatabaseHelper helper;

    public LoanDao(Context context) {
        this.helper = DatabaseHelper.get(context);
    }

    public List<Loan> list(String userId, String direction) {
        List<Loan> loans = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(selectWithRepaid() + " AND l.direction=? ORDER BY l.loan_date DESC, l.created_at DESC",
                new String[]{userId, direction})) {
            while (c.moveToNext()) {
                loans.add(Loan.fromCursor(c));
            }
        }
        return loans;
    }

    public List<Loan> listAll(String userId) {
        List<Loan> loans = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(selectWithRepaid() + " ORDER BY l.loan_date DESC, l.created_at DESC",
                new String[]{userId})) {
            while (c.moveToNext()) {
                loans.add(Loan.fromCursor(c));
            }
        }
        return loans;
    }

    public Loan findById(String userId, String id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(selectWithRepaid() + " AND l.id=?", new String[]{userId, id})) {
            if (c.moveToFirst()) {
                return Loan.fromCursor(c);
            }
        }
        return null;
    }

    public void upsert(Loan loan) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", loan.id);
        values.put("user_id", loan.userId);
        values.put("direction", loan.direction);
        values.put("person_name", loan.personName);
        values.put("principal_minor", loan.principalMinor);
        values.put("loan_date", loan.loanDate);
        values.put("description", loan.description);
        values.put("created_at", loan.createdAt);
        values.put("updated_at", loan.updatedAt);
        values.put("deleted", loan.deleted ? 1 : 0);
        int updated = db.update("loans", values, "id=?", new String[]{values.getAsString("id")});
        if (updated == 0) {
            db.insertOrThrow("loans", null, values);
        }
    }

    public void softDelete(String userId, String id, String updatedAt) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("deleted", 1);
        values.put("updated_at", updatedAt);
        db.update("loans", values, "user_id=? AND id=?", new String[]{userId, id});
    }

    public List<LoanRepayment> repayments(String userId, String loanId) {
        List<LoanRepayment> repayments = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT * FROM loan_repayments WHERE user_id=? AND loan_id=? AND deleted=0 " +
                        "ORDER BY repayment_date DESC, created_at DESC",
                new String[]{userId, loanId})) {
            while (c.moveToNext()) {
                repayments.add(LoanRepayment.fromCursor(c));
            }
        }
        return repayments;
    }

    public LoanRepayment findRepayment(String userId, String id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM loan_repayments WHERE user_id=? AND id=? AND deleted=0",
                new String[]{userId, id})) {
            if (c.moveToFirst()) {
                return LoanRepayment.fromCursor(c);
            }
        }
        return null;
    }

    public void upsertRepayment(LoanRepayment repayment) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", repayment.id);
        values.put("user_id", repayment.userId);
        values.put("loan_id", repayment.loanId);
        values.put("amount_minor", repayment.amountMinor);
        values.put("repayment_date", repayment.repaymentDate);
        values.put("notes", repayment.notes);
        values.put("created_at", repayment.createdAt);
        values.put("updated_at", repayment.updatedAt);
        values.put("deleted", repayment.deleted ? 1 : 0);
        db.insertWithOnConflict("loan_repayments", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void softDeleteRepayment(String userId, String id, String updatedAt) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("deleted", 1);
        values.put("updated_at", updatedAt);
        db.update("loan_repayments", values, "user_id=? AND id=?", new String[]{userId, id});
    }

    public long repaidTotal(String userId, String loanId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT COALESCE(SUM(amount_minor),0) FROM loan_repayments WHERE user_id=? AND loan_id=? AND deleted=0",
                new String[]{userId, loanId})) {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        }
    }

    public long outstanding(String userId, String direction) {
        long outstanding = 0L;
        for (Loan loan : list(userId, direction)) {
            outstanding += Math.max(0L, loan.principalMinor - loan.repaidMinor);
        }
        return outstanding;
    }

    public long balanceContribution(String userId) {
        long contribution = 0L;
        for (Loan loan : listAll(userId)) {
            contribution += loan.balanceContributionMinor();
        }
        return contribution;
    }

    public void applyRemoteLoan(Loan loan) {
        upsert(loan);
    }

    public void applyRemoteRepayment(LoanRepayment repayment) {
        upsertRepayment(repayment);
    }

    private String selectWithRepaid() {
        return "SELECT l.*, COALESCE((SELECT SUM(r.amount_minor) FROM loan_repayments r " +
                "WHERE r.loan_id=l.id AND r.deleted=0),0) AS repaid_minor " +
                "FROM loans l WHERE l.user_id=? AND l.deleted=0";
    }
}
