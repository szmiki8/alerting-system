package com.sonrisa.alerting.app.api;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * The one central time mapping of the API (CON-10, Section 9.2). Times are stored and passed around as
 * UTC {@link Instant}s; in JSON responses they are written as ISO-8601 with the offset of the configured
 * zone (default {@code Europe/Budapest}: {@code +01:00} in winter, {@code +02:00} in summer), for example
 * {@code 2026-07-15T12:00:00+02:00}. Response DTOs therefore use {@code Instant} for time values.
 *
 * <p>Spring Boot 4 uses Jackson 3 and registers every {@link JacksonModule} bean with its JSON mapper.
 */
@Configuration(proxyBeanMethods = false)
class ApiJsonConfiguration {

    @Bean
    JacksonModule alertingTimeModule(ZoneId alertingZone) {
        return new SimpleModule("alerting-time").addSerializer(Instant.class, new ZonedInstantSerializer(alertingZone));
    }

    /** Writes an instant as an offset date-time in the given zone. */
    static final class ZonedInstantSerializer extends StdSerializer<Instant> {

        private final DateTimeFormatter formatter;

        ZonedInstantSerializer(ZoneId zone) {
            super(Instant.class);
            this.formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zone);
        }

        @Override
        public void serialize(Instant value, JsonGenerator generator, SerializationContext context) {
            generator.writeString(formatter.format(value));
        }
    }
}
