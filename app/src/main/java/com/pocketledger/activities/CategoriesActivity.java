package com.pocketledger.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.pocketledger.R;
import com.pocketledger.adapters.CategoryAdapter;
import com.pocketledger.customviews.SegmentedControl;
import com.pocketledger.models.Category;
import com.pocketledger.models.Session;
import com.pocketledger.repositories.CategoryRepository;
import com.pocketledger.utils.AppExecutors;
import com.pocketledger.utils.CategoryIcons;
import com.pocketledger.utils.Constants;
import com.pocketledger.utils.SessionManager;
import com.pocketledger.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class CategoriesActivity extends AppCompatActivity {

    private CategoryRepository repository;
    private CategoryAdapter adapter;
    private RecyclerView list;
    private View empty;
    private String filter = "";
    private List<Category> current = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);
        repository = new CategoryRepository(this);
        list = findViewById(R.id.categoriesList);
        empty = findViewById(R.id.categoriesEmpty);
        SegmentedControl categoryFilter = findViewById(R.id.categoryFilter);
        categoryFilter.setItems(new String[]{"All", "Expense", "Income"},
                new String[]{"", Constants.TYPE_EXPENSE, Constants.TYPE_INCOME});
        categoryFilter.setOnSelectionChanged((index, value) -> {
            filter = value == null ? "" : value;
            load();
        });
        adapter = new CategoryAdapter(new CategoryAdapter.Listener() {
            @Override
            public void onEdit(Category category) {
                showDialog(category);
            }

            @Override
            public void onDelete(Category category) {
                confirmDelete(category);
            }
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.addCategoryButton).setOnClickListener(v -> showDialog(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        Session session = SessionManager.get(this).getSession();
        if (session == null) {
            finish();
            return;
        }
        String type = filter.isEmpty() ? null : filter;
        AppExecutors.io().execute(() -> {
            List<Category> categories = repository.list(session.userId, type);
            AppExecutors.main(() -> {
                if (isFinishing()) {
                    return;
                }
                current = categories;
                adapter.setItems(categories);
                boolean isEmpty = categories.isEmpty();
                list.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void showDialog(Category existing) {
        Session session = SessionManager.get(this).getSession();
        if (session == null) {
            return;
        }
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_category_edit, null, false);
        TextInputLayout nameLayout = view.findViewById(R.id.categoryNameLayout);
        TextInputEditText nameInput = view.findViewById(R.id.categoryNameInput);
        SegmentedControl typeControl = view.findViewById(R.id.categoryTypeControl);
        SwitchMaterial foodSwitch = view.findViewById(R.id.categoryFoodSwitch);
        Spinner iconSpinner = view.findViewById(R.id.iconSpinner);
        Spinner colorSpinner = view.findViewById(R.id.colorSpinner);

        typeControl.setItems(new String[]{"Expense", "Income"},
                new String[]{Constants.TYPE_EXPENSE, Constants.TYPE_INCOME});
        typeControl.setSelectedValue(existing == null ? Constants.TYPE_EXPENSE : existing.type);
        List<String> iconLabels = new ArrayList<>();
        for (String key : CategoryIcons.ICON_KEYS) {
            iconLabels.add(capitalize(key));
        }
        iconSpinner.setAdapter(new ArrayAdapter<>(this, R.layout.item_dropdown, iconLabels));
        String[] colorLabels = {"Emerald", "Coral", "Amber", "Blue", "Violet",
                "Pink", "Cyan", "Gold", "Rose", "Slate"};
        colorSpinner.setAdapter(new ArrayAdapter<>(this, R.layout.item_dropdown, colorLabels));
        if (existing != null) {
            nameInput.setText(existing.name);
            foodSwitch.setChecked(existing.isFood);
            int iconIndex = CategoryIcons.ICON_KEYS.indexOf(existing.icon);
            iconSpinner.setSelection(Math.max(0, iconIndex));
            int colorIndex = CategoryIcons.COLORS.indexOf(existing.color);
            colorSpinner.setSelection(Math.max(0, colorIndex));
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? R.string.new_category : R.string.edit_category)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String name = nameInput.getText() == null
                            ? "" : nameInput.getText().toString().trim();
                    if (name.isEmpty()) {
                        nameLayout.setError(getString(R.string.error_name));
                        return;
                    }
                    String type = typeControl.getSelectedValue();
                    String icon = CategoryIcons.ICON_KEYS.get(iconSpinner.getSelectedItemPosition());
                    String color = CategoryIcons.COLORS.get(colorSpinner.getSelectedItemPosition());
                    boolean isFood = foodSwitch.isChecked();
                    String excludeId = existing == null ? null : existing.id;
                    AppExecutors.io().execute(() -> {
                        boolean exists = repository.nameExists(session.userId, name, excludeId);
                        AppExecutors.main(() -> {
                            if (isFinishing()) {
                                return;
                            }
                            if (exists) {
                                nameLayout.setError(getString(R.string.category_exists));
                                return;
                            }
                            AppExecutors.io().execute(() -> {
                                if (existing == null) {
                                    repository.create(session.userId, name, type, icon, color, isFood);
                                } else {
                                    existing.name = name;
                                    existing.type = type;
                                    existing.icon = icon;
                                    existing.color = color;
                                    existing.isFood = isFood;
                                    repository.update(existing);
                                }
                                AppExecutors.main(() -> {
                                    dialog.dismiss();
                                    if (!isFinishing()) {
                                        load();
                                    }
                                });
                            });
                        });
                    });
                }));
        dialog.show();
    }

    private void confirmDelete(Category category) {
        Session session = SessionManager.get(this).getSession();
        if (session == null) {
            return;
        }
        UiUtils.confirm(this, getString(R.string.confirm_delete_category),
                getString(R.string.delete_category_message), getString(R.string.yes_delete),
                () -> AppExecutors.io().execute(() -> {
                    repository.delete(session.userId, category.id);
                    AppExecutors.main(() -> {
                        if (!isFinishing()) {
                            load();
                        }
                    });
                }));
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}
