package uz.jtscorp.xarajatlar.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Creates database date ranges with an inclusive start and exclusive end. */
public final class DateRanges {

    private DateRanges() {
    }

    public static Range forDays(LocalDate firstDay, LocalDate lastDay, ZoneId zone) {
        return new Range(startOfDay(firstDay, zone), startOfDay(lastDay.plusDays(1), zone));
    }

    public static long startOfDay(LocalDate day, ZoneId zone) {
        return day.atStartOfDay(zone).toInstant().toEpochMilli();
    }

    /** MaterialDatePicker values are UTC dates, rather than instants in the device time zone. */
    public static LocalDate localDateFromUtcPicker(long selection) {
        return Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate();
    }

    public static LocalDate localDateFromInstant(long instant, ZoneId zone) {
        return Instant.ofEpochMilli(instant).atZone(zone).toLocalDate();
    }

    public static long utcPickerSelection(LocalDate day) {
        return day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    public static final class Range {
        private final long start;
        private final long endExclusive;

        private Range(long start, long endExclusive) {
            this.start = start;
            this.endExclusive = endExclusive;
        }

        public long start() {
            return start;
        }

        public long endExclusive() {
            return endExclusive;
        }
    }
}
