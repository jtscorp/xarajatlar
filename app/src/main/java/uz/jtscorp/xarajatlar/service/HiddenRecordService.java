package uz.jtscorp.xarajatlar.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleService;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import uz.jtscorp.xarajatlar.MainActivity;
import uz.jtscorp.xarajatlar.R;

public class HiddenRecordService extends LifecycleService {

    private static final String TAG = "HiddenRecordService";
    private static final String CHANNEL_ID = "hidden_record_channel";
    private static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_START_RECORDING = "uz.jtscorp.xarajatlar.START_RECORDING";
    public static final String ACTION_STOP_RECORDING = "uz.jtscorp.xarajatlar.STOP_RECORDING";

    private VideoCapture<Recorder> videoCapture;
    private Recording currentRecording;
    private boolean isRecording = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_START_RECORDING.equals(action)) {
            startForeground(NOTIFICATION_ID, createNotification(getString(R.string.record_starting)));
            vibrateOnce();
            startRecording();
        } else if (ACTION_STOP_RECORDING.equals(action)) {
            vibrateTwice();
            stopRecording();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.record_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.record_channel_description));

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void startRecording() {
        if (isRecording) {
            Log.w(TAG, "Already recording");
            return;
        }

        // Check permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Camera permission not granted");
            stopSelf();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Audio permission not granted");
            stopSelf();
            return;
        }

        // Create output file
        File outputDir = new File(getFilesDir(), "debts/temp");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault());
        String filename = "hidden_" + sdf.format(new Date()) + ".mp4";
        File outputFile = new File(outputDir, filename);

        // Initialize camera
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Create recorder
                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HD))
                        .build();

                videoCapture = VideoCapture.withOutput(recorder);

                // Select back camera
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Unbind any existing use cases
                cameraProvider.unbindAll();

                // Bind video capture use case
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        videoCapture
                );

                // Start recording
                FileOutputOptions outputOptions = new FileOutputOptions.Builder(outputFile).build();

                currentRecording = videoCapture.getOutput()
                        .prepareRecording(this, outputOptions)
                        .withAudioEnabled()
                        .start(ContextCompat.getMainExecutor(this), videoRecordEvent -> {
                            if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                                isRecording = true;
                                Log.d(TAG, "Recording started");
                                updateNotification(getString(R.string.record_running));
                            } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                                isRecording = false;
                                VideoRecordEvent.Finalize finalizeEvent =
                                        (VideoRecordEvent.Finalize) videoRecordEvent;

                                if (!finalizeEvent.hasError()) {
                                    Log.d(TAG, "Recording saved: " + outputFile.getAbsolutePath());
                                    notifyRecordingComplete(outputFile.getAbsolutePath());
                                } else {
                                    Log.e(TAG, "Recording error: " + finalizeEvent.getError());
                                }
                            }
                        });

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed", e);
                stopSelf();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void stopRecording() {
        if (currentRecording != null) {
            currentRecording.stop();
            currentRecording = null;
        }
        isRecording = false;
    }

    private void updateNotification(String text) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(text));
        }
    }

    private void notifyRecordingComplete(String filePath) {
        // Broadcast that recording is complete
        Intent broadcastIntent = new Intent("uz.jtscorp.xarajatlar.RECORDING_COMPLETE");
        broadcastIntent.putExtra("file_path", filePath);
        sendBroadcast(broadcastIntent);
    }

    private void vibrateOnce() {
        vibrate(new long[]{0, 200});
    }

    private void vibrateTwice() {
        vibrate(new long[]{0, 150, 100, 150});
    }

    private void vibrate(long[] pattern) {
        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager =
                    (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vibratorManager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecording();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }
}
