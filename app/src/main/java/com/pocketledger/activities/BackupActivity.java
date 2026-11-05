package com.pocketledger.activities;

import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.pocketledger.R;
import com.pocketledger.backup.BackupManager;
import com.pocketledger.models.Session;
import com.pocketledger.utils.AppExecutors;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Prefs;
import com.pocketledger.utils.SessionManager;
import com.pocketledger.workers.WorkerScheduler;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupActivity extends AppCompatActivity {

    private static final DateTimeFormatter FILE_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private TextView lastBackup;
    private final ActivityResultLauncher<String> createJson =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"),
                    uri -> {
                        if (uri != null) {
                            exportJson(uri);
                        }
                    });
    private final ActivityResultLauncher<String> createCsv =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"),
                    uri -> {
                        if (uri != null) {
                            exportCsv(uri);
                        }
                    });
    private final ActivityResultLauncher<String[]> openRestore =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    uri -> {
                        if (uri != null) {
                            confirmRestore(uri);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);
        lastBackup = findViewById(R.id.lastBackup);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.exportJsonButton).setOnClickListener(v ->
                createJson.launch("pocket-ledger-backup-" + stamp() + ".json"));
        findViewById(R.id.exportCsvButton).setOnClickListener(v ->
                createCsv.launch("pocket-ledger-transactions-" + stamp() + ".csv"));
        findViewById(R.id.restoreButton).setOnClickListener(v ->
                openRestore.launch(new String[]{"application/json", "text/plain",
                        "application/octet-stream"}));
        updateLastBackup();
    }

    private String stamp() {
        return FILE_STAMP.format(LocalDateTime.now(DateUtils.ZONE));
    }

    private void updateLastBackup() {
        String value = Prefs.getLastBackupAt(this);
        lastBackup.setText(value == null ? getString(R.string.never_backed_up)
                : getString(R.string.backup_status) + ": " + DateUtils.dateTimeForDisplay(value));
    }

    private void exportJson(Uri uri) {
        Session session = SessionManager.get(this).getSession();
        if (session == null) {
            finish();
            return;
        }
        AppExecutors.io().execute(() -> {
            try {
                String json = BackupManager.exportJson(this, session.userId);
                write(uri, json.getBytes(StandardCharsets.UTF_8));
                AppExecutors.main(() -> {
                    Prefs.setLastBackupAt(this, DateUtils.nowIso());
                    updateLastBackup();
                    Toast.makeText(this, R.string.export_complete, Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                AppExecutors.main(() -> Toast.makeText(this, R.string.export_failed,
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private void exportCsv(Uri uri) {
        Session session = SessionManager.get(this).getSession();
        if (session == null) {
            finish();
            return;
        }
        AppExecutors.io().execute(() -> {
            try {
                String csv = BackupManager.exportTransactionsCsv(this, session.userId);
                write(uri, csv.getBytes(StandardCharsets.UTF_8));
                AppExecutors.main(() -> Toast.makeText(this, R.string.export_complete,
                        Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                AppExecutors.main(() -> Toast.makeText(this, R.string.export_failed,
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private void write(Uri uri, byte[] data) throws Exception {
        try (OutputStream output = getContentResolver().openOutputStream(uri, "wt")) {
            if (output == null) {
                throw new IllegalStateException("Could not open the selected file.");
            }
            output.write(data);
            output.flush();
        }
    }

    private void confirmRestore(Uri uri) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.restore_backup)
                .setMessage(R.string.restore_warning)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.restore_backup, (dialog, which) -> restore(uri))
                .show();
    }

    private void restore(Uri uri) {
        Session session = SessionManager.get(this).getSession();
        if (session == null) {
            finish();
            return;
        }
        AppExecutors.io().execute(() -> {
            try {
                String json = read(uri);
                int imported = BackupManager.restoreJson(this, session.userId, json);
                if (imported > 0) {
                    WorkerScheduler.requestImmediateSync(this);
                }
                AppExecutors.main(() -> Toast.makeText(this, R.string.restore_complete,
                        Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                AppExecutors.main(() -> Toast.makeText(this, R.string.restore_failed,
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private String read(Uri uri) throws Exception {
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new IllegalStateException("Could not open the selected file.");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }
}
