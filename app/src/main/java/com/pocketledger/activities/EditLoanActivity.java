package com.pocketledger.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.pocketledger.R;
import com.pocketledger.customviews.SegmentedControl;
import com.pocketledger.models.Loan;
import com.pocketledger.models.Session;
import com.pocketledger.repositories.LoanRepository;
import com.pocketledger.utils.AppExecutors;
import com.pocketledger.utils.Constants;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Money;
import com.pocketledger.utils.SessionManager;

import java.time.LocalDate;

public class EditLoanActivity extends AppCompatActivity {

    public static final String EXTRA_LOAN_ID = "loan_id";

    private SegmentedControl directionControl;
    private TextInputEditText personInput;
    private TextInputEditText principalInput;
    private TextInputEditText notesInput;
    private TextView dateValue;
    private TextView errorText;
    private View saveButton;

    private String loanId;
    private Loan existing;
    private String direction = Constants.LOAN_BORROWED;
    private LocalDate date = DateUtils.today();
    private boolean saving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_loan);
        loanId = getIntent().getStringExtra(EXTRA_LOAN_ID);
        directionControl = findViewById(R.id.directionControl);
        personInput = findViewById(R.id.personInput);
        principalInput = findViewById(R.id.principalInput);
        notesInput = findViewById(R.id.notesInput);
        dateValue = findViewById(R.id.dateValue);
        errorText = findViewById(R.id.editError);
        saveButton = findViewById(R.id.saveButton);

        directionControl.setItems(new String[]{"Borrowed", "Lent"},
                new String[]{Constants.LOAN_BORROWED, Constants.LOAN_LENT});
        directionControl.setSelectedValue(direction);
        directionControl.setOnSelectionChanged((index, value) -> direction = value);
        updateDate();
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.dateRow).setOnClickListener(v -> pickDate());
        saveButton.setOnClickListener(v -> save());
        load();
    }

    private void load() {
        Session session = SessionManager.get(this).getSession();
        if (session == null) {
            finish();
            return;
        }
        if (loanId == null) {
            ((TextView) findViewById(R.id.screenTitle)).setText(R.string.add_loan);
            return;
        }
        ((TextView) findViewById(R.id.screenTitle)).setText(R.string.edit_loan);
        AppExecutors.io().execute(() -> {
            Loan loan = new LoanRepository(this).findLoan(session.userId, loanId);
            AppExecutors.main(() -> {
                if (isFinishing()) {
                    return;
                }
                if (loan == null) {
                    finish();
                    return;
                }
                existing = loan;
                direction = loan.direction;
                directionControl.setSelectedValue(direction);
                personInput.setText(loan.personName);
                principalInput.setText(Money.decimal(loan.principalMinor));
                notesInput.setText(loan.description);
                date = LocalDate.parse(loan.loanDate);
                updateDate();
            });
        });
    }

    private void updateDate() {
        dateValue.setText(DateUtils.dateForDisplay(date));
    }

    private void pickDate() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            date = LocalDate.of(year, month + 1, dayOfMonth);
            updateDate();
        }, date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth()).show();
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
        String person = personInput.getText() == null
                ? "" : personInput.getText().toString().trim();
        if (person.isEmpty()) {
            showError(getString(R.string.error_person));
            return;
        }
        String principalText = principalInput.getText() == null
                ? "" : principalInput.getText().toString();
        if (!Money.isValidPositive(principalText)) {
            showError(getString(R.string.error_amount));
            return;
        }
        long principalMinor = Money.parseMinor(principalText);
        if (existing != null && principalMinor < existing.repaidMinor) {
            showError(getString(R.string.error_principal_repaid));
            return;
        }
        String notes = notesInput.getText() == null
                ? null : notesInput.getText().toString().trim();
        if (notes != null && notes.isEmpty()) {
            notes = null;
        }
        Loan loan = existing == null ? new Loan() : existing;
        loan.userId = session.userId;
        loan.direction = direction;
        loan.personName = person;
        loan.principalMinor = principalMinor;
        loan.loanDate = date.toString();
        loan.description = notes;
        saving = true;
        saveButton.setEnabled(false);
        AppExecutors.io().execute(() -> {
            new LoanRepository(this).saveLoan(loan);
            AppExecutors.main(() -> {
                saving = false;
                saveButton.setEnabled(true);
                if (!isFinishing()) {
                    finish();
                }
            });
        });
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        errorText.setVisibility(View.GONE);
    }
}
