package uz.jtscorp.xarajatlar.utils;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import uz.jtscorp.xarajatlar.R;

@RunWith(AndroidJUnit4.class)
public class PluralsTest {
    private static String records(String tag, int count) {
        Context context = FormatsLocaleTest.localized(tag);
        return context.getResources().getQuantityString(R.plurals.record_count, count, count);
    }

    @Test
    public void russianRecordCountUsesTheRightForm() {
        assertEquals("1 запись", records("ru", 1));
        assertEquals("2 записи", records("ru", 2));
        assertEquals("5 записей", records("ru", 5));
        assertEquals("11 записей", records("ru", 11));
        assertEquals("21 запись", records("ru", 21));
        assertEquals("0 записей", records("ru", 0));
    }

    @Test
    public void englishAndUzbekRecordCount() {
        assertEquals("1 record", records("en", 1));
        assertEquals("2 records", records("en", 2));
        assertEquals("1 ta yozuv", records("uz", 1));
        assertEquals("5 ta yozuv", records("uz", 5));
    }

    @Test
    public void moneyUsesTheCurrencyLabelOfTheLanguage() {
        char nbsp = ' ';
        assertEquals("50" + nbsp + "000 so‘m", Formats.money(FormatsLocaleTest.localized("uz"), 50_000));
        assertEquals("50" + nbsp + "000 сум", Formats.money(FormatsLocaleTest.localized("ru"), 50_000));
        assertEquals("50" + nbsp + "000 UZS", Formats.money(FormatsLocaleTest.localized("en"), 50_000));
    }
}
