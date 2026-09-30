package com.sonrisa.alerting.app.resilience;

import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Parses the HTTP {@code Retry-After} header (RFC 9110, Section 10.2.3): either a number of seconds or an
 * HTTP date. A date in the past means "no wait".
 */
final class RetryAfter {

    private RetryAfter() {
    }

    static Optional<Duration> parse(String value, Clock clock) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        if (trimmed.chars().allMatch(Character::isDigit)) {
            try {
                return Optional.of(Duration.ofSeconds(Long.parseLong(trimmed)));
            } catch (NumberFormatException tooLarge) {
                return Optional.empty();
            }
        }
        try {
            ZonedDateTime date = ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME);
            Duration wait = Duration.between(clock.instant(), date.toInstant());
            return Optional.of(wait.isNegative() ? Duration.ZERO : wait);
        } catch (DateTimeParseException invalid) {
            return Optional.empty();
        }
    }
}
