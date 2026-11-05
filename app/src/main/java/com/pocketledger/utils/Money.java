package com.pocketledger.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Money {

    public static final String SYMBOL = "৳";
    public static final Locale LOCALE = Locale.forLanguageTag("en-BD");
    public static final BigDecimal ZERO = new BigDecimal("0.00");
    public static final int SCALE = 2;

    private Money() {
    }

    public static BigDecimal of(String value) {
        if (value == null) {
            return ZERO;
        }
        String cleaned = value.replace(SYMBOL, "")
                .replace("BDT", "")
                .replace("Tk", "")
                .replace(",", "")
                .replace(" ", "")
                .trim();
        if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals(".")) {
            throw new NumberFormatException("Empty amount");
        }
        return new BigDecimal(cleaned).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(long minor) {
        return BigDecimal.valueOf(minor, SCALE);
    }

    public static long toMinor(BigDecimal value) {
        if (value == null) {
            return 0L;
        }
        return value.setScale(SCALE, RoundingMode.HALF_UP).movePointRight(SCALE).longValueExact();
    }

    public static long parseMinor(String value) {
        return toMinor(of(value));
    }

    public static boolean isValidPositive(String value) {
        try {
            return of(value).compareTo(BigDecimal.ZERO) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static String format(long minor) {
        return SYMBOL + plain(minor);
    }

    public static String format(BigDecimal value) {
        return SYMBOL + plain(toMinor(value));
    }

    public static String plain(long minor) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(LOCALE));
        format.setGroupingUsed(true);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(of(minor));
    }

    public static String decimal(long minor) {
        return of(minor).setScale(SCALE, RoundingMode.HALF_UP).toPlainString();
    }

    public static String signed(long minor, boolean positive) {
        return (positive ? "+" : "-") + SYMBOL + plain(Math.abs(minor));
    }

    public static String compact(long minor) {
        BigDecimal value = of(minor);
        BigDecimal abs = value.abs();
        if (abs.compareTo(new BigDecimal("1000000")) >= 0) {
            return SYMBOL + value.divide(new BigDecimal("1000000"), 1, RoundingMode.HALF_UP) + "M";
        }
        if (abs.compareTo(new BigDecimal("1000")) >= 0) {
            return SYMBOL + value.divide(new BigDecimal("1000"), 1, RoundingMode.HALF_UP) + "k";
        }
        return SYMBOL + value.setScale(0, RoundingMode.HALF_UP);
    }

    public static long sum(long a, long b) {
        return Math.addExact(a, b);
    }
}
