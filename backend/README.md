# Backend

REST API of the BLE Booking System (Spring Boot, Spring Modulith, PostgreSQL). Commands and conventions are in the
root `CLAUDE.md`; the API contract for bookings is `docs/api/booking-api.md`.

## Booking emails

After a booking is created or cancelled, the owner gets a plain-text email (FR-07.01, contract §6):

- `booking` publishes `BookingConfirmed(bookingId)` / `BookingCancelled(bookingId)` inside the booking transaction.
- `notification` listens with `@ApplicationModuleListener`: asynchronously, **after** the commit, in its own
  transaction. It loads the booking and the owner through the public APIs and sends the email over SMTP.
- Rejected bookings and the repeated cancellation of an already cancelled booking publish no event, so they send
  nothing. An email failure never undoes a booking or a cancellation.

### Configuration

| Variable | Default | Meaning |
|---|---|---|
| `APP_MAIL_ENABLED` | `false` | Send emails at all. If `false`, the listener logs `mail disabled, skipped booking <id>` and completes. |
| `APP_MAIL_FROM` | `no-reply@ble-booking.local` | Sender address |
| `SPRING_MAIL_HOST` | – | SMTP host. If it is empty while mails are enabled, every delivery fails and is retried later (see below). |
| `SPRING_MAIL_PORT` | `25` | SMTP port |
| `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | – | SMTP login, only for a real provider |

Locally, `docker compose up -d postgres mailpit` starts [Mailpit](https://mailpit.axllent.org/): SMTP on
`localhost:1025`, web UI on http://localhost:8025. With the values from `.env.example`
(`APP_MAIL_ENABLED=true`, `SPRING_MAIL_HOST=localhost`, `SPRING_MAIL_PORT=1025`) every booking email shows up there.

On Railway no SMTP provider is chosen yet, so `APP_MAIL_ENABLED` stays `false` there (open point in contract §9).

The SMTP server is not part of `/actuator/health` (`management.health.mail.enabled=false`): an SMTP outage must
not make the API look unhealthy, because failed emails are retried (see below).

### Failed deliveries and retry (FR-07.02)

The Spring Modulith **event publication registry** records every listener call in the table `event_publication`
(migration `V5`), in the same transaction as the booking:

1. The row is written with `completion_date = NULL` when the booking commits.
2. If the email is sent (or skipped because mail is disabled), the row is marked completed (`completion_date` set,
   `status = 'COMPLETED'`).
3. If sending fails (SMTP down, wrong credentials, no host), the listener logs
   `Could not send <CONFIRMED|CANCELLED> mail for booking <id> (<error type>), will be retried on restart` and the
   row stays incomplete (`status = 'FAILED'`). The log never contains the address or the email text.
4. On the next start of the application, all incomplete rows are re-submitted
   (`spring.modulith.events.republish-outstanding-events-on-restart=true`), so the email is sent once the SMTP
   server works again.

Show the deliveries that have not succeeded (yet):

```sql
SELECT id, event_type, serialized_event, status, completion_attempts, publication_date, last_resubmission_date
FROM event_publication
WHERE completion_date IS NULL
ORDER BY publication_date;
```

`serialized_event` contains only the booking id, e.g. `{"bookingId":42}`. To retry, fix the mail configuration and
restart the backend. Completed rows can be removed at any time:
`DELETE FROM event_publication WHERE completion_date IS NOT NULL;`

Before a confirmation is (re-)sent, the listener checks that the booking is still `BOOKED`, so a confirmation
retried after a restart never arrives after the booking was cancelled.

### Delivery guarantee: at-least-once

Every event creates exactly one publication and one email in normal operation; the repeated cancellation creates
none. A **duplicate** email is possible in one case only: the application crashes after the SMTP server accepted the
email and before the publication was marked completed. On restart the publication is still incomplete and the email
is sent again.

This is accepted. A table of sent emails would not close the gap completely, because the crash can also happen
between sending and recording, so no extra migration was added for it.
