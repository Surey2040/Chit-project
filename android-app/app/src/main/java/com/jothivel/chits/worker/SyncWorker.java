package com.jothivel.chits.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.ExistingWorkPolicy;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";

    public static void enqueueSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncWorkRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(
                        androidx.work.BackoffPolicy.EXPONENTIAL,
                        OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                        java.util.concurrent.TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                "JothiVelChitsSync",
                ExistingWorkPolicy.REPLACE,
                syncWorkRequest
        );
    }

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting background sync...");
        
        try {
            // 1. Fetch pending local changes (e.g. from Room DB)
            // 2. POST them to the Retrofit ApiService
            // 3. Mark them as synced in Room DB
            
            // Mock network delay
            Thread.sleep(2000);
            
            // 4. Fetch latest server state (groups, installments)
            // 5. Update local Room DB
            Log.d(TAG, "Sync completed successfully.");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Sync failed: " + e.getMessage());
            return Result.retry();
        }
    }
}
