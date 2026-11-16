package ch.hslu.blebooking.backend.common;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Reads the Spring Modulith event publication registry (table event_publication) in tests,
 * to wait for the asynchronous listeners.
 */
public final class EventPublications {

	private static final Duration TIMEOUT = Duration.ofSeconds(15);

	private final JdbcTemplate jdbc;

	public EventPublications(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public int count() {
		return jdbc.queryForObject("SELECT COUNT(*) FROM event_publication", Integer.class);
	}

	public int incomplete() {
		return jdbc.queryForObject("SELECT COUNT(*) FROM event_publication WHERE completion_date IS NULL",
				Integer.class);
	}

	public List<Map<String, Object>> all() {
		return jdbc.queryForList("SELECT * FROM event_publication ORDER BY publication_date");
	}

	/** Waits until no listener is running any more: every publication is completed or has failed. */
	public void awaitSettled() {
		await().atMost(TIMEOUT).until(() -> jdbc.queryForObject("""
				SELECT COUNT(*) FROM event_publication
				WHERE completion_date IS NULL AND (status IS NULL OR status <> 'FAILED')
				""", Integer.class) == 0);
	}

	/** Waits until all publications are completed. */
	public void awaitAllCompleted() {
		await().atMost(TIMEOUT).until(() -> incomplete() == 0);
	}

}
