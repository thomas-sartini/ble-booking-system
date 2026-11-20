package ch.hslu.blebooking.backend.resource;

import java.util.List;

/**
 * Resource with its time zone and the weekly windows the current user is eligible for,
 * sorted by day and start.
 */
public record ResourceDetails(long id, String name, String description, ResourceType type, String position,
		String pricePerHour, String timeZone, List<WeeklyWindow> windows) {
}
