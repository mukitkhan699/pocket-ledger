package com.pocketledger.models;

import java.util.ArrayList;
import java.util.List;

public class ReportSummary {
    public long incomeMinor;
    public long expenseMinor;
    public long foodMinor;
    public long netMinor;
    public long averageDailyMinor;
    public String highestDayDate;
    public long highestDayMinor;
    public long totalMealConsumedMinor;
    public long totalMealPlannedMinor;
    public final List<CategoryTotal> categoryTotals = new ArrayList<>();
    public final List<DayTotal> dailyTotals = new ArrayList<>();
    public final List<MonthTotal> monthlyTotals = new ArrayList<>();
    public final List<MonthTotal> chartMonthlyTotals = new ArrayList<>();
    public final List<MonthTotal> incomeMonthlyTotals = new ArrayList<>();
    public final List<MonthTotal> incomeChartMonthlyTotals = new ArrayList<>();

    public static class CategoryTotal {
        public String categoryId;
        public String name;
        public String color;
        public String icon;
        public long amountMinor;

        public CategoryTotal(String categoryId, String name, String color, String icon, long amountMinor) {
            this.categoryId = categoryId;
            this.name = name;
            this.color = color;
            this.icon = icon;
            this.amountMinor = amountMinor;
        }
    }

    public static class DayTotal {
        public String date;
        public long amountMinor;

        public DayTotal(String date, long amountMinor) {
            this.date = date;
            this.amountMinor = amountMinor;
        }
    }

    public static class MonthTotal {
        public String label;
        public long amountMinor;

        public MonthTotal(String label, long amountMinor) {
            this.label = label;
            this.amountMinor = amountMinor;
        }
    }
}
