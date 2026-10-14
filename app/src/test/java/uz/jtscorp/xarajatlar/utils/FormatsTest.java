package uz.jtscorp.xarajatlar.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDate;

public class FormatsTest {
    private static final char NBSP = ' ';

    @Test
    public void numberGroupsThousandsWithNonBreakingSpace() {
        assertEquals("0", Formats.number(0));
        assertEquals("999", Formats.number(999));
        assertEquals("1" + NBSP + "234" + NBSP + "567", Formats.number(1_234_567));
        assertEquals("-50" + NBSP + "000", Formats.number(-50_000));
    }

    @Test
    public void numberUsesCommaForFractionAndDropsTrailingZeros() {
        assertEquals("1" + NBSP + "234,5", Formats.number(1234.5));
        assertEquals("12", Formats.number(12.0));
        assertEquals("0,125", Formats.number(0.125));
    }

    @Test
    public void numericDateIsLocaleIndependent() {
        assertEquals("09.10.2026", Formats.date(LocalDate.of(2026, 10, 9)));
    }
}
