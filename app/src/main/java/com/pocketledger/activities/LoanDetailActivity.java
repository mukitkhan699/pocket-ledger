package com.pocketledger.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.pocketledger.R;
import com.pocketledger.adapters.RepaymentAdapter;
import com.pocketledger.models.Loan;
import com.pocketledger.models.LoanRepayment;
import com.pocketledger.models.Session;
import com.pocketledger.repositories.LoanRepository;
import com.pocketledger.utils.AppExecutors;
import com.pocketledger.utils.Constants;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Money;
import com.pocketledger.utils.SessionManager;
import com.pocketledger.utils.UiUtils;

import java.time.LocalDate;
import java.util.List;

public class LoanDetailActivity extends AppCompatActivity {

    public static final String EXTRA_LOAN_ID = "loan_id";

    private String loanId;
    private Loan loan;
    private LoanRepository repository;
    private RepaymentAdapter adapter;
    private View repaymentsEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loan_detail);
        loanId = getIntent().getStringExtra(EXTRA_LOAN_ID);
        if (loanId == null) {
            finish();
            return;
        }
        repository = new LoanRepository(this);
        repaymentsEmpty = findViewById(R.id.repaymentsEmpty);
        adapter = new RepaymentAdapter(this::confirmDeleteRepayment);
        androidx.recyclerview.widget.RecyclerView list = findViewById(R.id.repaymentsContainer);
        list.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        list.setAdapter(adapter);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.editButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, EditLoanActivity.class);
            intent.putExtra(EditLoanActivity.EXTRA_LOAN_ID, loanId);
            startActivity(intent);
        });
        findViewById(R.id.deleteButton).setOnClickListener(v -> confirmDeleteLoan());
        findViewById(R.id.addRepaymentButton).setOnClickListener(v -> addRepayment());
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
            Loan loaded = repository.findLoan(session.userId, loanId);
            List<LoanRepayment> repayments = repository.repayments(session.userId, loanId);
            AppExecutors.main(() -> {
                if (isFinishing()) {
                    return;
                }
                if (loaded == null) {
                    finish();
                    return;
                }
                loan = loaded;
                bind(loaded, repayments);
            });
        });
    }

    private void bind(Loan loan, List<LoanRepayment> repayments) {
        TextView remaining = findViewById(R.id.loanRemaining);
        TextView direction = findViewById(R.id.loanDirection);
        TextView person = findViewById(R.id.loanPerson);
        TextView principal = findViewById(R.id.loanPrincipal);
        TextView repaid = findViewById(R.id.loanRepaid);
        TextView date = findViewById(R.id.loanDate);
        TextView notes = findViewById(R.id.loanDescription);

        remaining.setText(Money.format(Math.max(0, loan.remainingMinor)));
        direction.setText(loan.isBorrowed() ? R.string.direction_borrowed : R.string.direction_lent);
        person.setText(loan.personName);
        principal.setText(Money.format(loan.principalMinor));
        repaid.setText(Money.format(loan.repaidMinor));
        date.setText(DateUtils.dateForDisplay(LocalDate.parse(loan.loanDate)));
        notes.setText(loan.description == null || loan.description.trim().isEmpty()
                ? "—" : loan.description);
        adapter.setItems(repayments);
        repaymentsEmpty.setVisibility(repayments.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void addRepayment() {
        if (loan == null) {
            return;
        }
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_repayment, null, false);
        TextInputEditText amountInput = view.findViewById(R.id.repaymentAmountInput);
        TextInputEditText notesInput = view.findViewById(R.id.repaymentNotesInput);
        TextView dateValue = view.findViewById(R.id.repaymentDateValue);
        final LocalDate[] date = {DateUtils.today()};
        dateValue.setText(DateUtils.dateForDisplay(date[0]));
        view.findViewById(R.id.repaymentDateRow).setOnClickListener(v ->
                new DatePickerDialog(this, (picker, year, month, dayOfMonth) -> {
                    date[0] = LocalDate.of(year, month + 1, dayOfMonth);
                    dateValue.setText(DateUtils.dateForDisplay(date[0]));
                }, date[0].getYear(), date[0].getMonthValue() - 1,
                        date[0].getDayOfMonth()).show());

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.record_repayment)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String text = amountInput.getText() == null
                            ? "" : amountInput.getText().toString();
                    if (!Money.isValidPositive(text)) {
                        amountInput.setError(getString(R.string.error_amount));
                        return;
                    }
                    long amountMinor = Money.parseMinor(text);
                    long remainingMinor = Math.max(0, loan.principalMinor - loan.repaidMinor);
                    if (amountMinor > remainingMinor) {
                        amountInput.setError(getString(R.string.error_repayment_too_large));
                        return;
                    }
                    String notes = notesInput.getText() == null
                            ? null : notesInput.getText().toString().trim();
                    if (notes != null && notes.isEmpty()) {
                        notes = null;
                    }
                    Session session = SessionManager.get(this).getSession();
                    if (session == null) {
                        dialog.dismiss();
                        return;
                    }
                    String finalNotes = notes;
                    AppExecutors.io().execute(() -> {
                        try {
                            repository.addRepayment(session.userId, loanId, amountMinor,
                                    date[0].toString(), finalNotes);
                            AppExecutors.main(() -> {
                                dialog.dismiss();
                                if (!isFinishing()) {
                                    load();
                                }
                            });
                        } catch (IllegalArgumentException e) {
                            AppExecutors.main(() -> amountInput.setError(e.getMessage()));
                        }
                    });
                }));
        dialog.show();
    }

    private void confirmDeleteRepayment(LoanRepayment repayment) {
        Session session = SessionManager.get(this).getSession();
        if (session == null) {
            return;
        }
        UiUtils.confirm(this, getString(R.string.delete_repayment),
                getString(R.string.delete_repayment_confirm), getString(R.string.yes_delete),
                () -> AppExecutors.io().execute(() -> {
                    repository.deleteRepayment(session.userId, repayment.id);
                    AppExecutors.main(() -> {
                        if (!isFinishing()) {
                            load();
                        }
                    });
                }));
    }

    private void confirmDeleteLoan() {
        Session session = SessionManager.get(this).getSession();
        if (session == null || loan == null) {
            return;
        }
        UiUtils.confirm(this, getString(R.string.confirm_delete_loan),
                getString(R.string.delete_loan_confirm), getString(R.string.yes_delete),
                () -> AppExecutors.io().execute(() -> {
                    repository.deleteLoan(session.userId, loan.id);
                    AppExecutors.main(() -> {
                        if (!isFinishing()) {
                            finish();
                        }
                    });
                }));
    }
}
