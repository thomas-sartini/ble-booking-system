# Database Setup & Local Execution

## Local Execution & Clean State Verification

Developers can initialize, reset, and verify the database lifecycle cleanly using Docker.

1.  **Clean Reset (Wipe existing data and containers):**
    ```bash
    docker compose down -v
    ```
    (The `-v` flag ensures that the persistent `postgres_data` volume is completely removed, establishing a true clean state).

2.  **Start and Initialize:**
    ```bash
    docker compose up -d
    ```
    (PostgreSQL starts up with an empty database. The schema is created by Flyway when the backend starts, from `backend/src/main/resources/db/migration`).

3.  **Verification:**
    After the backend has started once, developers can connect to the container via `psql` to verify the schema:
    ```bash
    docker exec -it ble_booking_db_container psql -U postgres -d ble_booking_db
    ```
    Running `\dt` inside the psql shell confirms that the initial tables have been successfully created.
