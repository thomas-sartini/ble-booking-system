package ch.hslu.blebooking.backend.common.config;

import java.time.Duration;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Booking rules from the API contract (docs/api/booking-api.md, §1).
 *
 * @param zone         time zone of the bookable windows (D1)
 * @param slotMinutes  slot size in minutes (D6)
 * @param maxDuration  longest allowed booking (D7)
 * @param horizon      how far ahead a booking may start (D8)
 * @param maxQueryDays most dates per availability request, both ends inclusive (D9)
 */
@ConfigurationProperties("app.booking")
public record BookingProperties(ZoneId zone, int slotMinutes, Duration maxDuration, Duration horizon,
		int maxQueryDays) {
}
