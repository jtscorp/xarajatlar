package uz.jtscorp.xarajatlar.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import uz.jtscorp.xarajatlar.MainActivity;
import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.ui.camera.QrScanActivity;
import uz.jtscorp.xarajatlar.ui.camera.OcrScanActivity;
import uz.jtscorp.xarajatlar.ui.expense.ManualExpenseActivity;

import java.util.concurrent.Executor;

public class LockActivity extends BasePinActivity {
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION = 30000; // 30 soniya

    private int failedAttempts = 0;
    private boolean isLockedOut = false;
    private final Handler lockoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable lockoutRunnable = () -> {
        isLockedOut = false;
        failedAttempts = 0;
        securePrefs.setFailedAttempts(0);
        securePrefs.setLockoutEndTime(0);
        errorText.setVisibility(View.GONE);
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setup tekshirish — super.onCreate dan OLDIN
        securePrefs = new uz.jtscorp.xarajatlar.utils.SecurePreferences(this);
        if (!securePrefs.isSetupComplete()) {
            startActivity(new Intent(this, LanguageActivity.class));
            finish();
            // super.onCreate ni chaqirmaslik — setContentView kerak emas
            super.onCreate(savedInstanceState);
            return;
        }
        // Yangilangan mavjud foydalanuvchi: til tanlanmagan bo'lsa o'zbekcha qoladi.
        uz.jtscorp.xarajatlar.utils.AppLanguage.ensureDefault();

        super.onCreate(savedInstanceState);

        // Lock screen UI
        View lockIcon = findViewById(R.id.lockIcon);
        lockIcon.setVisibility(View.VISIBLE);
        titleText.setText(R.string.pin_enter_title);
        subtitleText.setVisibility(View.GONE);

        restoreLockoutState();
        checkBiometric();
        syncCurrencyLabel();
    }

    @Override
    protected void onPinComplete(String pin) {
        if (securePrefs.verifyPin(pin)) {
            securePrefs.setFailedAttempts(0);
            securePrefs.setLockoutEndTime(0);
            onAuthSuccess();
        } else {
            failedAttempts++;
            securePrefs.setFailedAttempts(failedAttempts);
            if (failedAttempts >= MAX_ATTEMPTS) {
                startLockout();
            } else {
                errorText.setText(getResources().getQuantityString(R.plurals.pin_wrong_attempts_left,
                        MAX_ATTEMPTS - failedAttempts, MAX_ATTEMPTS - failedAttempts));
                errorText.setVisibility(View.VISIBLE);
                resetPin();
            }
        }
    }

    @Override
    protected boolean canInput() {
        return !isLockedOut;
    }

    private void startLockout() {
        isLockedOut = true;
        long lockoutEndTime = System.currentTimeMillis() + LOCKOUT_DURATION;
        securePrefs.setLockoutEndTime(lockoutEndTime);
        errorText.setText(getResources().getQuantityString(R.plurals.pin_lockout_seconds,
                (int) (LOCKOUT_DURATION / 1000), (int) (LOCKOUT_DURATION / 1000)));
        errorText.setVisibility(View.VISIBLE);
        resetPin();

        lockoutHandler.postDelayed(lockoutRunnable, LOCKOUT_DURATION);
    }

    private void restoreLockoutState() {
        failedAttempts = securePrefs.getFailedAttempts();
        long lockoutEndTime = securePrefs.getLockoutEndTime();

        if (lockoutEndTime > 0) {
            long remaining = lockoutEndTime - System.currentTimeMillis();
            if (remaining > 0) {
                isLockedOut = true;
                errorText.setText(R.string.pin_wait);
                errorText.setVisibility(View.VISIBLE);
                resetPin();
                lockoutHandler.postDelayed(lockoutRunnable, remaining);
            } else {
                securePrefs.setFailedAttempts(0);
                securePrefs.setLockoutEndTime(0);
                failedAttempts = 0;
            }
        }
    }

    /** Bazadagi valyuta belgisini tez o'qiladigan nusxaga ko'chiradi (asosiy manba — baza). */
    private void syncCurrencyLabel() {
        android.content.Context appContext = getApplicationContext();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            var settings = AppDatabase.getDatabase(appContext).appSettingsDao().getSettingsSync();
            if (settings != null) {
                uz.jtscorp.xarajatlar.utils.CurrencyLabel.cache(appContext, settings.getCurrencyLabel());
            }
        });
    }

    private void checkBiometric() {
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS) {

            AppDatabase.databaseWriteExecutor.execute(() -> {
                var settings = AppDatabase.getDatabase(this).appSettingsDao().getSettingsSync();
                if (settings != null && settings.isBiometricEnabled()) {
                    runOnUiThread(() -> {
                        biometricButton.setVisibility(View.VISIBLE);
                        biometricButton.setText(R.string.ui_barmoq_izi_bilan_kirish);
                        biometricButton.setOnClickListener(v -> showBiometricPrompt());
                        showBiometricPrompt();
                    });
                }
            });
        }
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        securePrefs.setFailedAttempts(0);
                        securePrefs.setLockoutEndTime(0);
                        onAuthSuccess();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.ui_barmoq_izi_bilan_kirish))
                .setSubtitle(getString(R.string.biometric_subtitle))
                .setNegativeButtonText(getString(R.string.biometric_use_pin))
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void onAuthSuccess() {
        securePrefs.setLastLockTime(System.currentTimeMillis());

        String shortcutTarget = getIntent().getStringExtra("shortcut_target");
        if (shortcutTarget != null) {
            Class<?> targetClass = switch (shortcutTarget) {
                case "qr_scan" -> QrScanActivity.class;
                case "add_expense" -> ManualExpenseActivity.class;
                case "ocr_scan" -> OcrScanActivity.class;
                default -> null;
            };
            if (targetClass != null) {
                Intent mainIntent = new Intent(this, MainActivity.class);
                mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                Intent targetIntent = new Intent(this, targetClass);
                startActivities(new Intent[]{mainIntent, targetIntent});
                finish();
                return;
            }
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        lockoutHandler.removeCallbacks(lockoutRunnable);
    }
}
