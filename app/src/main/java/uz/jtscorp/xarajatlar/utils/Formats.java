package uz.jtscorp.xarajatlar.utils;

import android.content.Context;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;

import uz.jtscorp.xarajatlar.R;

/** Son, pul va sana formatlari. Til almashganda eskirmasligi uchun holat saqlamaydi. */
public final class Formats {
    private static final String NUMERIC_DATE = "dd.MM.yyyy";
    private static final String NUMERIC_DATE_TIME = "dd.MM.yyyy, HH:mm";

    private Formats() {}

    public static String number(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.###", symbols).format(value);
    }

    public static String money(Context context, double value) {
        return withCurrency(context, number(value));
    }

    /** Tayyor matnga (masalan, oraliq) valyuta belgisini qo'shadi. */
    public static String withCurrency(Context context, String amountText) {
        return context.getString(R.string.money_amount, amountText, CurrencyLabel.current(context));
    }

    public static String signedMoney(Context context, String sign, double value) {
        return context.getString(R.string.signed_money_amount, sign, number(Math.abs(value)),
                CurrencyLabel.current(context));
    }

    public static Locale locale(Context context) {
        return context.getResources().getConfiguration().getLocales().get(0);
    }

    public static String date(long epochMillis) {
        return new SimpleDateFormat(NUMERIC_DATE, Locale.ROOT).format(new Date(epochMillis));
    }

    public static String date(LocalDate date) {
        return DateTimeFormatter.ofPattern(NUMERIC_DATE, Locale.ROOT).format(date);
    }

    public static String dateTime(long epochMillis) {
        return new SimpleDateFormat(NUMERIC_DATE_TIME, Locale.ROOT).format(new Date(epochMillis));
    }

    public static String monthYear(Context context, LocalDate date) {
        return DateTimeFormatter.ofPattern("LLLL yyyy", locale(context)).format(date);
    }

    public static String dayMonthTime(Context context, TemporalAccessor time) {
        return DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", locale(context)).format(time);
    }
}
