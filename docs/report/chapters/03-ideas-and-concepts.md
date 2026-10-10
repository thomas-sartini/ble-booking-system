# Ideas and Concepts

- *Present the proposed solution, component designs, and main design choices.*

## Use Cases

### Web Application

- *Include a Use Case Diagram for resource search, booking management, invoice viewing, and administration through the web application.*
- *Show which actions are available to Student, Lecturer, and Admin.*

### Mobile Application

- *Include a Use Case Diagram for login, viewing the current booking or access entitlement, BLE identification, and check-in/check-out.*
- *Show the relevant user roles and distinguish required mobile functions from optional functions.*

## General System Flow

- *Show the overall process from login and booking to BLE check-in, resource use, check-out, and billing.*
- *Include permitted access without a prior booking and indicate where offline operation affects the process.*

## Proposed System Architecture

- *Include a conceptual component diagram with Web, Backend, Database, Shared Mobile Core, Android, iOS, and Door Terminal controlling the door.*
- *Briefly explain each component's responsibility, the proposed module boundaries, and the connections through REST/HTTPS, shared mobile interfaces, and BLE.*

## Backend Design

The backend is a Spring Boot application with a PostgreSQL database. It is divided into eight modules. The web frontend and the mobile app communicate with it through a REST API over HTTPS. The door terminal has no connection to the backend; all data between terminal and backend is carried by the smartphone.

### Module Structure

**Auth.** Handles the identity of a user: account activation, login, access and refresh tokens, and password reset. Users cannot register themselves. An admin creates the user, and the user receives an activation link by email to set a password. Activation and password reset use the same mechanism: a random one-time token with an expiry date, stored only as a hash. *Depends on:* Users.

**Users.** Manages users, their profile data, role (STUDENT, STAFF, ADMIN) and status (INVITED, ACTIVE, DISABLED). Only admins can create users and change roles. The first admin is created when the database is set up. *Depends on:* nothing.

**Resources.** Manages the university's resources (name, type, position, price per hour) and the terminals with their public keys. Each terminal belongs to one resource; a resource can have more than one terminal. *Depends on:* nothing.

**Bookings.** Creates, changes and cancels bookings, prevents overlapping bookings, calculates free time slots and handles check-in and check-out. A scheduled job detects missed check-ins and missing check-outs. When a booking ends, the module publishes an event so that an invoice can be created. *Depends on:* Users, Resources.

**Devices.** Manages the users' smartphones and stores the public key of each device, so that tickets can be bound to one specific device. A lost device can be blocked. *Depends on:* Users.

**Tickets.** Issues the signed tickets (JWT) that the smartphone presents at the terminal. Tickets are signed with a separate asymmetric key (ES256), so the terminal can verify them with the public key only. A ticket is issued shortly before the booking starts and is valid for a few minutes. *Depends on:* Bookings, Devices.

**Invoices.** Creates invoices automatically when a booking ends or when a user does not show up. Invoices are Swiss QR invoices and can be downloaded as PDF. The module also manages the payment status. *Depends on:* Bookings, Users, Resources.

**Audit Logs.** Stores security-relevant events of the whole application. The other modules publish events (Spring Events) to which this module listens. Only admins can read the logs, and nobody can change or delete them. *Depends on:* nothing.

| Module | Depends on |
|---|---|
| Auth | Users |
| Users | – |
| Resources | – |
| Bookings | Users, Resources |
| Devices | Users |
| Tickets | Bookings, Devices |
| Invoices | Bookings, Users, Resources |
| Audit Logs | – (receives events from all modules) |

: Backend module dependencies

Bookings does not call Invoices directly. It publishes an event when a booking ends, and Invoices reacts to it. This avoids a circular dependency between the two modules.

![Backend architecture. Solid arrows between modules mean "depends on"; dotted arrows are events or BLE communication.](assets/backend-architecture.png){width=100%}

