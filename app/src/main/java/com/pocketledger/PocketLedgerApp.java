package com.pocketledger;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.pocketledger.utils.Constants;
import com.pocketledger.utils.ThemeUtils;
import com.pocketledger.workers.WorkerScheduler;

public class PocketLedgerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeUtils.applySavedTheme(this);
        createNotificationChannel();
        WorkerScheduler.scheduleAll(this);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL,
                    "Pocket Ledger",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Meal reminders and sync notices");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
