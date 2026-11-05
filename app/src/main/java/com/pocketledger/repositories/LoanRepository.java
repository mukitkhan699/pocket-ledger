package com.pocketledger.repositories;

import android.content.Context;

import com.pocketledger.database.LoanDao;
import com.pocketledger.database.SettingsDao;
import com.pocketledger.models.Loan;
import com.pocketledger.models.LoanRepayment;
import com.pocketledger.models.Session;
import com.pocketledger.utils.Constants;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Ids;
import com.pocketledger.utils.Money;
import com.pocketledger.utils.SessionManager;

import java.math.BigDecimal;
import java.util.List;

public class LoanRepository {

    private final LoanDao loanDao;
    private final SettingsDao settingsDao;
    private final SessionManager sessionManager;

    public LoanRepository(Context context) {
        this.loanDao = new LoanDao(context);
        this.settingsDao = new SettingsDao(context);
        this.sessionManager = SessionManager.get(context);
    }

    public Loan saveLoan(Loan loan) {
        String now = DateUtils.nowIso();
        if (loan.id == null || loan.id.isEmpty()) {
            loan.id = Ids.newId();
        }
        if (loan.createdAt == null) {
            loan.createdAt = now;
        }
        loan.updatedAt = now;
        loan.deleted = false;
        loanDao.upsert(loan);
        enqueue(loan.userId, Constants.TABLE_LOANS, loan.id);
        return loan;
    }

    public void deleteLoan(String userId, String loanId) {
        String now = DateUtils.nowIso();
        for (LoanRepayment repayment : loanDao.repayments(userId, loanId)) {
            loanDao.softDeleteRepayment(userId, repayment.id, now);
            enqueue(userId, Constants.TABLE_LOAN_REPAYMENTS, repayment.id);
        }
        loanDao.softDelete(userId, loanId, now);
        enqueue(userId, Constants.TABLE_LOANS, loanId);
    }

    public Loan findLoan(String userId, String loanId) {
        return loanDao.findById(userId, loanId);
    }

    public List<Loan> list(String userId, String direction) {
        return loanDao.list(userId, direction);
    }

    public List<Loan> listAll(String userId) {
        return loanDao.listAll(userId);
    }

    public List<LoanRepayment> repayments(String userId, String loanId) {
        return loanDao.repayments(userId, loanId);
    }

    public long outstanding(String userId, String direction) {
        return loanDao.outstanding(userId, direction);
    }

    public long balanceContribution(String userId) {
        return loanDao.balanceContribution(userId);
    }

    public long remaining(String userId, String loanId) {
        Loan loan = loanDao.findById(userId, loanId);
        if (loan == null) {
            return 0L;
        }
        return Math.max(0L, loan.principalMinor - loan.repaidMinor);
    }

    public LoanRepayment addRepayment(String userId, String loanId, long amountMinor, String date, String notes)
            throws IllegalArgumentException {
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("Enter a valid repayment amount.");
        }
        BigDecimal remaining = Money.of(remaining(userId, loanId));
        BigDecimal amount = Money.of(amountMinor);
        if (amount.compareTo(remaining) > 0) {
            throw new IllegalArgumentException("Repayment cannot exceed the remaining amount.");
        }
        LoanRepayment repayment = new LoanRepayment();
        repayment.id = Ids.newId();
        repayment.userId = userId;
        repayment.loanId = loanId;
        repayment.amountMinor = amountMinor;
        repayment.repaymentDate = date;
        repayment.notes = notes;
        repayment.createdAt = DateUtils.nowIso();
        repayment.updatedAt = repayment.createdAt;
        repayment.deleted = false;
        loanDao.upsertRepayment(repayment);
        enqueue(userId, Constants.TABLE_LOAN_REPAYMENTS, repayment.id);
        return repayment;
    }

    public void deleteRepayment(String userId, String repaymentId) {
        loanDao.softDeleteRepayment(userId, repaymentId, DateUtils.nowIso());
        enqueue(userId, Constants.TABLE_LOAN_REPAYMENTS, repaymentId);
    }

    private void enqueue(String userId, String table, String recordId) {
        Session session = sessionManager.getSession();
        if (session != null && !session.localOnly) {
            settingsDao.enqueue(userId, table, recordId);
        }
    }
}
