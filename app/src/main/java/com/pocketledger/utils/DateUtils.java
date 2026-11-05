package com.pocketledger.utils;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

public final class DateUtils {

    public static final ZoneId ZONE = ZoneId.of("Asia/Dhaka");
    public static final Locale LOCALE = Locale.forLanguageTag("en-BD");

    private static final DateTimeFormatter ISO_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC"));
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy", LOCALE);
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a", LOCALE);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", LOCALE);
    private static final DateTimeFormatter DAY_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", LOCALE);
    private static final DateTimeFormatter MONTH_FORMAT =
            DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE);
    private static final DateTimeFormatter SHORT_MONTH_FORMAT =
            DateTimeFormatter.ofPattern("MMM", LOCALE);

    private DateUtils() {
    }

    public static String nowIso() {
        return ISO_UTC.format(Instant.now());
    }

    public static String toIso(Instant instant) {
        return ISO_UTC.format(instant);
    }

    public static Instant parseInstant(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Instant.EPOCH;
        }
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            try {
                return ZonedDateTime.parse(value).toInstant();
            } catch (Exception ignoredAgain) {
                return Instant.EPOCH;
            }
        }
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static String todayDate() {
        return today().toString();
    }

    public static String localDate(String iso) {
        return parseInstant(iso).atZone(ZONE).toLocalDate().toString();
    }

    public static LocalDate localDateOf(String iso) {
        return parseInstant(iso).atZone(ZONE).toLocalDate();
    }

    public static String startOfDayIso(LocalDate date) {
        return toIso(date.atStartOfDay(ZONE).toInstant());
    }

    public static String endOfDayIso(LocalDate date) {
        return toIso(date.atTime(LocalTime.MAX).atZone(ZONE).toInstant());
    }

    public static String atLocalDateTime(LocalDate date, int hour, int minute) {
        return toIso(date.atTime(hour, minute).atZone(ZONE).toInstant());
    }

    public static String dateTimeForDisplay(String iso) {
        return DATE_TIME_FORMAT.format(parseInstant(iso).atZone(ZONE));
    }

    public static String dateForDisplay(LocalDate date) {
        return DATE_FORMAT.format(date);
    }

    public static String dateForDisplay(String iso) {
        return dateForDisplay(localDateOf(iso));
    }

    public static String timeForDisplay(String iso) {
        return TIME_FORMAT.format(parseInstant(iso).atZone(ZONE));
    }

    public static String dayHeader(LocalDate date) {
        LocalDate today = today();
        if (date.equals(today)) {
            return "Today · " + DATE_FORMAT.format(date);
        }
        if (date.equals(today.minusDays(1))) {
            return "Yesterday · " + DATE_FORMAT.format(date);
        }
        return DAY_FORMAT.format(date);
    }

    public static String monthLabel(YearMonth month) {
        return MONTH_FORMAT.format(month);
    }

    public static String shortMonth(YearMonth month) {
        return SHORT_MONTH_FORMAT.format(month);
    }

    public static String greeting() {
        int hour = LocalTime.now(ZONE).getHour();
        if (hour < 5) {
            return "Good night";
        }
        if (hour < 12) {
            return "Good morning";
        }
        if (hour < 17) {
            return "Good afternoon";
        }
        if (hour < 21) {
            return "Good evening";
        }
        return "Good night";
    }

    public static String weekdayShort(LocalDate date) {
        return date.getDayOfWeek().getDisplayName(TextStyle.SHORT, LOCALE);
    }

    public static LocalDate startOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static LocalDate endOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    public static LocalDate startOfMonth(LocalDate date) {
        return date.withDayOfMonth(1);
    }

    public static LocalDate endOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfMonth());
    }

    public static LocalDate startOfYear(LocalDate date) {
        return date.withDayOfYear(1);
    }

    public static LocalDate endOfYear(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfYear());
    }

    public static long daysInclusive(LocalDate start, LocalDate end) {
        return Math.max(1, end.toEpochDay() - start.toEpochDay() + 1);
    }

    public static int daysInMonth(YearMonth month) {
        return month.lengthOfMonth();
    }

    public static String nextMealGenerationDelayLabel() {
        LocalDateTime next = LocalDateTime.now(ZONE).toLocalDate().plusDays(1).atTime(0, 5);
        return DATE_TIME_FORMAT.format(next.atZone(ZONE));
    }
}
