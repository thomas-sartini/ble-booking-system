-- Initiales Datenbankschema für das BLE Booking System (T-012)

CREATE TABLE IF NOT EXISTS sample_bookings (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Beispiel-Eintrag zur Verifizierung
INSERT INTO sample_bookings (title) VALUES ('Initial Test Booking');