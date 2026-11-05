package com.pocketledger.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.pocketledger.R;
import com.pocketledger.customviews.SegmentedControl;
import com.pocketledger.models.Category;
import com.pocketledger.models.Session;
import com.pocketledger.models.Transaction;
import com.pocketledger.repositories.CategoryRepository;
import com.pocketledger.repositories.TransactionRepository;
import com.pocketledger.utils.AppExecutors;
import com.pocketledger.utils.Constants;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Money;
import com.pocketledger.utils.SessionManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class EditTransactionActivity extends AppCompatActivity {

    public static final String EXTRA_TRANSACTION_ID = "transaction_id";

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", DateUtils.LOCALE);

    private SegmentedControl typeControl;
    private TextInputEditText amountInput;
    private MaterialAutoCompleteTextView categoryInput;
    private TextInputEditText descriptionInput;
    private TextInputEditText notesInput;
    private TextView dateValue;
    private TextView timeValue;
    private TextView errorText;
    private View saveButton;

    private String transactionId;
    private Transaction existing;
    private List<Category> categories = new ArrayList<>();
    private String selectedCategoryId;
    private String selectedCategoryName;
    private LocalDate date = DateUtils.today();
    private LocalTime time = LocalTime.now(DateUtils.ZONE).withSecond(0).withNano(0);
    private String type = Constants.TYPE_EXPENSE;
    private boolean saving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_transaction);
        transactionId = getIntent().getStringExtra(EXTRA_TRANSACTION_ID);
        typeControl = findViewById(R.id.typeControl);
        amountInput = findViewById(R.id.amountInput);
        categoryInput = findViewById(R.id.categoryInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        notesInput = findViewById(R.id.notesInput);
        dateValue = findViewById(R.id.dateValue);
        timeValue = findViewById(R.id.timeValue);
        errorText = findViewById(R.id.editError);
        saveButton = findViewById(R.id.saveButton);

        typeControl.setItems(new String[]{"Expense", "Income"},
                new String[]{Constants.TYPE_EXPENSE, Constants.TYPE_INCOME});
        typeControl.setSelectedValue(type);
        typeControl.setOnSelectionChanged((index, value) -> {
            type = value;
            selectedCategoryId = null;
            selectedCategoryName = null;
            loadCategories(value, null, null);
        });
        updateDate();
        updateTime();
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.dateRow).setOnClickListener(v -> pickDate());
        findViewById(R.id.timeRow).setOnClickListener(v -> pickTime());
        categoryInput.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < categories.size()) {
                Category category = categories.get(position);
                selectedCategoryId = category.id;
                selectedCategoryName = category.name;
            }
        });
        saveButton.setOnClickListener(v -> save());
        load();
    }

    private void load() {
        Session session = SessionManager.get(this).getSession();
        if (session == null) {
            finish();
            return;
        }
        if (transactionId == null) {
            findViewById(R.id.screenTitle).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.screenTitle)).setText(R.string.add_transaction);
            loadCategories(type, null, null);
            return;
        }
        ((TextView) findViewById(R.id.screenTitle)).setText(R.string.edit_transaction);
        AppExecutors.io().execute(() -> {
            Transaction transaction = new TransactionRepository(this)
                    .find(session.userId, transactionId);
            AppExecutors.main(() -> {
                if (isFinishing()) {
                    return;
                }
                if (transaction == null) {
                    finish();
                    return;
                }
                existing = transaction;
                type = transaction.type;
                typeControl.setSelectedValue(type);
                amountInput.setText(Money.decimal(transaction.amountMinor));
                descriptionInput.setText(transaction.description);
                notesInput.setText(transaction.notes);
                date = DateUtils.localDateOf(transaction.occurredAt);
                time = DateUtils.parseInstant(transaction.occurredAt)
                        .atZone(DateUtils.ZONE).toLocalTime().withSecond(0).withNano(0);
                updateDate();
                updateTime();
                loadCategories(type, transaction.categoryId, transaction.categoryName);
            });
        });
    }

    private void loadCategories(String categoryType, String desiredId, String desiredName) {
        Session session = SessionManager.get(this).getSession();
        if (session == null) {
            return;
        }
        AppExecutors.io().execute(() -> {
            List<Category> loaded = new CategoryRepository(this).list(session.userId, categoryType);
            AppExecutors.main(() -> {
                if (isFinishing()) {
                    return;
                }
                categories = loaded;
                List<String> labels = new ArrayList<>();
                for (Category category : loaded) {
                    labels.add(category.name);
                }
                categoryInput.setAdapter(new ArrayAdapter<>(this,
                        R.layout.item_dropdown, labels));
                if (desiredId != null) {
                    Category match = null;
                    for (Category category : loaded) {
                        if (category.id.equals(desiredId)) {
                            match = category;
                            break;
                        }
                    }
                    selectedCategoryId = desiredId;
                    selectedCategoryName = match != null ? match.name : desiredName;
                    if (selectedCategoryName != null) {
                        categoryInput.setText(selectedCategoryName, false);
                    }
                } else if (!loaded.isEmpty()) {
                    Category first = loaded.get(0);
                    selectedCategoryId = first.id;
                    selectedCategoryName = first.name;
                    categoryInput.setText(first.name, false);
                } else {
                    selectedCategoryId = null;
                    selectedCategoryName = null;
                    categoryInput.setText("", false);
                }
            });
        });
    }

    private void updateDate() {
        dateValue.setText(DateUtils.dateForDisplay(date));
    }

    private void updateTime() {
        timeValue.setText(TIME_FORMAT.format(time));
    }

    private void pickDate() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            date = LocalDate.of(year, month + 1, dayOfMonth);
            updateDate();
        }, date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth()).show();
    }

    private void pickTime() {
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            time = LocalTime.of(hourOfDay, minute);
            updateTime();
        }, time.getHour(), time.getMinute(), false).show();
    }

    private void save() {
        if (saving) {
            return;
        }
        hideError();
        Session session = SessionManager.get(this).getSession();
        if (session == null) {
            finish();
            return;
        }
        String amountText = amountInput.getText() == null ? "" : amountInput.getText().toString();
        if (!Money.isValidPositive(amountText)) {
            showError(getString(R.string.error_amount));
            return;
        }
        long amountMinor = Money.parseMinor(amountText);
        String description = descriptionInput.getText() == null
                ? "" : descriptionInput.getText().toString().trim();
        if (description.isEmpty()) {
            showError(getString(R.string.error_description));
            return;
        }
        Category category = resolveCategory();
        if (category == null) {
            showError(getString(R.string.error_category));
            return;
        }
        String notes = notesInput.getText() == null
                ? null : notesInput.getText().toString().trim();
        if (notes != null && notes.isEmpty()) {
            notes = null;
        }
        Transaction transaction = existing == null ? new Transaction() : existing;
        transaction.userId = session.userId;
        transaction.type = type;
        transaction.amountMinor = amountMinor;
        transaction.categoryId = category.id;
        transaction.description = description;
        transaction.notes = notes;
        transaction.occurredAt = DateUtils.atLocalDateTime(date, time.getHour(), time.getMinute());
        saving = true;
        saveButton.setEnabled(false);
        AppExecutors.io().execute(() -> {
            new TransactionRepository(this).save(transaction);
            AppExecutors.main(() -> {
                saving = false;
                saveButton.setEnabled(true);
                if (!isFinishing()) {
                    finish();
                }
            });
        });
    }

    private Category resolveCategory() {
        String typed = categoryInput.getText() == null
                ? "" : categoryInput.getText().toString().trim();
        if (typed.isEmpty()) {
            return null;
        }
        for (Category category : categories) {
            if (category.name.equalsIgnoreCase(typed)) {
                return category;
            }
        }
        if (selectedCategoryId != null && selectedCategoryName != null
                && selectedCategoryName.equalsIgnoreCase(typed)) {
            Category fallback = new Category();
            fallback.id = selectedCategoryId;
            fallback.name = selectedCategoryName;
            fallback.type = type;
            return fallback;
        }
        return null;
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        errorText.setVisibility(View.GONE);
    }
}
