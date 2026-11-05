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
import com.pocketledger.activities.MainActivity;
import com.pocketledger.database.SettingsDao;
import com.pocketledger.models.Session;
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

public class SettingsFragment extends Fragment {

    private TextView themeValue;
    private TextView syncStatusValue;
    private TextView lastSyncValue;
    private TextView versionValue;
    private SwitchMaterial notificationsSwitch;
    private SwitchMaterial autoSyncSwitch;
    private UserSettings settings;
    private boolean binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        themeValue = view.findViewById(R.id.themeValue);
        syncStatusValue = view.findViewById(R.id.syncStatusValue);
        lastSyncValue = view.findViewById(R.id.lastSyncValue);
        versionValue = view.findViewById(R.id.versionValue);
        notificationsSwitch = view.findViewById(R.id.notificationsSwitch);
        autoSyncSwitch = view.findViewById(R.id.autoSyncSwitch);

        versionValue.setText(BuildConfig.VERSION_NAME);

        view.findViewById(R.id.themeRow).setOnClickListener(v -> chooseTheme());
        view.findViewById(R.id.syncNowRow).setOnClickListener(v -> syncNow());
        view.findViewById(R.id.autoSyncRow).setOnClickListener(v -> {
            if (autoSyncSwitch != null) {
                autoSyncSwitch.setChecked(!autoSyncSwitch.isChecked());
            }
        });
        view.findViewById(R.id.backupRow).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), BackupActivity.class)));
        view.findViewById(R.id.logoutButton).setOnClickListener(v -> confirmLogout());

        notificationsSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            if (binding || settings == null) return;
            settings.notificationsEnabled = checked;
            saveSettings();
        });
        autoSyncSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            if (binding || settings == null) return;
            settings.autoSyncEnabled = checked;
            if (checked) WorkerScheduler.scheduleSync(requireContext());
            else WorkerScheduler.cancelSync(requireContext());
            saveSettings();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        Session session = SessionManager.get(requireContext()).getSession();
        if (session == null) return;

        AppExecutors.io().execute(() -> {
            SettingsDao dao = new SettingsDao(requireContext());
            UserSettings loaded = dao.getUserSettings(session.userId);
            int pending = dao.queueCount(session.userId);
            AppExecutors.main(() -> {
                if (!isAdded()) return;
                settings = loaded;
                binding = true;
                notificationsSwitch.setChecked(loaded.notificationsEnabled);
                autoSyncSwitch.setChecked(loaded.autoSyncEnabled);
                binding = false;
                themeValue.setText(ThemeUtils.label(loaded.theme));
                lastSyncValue.setText(loaded.lastSyncAt == null ? getString(R.string.never)
                        : DateUtils.dateTimeForDisplay(loaded.lastSyncAt));
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
        if (session == null || settings == null) return;
        settings.updatedAt = DateUtils.nowIso();
        AppExecutors.io().execute(() -> {
            SettingsDao dao = new SettingsDao(requireContext());
            dao.saveUserSettings(settings);
            dao.enqueue(session.userId, Constants.TABLE_USER_SETTINGS, settings.id);
        });
    }

    private void chooseTheme() {
        if (settings == null) return;
        String[] labels = {getString(R.string.theme_dark), getString(R.string.theme_light), getString(R.string.theme_system)};
        String[] values = {Constants.THEME_DARK, Constants.THEME_LIGHT, Constants.THEME_SYSTEM};
        int selected = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(settings.theme)) selected = i;
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
        if (session == null) return;
        if (session.localOnly) {
            ((MainActivity) requireActivity()).showMessage(getString(R.string.sync_local_only));
            return;
        }
        ((MainActivity) requireActivity()).showMessage(getString(R.string.sync_running));
        SyncManager.get(requireContext()).syncAsync(this::onSyncFinished);
    }

    private void onSyncFinished(SyncResult result) {
        if (!isAdded()) return;
        ((MainActivity) requireActivity()).showMessage(result.message);
        load();
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
}
