package ch.hslu.blebooking.backend.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Inserts test fixtures with plain SQL, so entities need no test-only setters.
 */
public final class TestData {

	private final JdbcTemplate jdbc;

	public TestData(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/** Deletes all rows, children before parents. */
	public void deleteAll() {
		for (String table : new String[] { "receipt", "invoice", "booking", "terminal", "availability", "resource",
				"refresh_token", "one_time_token", "device", "\"user\"" }) {
			jdbc.update("DELETE FROM " + table);
		}
	}

	public long user(String username, String role) {
		return user(username, role, null);
	}

	/** ACTIVE user that can log in with the password behind the given hash. */
	public long user(String username, String role, String passwordHash) {
		return jdbc.queryForObject("""
				INSERT INTO "user" (email, username, password_hash, role, status, first_name, last_name,
				                    failed_attempts)
				VALUES (?, ?, ?, ?, 'ACTIVE', 'Test', 'User', 0) RETURNING id
				""", Long.class, username + "@test.com", username, passwordHash, role);
	}

	public void userStatus(long userId, String status) {
		jdbc.update("UPDATE \"user\" SET status = ? WHERE id = ?", status, userId);
	}

	public void resourceStatus(long resourceId, String status) {
		jdbc.update("UPDATE resource SET status = ? WHERE id = ?", status, resourceId);
	}

	public int bookingCount() {
		return jdbc.queryForObject("SELECT COUNT(*) FROM booking", Integer.class);
	}

	public String bookingStatus(long bookingId) {
		return jdbc.queryForObject("SELECT status FROM booking WHERE id = ?", String.class, bookingId);
	}

	public long resource(String name, String description, String type, String price, String status,
			String position) {
		return jdbc.queryForObject("""
				INSERT INTO resource (name, description, price_per_hour, status, type, position)
				VALUES (?, ?, ?, ?, ?, ?) RETURNING id
				""", Long.class, name, description, new BigDecimal(price), status, type, position);
	}

	public void window(long resourceId, int dayOfWeek, String start, String end, String role) {
		jdbc.update("""
				INSERT INTO availability (resource_id, day_of_week, start_time, end_time, permitted_role)
				VALUES (?, ?, ?, ?, ?)
				""", resourceId, dayOfWeek, LocalTime.parse(start), LocalTime.parse(end), role);
	}

	public long booking(long userId, long resourceId, Instant start, Instant end, String status) {
		return jdbc.queryForObject("""
				INSERT INTO booking (user_id, resource_id, start_time, end_time, status, locked_price)
				VALUES (?, ?, ?, ?, ?, 0) RETURNING id
				""", Long.class, userId, resourceId, utc(start), utc(end), status);
	}

	private static OffsetDateTime utc(Instant instant) {
		return instant.atOffset(ZoneOffset.UTC);
	}

}
