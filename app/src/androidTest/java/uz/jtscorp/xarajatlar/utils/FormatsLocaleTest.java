package uz.jtscorp.xarajatlar.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.util.Locale;

@RunWith(AndroidJUnit4.class)
public class FormatsLocaleTest {
    static Context localized(String tag) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocales(new LocaleList(Locale.forLanguageTag(tag)));
        return base.createConfigurationContext(config);
    }

    @Test
    public void monthNameFollowsTheContextPassedInNotACachedLocale() {
        LocalDate october = LocalDate.of(2026, 10, 9);
        String russian = Formats.monthYear(localized("ru"), october);
        String english = Formats.monthYear(localized("en"), october);

        assertEquals("October 2026", english);
        assertEquals("октябрь 2026", russian.toLowerCase(Locale.ROOT));
        assertNotEquals(russian, Formats.monthYear(localized("uz"), october));
    }

    @Test
    public void localeIsReadFromTheContext() {
        assertEquals("ru", Formats.locale(localized("ru")).getLanguage());
        assertEquals("en", Formats.locale(localized("en")).getLanguage());
    }
}
