package com.pocketledger.repositories;

import android.content.Context;

import com.pocketledger.database.LoanDao;
import com.pocketledger.database.SettingsDao;
import com.pocketledger.database.TransactionDao;
import com.pocketledger.models.DashboardSummary;
import com.pocketledger.utils.Constants;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Money;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DashboardRepository {

    private final TransactionDao transactionDao;
    private final LoanDao loanDao;
    private final SettingsDao settingsDao;

    public DashboardRepository(Context context) {
        this.transactionDao = new TransactionDao(context);
        this.loanDao = new LoanDao(context);
        this.settingsDao = new SettingsDao(context);
    }

    public DashboardSummary load(String userId) {
        return load(userId, null, null);
    }

    public DashboardSummary load(String userId, String fromIso, String toIso) {
        DashboardSummary summary = new DashboardSummary();

        summary.incomeMinor = transactionDao.totalIncome(userId);
        summary.expenseMinor = transactionDao.totalExpense(userId);

        if (fromIso != null && toIso != null) {
            summary.todayIncomeMinor = transactionDao.sumByType(userId, Constants.TYPE_INCOME, fromIso, toIso);
            summary.todayExpenseMinor = transactionDao.sumByType(userId, Constants.TYPE_EXPENSE, fromIso, toIso);
            summary.monthIncomeMinor = transactionDao.sumByType(userId, Constants.TYPE_INCOME, fromIso, toIso);
            summary.monthExpenseMinor = transactionDao.sumByType(userId, Constants.TYPE_EXPENSE, fromIso, toIso);
            summary.monthFoodMinor = transactionDao.sumFood(userId, fromIso, toIso);
        } else {
            LocalDate today = DateUtils.today();
            summary.todayIncomeMinor = transactionDao.sumByType(userId, Constants.TYPE_INCOME,
                    DateUtils.startOfDayIso(today), DateUtils.endOfDayIso(today));
            summary.todayExpenseMinor = transactionDao.sumByType(userId, Constants.TYPE_EXPENSE,
                    DateUtils.startOfDayIso(today), DateUtils.endOfDayIso(today));
            LocalDate monthStart = DateUtils.startOfMonth(today);
            LocalDate monthEnd = DateUtils.endOfMonth(today);
            summary.monthIncomeMinor = transactionDao.sumByType(userId, Constants.TYPE_INCOME,
                    DateUtils.startOfDayIso(monthStart), DateUtils.endOfDayIso(monthEnd));
            summary.monthExpenseMinor = transactionDao.sumByType(userId, Constants.TYPE_EXPENSE,
                    DateUtils.startOfDayIso(monthStart), DateUtils.endOfDayIso(monthEnd));
            summary.monthFoodMinor = transactionDao.sumFood(userId,
                    DateUtils.startOfDayIso(monthStart), DateUtils.endOfDayIso(monthEnd));
        }

        summary.borrowedOutstandingMinor = loanDao.outstanding(userId, Constants.LOAN_BORROWED);
        summary.lentOutstandingMinor = loanDao.outstanding(userId, Constants.LOAN_LENT);

        BigDecimal balance = Money.of(summary.incomeMinor)
                .subtract(Money.of(summary.expenseMinor))
                .add(Money.of(loanDao.balanceContribution(userId)));
        summary.totalBalanceMinor = Money.toMinor(balance);
        summary.pendingSyncCount = settingsDao.queueCount(userId);
        return summary;
    }
}
