package com.pocketledger.repositories;

import android.content.Context;

import com.pocketledger.database.MealDao;
import com.pocketledger.database.TransactionDao;
import com.pocketledger.models.ReportSummary;
import com.pocketledger.models.Transaction;
import com.pocketledger.utils.Constants;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ReportRepository {

    private final TransactionDao transactionDao;
    private final MealDao mealDao;

    public ReportRepository(Context context) {
        this.transactionDao = new TransactionDao(context);
        this.mealDao = new MealDao(context);
    }

    public LocalDate[] range(String period, LocalDate anchor) {
        switch (period) {
            case "daily":
                return new LocalDate[]{anchor, anchor};
            case "weekly":
                return new LocalDate[]{DateUtils.startOfWeek(anchor), DateUtils.endOfWeek(anchor)};
            case "yearly":
                return new LocalDate[]{DateUtils.startOfYear(anchor), DateUtils.endOfYear(anchor)};
            default:
                return new LocalDate[]{DateUtils.startOfMonth(anchor), DateUtils.endOfMonth(anchor)};
        }
    }

    public ReportSummary build(String userId, String period, LocalDate anchor) {
        LocalDate[] range = range(period, anchor);
        LocalDate start = range[0];
        LocalDate end = range[1];
        ReportSummary summary = new ReportSummary();

        List<Transaction> transactions = transactionDao.listForRange(userId,
                DateUtils.startOfDayIso(start), DateUtils.endOfDayIso(end));
        BigDecimal income = Money.ZERO;
        BigDecimal expense = Money.ZERO;
        TreeMap<LocalDate, BigDecimal> daily = new TreeMap<>();
        for (Transaction transaction : transactions) {
            BigDecimal amount = Money.of(transaction.amountMinor);
            if (transaction.isIncome()) {
                income = income.add(amount);
            } else {
                expense = expense.add(amount);
                LocalDate date = DateUtils.localDateOf(transaction.occurredAt);
                BigDecimal current = daily.get(date);
                daily.put(date, current == null ? amount : current.add(amount));
            }
        }
        summary.incomeMinor = Money.toMinor(income);
        summary.expenseMinor = Money.toMinor(expense);
        summary.netMinor = Money.toMinor(income.subtract(expense));
        summary.foodMinor = transactionDao.sumFood(userId,
                DateUtils.startOfDayIso(start), DateUtils.endOfDayIso(end));
        long days = DateUtils.daysInclusive(start, end);
        summary.averageDailyMinor = Money.toMinor(expense.divide(
                BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP));

        LocalDate highestDate = null;
        BigDecimal highest = Money.ZERO;
        for (Map.Entry<LocalDate, BigDecimal> entry : daily.entrySet()) {
            if (entry.getValue().compareTo(highest) > 0) {
                highest = entry.getValue();
                highestDate = entry.getKey();
            }
        }
        summary.highestDayDate = highestDate == null ? null : highestDate.toString();
        summary.highestDayMinor = Money.toMinor(highest);

        summary.categoryTotals.addAll(transactionDao.sumByCategory(userId,
                DateUtils.startOfDayIso(start), DateUtils.endOfDayIso(end)));
        summary.totalMealConsumedMinor = mealDao.sumState(userId, start.toString(), end.toString(),
                Constants.STATE_CONSUMED);
        summary.totalMealPlannedMinor = mealDao.sumState(userId, start.toString(), end.toString(),
                Constants.STATE_PLANNED);

        buildChart(userId, period, anchor, summary);
        buildMonthlyComparison(userId, anchor, summary);
        return summary;
    }

    private void buildChart(String userId, String period, LocalDate anchor, ReportSummary summary) {
        if ("yearly".equals(period)) {
            int year = anchor.getYear();
            for (int month = 1; month <= 12; month++) {
                YearMonth yearMonth = YearMonth.of(year, month);
                long expenseTotal = transactionDao.sumByType(userId, Constants.TYPE_EXPENSE,
                        DateUtils.startOfDayIso(yearMonth.atDay(1)),
                        DateUtils.endOfDayIso(yearMonth.atEndOfMonth()));
                long incomeTotal = transactionDao.sumByType(userId, Constants.TYPE_INCOME,
                        DateUtils.startOfDayIso(yearMonth.atDay(1)),
                        DateUtils.endOfDayIso(yearMonth.atEndOfMonth()));
                summary.chartMonthlyTotals.add(new ReportSummary.MonthTotal(
                        DateUtils.shortMonth(yearMonth), expenseTotal));
                summary.incomeChartMonthlyTotals.add(new ReportSummary.MonthTotal(
                        DateUtils.shortMonth(yearMonth), incomeTotal));
            }
            return;
        }

        LocalDate chartStart;
        LocalDate chartEnd;
        if ("daily".equals(period)) {
            chartStart = anchor.minusDays(6);
            chartEnd = anchor;
        } else {
            LocalDate[] range = range(period, anchor);
            chartStart = range[0];
            chartEnd = range[1];
        }
        TreeMap<LocalDate, BigDecimal> chartDaily = new TreeMap<>();
        TreeMap<LocalDate, BigDecimal> incomeChartDaily = new TreeMap<>();
        List<Transaction> chartTransactions = transactionDao.listForRange(userId,
                DateUtils.startOfDayIso(chartStart), DateUtils.endOfDayIso(chartEnd));
        for (Transaction transaction : chartTransactions) {
            LocalDate date = DateUtils.localDateOf(transaction.occurredAt);
            BigDecimal amount = Money.of(transaction.amountMinor);
            if (transaction.isIncome()) {
                BigDecimal current = incomeChartDaily.get(date);
                incomeChartDaily.put(date, current == null ? amount : current.add(amount));
            } else {
                BigDecimal current = chartDaily.get(date);
                chartDaily.put(date, current == null ? amount : current.add(amount));
            }
        }
        LocalDate cursor = chartStart;
        while (!cursor.isAfter(chartEnd)) {
            BigDecimal expenseValue = chartDaily.get(cursor);
            BigDecimal incomeValue = incomeChartDaily.get(cursor);
            summary.dailyTotals.add(new ReportSummary.DayTotal(
                    cursor.toString(), expenseValue == null ? 0L : Money.toMinor(expenseValue)));
            summary.incomeChartMonthlyTotals.add(new ReportSummary.MonthTotal(
                    cursor.toString(), incomeValue == null ? 0L : Money.toMinor(incomeValue)));
            cursor = cursor.plusDays(1);
        }
    }

    private void buildMonthlyComparison(String userId, LocalDate anchor, ReportSummary summary) {
        YearMonth current = YearMonth.from(anchor);
        for (int i = 5; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            long expenseTotal = transactionDao.sumByType(userId, Constants.TYPE_EXPENSE,
                    DateUtils.startOfDayIso(month.atDay(1)),
                    DateUtils.endOfDayIso(month.atEndOfMonth()));
            long incomeTotal = transactionDao.sumByType(userId, Constants.TYPE_INCOME,
                    DateUtils.startOfDayIso(month.atDay(1)),
                    DateUtils.endOfDayIso(month.atEndOfMonth()));
            summary.monthlyTotals.add(new ReportSummary.MonthTotal(DateUtils.shortMonth(month), expenseTotal));
            summary.incomeMonthlyTotals.add(new ReportSummary.MonthTotal(DateUtils.shortMonth(month), incomeTotal));
        }
    }
}
