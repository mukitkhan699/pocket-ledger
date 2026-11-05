package com.pocketledger.fragments;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.pocketledger.R;
import com.pocketledger.activities.CategoriesActivity;
import com.pocketledger.activities.EditTransactionActivity;
import com.pocketledger.activities.MainActivity;
import com.pocketledger.activities.MealsActivity;
import com.pocketledger.activities.TransactionDetailActivity;
import com.pocketledger.adapters.CategoryBreakdownAdapter;
import com.pocketledger.adapters.DateFilterAdapter;
import com.pocketledger.adapters.RecentTransactionsAdapter;
import com.pocketledger.customviews.BarChartView;
import com.pocketledger.customviews.SegmentedControl;
import com.pocketledger.models.CategoryBreakdownItem;
import com.pocketledger.models.DashboardSummary;
import com.pocketledger.models.MealEntry;
import com.pocketledger.models.MealSettings;
import com.pocketledger.models.ReportSummary;
import com.pocketledger.models.Session;
import com.pocketledger.models.Transaction;
import com.pocketledger.network.NetworkMonitor;
import com.pocketledger.repositories.DashboardRepository;
import com.pocketledger.repositories.MealRepository;
import com.pocketledger.repositories.ReportRepository;
import com.pocketledger.repositories.TransactionRepository;
import com.pocketledger.sync.SyncManager;
import com.pocketledger.sync.SyncResult;
import com.pocketledger.utils.AppExecutors;
import com.pocketledger.utils.CategoryIcons;
import com.pocketledger.utils.Constants;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Money;
import com.pocketledger.utils.Prefs;
import com.pocketledger.utils.SessionManager;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    // Header elements
    private TextView greetingText;
    private TextView userName;
    private TextView avatar;
    private ImageButton notificationIcon;

    // Date filter
    private DateFilterAdapter dateFilterAdapter;
    private RecyclerView dateFilterRecyclerView;
    private TextView dateFilterLabel;

    // Segmented control
    private SegmentedControl dashboardSegmentControl;

    // Net Worth card
    private TextView netWorthChange;
    private TextView netWorthAmount;
    private FrameLayout netWorthChartContainer;
    private TextView monthlyNetWorth;
    private TextView previousMonthNetWorth;

    // Metric cards (replacing fake Cash/Savings/Investment)
    private TextView totalBalanceCard;
    private TextView monthlyNetCard;
    private TextView loanNetCard;

    // Income/Expense cards
    private TextView incomeAmount;
    private FrameLayout incomeChartContainer;
    private TextView incomeMonthLabel;
    private TextView expenseAmount;
    private FrameLayout expenseChartContainer;
    private TextView expenseMonthLabel;

    // Trends chart
    private FrameLayout trendsChartContainer;
    private LinearLayout trendsChartSection;
    private TextView trendsPeriodLabel;

    // Category breakdown
    private TextView categoryViewAll;
    private RecyclerView categoryBreakdownRecyclerView;

    // Recent transactions
    private TextView transactionsViewAll;
    private RecyclerView recentTransactionsRecyclerView;
    private LinearLayout recentTransactionsEmpty;

    // Monthly savings summary
    private TextView savedAmount;
    private TextView savingsTarget;
    private LinearProgressIndicator savingsProgress;
    private TextView savingsProgressText;

    // Quick add button
    private MaterialButton quickAddButton;

    private boolean balanceHidden;
    private long totalBalanceMinor;
    private String currentSegment = "summary";
    private String currentDateFilter = "This month";
    private LocalDate customStartDate;
    private LocalDate customEndDate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Header elements
        greetingText = view.findViewById(R.id.greetingText);
        userName = view.findViewById(R.id.userName);
        avatar = view.findViewById(R.id.avatar);
        notificationIcon = view.findViewById(R.id.notificationIcon);

        // Date filter
        dateFilterRecyclerView = view.findViewById(R.id.dateFilterRecyclerView);
        dateFilterRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        dateFilterAdapter = new DateFilterAdapter();
        dateFilterAdapter.setOnDateFilterSelectedListener((position, filter) -> {
            if ("Custom".equals(filter)) {
                showCustomDatePicker();
            } else {
                currentDateFilter = filter;
                dateFilterLabel.setText(filter);
                refreshData();
            }
        });
        dateFilterRecyclerView.setAdapter(dateFilterAdapter);

        dateFilterLabel = view.findViewById(R.id.dateFilterLabel);

        // Segmented control
        dashboardSegmentControl = view.findViewById(R.id.dashboardSegmentControl);
        dashboardSegmentControl.setItems(new String[]{"Summary", "Trends", "Categories"},
                new String[]{"summary", "trends", "categories"});
        dashboardSegmentControl.setSelectedValue("summary");
        dashboardSegmentControl.setOnSelectionChanged((index, value) -> {
            currentSegment = value;
            refreshData();
        });

        // Net Worth card
        netWorthChange = view.findViewById(R.id.netWorthChange);
        netWorthAmount = view.findViewById(R.id.netWorthAmount);
        netWorthChartContainer = view.findViewById(R.id.netWorthChartContainer);
        monthlyNetWorth = view.findViewById(R.id.monthlyNetWorth);
        previousMonthNetWorth = view.findViewById(R.id.previousMonthNetWorth);

        // Metric cards (replacing fake Cash/Savings/Investment)
        totalBalanceCard = view.findViewById(R.id.totalBalanceCard);
        monthlyNetCard = view.findViewById(R.id.monthlyNetCard);
        loanNetCard = view.findViewById(R.id.loanNetCard);

        // Income/Expense cards
        incomeAmount = view.findViewById(R.id.incomeAmount);
        incomeChartContainer = view.findViewById(R.id.incomeChartContainer);
        incomeMonthLabel = view.findViewById(R.id.incomeMonthLabel);
        expenseAmount = view.findViewById(R.id.expenseAmount);
        expenseChartContainer = view.findViewById(R.id.expenseChartContainer);
        expenseMonthLabel = view.findViewById(R.id.expenseMonthLabel);

        // Trends chart
        trendsChartContainer = view.findViewById(R.id.trendsChartContainer);
        trendsChartSection = view.findViewById(R.id.trendsChartSection);
        trendsPeriodLabel = view.findViewById(R.id.trendsPeriodLabel);

        // Category breakdown
        categoryViewAll = view.findViewById(R.id.categoryViewAll);
        categoryBreakdownRecyclerView = view.findViewById(R.id.categoryBreakdownRecyclerView);
        categoryBreakdownRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Recent transactions
        transactionsViewAll = view.findViewById(R.id.transactionsViewAll);
        recentTransactionsRecyclerView = view.findViewById(R.id.recentTransactionsRecyclerView);
        recentTransactionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentTransactionsEmpty = view.findViewById(R.id.recentTransactionsEmpty);

        // Monthly savings summary
        savedAmount = view.findViewById(R.id.savedAmount);
        savingsTarget = view.findViewById(R.id.savingsTarget);
        savingsProgress = view.findViewById(R.id.savingsProgress);
        savingsProgressText = view.findViewById(R.id.savingsProgressText);

        // Quick add button
        quickAddButton = view.findViewById(R.id.quickAddButton);

        // Set up listeners
        notificationIcon.setOnClickListener(v -> {
            // Navigate to notifications/settings
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).showTab(R.id.nav_settings);
            }
        });

        categoryViewAll.setOnClickListener(v -> {
            // Navigate to categories
            startActivity(new Intent(requireContext(), CategoriesActivity.class));
        });

        transactionsViewAll.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openTransactions();
            }
        });

        quickAddButton.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), EditTransactionActivity.class));
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    public void refresh() {
        Session session = SessionManager.get(requireContext()).getSession();
        if (session == null) return;

        greetingText.setText(DateUtils.greeting());
        String name = session.fullName == null || session.fullName.trim().isEmpty()
                ? "Friend" : session.fullName.trim();
        userName.setText(name);
        avatar.setText(name.substring(0, 1).toUpperCase());

        if (session.localOnly) {
            netWorthChange.setText(R.string.local_only_badge);
        } else if (!NetworkMonitor.isOnline(requireContext())) {
            netWorthChange.setText(R.string.offline);
        } else {
            netWorthChange.setText(R.string.cloud_connected);
        }

        refreshData();
    }

    private void refreshData() {
        AppExecutors.io().execute(() -> {
            Session session = SessionManager.get(requireContext()).getSession();
            if (session == null) return;

            String[] dateRange = getDateRangeForFilter(currentDateFilter);
            String fromIso = dateRange[0];
            String toIso = dateRange[1];

            DashboardSummary summary = new DashboardRepository(requireContext()).load(session.userId, fromIso, toIso);

            // Load trend data for Trends tab
            ReportSummary trendSummary = null;
            if ("trends".equals(currentSegment)) {
                LocalDate anchor = getAnchorDateForFilter(currentDateFilter, dateRange);
                trendSummary = new ReportRepository(requireContext()).build(session.userId, getPeriodForFilter(currentDateFilter), anchor);
            }

            final ReportSummary finalTrendSummary = trendSummary;

            AppExecutors.main(() -> {
                if (!isAdded()) return;

                // Update Net Worth card
                totalBalanceMinor = summary.totalBalanceMinor;
                netWorthAmount.setText(Money.format(totalBalanceMinor));

                // Update metric cards - use REAL data from DashboardSummary
                long periodNet = summary.monthIncomeMinor - summary.monthExpenseMinor;
                long loanNet = summary.lentOutstandingMinor - summary.borrowedOutstandingMinor;

                totalBalanceCard.setText(Money.format(summary.totalBalanceMinor));
                monthlyNetCard.setText(Money.format(periodNet));
                loanNetCard.setText(Money.format(loanNet));

                // Update income/expense cards
                incomeAmount.setText(Money.format(summary.incomeMinor));
                expenseAmount.setText(Money.format(summary.expenseMinor));

                // Update date filter label
                dateFilterLabel.setText(currentDateFilter);

                // Load recent transactions (filtered by date range)
                loadRecentTransactions(session.userId, fromIso, toIso);

                // Handle segment-specific content
                switch (currentSegment) {
                    case "summary":
                        showSummaryContent(summary, periodNet);
                        break;
                    case "trends":
                        showTrendsContent(finalTrendSummary, fromIso, toIso);
                        break;
                    case "categories":
                        showCategoriesContent(session.userId, fromIso, toIso);
                        break;
                }

                // Update savings summary
                updateSavingsSummary();
            });
        });
    }

    private String[] getDateRangeForFilter(String filter) {
        LocalDate today = DateUtils.today();
        LocalDate start, end;

        switch (filter) {
            case "This month":
                start = DateUtils.startOfMonth(today);
                end = DateUtils.endOfMonth(today);
                break;
            case "Last month":
                start = DateUtils.startOfMonth(today.minusMonths(1));
                end = DateUtils.endOfMonth(today.minusMonths(1));
                break;
            case "This year":
                start = DateUtils.startOfYear(today);
                end = DateUtils.endOfYear(today);
                break;
            case "Last year":
                start = DateUtils.startOfYear(today.minusYears(1));
                end = DateUtils.endOfYear(today.minusYears(1));
                break;
            case "Custom":
                if (customStartDate != null && customEndDate != null) {
                    start = customStartDate;
                    end = customEndDate;
                } else {
                    // Fallback to this month
                    start = DateUtils.startOfMonth(today);
                    end = DateUtils.endOfMonth(today);
                }
                break;
            default:
                // Default to this month
                start = DateUtils.startOfMonth(today);
                end = DateUtils.endOfMonth(today);
                break;
        }

        return new String[]{
            DateUtils.startOfDayIso(start),
            DateUtils.endOfDayIso(end)
        };
    }

    private void showSummaryContent(DashboardSummary summary, long periodNet) {
        // Show income/expense cards and charts
        incomeChartContainer.setVisibility(View.VISIBLE);
        expenseChartContainer.setVisibility(View.VISIBLE);
        incomeAmount.setVisibility(View.VISIBLE);
        expenseAmount.setVisibility(View.VISIBLE);

        // Update summary charts
        setupCharts(summary.incomeMinor, summary.expenseMinor);

        // Update monthly net worth
        monthlyNetWorth.setText(Money.format(periodNet));
        previousMonthNetWorth.setText(Money.format(periodNet));

        // Show net worth card details
        netWorthChartContainer.setVisibility(View.VISIBLE);

        // Show recent transactions
        transactionsViewAll.setVisibility(View.VISIBLE);
        recentTransactionsRecyclerView.setVisibility(View.VISIBLE);

        // Hide trends and categories
        if (trendsChartSection != null) trendsChartSection.setVisibility(View.GONE);
        categoryBreakdownRecyclerView.setVisibility(View.GONE);
        categoryViewAll.setVisibility(View.GONE);
    }

    private void showTrendsContent(ReportSummary trendSummary, String fromIso, String toIso) {
        // Hide summary content
        incomeChartContainer.setVisibility(View.GONE);
        expenseChartContainer.setVisibility(View.GONE);
        incomeAmount.setVisibility(View.GONE);
        expenseAmount.setVisibility(View.GONE);
        netWorthChartContainer.setVisibility(View.GONE);
        transactionsViewAll.setVisibility(View.GONE);
        recentTransactionsRecyclerView.setVisibility(View.GONE);
        recentTransactionsEmpty.setVisibility(View.GONE);
        categoryBreakdownRecyclerView.setVisibility(View.GONE);
        categoryViewAll.setVisibility(View.GONE);

        // Show trends chart
        if (trendsChartSection != null) trendsChartSection.setVisibility(View.VISIBLE);

        if (trendSummary != null && trendsChartContainer != null) {
            setupTrendsChart(trendSummary);
            trendsPeriodLabel.setText(currentDateFilter);
        }
    }

    private void showCategoriesContent(String userId, String fromIso, String toIso) {
        // Hide summary content
        incomeChartContainer.setVisibility(View.GONE);
        expenseChartContainer.setVisibility(View.GONE);
        incomeAmount.setVisibility(View.GONE);
        expenseAmount.setVisibility(View.GONE);
        netWorthChartContainer.setVisibility(View.GONE);
        transactionsViewAll.setVisibility(View.GONE);
        recentTransactionsRecyclerView.setVisibility(View.GONE);
        recentTransactionsEmpty.setVisibility(View.GONE);

        // Show categories
        categoryBreakdownRecyclerView.setVisibility(View.VISIBLE);
        categoryViewAll.setVisibility(View.VISIBLE);

        // Hide trends
        if (trendsChartSection != null) trendsChartSection.setVisibility(View.GONE);

        // Load category breakdown
        loadCategoryBreakdown(userId, fromIso, toIso);
    }

    private LocalDate getAnchorDateForFilter(String filter, String[] dateRange) {
        // Use the end date as anchor
        LocalDate today = DateUtils.today();
        switch (filter) {
            case "This month":
            case "Last month":
            case "This year":
            case "Last year":
                return today;
            case "Custom":
                if (customEndDate != null) return customEndDate;
                return today;
            default:
                return today;
        }
    }

    private String getPeriodForFilter(String filter) {
        switch (filter) {
            case "This month":
            case "Last month":
                return "monthly";
            case "This year":
            case "Last year":
                return "yearly";
            case "Custom":
                // Determine period based on range length
                if (customStartDate != null && customEndDate != null) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(customStartDate, customEndDate);
                    if (days <= 7) return "daily";
                    if (days <= 31) return "monthly";
                    return "yearly";
                }
                return "monthly";
            default:
                return "monthly";
        }
    }

    private void setupTrendsChart(ReportSummary trendSummary) {
        BarChartView trendsChart = new BarChartView(requireContext());
        trendsChart.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        trendsChartContainer.removeAllViews();
        trendsChartContainer.addView(trendsChart);

        // Use monthly totals for trend chart - both income and expense
        List<Long> expenseValues = new ArrayList<>();
        List<Long> incomeValues = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        if (trendSummary.monthlyTotals != null && !trendSummary.monthlyTotals.isEmpty()
                && trendSummary.incomeMonthlyTotals != null && !trendSummary.incomeMonthlyTotals.isEmpty()) {
            // Monthly comparison (6 months)
            for (int i = 0; i < trendSummary.monthlyTotals.size(); i++) {
                ReportSummary.MonthTotal expenseMt = trendSummary.monthlyTotals.get(i);
                ReportSummary.MonthTotal incomeMt = i < trendSummary.incomeMonthlyTotals.size()
                        ? trendSummary.incomeMonthlyTotals.get(i) : new ReportSummary.MonthTotal("", 0L);
                expenseValues.add(expenseMt.amountMinor);
                incomeValues.add(incomeMt.amountMinor);
                labels.add(expenseMt.label);
            }
        } else if (trendSummary.chartMonthlyTotals != null && !trendSummary.chartMonthlyTotals.isEmpty()
                && trendSummary.incomeChartMonthlyTotals != null && !trendSummary.incomeChartMonthlyTotals.isEmpty()) {
            // Yearly chart (12 months)
            for (int i = 0; i < trendSummary.chartMonthlyTotals.size(); i++) {
                ReportSummary.MonthTotal expenseMt = trendSummary.chartMonthlyTotals.get(i);
                ReportSummary.MonthTotal incomeMt = i < trendSummary.incomeChartMonthlyTotals.size()
                        ? trendSummary.incomeChartMonthlyTotals.get(i) : new ReportSummary.MonthTotal("", 0L);
                expenseValues.add(expenseMt.amountMinor);
                incomeValues.add(incomeMt.amountMinor);
                labels.add(expenseMt.label);
            }
        } else if (trendSummary.dailyTotals != null && !trendSummary.dailyTotals.isEmpty()
                && trendSummary.incomeChartMonthlyTotals != null && !trendSummary.incomeChartMonthlyTotals.isEmpty()) {
            // Daily chart (7 days)
            for (int i = 0; i < trendSummary.dailyTotals.size(); i++) {
                ReportSummary.DayTotal expenseDt = trendSummary.dailyTotals.get(i);
                ReportSummary.MonthTotal incomeMt = i < trendSummary.incomeChartMonthlyTotals.size()
                        ? trendSummary.incomeChartMonthlyTotals.get(i) : new ReportSummary.MonthTotal("", 0L);
                expenseValues.add(expenseDt.amountMinor);
                incomeValues.add(incomeMt.amountMinor);
                labels.add(DateUtils.weekdayShort(LocalDate.parse(expenseDt.date)));
            }
        }

        trendsChart.setData(expenseValues, incomeValues, labels);
        trendsChart.setShowLegend(true);
    }

    private void setupCharts(long income, long expense) {
        // Income chart
        View incomeView = LayoutInflater.from(requireContext())
                .inflate(R.layout.view_chart_bar, null);
        TextView incomeLabel = incomeView.findViewById(R.id.chartLabel);
        View incomeBar = incomeView.findViewById(R.id.chartBar);
        incomeLabel.setText("Income");
        LinearLayout.LayoutParams incomeParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        if (income > 0 && expense > 0) {
            incomeParams.weight = (float) income / (income + expense);
        } else {
            incomeParams.weight = 1f;
        }
        incomeBar.setLayoutParams(incomeParams);
        incomeBar.setBackgroundColor(requireContext().getColor(R.color.income));
        incomeChartContainer.removeAllViews();
        incomeChartContainer.addView(incomeView);

        // Expense chart
        View expenseView = LayoutInflater.from(requireContext())
                .inflate(R.layout.view_chart_bar, null);
        TextView expenseLabel = expenseView.findViewById(R.id.chartLabel);
        View expenseBar = expenseView.findViewById(R.id.chartBar);
        expenseLabel.setText("Expense");
        LinearLayout.LayoutParams expenseParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        if (income > 0 && expense > 0) {
            expenseParams.weight = (float) expense / (income + expense);
        } else {
            expenseParams.weight = 1f;
        }
        expenseBar.setLayoutParams(expenseParams);
        expenseBar.setBackgroundColor(requireContext().getColor(R.color.expense));
        expenseChartContainer.removeAllViews();
        expenseChartContainer.addView(expenseView);
    }

    private void loadRecentTransactions(String userId, String fromIso, String toIso) {
        AppExecutors.io().execute(() -> {
            List<Transaction> transactions = new TransactionRepository(requireContext())
                    .query(userId, null, null, null, fromIso, toIso, "newest");

            AppExecutors.main(() -> {
                if (!isAdded()) return;

                if (transactions.isEmpty()) {
                    recentTransactionsRecyclerView.setVisibility(View.GONE);
                    recentTransactionsEmpty.setVisibility(View.VISIBLE);
                } else {
                    recentTransactionsRecyclerView.setVisibility(View.VISIBLE);
                    recentTransactionsEmpty.setVisibility(View.GONE);
                    RecentTransactionsAdapter adapter = new RecentTransactionsAdapter(transactions);
                    recentTransactionsRecyclerView.setAdapter(adapter);
                }
            });
        });
    }

    private void loadCategoryBreakdown(String userId, String fromIso, String toIso) {
        AppExecutors.io().execute(() -> {
            List<ReportSummary.CategoryTotal> categoryTotals = new TransactionRepository(requireContext()).getCategoryTotals(userId, fromIso, toIso);

            AppExecutors.main(() -> {
                if (!isAdded()) return;

                List<CategoryBreakdownItem> categories = new ArrayList<>();
                long total = categoryTotals.stream().mapToLong(c -> c.amountMinor).sum();
                if (total == 0) total = 1;

                for (ReportSummary.CategoryTotal ct : categoryTotals) {
                    categories.add(new CategoryBreakdownItem(ct.name, ct.color, ct.amountMinor, total));
                }

                if (categories.isEmpty()) {
                    categoryBreakdownRecyclerView.setVisibility(View.GONE);
                    categoryViewAll.setVisibility(View.GONE);
                } else {
                    categoryBreakdownRecyclerView.setVisibility(View.VISIBLE);
                    categoryViewAll.setVisibility(View.VISIBLE);
                    CategoryBreakdownAdapter adapter = new CategoryBreakdownAdapter(categories);
                    categoryBreakdownRecyclerView.setAdapter(adapter);
                }
            });
        });
    }

    private void updateSavingsSummary() {
        AppExecutors.io().execute(() -> {
            Session session = SessionManager.get(requireContext()).getSession();
            if (session == null) return;

            String[] dateRange = getDateRangeForFilter(currentDateFilter);
            String fromIso = dateRange[0];
            String toIso = dateRange[1];

            DashboardSummary summary = new DashboardRepository(requireContext()).load(session.userId, fromIso, toIso);
            long periodNet = summary.monthIncomeMinor - summary.monthExpenseMinor;

            AppExecutors.main(() -> {
                if (!isAdded()) return;
                savedAmount.setText(Money.format(periodNet));
                savingsTarget.setText(Money.format(summary.monthIncomeMinor));
                if (summary.monthIncomeMinor > 0) {
                    int progress = (int) ((periodNet * 100L) / summary.monthIncomeMinor);
                    savingsProgress.setProgress(Math.max(0, Math.min(100, progress)));
                    savingsProgressText.setText(progress + "% of income saved");
                } else {
                    savingsProgress.setProgress(0);
                    savingsProgressText.setText("No income in period");
                }
            });
        });
    }

    private void showCustomDatePicker() {
        DatePickerDialog startPicker = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    customStartDate = LocalDate.of(year, month + 1, dayOfMonth);
                    showEndDatePicker();
                },
                customStartDate != null ? customStartDate.getYear() : DateUtils.today().getYear(),
                customStartDate != null ? customStartDate.getMonthValue() - 1 : DateUtils.today().getMonthValue() - 1,
                customStartDate != null ? customStartDate.getDayOfMonth() : DateUtils.today().getDayOfMonth());
        startPicker.setTitle("Select Start Date");
        startPicker.show();
    }

    private void showEndDatePicker() {
        LocalDate minEndDate = customStartDate != null ? customStartDate : DateUtils.today();
        DatePickerDialog endPicker = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    customEndDate = LocalDate.of(year, month + 1, dayOfMonth);
                    if (customEndDate.isBefore(customStartDate)) {
                        customEndDate = customStartDate;
                    }
                    currentDateFilter = "Custom";
                    dateFilterLabel.setText("Custom: " + DateUtils.dateForDisplay(customStartDate) + " – " + DateUtils.dateForDisplay(customEndDate));
                    refreshData();
                },
                customEndDate != null ? customEndDate.getYear() : DateUtils.today().getYear(),
                customEndDate != null ? customEndDate.getMonthValue() - 1 : DateUtils.today().getMonthValue() - 1,
                customEndDate != null ? customEndDate.getDayOfMonth() : DateUtils.today().getDayOfMonth());
        endPicker.setTitle("Select End Date");
        long minDateMillis = minEndDate.atStartOfDay(DateUtils.ZONE).toInstant().toEpochMilli();
        endPicker.getDatePicker().setMinDate(minDateMillis);
        endPicker.show();
    }
}