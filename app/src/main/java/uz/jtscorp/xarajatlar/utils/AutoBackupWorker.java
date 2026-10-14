package uz.jtscorp.xarajatlar.utils;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.Calendar;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoBackupWorker extends Worker {

    private static final long TIMEOUT_MINUTES = 5;
    private static final String WORK_NAME = "auto_backup";

    public AutoBackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context appContext = getApplicationContext();
        if (!AutoBackupState.isEnabled(appContext)) return Result.success();
        DataExportImport exporter = new DataExportImport(getApplicationContext());
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences(DataExportImport.PREFS_NAME, Context.MODE_PRIVATE);
        String uriString = prefs.getString(DataExportImport.KEY_BACKUP_FOLDER_URI, null);

        DataExportImport.ExportCallback callback = new DataExportImport.ExportCallback() {
            @Override
            public void onSuccess(Uri fileUri) {
                AutoBackupState.recordSuccess(appContext, System.currentTimeMillis());
                success.set(true);
                latch.countDown();
            }

            @Override
            public void onError(String message) {
                AutoBackupState.recordFailure(appContext, message);
                success.set(false);
                latch.countDown();
            }
        };

        if (uriString != null) {
            exporter.exportData(Uri.parse(uriString), callback);
        } else {
            exporter.exportData(callback);
        }

        try {
            if (!latch.await(TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                return Result.failure();
            }
        } catch (InterruptedException e) {
            return Result.failure();
        } finally {
            exporter.shutdown();
        }

        return success.get() ? Result.success() : Result.failure();
    }

    /** Sozlamaga qarab kunlik zaxirani rejalashtiradi yoki bekor qiladi. */
    public static void sync(Context context) {
        WorkManager workManager = WorkManager.getInstance(context.getApplicationContext());
        if (!AutoBackupState.isEnabled(context)) {
            workManager.cancelUniqueWork(WORK_NAME);
            return;
        }
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, 22);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        if (now.after(target)) target.add(Calendar.DAY_OF_MONTH, 1);
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                AutoBackupWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(target.getTimeInMillis() - now.getTimeInMillis(), TimeUnit.MILLISECONDS)
                .build();
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }
}
