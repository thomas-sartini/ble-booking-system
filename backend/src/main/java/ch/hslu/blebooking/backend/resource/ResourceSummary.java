package ch.hslu.blebooking.backend.resource;

/**
 * Resource as shown in the catalog. The price is a string with 2 decimals, e.g. "12.50".
 */
public record ResourceSummary(long id, String name, String description, ResourceType type, String position,
		String pricePerHour) {
}
