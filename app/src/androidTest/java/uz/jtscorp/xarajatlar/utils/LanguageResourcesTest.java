package uz.jtscorp.xarajatlar.utils;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import uz.jtscorp.xarajatlar.R;

@RunWith(AndroidJUnit4.class)
public class LanguageResourcesTest {
    private static String tagFor(String... localeTags) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration config = new Configuration(base.getResources().getConfiguration());
        Locale[] locales = new Locale[localeTags.length];
        for (int i = 0; i < localeTags.length; i++) locales[i] = Locale.forLanguageTag(localeTags[i]);
        config.setLocales(new LocaleList(locales));
        return base.createConfigurationContext(config).getString(R.string.language_tag);
    }

    @Test
    public void uzbekAppLanguageWinsOverRussianPhoneLanguage() {
        assertEquals("uz", tagFor("uz", "ru"));
        assertEquals("uz", tagFor("uz", "en"));
    }

    @Test
    public void eachSupportedLanguageResolvesToItself() {
        assertEquals("ru", tagFor("ru"));
        assertEquals("en", tagFor("en"));
        assertEquals("uz", tagFor("uz"));
    }

    @Test
    public void unsupportedLanguageFallsBackToUzbek() {
        assertEquals("uz", tagFor("kk"));
    }
}
