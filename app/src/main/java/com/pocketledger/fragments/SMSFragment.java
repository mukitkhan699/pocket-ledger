package com.pocketledger.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.pocketledger.R;
import com.pocketledger.models.Session;
import com.pocketledger.repositories.TransactionRepository;
import com.pocketledger.utils.AppExecutors;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Money;
import com.pocketledger.utils.SessionManager;
import com.pocketledger.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class SMSFragment extends Fragment {

    private static final int PERMISSION_REQUEST_SMS = 1001;

    private RecyclerView smsList;
    private TextView emptyState;
    private TextView permissionHint;
    private View permissionCard;
    private View actionsCard;
    private boolean permissionsRequested;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sms, container, false);
        smsList = view.findViewById(R.id.smsList);
        emptyState = view.findViewById(R.id.smsEmpty);
        permissionHint = view.findViewById(R.id.permissionHint);
        permissionCard = view.findViewById(R.id.permissionCard);
        actionsCard = view.findViewById(R.id.actionsCard);

        smsList.setLayoutManager(new LinearLayoutManager(requireContext()));

        view.findViewById(R.id.grantSmsButton).setOnClickListener(v -> requestSmsPermission());
        view.findViewById(R.id.manualAddButton).setOnClickListener(v ->
                startActivity(new android.content.Intent(requireContext(),
                        com.pocketledger.activities.EditTransactionActivity.class)));

        updateUI();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        boolean hasPermission = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
        permissionCard.setVisibility(hasPermission ? View.GONE : View.VISIBLE);
        actionsCard.setVisibility(hasPermission ? View.VISIBLE : View.GONE);
        if (hasPermission) {
            loadSmsMessages();
        } else {
            emptyState.setVisibility(View.GONE);
            smsList.setVisibility(View.GONE);
            permissionHint.setText(R.string.sms_permission_needed);
        }
    }

    private void requestSmsPermission() {
        if (permissionsRequested) {
            return;
        }
        permissionsRequested = true;
        requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS}, PERMISSION_REQUEST_SMS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_SMS) {
            permissionsRequested = false;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateUI();
            } else {
                permissionHint.setText(R.string.sms_permission_denied);
            }
        }
    }

    private void loadSmsMessages() {
        Session session = SessionManager.get(requireContext()).getSession();
        if (session == null) {
            return;
        }
        AppExecutors.io().execute(() -> {
            List<String> messages = new ArrayList<>();
            AppExecutors.main(() -> {
                if (!isAdded()) {
                    return;
                }
                boolean isEmpty = messages.isEmpty();
                smsList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void createTransactionFromSms(String message) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.create_transaction)
                .setMessage(message)
                .setPositiveButton(R.string.create_transaction, (dialog, which) -> {
                    startActivity(new android.content.Intent(requireContext(),
                            com.pocketledger.activities.EditTransactionActivity.class));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
