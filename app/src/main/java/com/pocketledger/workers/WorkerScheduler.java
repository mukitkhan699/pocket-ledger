package com.pocketledger.workers;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.pocketledger.utils.Constants;
import com.pocketledger.utils.DateUtils;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

public final class WorkerScheduler {

    private WorkerScheduler() {
    }

    public static void scheduleAll(Context context) {
        scheduleMealGeneration(context);
        scheduleSync(context);
    }

    public static void scheduleMealGeneration(Context context) {
        long delayMillis = delayUntilNextGeneration();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                MealGenerationWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .addTag(Constants.WORK_MEALS)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                Constants.WORK_MEALS, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    public static void scheduleSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                SyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(Constants.WORK_SYNC)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                Constants.WORK_SYNC, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    public static void cancelSync(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(Constants.WORK_SYNC);
        WorkManager.getInstance(context).cancelUniqueWork(Constants.WORK_SYNC + "-now");
    }

    public static void requestImmediateSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .addTag(Constants.WORK_SYNC + "-now")
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                Constants.WORK_SYNC + "-now", ExistingWorkPolicy.REPLACE, request);
    }

    private static long delayUntilNextGeneration() {
        ZonedDateTime now = ZonedDateTime.now(DateUtils.ZONE);
        ZonedDateTime next = now.toLocalDate().atTime(LocalTime.of(0, 5)).atZone(DateUtils.ZONE);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        return Math.max(1000L, Duration.between(now, next).toMillis());
    }
}
