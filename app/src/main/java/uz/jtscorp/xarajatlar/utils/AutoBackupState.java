package uz.jtscorp.xarajatlar.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

/** Avto-zaxira yoqilganmi va oxirgi urinish qanday tugagani. Sozlamalarda ko'rsatiladi. */
public final class AutoBackupState {
    private static final String KEY_ENABLED = "auto_backup_enabled";
    private static final String KEY_LAST_SUCCESS = "auto_backup_last_success";
    private static final String KEY_LAST_ERROR = "auto_backup_last_error";

    private AutoBackupState() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(DataExportImport.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    /** Oxirgi muvaffaqiyatli zaxira vaqti; hali bo'lmagan bo'lsa 0. */
    public static long lastSuccessTime(Context context) {
        return prefs(context).getLong(KEY_LAST_SUCCESS, 0);
    }

    /** Oxirgi muvaffaqiyatdan keyingi xato; xato bo'lmagan bo'lsa null. */
    @Nullable
    public static String lastError(Context context) {
        return prefs(context).getString(KEY_LAST_ERROR, null);
    }

    public static void recordSuccess(Context context, long timeMillis) {
        prefs(context).edit().putLong(KEY_LAST_SUCCESS, timeMillis).remove(KEY_LAST_ERROR).apply();
    }

    public static void recordFailure(Context context, @Nullable String message) {
        prefs(context).edit().putString(KEY_LAST_ERROR, message == null ? "" : message).apply();
    }

    static void clear(Context context) {
        prefs(context).edit().remove(KEY_ENABLED).remove(KEY_LAST_SUCCESS).remove(KEY_LAST_ERROR).apply();
    }
}
