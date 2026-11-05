package com.pocketledger.fragments;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.pocketledger.R;
import com.pocketledger.activities.TransactionDetailActivity;
import com.pocketledger.adapters.TransactionAdapter;
import com.pocketledger.models.Category;
import com.pocketledger.models.Session;
import com.pocketledger.models.Transaction;
import com.pocketledger.repositories.CategoryRepository;
import com.pocketledger.repositories.TransactionRepository;
import com.pocketledger.utils.AppExecutors;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.SessionManager;
import com.pocketledger.utils.UiUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransactionsFragment extends Fragment {

    private EditText searchInput;
    private TextView filterSummary;
    private RecyclerView list;
    private View empty;
    private TransactionAdapter adapter;
    private String type;
    private String categoryId;
    private String categoryName;
    private String sort = "newest";
    private LocalDate fromDate;
    private LocalDate toDate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);
        searchInput = view.findViewById(R.id.searchInput);
        filterSummary = view.findViewById(R.id.filterSummary);
        list = view.findViewById(R.id.transactionsList);
        empty = view.findViewById(R.id.transactionsEmpty);
        adapter = new TransactionAdapter(new TransactionAdapter.Listener() {
            @Override
            public void onClick(Transaction transaction) {
                Intent intent = new Intent(requireContext(), TransactionDetailActivity.class);
                intent.putExtra(TransactionDetailActivity.EXTRA_TRANSACTION_ID, transaction.id);
                startActivity(intent);
            }

            @Override
            public void onLongClick(Transaction transaction) {
                confirmDelete(transaction);
            }
        });
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                load();
            }
        });
        view.findViewById(R.id.filterButton).setOnClickListener(v -> openFilter());
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        Session session = SessionManager.get(requireContext()).getSession();
        if (session == null) {
            return;
        }
        String search = searchInput.getText() == null ? "" : searchInput.getText().toString().trim();
        String fromIso = fromDate == null ? null : DateUtils.startOfDayIso(fromDate);
        String toIso = toDate == null ? null : DateUtils.endOfDayIso(toDate);
        updateFilterSummary();
        AppExecutors.io().execute(() -> {
            List<Transaction> transactions = new TransactionRepository(requireContext())
                    .query(session.userId, search, categoryId, type, fromIso, toIso, sort);
            AppExecutors.main(() -> {
                if (!isAdded()) {
                    return;
                }
                adapter.setItems(transactions);
                boolean isEmpty = transactions.isEmpty();
                list.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void updateFilterSummary() {
        List<String> parts = new ArrayList<>();
        if (type != null) {
            parts.add("income".equals(type) ? "Income" : "Expense");
        }
        if (categoryName != null) {
            parts.add(categoryName);
        }
        if (fromDate != null || toDate != null) {
            String from = fromDate == null ? "Start" : DateUtils.dateForDisplay(fromDate);
            String to = toDate == null ? "Today" : DateUtils.dateForDisplay(toDate);
            parts.add(from + " – " + to);
        }
        if (parts.isEmpty()) {
            filterSummary.setVisibility(View.GONE);
        } else {
            filterSummary.setText(String.join(" · ", parts));
            filterSummary.setVisibility(View.VISIBLE);
        }
    }

    private void openFilter() {
        Session session = SessionManager.get(requireContext()).getSession();
        if (session == null) {
            return;
        }
        AppExecutors.io().execute(() -> {
            List<Category> categories = new CategoryRepository(requireContext()).list(session.userId, null);
            AppExecutors.main(() -> {
                if (isAdded()) {
                    showFilterDialog(categories);
                }
            });
        });
    }

    private void showFilterDialog(List<Category> categories) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filter, null, false);
        MaterialAutoCompleteTextView typeInput = view.findViewById(R.id.filterType);
        MaterialAutoCompleteTextView categoryInput = view.findViewById(R.id.filterCategory);
        MaterialAutoCompleteTextView sortInput = view.findViewById(R.id.filterSort);
        TextView fromValue = view.findViewById(R.id.filterFromValue);
        TextView toValue = view.findViewById(R.id.filterToValue);
        View fromRow = view.findViewById(R.id.filterFromRow);
        View toRow = view.findViewById(R.id.filterToRow);

        String[] typeLabels = {"All types", "Income", "Expense"};
        String[] typeValues = {null, "income", "expense"};
        typeInput.setAdapter(new ArrayAdapter<>(requireContext(),
                R.layout.item_dropdown, typeLabels));
        typeInput.setText(type == null ? typeLabels[0] : ("income".equals(type) ? "Income" : "Expense"), false);
        final String[] chosenType = {type};
        typeInput.setOnItemClickListener((parent, v, position, id) -> chosenType[0] = typeValues[position]);

        List<String> categoryLabels = new ArrayList<>();
        List<String> categoryIds = new ArrayList<>();
        categoryLabels.add("All categories");
        categoryIds.add(null);
        for (Category category : categories) {
            categoryLabels.add(category.name);
            categoryIds.add(category.id);
        }
        categoryInput.setAdapter(new ArrayAdapter<>(requireContext(),
                R.layout.item_dropdown, categoryLabels));
        int selectedCategory = categoryId == null ? 0 : categoryIds.indexOf(categoryId);
        categoryInput.setText(categoryLabels.get(Math.max(0, selectedCategory)), false);
        final String[] chosenCategory = {categoryId};
        final String[] chosenCategoryName = {categoryName};
        categoryInput.setOnItemClickListener((parent, v, position, id) -> {
            chosenCategory[0] = categoryIds.get(position);
            chosenCategoryName[0] = position == 0 ? null : categoryLabels.get(position);
        });

        String[] sortLabels = {"Newest first", "Oldest first", "Amount: high to low", "Amount: low to high"};
        String[] sortValues = {"newest", "oldest", "amount_high", "amount_low"};
        sortInput.setAdapter(new ArrayAdapter<>(requireContext(),
                R.layout.item_dropdown, sortLabels));
        int selectedSort = 0;
        for (int i = 0; i < sortValues.length; i++) {
            if (sortValues[i].equals(sort)) {
                selectedSort = i;
            }
        }
        sortInput.setText(sortLabels[selectedSort], false);
        final String[] chosenSort = {sort};
        sortInput.setOnItemClickListener((parent, v, position, id) -> chosenSort[0] = sortValues[position]);

        final LocalDate[] chosenFrom = {fromDate};
        final LocalDate[] chosenTo = {toDate};
        fromValue.setText(fromDate == null ? "Any" : DateUtils.dateForDisplay(fromDate));
        toValue.setText(toDate == null ? "Any" : DateUtils.dateForDisplay(toDate));
        fromRow.setOnClickListener(v -> pickDate(chosenFrom[0], date -> {
            chosenFrom[0] = date;
            fromValue.setText(DateUtils.dateForDisplay(date));
        }));
        toRow.setOnClickListener(v -> pickDate(chosenTo[0], date -> {
            chosenTo[0] = date;
            toValue.setText(DateUtils.dateForDisplay(date));
        }));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.filter)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.apply, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    type = chosenType[0];
                    categoryId = chosenCategory[0];
                    categoryName = chosenCategoryName[0];
                    sort = chosenSort[0];
                    fromDate = chosenFrom[0];
                    toDate = chosenTo[0];
                    dialog.dismiss();
                    load();
                }));
        dialog.show();
        view.findViewById(R.id.clearFilters).setOnClickListener(v -> {
            type = null;
            categoryId = null;
            categoryName = null;
            sort = "newest";
            fromDate = null;
            toDate = null;
            dialog.dismiss();
            load();
        });
    }

    private interface DateCallback {
        void onDate(LocalDate date);
    }

    private void pickDate(LocalDate current, DateCallback callback) {
        LocalDate base = current == null ? DateUtils.today() : current;
        DatePickerDialog picker = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) ->
                        callback.onDate(LocalDate.of(year, month + 1, dayOfMonth)),
                base.getYear(), base.getMonthValue() - 1, base.getDayOfMonth());
        picker.show();
    }

    private void confirmDelete(Transaction transaction) {
        Session session = SessionManager.get(requireContext()).getSession();
        if (session == null) {
            return;
        }
        UiUtils.confirm(requireContext(), getString(R.string.confirm_delete),
                getString(R.string.confirm_delete_message), getString(R.string.yes_delete),
                () -> AppExecutors.io().execute(() -> {
                    new TransactionRepository(requireContext()).delete(session.userId, transaction.id);
                    AppExecutors.main(() -> {
                        if (isAdded()) {
                            load();
                        }
                    });
                }));
    }
}
