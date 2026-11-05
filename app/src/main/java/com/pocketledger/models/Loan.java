package com.pocketledger.models;

import android.database.Cursor;

import com.pocketledger.utils.Constants;

public class Loan {
    public String id;
    public String userId;
    public String direction;
    public String personName;
    public long principalMinor;
    public String loanDate;
    public String description;
    public String createdAt;
    public String updatedAt;
    public boolean deleted;
    public long repaidMinor;
    public long remainingMinor;

    public Loan() {
    }

    public boolean isBorrowed() {
        return Constants.LOAN_BORROWED.equals(direction);
    }

    public long balanceContributionMinor() {
        long outstanding = principalMinor - repaidMinor;
        return isBorrowed() ? outstanding : -outstanding;
    }

    public static Loan fromCursor(Cursor c) {
        Loan loan = new Loan();
        loan.id = c.getString(c.getColumnIndexOrThrow("id"));
        loan.userId = c.getString(c.getColumnIndexOrThrow("user_id"));
        loan.direction = c.getString(c.getColumnIndexOrThrow("direction"));
        loan.personName = c.getString(c.getColumnIndexOrThrow("person_name"));
        loan.principalMinor = c.getLong(c.getColumnIndexOrThrow("principal_minor"));
        loan.loanDate = c.getString(c.getColumnIndexOrThrow("loan_date"));
        loan.description = c.getString(c.getColumnIndexOrThrow("description"));
        loan.createdAt = c.getString(c.getColumnIndexOrThrow("created_at"));
        loan.updatedAt = c.getString(c.getColumnIndexOrThrow("updated_at"));
        loan.deleted = c.getInt(c.getColumnIndexOrThrow("deleted")) == 1;
        int repaidIndex = c.getColumnIndex("repaid_minor");
        loan.repaidMinor = repaidIndex >= 0 ? c.getLong(repaidIndex) : 0L;
        loan.remainingMinor = loan.principalMinor - loan.repaidMinor;
        return loan;
    }
}
