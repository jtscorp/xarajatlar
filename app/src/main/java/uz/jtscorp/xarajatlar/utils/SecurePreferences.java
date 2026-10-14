package uz.jtscorp.xarajatlar.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SecurePreferences {
    private static final String PREF_NAME = "secure_prefs";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_IS_SETUP_COMPLETE = "is_setup_complete";
    private static final String KEY_LAST_LOCK_TIME = "last_lock_time";
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";
    private static final String KEY_LOCKOUT_END_TIME = "lockout_end_time";

    private final SharedPreferences prefs;

    public SecurePreferences(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            prefs = EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to create secure preferences", e);
        }
    }

    public void savePin(String pin) {
        String hash = hashPin(pin);
        prefs.edit().putString(KEY_PIN_HASH, hash).apply();
    }

    public boolean verifyPin(String pin) {
        String savedHash = prefs.getString(KEY_PIN_HASH, null);
        if (savedHash == null) return false;
        return savedHash.equals(hashPin(pin));
    }

    public boolean hasPin() {
        return prefs.getString(KEY_PIN_HASH, null) != null;
    }

    public void setSetupComplete(boolean complete) {
        prefs.edit().putBoolean(KEY_IS_SETUP_COMPLETE, complete).apply();
    }

    public boolean isSetupComplete() {
        return prefs.getBoolean(KEY_IS_SETUP_COMPLETE, false);
    }

    public void setLastLockTime(long time) {
        prefs.edit().putLong(KEY_LAST_LOCK_TIME, time).apply();
    }

    public long getLastLockTime() {
        return prefs.getLong(KEY_LAST_LOCK_TIME, 0);
    }

    public void setFailedAttempts(int attempts) {
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts).apply();
    }

    public int getFailedAttempts() {
        return prefs.getInt(KEY_FAILED_ATTEMPTS, 0);
    }

    public void setLockoutEndTime(long time) {
        prefs.edit().putLong(KEY_LOCKOUT_END_TIME, time).apply();
    }

    public long getLockoutEndTime() {
        return prefs.getLong(KEY_LOCKOUT_END_TIME, 0);
    }

    private String hashPin(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(pin.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
