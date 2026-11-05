package com.pocketledger.models;

import android.database.Cursor;

public class LoanRepayment {
    public String id;
    public String userId;
    public String loanId;
    public long amountMinor;
    public String repaymentDate;
    public String notes;
    public String createdAt;
    public String updatedAt;
    public boolean deleted;

    public LoanRepayment() {
    }

    public static LoanRepayment fromCursor(Cursor c) {
        LoanRepayment repayment = new LoanRepayment();
        repayment.id = c.getString(c.getColumnIndexOrThrow("id"));
        repayment.userId = c.getString(c.getColumnIndexOrThrow("user_id"));
        repayment.loanId = c.getString(c.getColumnIndexOrThrow("loan_id"));
        repayment.amountMinor = c.getLong(c.getColumnIndexOrThrow("amount_minor"));
        repayment.repaymentDate = c.getString(c.getColumnIndexOrThrow("repayment_date"));
        repayment.notes = c.getString(c.getColumnIndexOrThrow("notes"));
        repayment.createdAt = c.getString(c.getColumnIndexOrThrow("created_at"));
        repayment.updatedAt = c.getString(c.getColumnIndexOrThrow("updated_at"));
        repayment.deleted = c.getInt(c.getColumnIndexOrThrow("deleted")) == 1;
        return repayment;
    }
}
