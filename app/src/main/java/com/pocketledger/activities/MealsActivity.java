package com.pocketledger.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.pocketledger.R;
import com.pocketledger.adapters.MealAdapter;
import com.pocketledger.models.MealEntry;
import com.pocketledger.models.MealSettings;
import com.pocketledger.models.Session;
import com.pocketledger.repositories.MealRepository;
import com.pocketledger.utils.AppExecutors;
import com.pocketledger.utils.Constants;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Money;
import com.pocketledger.utils.SessionManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MealsActivity extends AppCompatActivity {

    private LocalDate selectedDate = DateUtils.today();
    private MealRepository repository;
    private MealSettings settings;
    private MealAdapter adapter;
    private TextView dateLabel;
    private TextView todayChip;
    private TextView consumedTotal;
    private TextView plannedTotal;
    private TextView dailyTotal;
    private RecyclerView list;
    private View empty;
    private TextInputEditText morningInput;
    private TextInputEditText lunchInput;
    private TextInputEditText dinnerInput;
    private SwitchMaterial autoGenerateSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meals);
        repository = new MealRepository(this);
        dateLabel = findViewById(R.id.dateLabel);
        todayChip = findViewById(R.id.todayChip);
        consumedTotal = findViewById(R.id.consumedTotal);
        plannedTotal = findViewById(R.id.plannedTotal);
        dailyTotal = findViewById(R.id.dailyTotal);
        list = findViewById(R.id.mealsList);
        empty = findViewById(R.id.mealsEmpty);
        morningInput = findViewById(R.id.morningInput);
        lunchInput = findViewById(R.id.lunchInput);
        dinnerInput = findViewById(R.id.dinnerInput);
        autoGenerateSwitch = findViewById(R.id.autoGenerateSwitch);

        adapter = new MealAdapter(new MealAdapter.Listener() {
            @Override
            public void onConsume(MealEntry entry) {
                if (!entry.isConsumed()) {
                    mutate(() -> repository.setState(entry, Constants.STATE_CONSUMED));
                }
            }

            @Override
            public void onSkip(MealEntry entry) {
                if (!entry.isSkipped()) {
                    mutate(() -> repository.setState(entry, Constants.STATE_SKIPPED));
                }
            }

            @Override
            public void onEdit(MealEntry entry) {
                editAmount(entry);
            }
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.prevDay).setOnClickListener(v -> {
            selectedDate = selectedDate.minusDays(1);
            loadDay();
        });
        findViewById(R.id.nextDay).setOnClickListener(v -> {
            selectedDate = selectedDate.plusDays(1);
            loadDay();
        });
        findViewById(R.id.todayChip).setOnClickListener(v -> {
            selectedDate = DateUtils.today();
            loadDay();
        });
        findViewById(R.id.dateLabel).setOnClickListener(v -> pickDate());
        findViewById(R.id.addMealButton).setOnClickListener(v -> addMeal());
        findViewById(R.id.generateButton).setOnClickListener(v -> generate());
        findViewById(R.id.saveSettingsButton).setOnClickListener(v -> saveSettings());
        loadSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDay();
    }

    private void loadSettings() {
        Session session = SessionManager.get(this).getSession();
        if (session == null) {
            finish();
            return;
        }
        AppExecutors.io().execute(() -> {
            MealSettings loaded = repository.getSettings(session.userId);
            AppExecutors.main(() -> {
                if (isFinishing()) {
                    return;
                }
                settings = loaded;
                morningInput.setText(Money.decimal(loaded.morningMinor));
                lunchInput.setText(Money.decimal(loaded.lunchMinor));
                dinnerInput.setText(Money.decimal(loaded.dinnerMinor));
                autoGenerateSwitch.setChecked(loaded.autoGenerate);
            });
        });
    }

    private void loadDay() {
        Session session = SessionManager.get(this).getSession();
        if (session == null) {
            finish();
            return;
        }
        dateLabel.setText(DateUtils.dayHeader(selectedDate));
        todayChip.setVisibility(selectedDate.equals(DateUtils.today()) ? View.VISIBLE : View.GONE);
        String date = selectedDate.toString();
        AppExecutors.io().execute(() -> {
            List<MealEntry> entries = repository.getForDate(session.userId, date);
            long consumed = repository.consumedForDate(session.userId, date);
            long planned = repository.plannedForDate(session.userId, date);
            long nonSkipped = repository.nonSkippedForDate(session.userId, date);
            AppExecutors.main(() -> {
                if (isFinishing()) {
                    return;
                }
                adapter.setItems(entries);
                boolean isEmpty = entries.isEmpty();
                list.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                consumedTotal.setText(Money.format(consumed));
                plannedTotal.setText(Money.format(planned));
                dailyTotal.setText(Money.format(nonSkipped));
            });
        });
    }

    private void pickDate() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
            loadDay();
        }, selectedDate.getYear(), selectedDate.getMonthValue() - 1,
                selectedDate.getDayOfMonth()).show();
    }

    private void generate() {
        Session session = SessionManager.get(this).getSession();
        if (session == null || settings == null) {
            return;
        }
        AppExecutors.io().execute(() -> {
            List<MealEntry> existing = repository.getForDate(session.userId, selectedDate.toString());
            List<String> present = new ArrayList<>();
            for (MealEntry entry : existing) {
                present.add(entry.mealType);
            }
            for (String type : new String[]{Constants.MEAL_MORNING, Constants.MEAL_LUNCH,
                    Constants.MEAL_DINNER}) {
                if (!present.contains(type)) {
                    repository.createOrRestore(session.userId, selectedDate.toString(), type,
                            settings.priceFor(type), Constants.STATE_PLANNED);
                }
            }
            AppExecutors.main(() -> {
                if (!isFinishing()) {
                    loadDay();
                }
            });
        });
    }

    private void addMeal() {
        Session session = SessionManager.get(this).getSession();
        if (session == null || settings == null) {
            return;
        }
        AppExecutors.io().execute(() -> {
            List<MealEntry> existing = repository.getForDate(session.userId, selectedDate.toString());
            List<String> present = new ArrayList<>();
            for (MealEntry entry : existing) {
                present.add(entry.mealType);
            }
            List<String> missingTypes = new ArrayList<>();
            List<String> missingLabels = new ArrayList<>();
            if (!present.contains(Constants.MEAL_MORNING)) {
                missingTypes.add(Constants.MEAL_MORNING);
                missingLabels.add("Morning");
            }
            if (!present.contains(Constants.MEAL_LUNCH)) {
                missingTypes.add(Constants.MEAL_LUNCH);
                missingLabels.add("Lunch");
            }
            if (!present.contains(Constants.MEAL_DINNER)) {
                missingTypes.add(Constants.MEAL_DINNER);
                missingLabels.add("Dinner");
            }
            AppExecutors.main(() -> {
                if (isFinishing()) {
                    return;
                }
                if (missingTypes.isEmpty()) {
                    Toast.makeText(this, R.string.meal_exists, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (missingTypes.size() == 1) {
                    showAmountDialog(missingLabels.get(0),
                            settings.priceFor(missingTypes.get(0)), amount ->
                                    createMeal(session.userId, missingTypes.get(0), amount));
                    return;
                }
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.add_meal)
                        .setItems(missingLabels.toArray(new String[0]), (dialog, which) -> {
                            String type = missingTypes.get(which);
                            showAmountDialog(missingLabels.get(which), settings.priceFor(type),
                                    amount -> createMeal(session.userId, type, amount));
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            });
        });
    }

    private void createMeal(String userId, String mealType, long amountMinor) {
        AppExecutors.io().execute(() -> {
            repository.createOrRestore(userId, selectedDate.toString(), mealType, amountMinor,
                    Constants.STATE_PLANNED);
            AppExecutors.main(() -> {
                if (!isFinishing()) {
                    loadDay();
                }
            });
        });
    }

    private void editAmount(MealEntry entry) {
        showAmountDialog(entry.label(), entry.amountMinor, amount ->
                mutate(() -> repository.updateAmount(entry, amount)));
    }

    private interface AmountCallback {
        void onAmount(long amountMinor);
    }

    private void showAmountDialog(String label, long currentMinor, AmountCallback callback) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_meal_amount, null, false);
        TextInputEditText input = view.findViewById(R.id.mealAmountInput);
        input.setText(Money.decimal(currentMinor));
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(label)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String text = input.getText() == null ? "" : input.getText().toString();
                    if (!Money.isValidPositive(text)) {
                        input.setError(getString(R.string.error_amount));
                        return;
                    }
                    dialog.dismiss();
                    callback.onAmount(Money.parseMinor(text));
                }));
        dialog.show();
    }

    private void saveSettings() {
        Session session = SessionManager.get(this).getSession();
        if (session == null || settings == null) {
            return;
        }
        if (!Money.isValidPositive(text(morningInput)) || !Money.isValidPositive(text(lunchInput))
                || !Money.isValidPositive(text(dinnerInput))) {
            Toast.makeText(this, R.string.error_amount, Toast.LENGTH_SHORT).show();
            return;
        }
        settings.morningMinor = Money.parseMinor(text(morningInput));
        settings.lunchMinor = Money.parseMinor(text(lunchInput));
        settings.dinnerMinor = Money.parseMinor(text(dinnerInput));
        settings.autoGenerate = autoGenerateSwitch.isChecked();
        AppExecutors.io().execute(() -> {
            repository.saveSettings(settings);
            AppExecutors.main(() -> {
                if (!isFinishing()) {
                    Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private interface Action {
        void run();
    }

    private void mutate(Action action) {
        AppExecutors.io().execute(() -> {
            action.run();
            AppExecutors.main(() -> {
                if (!isFinishing()) {
                    loadDay();
                }
            });
        });
    }
}
