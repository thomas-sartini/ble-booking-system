-- =========================================================================
-- T-054: event publication registry of Spring Modulith (version 2.1.1)
-- =========================================================================
-- Schema copied unchanged from spring-modulith-events-jdbc 2.1.1,
-- org/springframework/modulith/events/jdbc/schemas/v2/schema-postgresql.sql.
-- It matches the JPA entity DefaultJpaEventPublication of spring-modulith-events-jpa.
-- Rows with completion_date IS NULL are deliveries that did not succeed (yet), see backend/README.md.

CREATE TABLE IF NOT EXISTS event_publication
(
  id                     UUID NOT NULL,
  listener_id            TEXT NOT NULL,
  event_type             TEXT NOT NULL,
  serialized_event       TEXT NOT NULL,
  publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
  completion_date        TIMESTAMP WITH TIME ZONE,
  status                 TEXT,
  completion_attempts    INT,
  last_resubmission_date TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS event_publication_serialized_event_hash_idx ON event_publication USING hash(serialized_event);
CREATE INDEX IF NOT EXISTS event_publication_by_completion_date_idx ON event_publication (completion_date);
