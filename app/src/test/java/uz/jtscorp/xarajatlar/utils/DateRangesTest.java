package uz.jtscorp.xarajatlar.utils;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.Test;

public class DateRangesTest {

    @Test
    public void monthRange_excludesFirstMomentOfFollowingMonth() {
        ZoneId zone = ZoneId.of("Asia/Tashkent");
        DateRanges.Range september = DateRanges.forDays(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), zone);

        assertEquals(LocalDate.of(2026, 9, 1).atStartOfDay(zone).toInstant().toEpochMilli(),
                september.start());
        assertEquals(LocalDate.of(2026, 10, 1).atStartOfDay(zone).toInstant().toEpochMilli(),
                september.endExclusive());
    }

    @Test
    public void utcPickerSelection_isInterpretedAsTheSameLocalCalendarDay() {
        ZoneId zone = ZoneId.of("Asia/Tashkent");
        long utcSelection = LocalDate.of(2026, 9, 1)
                .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli();

        assertEquals(LocalDate.of(2026, 9, 1), DateRanges.localDateFromUtcPicker(utcSelection));
        assertEquals(LocalDate.of(2026, 9, 1).atStartOfDay(zone).toInstant().toEpochMilli(),
                DateRanges.startOfDay(DateRanges.localDateFromUtcPicker(utcSelection), zone));
    }
}
