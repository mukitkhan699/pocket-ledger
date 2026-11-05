package com.pocketledger.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.pocketledger.BuildConfig;
import com.pocketledger.R;
import com.pocketledger.activities.AuthActivity;
import com.pocketledger.activities.BackupActivity;
import com.pocketledger.activities.CategoriesActivity;
import com.pocketledger.activities.LoansActivity;
import com.pocketledger.activities.MainActivity;
import com.pocketledger.activities.MealsActivity;
import com.pocketledger.database.SettingsDao;
import com.pocketledger.models.Session;
import com.pocketledger.models.SyncConflict;
import com.pocketledger.models.UserSettings;
import com.pocketledger.network.NetworkMonitor;
import com.pocketledger.sync.SyncManager;
import com.pocketledger.sync.SyncResult;
import com.pocketledger.utils.AppExecutors;
import com.pocketledger.utils.Constants;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Prefs;
import com.pocketledger.utils.SessionManager;
import com.pocketledger.utils.ThemeUtils;
import com.pocketledger.utils.UiUtils;
import com.pocketledger.workers.WorkerScheduler;

import java.util.List;

public class ManageFragment extends Fragment {

    private TextView profileAvatar;
    private TextView profileName;
    private TextView profileEmail;
    private TextView profileBadge;
    private TextView themeValue;
    private TextView syncStatusValue;
    private TextView lastSyncValue;
    private TextView pendingValue;
    private TextView conflictsValue;
    private TextView lastBackupValue;
    private TextView versionValue;
    private SwitchMaterial notificationsSwitch;
    private SwitchMaterial autoSyncSwitch;
    private UserSettings settings;
    private boolean binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage, container, false);
        profileAvatar = view.findViewById(R.id.profileAvatar);
        profileName = view.findViewById(R.id.profileName);
        profileEmail = view.findViewById(R.id.profileEmail);
        profileBadge = view.findViewById(R.id.profileBadge);
        themeValue = view.findViewById(R.id.themeValue);
        syncStatusValue = view.findViewById(R.id.syncStatusValue);
        lastSyncValue = view.findViewById(R.id.lastSyncValue);
        pendingValue = view.findViewById(R.id.pendingValue);
        conflictsValue = view.findViewById(R.id.conflictsValue);
        lastBackupValue = view.findViewById(R.id.lastBackupValue);
        versionValue = view.findViewById(R.id.versionValue);
        notificationsSwitch = view.findViewById(R.id.notificationsSwitch);
        autoSyncSwitch = view.findViewById(R.id.autoSyncSwitch);

        view.findViewById(R.id.mealPricesRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MealsActivity.class)));
        view.findViewById(R.id.categoriesRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CategoriesActivity.class)));
        view.findViewById(R.id.loansRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), LoansActivity.class)));
        view.findViewById(R.id.backupRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), BackupActivity.class)));
        view.findViewById(R.id.themeRow).setOnClickListener(v -> chooseTheme());
        view.findViewById(R.id.syncNowRow).setOnClickListener(v -> syncNow());
        view.findViewById(R.id.conflictsRow).setOnClickListener(v -> showConflicts());
        view.findViewById(R.id.logoutButton).setOnClickListener(v -> confirmLogout());

        notificationsSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            if (binding || settings == null) {
                return;
            }
            settings.notificationsEnabled = checked;
            saveSettings();
        });
        autoSyncSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            if (binding || settings == null) {
                return;
            }
            settings.autoSyncEnabled = checked;
            if (checked) {
                WorkerScheduler.scheduleSync(requireContext());
            } else {
                WorkerScheduler.cancelSync(requireContext());
            }
            saveSettings();
        });
        versionValue.setText(BuildConfig.VERSION_NAME);
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
        String name = session.fullName == null || session.fullName.trim().isEmpty()
                ? "Friend" : session.fullName.trim();
        profileName.setText(name);
        profileEmail.setText(session.email == null ? "" : session.email);
        profileAvatar.setText(name.substring(0, 1).toUpperCase());
        if (session.localOnly) {
            profileBadge.setText(R.string.local_only_badge);
        } else {
            profileBadge.setText(R.string.cloud_connected);
        }
        String lastBackup = Prefs.getLastBackupAt(requireContext());
        lastBackupValue.setText(lastBackup == null ? getString(R.string.never)
                : DateUtils.dateTimeForDisplay(lastBackup));
        AppExecutors.io().execute(() -> {
            SettingsDao dao = new SettingsDao(requireContext());
            UserSettings loaded = dao.getUserSettings(session.userId);
            int pending = dao.queueCount(session.userId);
            int conflicts = dao.conflictCount(session.userId);
            AppExecutors.main(() -> {
                if (!isAdded()) {
                    return;
                }
                settings = loaded;
                binding = true;
                notificationsSwitch.setChecked(loaded.notificationsEnabled);
                autoSyncSwitch.setChecked(loaded.autoSyncEnabled);
                binding = false;
                themeValue.setText(ThemeUtils.label(loaded.theme));
                lastSyncValue.setText(loaded.lastSyncAt == null ? getString(R.string.never)
                        : DateUtils.dateTimeForDisplay(loaded.lastSyncAt));
                pendingValue.setText(getResources().getQuantityString(
                        R.plurals.pending_changes, pending, pending));
                conflictsValue.setText(getResources().getQuantityString(
                        R.plurals.conflicts_count, conflicts, conflicts));
                if (session.localOnly) {
                    syncStatusValue.setText(R.string.local_only_badge);
                } else if (!NetworkMonitor.isOnline(requireContext())) {
                    syncStatusValue.setText(R.string.offline);
                } else {
                    syncStatusValue.setText(R.string.cloud_connected);
                }
            });
        });
    }

    private void saveSettings() {
        Session session = SessionManager.get(requireContext()).getSession();
        if (session == null || settings == null) {
            return;
        }
        settings.updatedAt = DateUtils.nowIso();
        AppExecutors.io().execute(() -> {
            SettingsDao dao = new SettingsDao(requireContext());
            dao.saveUserSettings(settings);
            dao.enqueue(session.userId, Constants.TABLE_USER_SETTINGS, settings.id);
        });
    }

    private void chooseTheme() {
        if (settings == null) {
            return;
        }
        String[] labels = {"Dark", "Light", "System"};
        String[] values = {Constants.THEME_DARK, Constants.THEME_LIGHT, Constants.THEME_SYSTEM};
        int selected = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(settings.theme)) {
                selected = i;
            }
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.theme)
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    String theme = values[which];
                    Prefs.setTheme(requireContext(), theme);
                    settings.theme = theme;
                    saveSettings();
                    ThemeUtils.applyTheme(theme);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void syncNow() {
        Session session = SessionManager.get(requireContext()).getSession();
        if (session == null) {
            return;
        }
        if (session.localOnly) {
            showMessage(getString(R.string.sync_local_only));
            return;
        }
        showMessage(getString(R.string.sync_running));
        SyncManager.get(requireContext()).syncAsync(this::onSyncFinished);
    }

    private void onSyncFinished(SyncResult result) {
        if (!isAdded()) {
            return;
        }
        showMessage(result.message);
        load();
    }

    private void showConflicts() {
        Session session = SessionManager.get(requireContext()).getSession();
        if (session == null) {
            return;
        }
        AppExecutors.io().execute(() -> {
            SettingsDao dao = new SettingsDao(requireContext());
            List<SyncConflict> conflicts = dao.conflicts(session.userId, 30);
            AppExecutors.main(() -> {
                if (!isAdded()) {
                    return;
                }
                if (conflicts.isEmpty()) {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.sync_conflicts)
                            .setMessage(R.string.no_conflicts)
                            .setPositiveButton(R.string.close, null)
                            .show();
                    return;
                }
                StringBuilder message = new StringBuilder();
                for (SyncConflict conflict : conflicts) {
                    if (message.length() > 0) {
                        message.append("\n\n");
                    }
                    message.append(conflict.tableName)
                            .append(" · ")
                            .append(conflict.recordId)
                            .append("\n")
                            .append(DateUtils.dateTimeForDisplay(conflict.createdAt))
                            .append(" — ")
                            .append(getString(R.string.server_version_kept));
                }
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.sync_conflicts)
                        .setMessage(message.toString())
                        .setNegativeButton(R.string.close, null)
                        .setPositiveButton(R.string.clear_conflicts, (dialog, which) ->
                                AppExecutors.io().execute(() -> {
                                    new SettingsDao(requireContext()).clearConflicts(session.userId);
                                    AppExecutors.main(() -> {
                                        if (isAdded()) {
                                            load();
                                        }
                                    });
                                }))
                        .show();
            });
        });
    }

    private void confirmLogout() {
        UiUtils.confirm(requireContext(), getString(R.string.logout),
                getString(R.string.logout_confirm), getString(R.string.logout), () -> {
                    SessionManager.get(requireContext()).clear();
                    WorkerScheduler.cancelSync(requireContext());
                    Intent intent = new Intent(requireContext(), AuthActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finishAffinity();
                });
    }

    private void showMessage(String message) {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).showMessage(message);
        }
    }
}