![Booking status transitions](assets/booking-status.png){width=60%}

### API Structure

**Auth**

| Method | Path | Body / Note |
|---|---------|--------|
| POST | `/auth/activate` | activation token (from the email link), new password |
| POST | `/auth/login` | email, password; returns access token (JWT) and refresh token |
| POST | `/auth/refresh` | refresh token; returns a new access token and a new refresh token |
| POST | `/auth/logout` | refresh token; the refresh token is no longer valid |
| POST | `/auth/password-reset/request` | email; the response is always the same, whether the email exists or not |
| POST | `/auth/password-reset/confirm` | reset code, new password |

**Users**

| Method | Path | Body / Note |
|---|---------|--------|
| POST | `/users` | email, firstName, lastName, role, startSemester and bachelorCourse (students only); sends the activation email |
| GET | `/users` | list of users; query: role, status; with pagination |
| POST | `/users/{id}/invitation` | sends the activation email again |
| GET | `/users/{id}` | |
| PATCH | `/users/{id}` | only the fields that change; the role cannot be changed here |
| PATCH | `/users/{id}/role` | changes the role (written to the audit log) |
| DELETE | `/users/{id}` | sets the status to DISABLED; the user is not deleted |

**Resources and Terminals**

| Method | Path | Body / Note |
|---|---------|--------|
| POST | `/resources` | name, type (MOBILE or FIXED), position, pricePerHour |
| GET | `/resources` | list of resources; query: type |
| GET | `/resources/{id}` | |
| PATCH | `/resources/{id}` | only the fields that change |
| DELETE | `/resources/{id}` | |
| GET | `/resources/{id}/availability` | free time slots; query: from, to; the logic is in the Bookings module |
| POST | `/terminals` | name, resourceId, publicKey (P-256, DER, Base64); the response contains the terminalId and the backend public key |
| GET | `/terminals` | list of terminals |
| GET | `/terminals/{id}` | |
| DELETE | `/terminals/{id}` | removes a terminal, e.g. if it is stolen |

**Bookings and Tickets**

| Method | Path | Body / Note |
|---|---------|--------|
| POST | `/bookings` | resourceId, start, end; the user is taken from the access token |
| GET | `/bookings` | list of bookings; query: from, to, resourceId, userId; users only receive their own |
| GET | `/bookings/{id}` | |
| PATCH | `/bookings/{id}` | only while the status is BOOKED |
| DELETE | `/bookings/{id}` | cancels the booking (status CANCELLED) |
| POST | `/bookings/{id}/check-in` | check-in receipt signed by the terminal |
| POST | `/bookings/{id}/check-out` | check-out receipt signed by the terminal |
| GET | `/bookings/{id}/ticket` | query: deviceId; ticket for an active booking, available shortly before the start |

**Devices**

| Method | Path | Body / Note |
|---|---------|--------|
| POST | `/users/me/devices` | publicKey (P-256, DER, Base64), device name; requires a recent login |
| GET | `/users/me/devices` | the user's own devices |
| DELETE | `/users/me/devices/{id}` | blocks a device, e.g. if it is lost |
| GET | `/users/{id}/devices` | devices of any user |
| DELETE | `/users/{id}/devices/{deviceId}` | blocks a device of any user |

**Invoices and Audit Logs**

| Method | Path | Body / Note |
|---|---------|--------|
| GET | `/invoices` | list of invoices; query: userId, paymentStatus; users only receive their own |
| GET | `/invoices/{id}/pdf` | invoice as PDF (Swiss QR invoice) |
| PATCH | `/invoices/{id}` | changes the payment status |
| POST | `/invoices` | creates an invoice manually for special cases |
| GET | `/admin/audit-logs` | audit log entries; query: from, to, userId; with pagination |

### Authentication and Authorization

Authentication:

