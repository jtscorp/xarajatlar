package uz.jtscorp.xarajatlar.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class CurrencyLabelTest {
    @Test
    public void normalizeTrimsAValidLabel() {
        assertEquals("$", CurrencyLabel.normalize(" $ "));
        assertEquals("USD", CurrencyLabel.normalize("USD"));
        assertEquals("12345678", CurrencyLabel.normalize("12345678"));
    }

    @Test
    public void normalizeRejectsBlankInput() {
        assertNull(CurrencyLabel.normalize(null));
        assertNull(CurrencyLabel.normalize(""));
        assertNull(CurrencyLabel.normalize("   "));
    }

    @Test
    public void normalizeRejectsLabelsLongerThanEightCharacters() {
        assertNull(CurrencyLabel.normalize("123456789"));
    }
}
