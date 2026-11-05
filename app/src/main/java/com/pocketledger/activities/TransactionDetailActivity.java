package com.pocketledger.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.pocketledger.R;
import com.pocketledger.models.Session;
import com.pocketledger.models.Transaction;
import com.pocketledger.repositories.TransactionRepository;
import com.pocketledger.utils.AppExecutors;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Money;
import com.pocketledger.utils.SessionManager;
import com.pocketledger.utils.UiUtils;

public class TransactionDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TRANSACTION_ID = "transaction_id";

    private String transactionId;
    private Transaction transaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);
        transactionId = getIntent().getStringExtra(EXTRA_TRANSACTION_ID);
        if (transactionId == null) {
            finish();
            return;
        }
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.editButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, EditTransactionActivity.class);
            intent.putExtra(EditTransactionActivity.EXTRA_TRANSACTION_ID, transactionId);
            startActivity(intent);
        });
        findViewById(R.id.deleteButton).setOnClickListener(v -> confirmDelete());
        load();
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
        AppExecutors.io().execute(() -> {
            Transaction loaded = new TransactionRepository(this).find(session.userId, transactionId);
            AppExecutors.main(() -> {
                if (isFinishing()) {
                    return;
                }
                if (loaded == null) {
                    finish();
                    return;
                }
                transaction = loaded;
                bind(loaded);
            });
        });
    }

    private void bind(Transaction tx) {
        TextView amount = findViewById(R.id.detailAmount);
        TextView type = findViewById(R.id.detailType);
        TextView category = findViewById(R.id.detailCategory);
        TextView date = findViewById(R.id.detailDate);
        TextView description = findViewById(R.id.detailDescription);
        TextView notes = findViewById(R.id.detailNotes);
        View mealBadge = findViewById(R.id.detailMealBadge);

        amount.setText(Money.signed(tx.amountMinor, tx.isIncome()));
        amount.setTextColor(getColor(tx.isIncome() ? R.color.income : R.color.expense));
        type.setText(tx.isIncome() ? R.string.income : R.string.type_expense);
        category.setText(tx.categoryName == null ? getString(R.string.uncategorised) : tx.categoryName);
        date.setText(DateUtils.dateTimeForDisplay(tx.occurredAt));
        description.setText(tx.description == null || tx.description.trim().isEmpty()
                ? getString(R.string.uncategorised) : tx.description);
        if (tx.notes == null || tx.notes.trim().isEmpty()) {
            notes.setVisibility(View.GONE);
        } else {
            notes.setText(tx.notes);
            notes.setVisibility(View.VISIBLE);
        }
        mealBadge.setVisibility(tx.mealEntryId == null ? View.GONE : View.VISIBLE);
    }

    private void confirmDelete() {
        Session session = SessionManager.get(this).getSession();
        if (session == null || transaction == null) {
            return;
        }
        UiUtils.confirm(this, getString(R.string.confirm_delete),
                getString(R.string.confirm_delete_message), getString(R.string.yes_delete), () ->
                        AppExecutors.io().execute(() -> {
                            new TransactionRepository(this).delete(session.userId, transaction.id);
                            AppExecutors.main(() -> {
                                if (!isFinishing()) {
                                    finish();
                                }
                            });
                        }));
    }
}
