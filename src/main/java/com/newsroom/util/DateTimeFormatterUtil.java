package com.newsroom.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Utility for formatting timestamps into readable editorial dates and relative times.
 */
public final class DateTimeFormatterUtil {
    private static final DateTimeFormatter FULL_HEADER_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH);

    private static final DateTimeFormatter CARD_DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    private static final DateTimeFormatter DETAIL_DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a", Locale.ENGLISH);

    private static final DateTimeFormatter SHORT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private DateTimeFormatterUtil() {
        // Private constructor prevents instantiation
    }

    public static String formatHeaderDate() {
        return LocalDate.now().format(FULL_HEADER_FORMAT);
    }

    public static String formatCardDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(CARD_DATE_FORMAT);
    }

    public static String formatDetailDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DETAIL_DATE_FORMAT);
    }

    public static String formatRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(dateTime, now);

        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return "Just now";
        }
        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + "m ago";
        }
        long hours = duration.toHours();
        if (hours < 24) {
            return hours + "h ago";
        }
        long days = duration.toDays();
        if (days == 1) {
            return "Yesterday";
        }
        if (days < 7) {
            return days + "d ago";
        }
        return dateTime.format(CARD_DATE_FORMAT);
    }
}
