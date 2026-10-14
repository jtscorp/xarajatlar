package uz.jtscorp.xarajatlar.utils;

import android.content.Context;

import androidx.annotation.Nullable;

import uz.jtscorp.xarajatlar.R;

/**
 * Summalardan keyin ko'rinadigan valyuta belgisi. Asosiy manba — app_settings.currencyLabel;
 * bu yerdagi SharedPreferences nusxasi ekran chizilayotganda bazaga murojaat qilmaslik uchun.
 */
public final class CurrencyLabel {
    public static final int MAX_LENGTH = 8;
    private static final String PREFS_NAME = "currency_label";
    private static final String KEY_LABEL = "label";

    private CurrencyLabel() {}

    /** Kiritilgan matnni tozalaydi; bo'sh yoki juda uzun bo'lsa null. */
    @Nullable
    public static String normalize(@Nullable String input) {
        if (input == null) return null;
        String label = input.trim();
        return label.isEmpty() || label.length() > MAX_LENGTH ? null : label;
    }

    /** Foydalanuvchi tanlagan belgi; tanlanmagan bo'lsa null (standart). */
    @Nullable
    public static String custom(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_LABEL, null);
    }

    /** Ko'rsatiladigan belgi: tanlangan, aks holda joriy til bo'yicha standart. */
    public static String current(Context context) {
        String custom = custom(context);
        return custom != null ? custom : context.getString(R.string.ui_som);
    }

    /** Bazadagi qiymat o'zgarganda nusxani yangilaydi; null — standartga qaytarish. */
    public static void cache(Context context, @Nullable String label) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_LABEL, normalize(label)).apply();
    }
}