- An access token is required for all endpoints except `/auth/*`.
- Access token: JWT, valid for 15 minutes, contains the userId and the role.
- Refresh token: valid for 7 days, stored as a hash, replaced on every use.
- The refresh token becomes invalid after logout, after a password change and when the user is disabled.
- Login attempts are limited, and disabled users cannot log in.

Authorization is checked in two steps:

1. Role check with Spring Security (`@PreAuthorize`).
2. Owner check in the service of the module: users may only access objects that belong to them.

Table: Access rights by role. *Yes* = allowed, *Own* = only objects that belong to the user, *No* = not allowed.

| Endpoint | Not logged in | STUDENT | STAFF | ADMIN |
|---|---|---|---|---|
| All `/auth/*` endpoints | Yes | Yes | Yes | Yes |
| `POST /users`, `GET /users` | No | No | No | Yes |
| `POST /users/{id}/invitation` | No | No | No | Yes |
| `GET /users/{id}`, `PATCH /users/{id}` | No | Own | Own | Yes |
| `PATCH /users/{id}/role`, `DELETE /users/{id}` | No | No | No | Yes |
| `GET /resources`, `GET /resources/{id}` | No | Yes | Yes | Yes |
| `GET /resources/{id}/availability` | No | Yes | Yes | Yes |
| `POST`, `PATCH`, `DELETE /resources` | No | No | No | Yes |
| All `/terminals` endpoints | No | No | No | Yes |
| `POST /bookings` | No | Yes | Yes | Yes |
| `GET`, `PATCH`, `DELETE /bookings/{id}` | No | Own | Own | Yes |
| `POST /bookings/{id}/check-in`, `/check-out` | No | Own | Own | Own |
| `GET /bookings/{id}/ticket` | No | Own | Own | Own |
| `/users/me/devices` (all methods) | No | Own | Own | Own |
| `GET`, `DELETE /users/{id}/devices` | No | No | No | Yes |
| `GET /invoices`, `GET /invoices/{id}/pdf` | No | Own | Own | Yes |
| `PATCH /invoices/{id}`, `POST /invoices` | No | No | No | Yes |
| `GET /admin/audit-logs` | No | No | No | Yes |

Check-in, check-out and tickets are restricted to the owner even for admins: only the owner of a booking can check in, with their own device.

### Database Access

Each module owns its tables:

| Module | Tables |
|---|---|
| Auth | refresh_tokens, one_time_tokens |
| Users | users |
| Resources | resources, terminals |
| Bookings | bookings, receipts |
| Devices | devices |
| Tickets | – (tickets are not stored) |
| Invoices | invoices |
| Audit Logs | audit_log |

: Table ownership

### Error Handling

All errors are returned as Problem Details (RFC 9457) and handled centrally with `@RestControllerAdvice`. An additional field `code` allows the clients to show the right message.

```json
{
  "type": "about:blank",
  "title": "Booking overlap",
  "status": 409,
  "detail": "The resource is already booked at this time.",
  "instance": "/bookings",
  "code": "BOOKING_OVERLAP"
}
```

| Status | Code (examples) | When |
|---|---|---|
| 400 | VALIDATION_ERROR, INVALID_RECEIPT | Invalid input, invalid signature |
| 401 | UNAUTHORIZED | Token missing, invalid or expired |
| 403 | FORBIDDEN | Role not allowed |
| 404 | NOT_FOUND | Object does not exist or does not belong to the user |
| 409 | BOOKING_OVERLAP, INVALID_STATUS | Conflict with existing data or booking status |
| 429 | TOO_MANY_REQUESTS | Too many login attempts |
| 500 | INTERNAL_ERROR | Unexpected error |

: HTTP status codes

- Responses contain no stack traces, SQL errors or other internal details; these are written only to the server log.
- Objects of other users return 404, not 403, so the API does not reveal whether they exist.
- A failed login always returns the same message.
- Security-relevant errors are written to the audit log.

