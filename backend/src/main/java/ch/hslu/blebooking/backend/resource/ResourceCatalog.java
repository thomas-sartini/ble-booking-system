package ch.hslu.blebooking.backend.resource;

import java.util.List;
import java.util.Optional;

/**
 * Public API of the resource catalog.
 * <p>
 * The role is the JWT "role" claim as a string ({@code STUDENT}, {@code STAFF}, {@code ADMIN}), because this
 * module does not depend on the user module. A window is eligible if its permitted role equals the role;
 * {@code ADMIN} is eligible for every window (D3). A resource is visible if it is ACTIVE and has at least one
 * eligible window (D4).
 */
public interface ResourceCatalog {

	/**
	 * Visible resources, sorted by name.
	 * @param role the user's role
	 * @param type optional type filter, {@code null} for all
	 * @param q optional case-insensitive search in name, description and position, {@code null} or blank for all
	 */
	List<ResourceSummary> findVisible(String role, ResourceType type, String q);

	/**
	 * A visible resource with its eligible windows, or empty if it is unknown, inactive or not visible.
	 */
	Optional<ResourceDetails> findVisible(long id, String role);

}
