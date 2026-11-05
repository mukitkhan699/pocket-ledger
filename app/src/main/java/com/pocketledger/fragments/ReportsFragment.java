package com.pocketledger.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.pocketledger.R;
import com.pocketledger.customviews.BarChartView;
import com.pocketledger.customviews.SegmentedControl;
import com.pocketledger.models.ReportSummary;
import com.pocketledger.models.Session;
import com.pocketledger.repositories.ReportRepository;
import com.pocketledger.utils.AppExecutors;
import com.pocketledger.utils.CategoryIcons;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Money;
import com.pocketledger.utils.SessionManager;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class ReportsFragment extends Fragment {

    private SegmentedControl periodControl;
    private TextView periodLabel;
    private TextView reportIncome;
    private TextView reportExpense;
    private TextView reportNet;
    private TextView reportAverage;
    private TextView reportFood;
    private TextView reportHighest;
    private TextView reportMeals;
    private TextView reportEmpty;
    private TextView categoryEmpty;
    private LinearLayout categoryContainer;
    private BarChartView reportChart;
    private BarChartView comparisonChart;
    private LocalDate anchor = DateUtils.today();
    private String period = "monthly";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports, container, false);
        periodControl = view.findViewById(R.id.periodControl);
        periodLabel = view.findViewById(R.id.periodLabel);
        reportIncome = view.findViewById(R.id.reportIncome);
        reportExpense = view.findViewById(R.id.reportExpense);
        reportNet = view.findViewById(R.id.reportNet);
        reportAverage = view.findViewById(R.id.reportAverage);
        reportFood = view.findViewById(R.id.reportFood);
        reportHighest = view.findViewById(R.id.reportHighest);
        reportMeals = view.findViewById(R.id.reportMeals);
        reportEmpty = view.findViewById(R.id.reportEmpty);
        categoryEmpty = view.findViewById(R.id.categoryEmpty);
        categoryContainer = view.findViewById(R.id.categoryContainer);
        reportChart = view.findViewById(R.id.reportChart);
        comparisonChart = view.findViewById(R.id.comparisonChart);
        periodControl.setItems(new String[]{"Daily", "Weekly", "Monthly", "Yearly"},
                new String[]{"daily", "weekly", "monthly", "yearly"});
        periodControl.setSelectedValue(period);
        periodControl.setOnSelectionChanged((index, value) -> {
            period = value;
            load();
        });
        view.findViewById(R.id.periodPrev).setOnClickListener(v -> shift(-1));
        view.findViewById(R.id.periodNext).setOnClickListener(v -> shift(1));
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void shift(int direction) {
        switch (period) {
            case "daily":
                anchor = anchor.plusDays(direction);
                break;
            case "weekly":
                anchor = anchor.plusWeeks(direction);
                break;
            case "yearly":
                anchor = anchor.plusYears(direction);
                break;
            default:
                anchor = anchor.plusMonths(direction);
                break;
        }
        load();
    }

    private void load() {
        Session session = SessionManager.get(requireContext()).getSession();
        if (session == null) {
            return;
        }
        ReportRepository repository = new ReportRepository(requireContext());
        LocalDate[] range = repository.range(period, anchor);
        periodLabel.setText(periodLabel(period, range[0], range[1]));
        AppExecutors.io().execute(() -> {
            ReportSummary summary = repository.build(session.userId, period, anchor);
            AppExecutors.main(() -> {
                if (isAdded()) {
                    bind(summary);
                }
            });
        });
    }

    private String periodLabel(String period, LocalDate start, LocalDate end) {
        switch (period) {
            case "daily":
                return DateUtils.dateForDisplay(start);
            case "weekly":
                return DateUtils.dateForDisplay(start) + " – " + DateUtils.dateForDisplay(end);
            case "yearly":
                return String.valueOf(start.getYear());
            default:
                return DateUtils.monthLabel(YearMonth.from(start));
        }
    }

    private void bind(ReportSummary summary) {
        reportIncome.setText(Money.format(summary.incomeMinor));
        reportExpense.setText(Money.format(summary.expenseMinor));
        reportNet.setText(Money.signed(summary.netMinor, summary.netMinor >= 0));
        reportNet.setTextColor(requireContext().getColor(
                summary.netMinor >= 0 ? R.color.income : R.color.expense));
        reportAverage.setText(Money.format(summary.averageDailyMinor));
        reportFood.setText(Money.format(summary.foodMinor));
        if (summary.highestDayDate == null) {
            reportHighest.setText("—");
        } else {
            reportHighest.setText(DateUtils.dateForDisplay(summary.highestDayDate)
                    + " · " + Money.format(summary.highestDayMinor));
        }
        reportMeals.setText(Money.format(summary.totalMealConsumedMinor));

        List<Long> chartValues = new ArrayList<>();
        List<String> chartLabels = new ArrayList<>();
        if ("yearly".equals(period)) {
            for (ReportSummary.MonthTotal total : summary.chartMonthlyTotals) {
                chartValues.add(total.amountMinor);
                chartLabels.add(total.label);
            }
        } else {
            for (ReportSummary.DayTotal total : summary.dailyTotals) {
                chartValues.add(total.amountMinor);
                LocalDate date = LocalDate.parse(total.date);
                if ("daily".equals(period) || "weekly".equals(period)) {
                    chartLabels.add(DateUtils.weekdayShort(date));
                } else {
                    chartLabels.add(String.valueOf(date.getDayOfMonth()));
                }
            }
        }
        reportChart.setData(chartValues, chartLabels);
        boolean empty = summary.expenseMinor == 0 && summary.incomeMinor == 0;
        reportEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        reportChart.setVisibility(empty ? View.GONE : View.VISIBLE);

        List<Long> comparisonValues = new ArrayList<>();
        List<String> comparisonLabels = new ArrayList<>();
        for (ReportSummary.MonthTotal total : summary.monthlyTotals) {
            comparisonValues.add(total.amountMinor);
            comparisonLabels.add(total.label);
        }
        comparisonChart.setData(comparisonValues, comparisonLabels);

        categoryContainer.removeAllViews();
        categoryEmpty.setVisibility(summary.categoryTotals.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        long total = Math.max(1L, summary.expenseMinor);
        for (ReportSummary.CategoryTotal category : summary.categoryTotals) {
            View row = inflater.inflate(R.layout.item_report_category, categoryContainer, false);
            ImageView icon = row.findViewById(R.id.reportCategoryIcon);
            TextView name = row.findViewById(R.id.reportCategoryName);
            TextView amount = row.findViewById(R.id.reportCategoryAmount);
            ProgressBar bar = row.findViewById(R.id.reportCategoryBar);
            name.setText(category.name);
            amount.setText(Money.format(category.amountMinor));
            icon.setImageResource(CategoryIcons.drawable(category.icon));
            icon.setColorFilter(CategoryIcons.color(category.color));
            bar.setProgress((int) Math.min(100,
                    Math.round(category.amountMinor * 100.0 / total)));
            categoryContainer.addView(row);
        }
    }
}
