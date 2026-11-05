package com.pocketledger.repositories;

import android.content.Context;

import com.pocketledger.database.SettingsDao;
import com.pocketledger.database.TransactionDao;
import com.pocketledger.models.ReportSummary;
import com.pocketledger.models.Session;
import com.pocketledger.models.Transaction;
import com.pocketledger.utils.Constants;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Ids;
import com.pocketledger.utils.SessionManager;

import java.util.List;

public class TransactionRepository {

    private final TransactionDao transactionDao;
    private final SettingsDao settingsDao;
    private final SessionManager sessionManager;

    public TransactionRepository(Context context) {
        this.transactionDao = new TransactionDao(context);
        this.settingsDao = new SettingsDao(context);
        this.sessionManager = SessionManager.get(context);
    }

    public Transaction save(Transaction transaction) {
        String now = DateUtils.nowIso();
        if (transaction.id == null || transaction.id.isEmpty()) {
            transaction.id = Ids.newId();
        }
        if (transaction.createdAt == null) {
            transaction.createdAt = now;
        }
        transaction.updatedAt = now;
        transaction.deleted = false;
        transactionDao.upsert(transaction);
        enqueue(transaction.userId, Constants.TABLE_TRANSACTIONS, transaction.id);
        return transaction;
    }

    public void delete(String userId, String id) {
        transactionDao.softDelete(userId, id, DateUtils.nowIso());
        enqueue(userId, Constants.TABLE_TRANSACTIONS, id);
    }

    public Transaction find(String userId, String id) {
        return transactionDao.findById(userId, id);
    }

    public List<Transaction> recent(String userId, int limit) {
        return transactionDao.recent(userId, limit);
    }

    public List<Transaction> query(String userId, String search, String categoryId, String type,
                                   String fromIso, String toIso, String sort) {
        return transactionDao.query(userId, search, categoryId, type, fromIso, toIso, sort, 0);
    }

    public long totalIncome(String userId) {
        return transactionDao.totalIncome(userId);
    }

    public long totalExpense(String userId) {
        return transactionDao.totalExpense(userId);
    }

    public long sumIncome(String userId, String fromIso, String toIso) {
        return transactionDao.sumByType(userId, Constants.TYPE_INCOME, fromIso, toIso);
    }

    public long sumExpense(String userId, String fromIso, String toIso) {
        return transactionDao.sumByType(userId, Constants.TYPE_EXPENSE, fromIso, toIso);
    }

    public long sumFood(String userId, String fromIso, String toIso) {
        return transactionDao.sumFood(userId, fromIso, toIso);
    }

    public List<ReportSummary.CategoryTotal> getCategoryTotals(String userId, String fromIso, String toIso) {
        return transactionDao.sumByCategory(userId, fromIso, toIso);
    }

    private void enqueue(String userId, String table, String recordId) {
        Session session = sessionManager.getSession();
        if (session != null && !session.localOnly) {
            settingsDao.enqueue(userId, table, recordId);
        }
    }
}
