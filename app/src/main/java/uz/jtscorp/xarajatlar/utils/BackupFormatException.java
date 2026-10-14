package uz.jtscorp.xarajatlar.utils;

import android.content.Context;

import androidx.annotation.StringRes;

import org.json.JSONException;

/** Zaxira ma'lumoti noto'g'ri. Matn ko'rsatish paytida joriy tilda hosil qilinadi. */
final class BackupFormatException extends JSONException {
    @StringRes private final int messageRes;
    private final Object[] args;

    BackupFormatException(@StringRes int messageRes, Object... args) {
        super("Invalid backup data");
        this.messageRes = messageRes;
        this.args = args;
    }

    String message(Context context) {
        return context.getString(messageRes, args);
    }
}
