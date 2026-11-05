package com.pocketledger.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pocketledger.R;
import com.pocketledger.adapters.LoanAdapter;
import com.pocketledger.customviews.SegmentedControl;
import com.pocketledger.models.Loan;
import com.pocketledger.models.Session;
import com.pocketledger.repositories.LoanRepository;
import com.pocketledger.utils.AppExecutors;
import com.pocketledger.utils.Constants;
import com.pocketledger.utils.Money;
import com.pocketledger.utils.SessionManager;

import java.util.List;

public class LoansActivity extends AppCompatActivity {

    private SegmentedControl filter;
    private TextView borrowedTotal;
    private TextView lentTotal;
    private RecyclerView list;
    private View empty;
    private LoanAdapter adapter;
    private String direction = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loans);
        filter = findViewById(R.id.loanFilter);
        borrowedTotal = findViewById(R.id.borrowedTotal);
        lentTotal = findViewById(R.id.lentTotal);
        list = findViewById(R.id.loansList);
        empty = findViewById(R.id.loansEmpty);
        filter.setItems(new String[]{"All", "Borrowed", "Lent"},
                new String[]{"", Constants.LOAN_BORROWED, Constants.LOAN_LENT});
        filter.setOnSelectionChanged((index, value) -> {
            direction = value == null ? "" : value;
            load();
        });
        adapter = new LoanAdapter(loan -> {
            Intent intent = new Intent(this, LoanDetailActivity.class);
            intent.putExtra(LoanDetailActivity.EXTRA_LOAN_ID, loan.id);
            startActivity(intent);
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.addLoanButton).setOnClickListener(v ->
                startActivity(new Intent(this, EditLoanActivity.class)));
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
            LoanRepository repository = new LoanRepository(this);
            List<Loan> loans = direction.isEmpty()
                    ? repository.listAll(session.userId)
                    : repository.list(session.userId, direction);
            long borrowed = repository.outstanding(session.userId, Constants.LOAN_BORROWED);
            long lent = repository.outstanding(session.userId, Constants.LOAN_LENT);
            AppExecutors.main(() -> {
                if (isFinishing()) {
                    return;
                }
                adapter.setItems(loans);
                boolean isEmpty = loans.isEmpty();
                list.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                borrowedTotal.setText(Money.format(borrowed));
                lentTotal.setText(Money.format(lent));
            });
        });
    }
}
