package uz.jtscorp.xarajatlar.utils;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import uz.jtscorp.xarajatlar.R;

/** Ilova tili. Tanlovni tizim saqlaydi (per-app language), bu klass holat saqlamaydi. */
public final class AppLanguage {
    public static final String[] TAGS = {"uz", "ru", "en"};
    private static final String DEFAULT_TAG = "uz";

    private AppLanguage() {}

    /** Hozir ekranda ko'rinayotgan til. */
    public static String current(Context context) {
        return context.getString(R.string.language_tag);
    }

    public static int indexOf(String tag) {
        for (int i = 0; i < TAGS.length; i++) {
            if (TAGS[i].equals(tag)) return i;
        }
        return 0;
    }

    public static void set(String tag) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag));
    }

    /** Til hali tanlanmagan bo'lsa o'zbekchani o'rnatadi (yangilangan mavjud foydalanuvchilar uchun). */
    public static void ensureDefault() {
        if (AppCompatDelegate.getApplicationLocales().isEmpty()) set(DEFAULT_TAG);
    }
}
