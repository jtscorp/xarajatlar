package uz.jtscorp.xarajatlar.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CurrencyLabelDeviceTest {
    private static final char NBSP = ' ';
    private final Context context =
            InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Before
    @After
    public void resetToDefault() {
        CurrencyLabel.cache(context, null);
    }

    @Test
    public void defaultLabelFollowsTheLanguage() {
        assertNull(CurrencyLabel.custom(context));
        assertEquals("so‘m", CurrencyLabel.current(FormatsLocaleTest.localized("uz")));
        assertEquals("сум", CurrencyLabel.current(FormatsLocaleTest.localized("ru")));
        assertEquals("UZS", CurrencyLabel.current(FormatsLocaleTest.localized("en")));
    }

    @Test
    public void customLabelIsUsedInEveryLanguage() {
        CurrencyLabel.cache(context, "$");

        assertEquals("$", CurrencyLabel.custom(context));
        for (String tag : new String[]{"uz", "ru", "en"}) {
            Context localized = FormatsLocaleTest.localized(tag);
            assertEquals(tag, "50" + NBSP + "000 $", Formats.money(localized, 50_000));
            assertEquals(tag, "−1" + NBSP + "200 $", Formats.signedMoney(localized, "−", -1200));
            assertEquals(tag, "10 – 20 $", Formats.withCurrency(localized, "10 – 20"));
        }
    }

    @Test
    public void clearingTheCustomLabelRestoresTheDefault() {
        CurrencyLabel.cache(context, "€");
        CurrencyLabel.cache(context, null);

        assertEquals("50" + NBSP + "000 сум", Formats.money(FormatsLocaleTest.localized("ru"), 50_000));
    }
}
