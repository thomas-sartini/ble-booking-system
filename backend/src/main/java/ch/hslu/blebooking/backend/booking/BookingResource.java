package ch.hslu.blebooking.backend.booking;

import ch.hslu.blebooking.backend.resource.ResourceType;

/**
 * The resource of a booking, as shown in the booking (contract §3).
 */
public record BookingResource(long id, String name, ResourceType type, String position) {
}
