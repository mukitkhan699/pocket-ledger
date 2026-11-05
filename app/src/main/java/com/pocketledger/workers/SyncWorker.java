package com.pocketledger.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pocketledger.database.SettingsDao;
import com.pocketledger.models.Session;
import com.pocketledger.models.UserSettings;
import com.pocketledger.sync.SyncManager;
import com.pocketledger.sync.SyncResult;
import com.pocketledger.utils.SessionManager;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Session session = SessionManager.get(getApplicationContext()).getSession();
        if (session == null || session.localOnly) {
            return Result.success();
        }
        UserSettings settings = new SettingsDao(getApplicationContext()).getUserSettings(session.userId);
        if (!settings.autoSyncEnabled) {
            return Result.success();
        }
        SyncResult result = SyncManager.get(getApplicationContext()).syncBlocking();
        if (result.success || result.localOnly || result.notConfigured || result.unauthorized) {
            return Result.success();
        }
        if (result.noNetwork) {
            return Result.retry();
        }
        return Result.retry();
    }
}
