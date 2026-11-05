package com.pocketledger.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pocketledger.database.SettingsDao;
import com.pocketledger.models.Session;
import com.pocketledger.models.UserSettings;
import com.pocketledger.repositories.MealRepository;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.SessionManager;

public class MealGenerationWorker extends Worker {

    public MealGenerationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Session session = SessionManager.get(getApplicationContext()).getSession();
        if (session == null) {
            return Result.success();
        }
        try {
            MealRepository repository = new MealRepository(getApplicationContext());
            int inserted = repository.ensureDailyMeals(session.userId, DateUtils.today());
            UserSettings settings = new SettingsDao(getApplicationContext()).getUserSettings(session.userId);
            if (inserted > 0 && settings.notificationsEnabled) {
                NotificationHelper.notifyMealsReady(getApplicationContext());
            }
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