The check-in and check-out process with tickets, receipts and the offline terminal is described in [BLE Access and Security Concept](#ble-access-and-security-concept).

### Backend Design Decisions

1. **Technology:** Java with Spring Boot and PostgreSQL. Spring Security provides password hashing (Argon2), roles and JWT.
2. **Availability:** Free time slots are calculated in the Bookings module, where the bookings are stored. This avoids a circular dependency with Resources.
3. **Overlapping bookings:** A PostgreSQL exclusion constraint prevents two bookings for the same resource at the same time, even under concurrent requests.
4. **No-show:** If there is no check-in 15 minutes after the start, a scheduled job sets the booking to NO_SHOW, and an invoice with a no-show fee is created.
5. **Missing check-out:** Without a check-out, the booking is charged until its end time.
6. **Invoices:** Invoices are created automatically by the backend, triggered by an event from Bookings.
7. **Users:** Users cannot register themselves. An admin creates them, and they receive an activation link by email.
8. **User types:** One users table with a role (STUDENT, STAFF, ADMIN). The fields startSemester and bachelorCourse are only filled in for students.
9. **Audit logs:** Written through Spring Events. The database user of the application has only INSERT and SELECT rights on the audit table. Passwords and tokens are never logged.
10. **Terminals and resources:** The relation is stored once, as resourceId in the terminals table, so a resource can have more than one terminal.
11. **Tokens:** Short-lived access tokens (15 minutes) and refresh tokens (7 days) that are stored as a hash and replaced on every use.
12. **Two-step access check:** First the role (Spring Security), then the owner of the object (service).
13. **No hard delete of users:** Users are disabled, not deleted, because bookings, invoices and audit logs still refer to them.

## Mobile Application Design

### Module Structure

- *Show the division between Shared Mobile Core, Android, and iOS, including shared REST/protocol logic and native BLE/storage adapters.*
- *Explain module responsibilities, dependencies, and the interfaces connecting shared logic to native platform features.*

### Class Structure

- *Include proposed Class Diagrams for the shared core and the important Android/iOS classes.*
- *Show how shared models and access logic relate to platform-specific BLE and protected-storage implementations.*

## Door Terminal Design

### Module Structure

- *Show the proposed firmware modules for BLE communication, credential verification, door control, local storage, and event handling.*
- *Explain their responsibilities, dependencies, and interfaces with the mobile app and the selected synchronization route.*

### Class and Data Structure Design

- *Include a proposed diagram of the important firmware classes or data structures, according to the chosen C/C++ design.*
- *Show how protocol messages, authorization data, terminal state, and queued usage events relate to the firmware modules.*

## Database Design

![ER Diagram](docs/report/assets/erd.png)

### 1. Core Entities

#### User
`id`, `email`, `password_hash`, `role` (VARCHAR), `status` (VARCHAR), `created_at` (TIMESTAMPTZ), `first_name`, `last_name`, `start_semester`, `field_of_study`, `failed_attempts`, `locked_until` (TIMESTAMPTZ)

#### Device
`id`, `user_id` (FK), `name`, `public_key`, `created_at` (TIMESTAMPTZ), `revoked_at` (TIMESTAMPTZ)

#### RefreshToken
`id`, `user_id` (FK), `token_hash`, `expires_at` (TIMESTAMPTZ), `revoked_at` (TIMESTAMPTZ)

#### OneTimeToken
`id`, `user_id` (FK), `token_hash`, `purpose` (VARCHAR), `expires_at` (TIMESTAMPTZ), `used_at` (TIMESTAMPTZ), `created_by_user_id` (FK)

#### Resource
`id`, `name`, `description`, `price_per_hour` (DECIMAL), `status` (VARCHAR), `type` (VARCHAR), `position` (VARCHAR)

#### Terminal
`id`, `public_key`, `status` (VARCHAR), `resource_id` (FK, UNIQUE)

#### Booking
`id`, `user_id` (FK), `resource_id` (FK), `start_time` (TIMESTAMPTZ), `end_time` (TIMESTAMPTZ), `status` (VARCHAR), `locked_price` (DECIMAL)

#### Receipt
`id` (UUID), `booking_id` (FK), `terminal_id` (FK), `ticket_id`, `type` (VARCHAR), `receipt_time` (TIMESTAMPTZ), `signature`, `received_at` (TIMESTAMPTZ)

#### Invoice
`id`, `invoice_number`, `booking_id` (FK, UNIQUE), `user_id` (FK), `final_price` (DECIMAL), `pdf_file_path`, `status` (VARCHAR), `created_at` (TIMESTAMPTZ), `type` (VARCHAR), `qr_reference`, `due_date`, `paid_at` (TIMESTAMPTZ), `currency` (VARCHAR)

#### Availability
`id`, `resource_id` (FK), `day_of_week` (SMALLINT), `start_time`, `end_time`, `permitted_role` (VARCHAR)

#### AuditLog
`id`, `actor_user_id` (FK), `action` (VARCHAR), `target_entity` (VARCHAR), `timestamp` (TIMESTAMPTZ), `details` (JSONB), `target_id`, `outcome` (VARCHAR), `ip_address` (VARCHAR)

### 2. Relationships & Cardinalities

- **User -> Booking ($1 : n$):** A user can place multiple bookings over time.
- **User -> Device ($1 : n$):** A user can register multiple authentication devices.
- **User -> RefreshToken ($1 : n$):** A user can maintain multiple active authentication sessions.
- **User -> OneTimeToken ($1 : n$):** A user can have multiple tokens issued for or by them (`created_by_user_id`).
- **User -> Invoice ($1 : n$):** A user is assigned to all invoices generated for their bookings.
- **User -> AuditLog ($1 : n$):** A user triggers multiple logged system actions (`actor_user_id`).
- **Resource -> Booking ($1 : n$):** A bookable resource can have multiple scheduled bookings.
- **Resource -> Availability ($1 : n$):** A resource defines multiple time-window access rules across days of the week.
- **Terminal -> Resource ($1 : 1$):** Each hardware terminal is assigned to exactly one physical resource (`resource_id` in `terminal`).
- **Terminal -> Receipt ($1 : n$):** A physical terminal generates and signs multiple access receipts.
- **Booking -> Receipt ($1 : n$):** A single booking session tracks check-in and check-out receipts.
- **Booking -> Invoice ($1 : 1$):** Every completed/billable booking correlates to exactly one invoice (`UNIQUE` constraint on `booking_id`).
## Booking and Usage Lifecycle

- *Show the proposed states and transitions for bookings and resource usage.*
- *Summarize the main policies for booking conflicts, walk-in access, no-shows, overtime, and missing check-out.*

## BLE Access and Security Concept

- *Explain the proposed mobile-to-terminal identification and authorization flow, from credential verification to door access and check-in/check-out.*
- *Summarize the security approach: role restrictions, protected credentials and keys, replay protection, and handling of lost or compromised devices.*

## Offline Operation and Synchronization

- *Explain how the terminal should authorize access and retain usage events while offline, within a defined authorization validity window.*
- *Describe the proposed route for later synchronization, including duplicate prevention and how conflicting events should be handled.*

## User Interface Design

### Web Application UI

- *Include wireframes for the main web screens: login, resource search, bookings, invoices, and administration.*
- *Show navigation and the actions available to each role, including important confirmation and error states.*

### Mobile Application UI

- *Include wireframes for the main mobile screens: login, current booking/access entitlement, BLE connection, and check-in/check-out.*
- *Show navigation and feedback for successful or denied access, disconnection, and offline operation.*

## Design Choices

- *Summarize the important choices in architecture, technologies, mobile integration, and BLE communication, with their reasons.*
- *Record unresolved design questions; describe the final implemented structures and any changes in Chapter 5.*
