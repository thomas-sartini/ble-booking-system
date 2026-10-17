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

### Purpose and Scope

This section defines the architecture of the mobile application and serves as the architectural baseline for implementation.

The design is based on Kotlin Multiplatform and focuses on the shared Mobile Core, its architectural components, and its boundaries with the native Android and iOS applications and the backend.

The component responsibilities and interaction flows describe the intended behavior of the system. The current implementation establishes its structural foundation; the implemented functionality and any deviations from this design are documented in Chapter 5.

The detailed internal architecture of the native applications and the BLE protocol remain to be defined. Backend implementation is outside the scope of this section.

### Component Architecture

The mobile architecture comprises a shared Mobile Core, native Android and iOS applications, and the backend.

#### Module Structure

The Mobile Core contains shared application logic, backend communication, and local authentication credential management. The native applications provide the user interface, BLE interaction, and platform-specific protected storage.

Native applications invoke shared operations through `MobileCoreApi`. They obtain a core instance through `MobileCoreFactory` and supply their platform-specific implementation of `SecureStorage`.

The following component diagram shows the architectural boundaries and the interfaces connecting these elements.

![Mobile Application Component Architecture](assets/mobile-component-diagram.png){width=72%}


#### Mobile Core

The Mobile Core contains the shared Kotlin Multiplatform logic used by both Android and iOS. It consists of three architectural components.

| Component | Main Responsibilities | Interfaces |
|:-----------------------------|:---------------------------------------------|:-----------------------------|
| **MobileCoreApplication** | Coordinate authentication, booking, and resource-access operations; expose shared application functionality to the native applications. | **Provides:** `MobileCoreApi`; **Requires:** `CommunicationGateway`. |
| **BackendCommunication** | Communicate with the backend through HTTP; convert between application models and DTOs; coordinate login, token refresh, and logout using `TokenManager` to manage credentials. | **Provides:** `CommunicationGateway`; **Requires:** `REST/API`. |
| **TokenManager** | Maintain the access token in memory; store and retrieve the refresh token through secure storage; update and clear locally managed credentials. | **Provides:** none; **Requires:** `SecureStorage`. |

#### External Components

**Backend** provides the REST API required by the mobile application. It authenticates requests, issues and renews credentials, revokes renewable sessions, supplies booking and access-related data, and processes access confirmations and check-out operations.

**AndroidApp** provides the Android user interface and native platform integration. It consumes `MobileCoreApi`, obtains the core through `MobileCoreFactory`, and provides an implementation of `SecureStorage`. BLE discovery, connection management, and terminal interaction remain native responsibilities.

**IOSApp** provides the iOS user interface and native platform integration. It consumes the same shared application contract and provides the iOS implementation of protected storage and BLE interaction.

#### Native Platform Design - TODO

The detailed native UI structure, navigation, BLE integration, protected-storage adapters, and key management will be documented in a later design step.

No native class diagram is included at this stage.

### Detailed Module Design

This section describes the public integration package and the three architectural modules of the Mobile Core.

The API package is documented through individual descriptions. Within the remaining modules, interfaces retain their detailed contracts and classes are summarized through their purpose, responsibilities, and dependencies.

Application models and error representations are described with the application module. DTOs and HTTP communication are described with the communication module.

The existing diagrams retain their original groupings. In the source code, `MobileCoreApi` and `SecureStorage` belong to the separate `api` package.

#### API Package (`api`)

The `api` package defines the public integration boundary between the Mobile Core and the native Android and iOS applications. It contains `MobileCoreApi`, `MobileCoreFactory`, and `SecureStorage`.

These elements are grouped separately because they define how a native application invokes shared operations, obtains a core instance, and supplies protected storage. This separates the integration contract from the internal application services, backend communication, and token management.

`SecureStorage` belongs to this package because native applications must be able to implement the storage contract independently from the internal token manager. Application models and shared error types are used by the public contract but are documented separately from the API entry points below.

##### MobileCoreApi

| Aspect | Description |
|:----------------|:--------------------------------------------------------------------------|
| **Purpose** | Define the public contract through which native Android and iOS applications invoke shared Mobile Core operations. |
| **Responsibilities** | Expose authentication, booking, and resource-access operations; hide the internal application services; provide a stable boundary between native code and shared application logic. |
| **Dependencies** | Application models and `MobileCoreException`. The interface is implemented by `ServiceFacade`. |

###### Interface Operations {.unnumbered}

- `login(identifier: String, password: String)` starts authentication with the supplied credentials. Returned authentication tokens remain internal to the core.

- `logout()` ends the session and coordinates backend revocation and local credential cleanup.

- `getActiveBookings()` returns the current user's active bookings as a list of `Booking` models.

- `requestAccess(request: ResourceAccessRequest)` requests resource-access authorization and returns a `ResourceAccessResponse`.

- `confirmAccess(confirmation: AccessConfirmation)` confirms the outcome of a previously initiated resource-access operation.

- `checkOut(resourceId: String)` initiates check-out for the specified resource.

These operations are asynchronous. Expected application and communication failures are represented by `MobileCoreException`.

The interface uses application models rather than transport DTOs. Backend transport details and authentication credentials remain internal to the core. BLE communication is handled by the native applications.

##### MobileCoreFactory

| Aspect | Description |
|:----------------|:--------------------------------------------------------------------------|
| **Purpose** | Provide the public creation entry point for a Mobile Core instance, receiving a native `SecureStorage` implementation and returning `MobileCoreApi`. |
| **Responsibilities** | Construct and connect the token, communication, and application-service components; create the facade implementing `MobileCoreApi`; encapsulate component construction within the shared core. |
| **Dependencies** | `SecureStorage`, `TokenManager`, `BackendCommunication`, `AuthService`, `BookingService`, `AccessService`, and `ServiceFacade`. |

###### Creation Operation {.unnumbered}

`create(secureStorage: SecureStorage)` returns a `MobileCoreApi` instance backed by `ServiceFacade`.

The native application is responsible for creating its concrete protected-storage adapter. The complete initialization of HTTP communication will be defined during implementation.

##### SecureStorage

| Aspect | Description |
|:----------------|:--------------------------------------------------------------------------|
| **Purpose** | Define a platform-independent contract for protected persistence of sensitive values. |
| **Responsibilities** | Define secure key-value storage operations; abstract platform-specific protected storage; allow Android and iOS applications to supply their respective implementations. |
| **Dependencies** | No concrete platform dependency. The implementation is supplied by the native application. |

###### Interface Operations {.unnumbered}

- `save(key: String, value: String)` stores a value under the specified key using protected storage.

- `get(key: String)` returns the value associated with the key, or no value when the key is absent.

- `remove(key: String)` removes the value associated with the key.

The interface exposes generic key-value operations. Token-specific decisions, including the stored values and their keys, remain the responsibility of `TokenManager`.


#### MobileCoreApplication

The application module coordinates shared authentication, booking, and resource-access operations. `ServiceFacade` implements the contract defined by `MobileCoreApi` and delegates operations to the specialized services.

![Shared Mobile Core Class Structure](assets/mobile-core-classes.png){width=95%}

| Element | Description |
|:----------------------------------------|:------------------------------------------------------------|
| **ServiceFacade** |  **Purpose:** Implement `MobileCoreApi` and provide the entry point to application services. `\par\smallskip`{=latex} **Responsibilities:** Delegate authentication to `AuthService`, booking retrieval to `BookingService`, and access operations to `AccessService`; keep individual services behind the public contract. `\par\smallskip`{=latex} **Dependencies:** `MobileCoreApi`, `AuthService`, `BookingService`, `AccessService`. |
| **AuthService** |  **Purpose:** Coordinate application-level authentication and session operations. `\par\smallskip`{=latex} **Responsibilities:** Handle login and logout through the communication gateway; remain independent from token storage and refresh details. `\par\smallskip`{=latex} **Dependencies:** `CommunicationGateway`. |
| **BookingService** |  **Purpose:** Retrieve booking information for the current user. `\par\smallskip`{=latex} **Responsibilities:** Request active bookings through the gateway and expose the result as application models. `\par\smallskip`{=latex} **Dependencies:** `CommunicationGateway`, `Booking`. |
| **AccessService** |  **Purpose:** Coordinate the application-level resource-access lifecycle. `\par\smallskip`{=latex} **Responsibilities:** Request access, confirm its outcome, and initiate check-out through the gateway. `\par\smallskip`{=latex} **Dependencies:** `CommunicationGateway`, `ResourceAccessRequest`, `ResourceAccessResponse`, `AccessConfirmation`. |

##### Application Models {.unnumbered}

\leavevmode\par

These models form the application-facing data contract shared by the public API, application services, and communication gateway. Their detailed attributes will be refined with the access and backend contracts.

| Element | Description |
|:----------------------------------------|:------------------------------------------------------------|
| **Booking** |  **Purpose:** Represent a booking at application level. `\par\smallskip`{=latex} **Responsibilities:** Provide booking information independently from its REST transport representation. `\par\smallskip`{=latex} **Dependencies:** None are defined in the current design. |
| **ResourceAccessRequest** |  **Purpose:** Represent an application-level request to access a resource. `\par\smallskip`{=latex} **Responsibilities:** Carry the information required by the access flow while keeping application services independent from the transport DTO. `\par\smallskip`{=latex} **Dependencies:** None are defined in the current design. |
| **ResourceAccessResponse** |  **Purpose:** Represent the application-level result of an access request. `\par\smallskip`{=latex} **Responsibilities:** Expose the access outcome while hiding the backend-specific transport representation. `\par\smallskip`{=latex} **Dependencies:** None are defined in the current design. |
| **AccessConfirmation** |  **Purpose:** Represent confirmation of a resource-access operation. `\par\smallskip`{=latex} **Responsibilities:** Carry confirmation information independently from the REST DTO. `\par\smallskip`{=latex} **Dependencies:** None are defined in the current design. |

##### Error Representation {.unnumbered}

\leavevmode\par

The shared error representation gives native applications a consistent description of application and communication failures.

| Element | Description |
|:----------------------------------------|:------------------------------------------------------------|
| **MobileCoreException** |  **Purpose:** Represent an expected application or communication failure. `\par\smallskip`{=latex} **Responsibilities:** Carry an error code, an optional diagnostic message, and an optional underlying cause. `\par\smallskip`{=latex} **Dependencies:** `MobileCoreErrorCode`, `Exception`, and an optional `Throwable` cause. |
| **MobileCoreErrorCode** | **Type:** Enumeration `\par\smallskip`{=latex} **Purpose:** Define the error categories exposed by the core. `\par\smallskip`{=latex} **Responsibilities:** Distinguish backend failures, connection errors, timeouts, and unrecognized failures. `\par\smallskip`{=latex} **Dependencies:** None. |

#### BackendCommunication

The communication module provides the boundary between application operations and the backend REST API. It contains the communication contract, its implementation, the REST client, transport DTOs, and HTTP communication infrastructure.

Application models are converted to transport representations when sending requests. Received transport data is converted back into application models before it reaches the application services.

![Backend Communication Class Structure](assets/mobile-communication-classes.png){width=100%}

##### CommunicationGateway

| Element | Description |
|:----------------------------------------|:------------------------------------------------------------|
| **CommunicationGateway** | **Purpose:** Define the application-facing contract for backend communication, independently from HTTP, endpoint formats, DTOs, and token-management details. `\par\smallskip`{=latex} **Responsibilities:** Expose backend capabilities using application-level types; isolate application services from transport details; hide authentication credential handling; define the boundary between application logic and backend communication. `\par\smallskip`{=latex} **Dependencies:** Application models, with no concrete HTTP dependency. The interface is implemented by `BackendCommunication`. |

###### Interface Operations {.unnumbered}

- `login(identifier: String, password: String)` authenticates the user. Returned credentials are handled internally by the communication and token components.

- `logout()` requests backend session revocation and local credential cleanup.

- `getActiveBookings()` returns active bookings as application `Booking` models.

- `requestAccess(request: ResourceAccessRequest)` sends an access request and returns a `ResourceAccessResponse`.

- `confirmAccess(confirmation: AccessConfirmation)` sends an access confirmation to the backend.

- `checkOut(resourceId: String)` requests completion of resource usage.

The operations are asynchronous. DTO creation, authentication headers, token refresh, and credential persistence remain internal concerns and are not exposed through the operation signatures.

##### Classes {.unnumbered}

\leavevmode\par

| Element | Description |
|:----------------------------------------|:------------------------------------------------------------|
| **BackendCommunication** | **Purpose:** Implement `CommunicationGateway` and adapt application operations to backend communication. `\par\smallskip`{=latex} **Responsibilities:** Convert models and DTOs; coordinate login, refresh, and logout; manage credentials through `TokenManager`; retry a protected operation once after successful refresh. `\par\smallskip`{=latex} **Dependencies:** `CommunicationGateway`, `BackendApiClient`, `TokenManager`, application models, and transport DTOs. |
| **BackendApiClient** | **Purpose:** Provide the low-level REST communication operations. `\par\smallskip`{=latex} **Responsibilities:** Send HTTP requests, exchange DTOs, attach supplied access credentials, and perform transport operations for authentication, bookings, resource access, and check-out. `\par\smallskip`{=latex} **Dependencies:** An HTTP communication system and transport DTOs. |

##### Data Transfer Objects {.unnumbered}

\leavevmode\par

The communication module defines DTOs for the backend contract. They remain separate from application models so that transport formats can evolve independently. Their final attributes will be defined with the backend API.

| Element | Description |
|:----------------------------------------|:------------------------------------------------------------|
| **LoginRequestDto** | Contains the user identifier and password sent to the authentication endpoint. |
| **LoginResponseDto** | Contains the access and refresh tokens returned after successful authentication. |
| **RefreshTokenRequestDto** | Contains the current refresh token sent to renew authentication credentials. |
| **RefreshTokenResponseDto** | Contains the new access token and any replacement refresh token returned after successful renewal. |
| **BookingDto** | Contains booking information received from the backend and converted into an application `Booking` model. |
| **ResourceAccessRequestDto** | Contains the information sent to the backend to request access to a resource. |
| **ResourceAccessResponseDto** | Contains the backend's response to an access request, converted into an application `ResourceAccessResponse` model. |
| **AccessConfirmationRequestDto** | Contains the information sent to confirm the outcome of a resource-access operation. |
| **CheckOutRequestDto** | Contains the information sent to complete resource usage through check-out. |
##### HTTP Communication {.unnumbered}

\leavevmode\par

The communication module will use an HTTP communication system to exchange requests and responses with the backend. This system will support request execution, authentication headers, transport-data conversion, timeouts, and communication-error handling.

The concrete HTTP library, platform configuration, and client initialization are implementation choices and will be documented in Chapter 5. They do not define additional architectural modules in this design.

#### TokenManager

The token-management module owns local authentication credentials. It uses the public `SecureStorage` contract supplied by the native application and remains independent from backend communication.

The interface is defined in the `api` package. The existing diagram retains the relationship between the token manager and this external storage contract.

![Token Management Class Structure](assets/mobile-token-classes.png){width=80%}

| Element | Description |
|:----------------------------------------|:------------------------------------------------------------|
| **TokenManager** | **Purpose:** Manage local authentication credentials. `\par\smallskip`{=latex} **Responsibilities:** Keep the access token in memory; persist and retrieve the refresh token through protected storage; replace credentials after login or refresh; clear credentials when the session ends. `\par\smallskip`{=latex} **Dependencies:** `SecureStorage`. |

The component provides operations to obtain the access token, obtain the refresh token, store replacement credentials, and clear local credentials. Token renewal itself is coordinated by `BackendCommunication`.

### Authentication and Token Lifecycle

Authentication is coordinated by `BackendCommunication` and `TokenManager`. The communication component owns backend interactions, while the token component owns local credential state.

The following flows describe the intended behavior. Authentication credentials remain internal to the core.

#### Login Flow

1. The native application calls `MobileCoreApi.login(identifier, password)`.
2. `ServiceFacade` delegates the operation to `AuthService`.
3. `AuthService` invokes `CommunicationGateway.login(...)`.
4. `BackendCommunication` creates a `LoginRequestDto` and delegates the HTTP request to `BackendApiClient`.
5. The backend validates the credentials and returns a short-lived access token and a long-lived refresh token.
6. `BackendCommunication` stores the credentials through `TokenManager.storeTokens(...)`.
7. The password is not persisted or reused for automatic authentication.

![Mobile Login Flow](assets/mobile-login-flow.png){width=100%}

#### Access Token Refresh

Refresh is transparent to callers and is not part of `MobileCoreApi` or `CommunicationGateway`.

1. `BackendCommunication` obtains the access token from `TokenManager`.
2. When available, the token is used for the protected backend request.
3. If the token is absent or rejected as expired, the communication component obtains the refresh token.
4. It creates a `RefreshTokenRequestDto` and calls `BackendApiClient.refresh(...)`.
5. On success, the replacement credentials are stored through `TokenManager`.
6. The protected operation is performed with the new token, or retried once if previously rejected.
7. If the refresh token is invalid, expired, or revoked, local credentials are cleared and authentication is required again.

![Mobile Token Refresh Flow](assets/mobile-token-refresh-flow.png){width=95%}

#### Logout Flow

1. The native application calls `MobileCoreApi.logout()`.
2. The operation reaches `BackendCommunication` through the facade, authentication service, and communication gateway.
3. The communication component obtains the refresh token from `TokenManager`.
4. The backend is asked to revoke the renewable session.
5. `TokenManager.clearTokens()` removes the local access token and persisted refresh token.

Backend revocation prevents the renewable credential from being used to obtain new access credentials.

#### Token and Session Lifecycle

**Password.** Used for the initial authentication operation and not persisted.

**Access token.** Used to authenticate protected backend requests. It is short-lived and retained in memory.

**Refresh token.** Used to obtain replacement access credentials. It is long-lived, revocable, and persisted through protected storage.

### Design Decisions and Assumptions

#### Design Decisions

| Decision | Rationale |
|:--------------------------|:------------------------------------------------------|
| Shared Mobile Core | Share application, communication, and credential-management logic across Android and iOS. |
| Three internal components | Separate application orchestration, backend communication, and local credential management. |
| Separate API package | Group the public application contract, core creation entry point, and native storage contract. |
| Single application contract | Let native applications invoke shared operations through `MobileCoreApi`. |
| Communication boundary | Keep application services independent from HTTP and transport formats through `CommunicationGateway`. |
| Separate data models | Allow backend transport representations to evolve independently from the application contract. |
| HTTP communication | Keep transport mechanisms and configuration within the backend communication boundary. |
| Internal token renewal | Keep login credentials and token-refresh mechanics out of native application flows. |
| Protected credentials | Preserve renewable sessions without storing the password. |
| Native adapters | Integrate platform-specific functionality through native applications and shared contracts. |

#### Assumptions

- The backend provides REST endpoints for authentication, bookings, and resource-access operations.
- Login returns a short-lived access token and a long-lived refresh token.
- Refresh may rotate the refresh token; any replacement is stored immediately.
- Android and iOS provide concrete implementations of `SecureStorage`.
- Detailed native architecture and the BLE protocol will be designed separately.
- DTO attributes, endpoint contracts, and error mappings may be refined with the backend contract.
- Check-out uses `resourceId`; its transport representation may evolve independently.
- A protected operation is retried at most once after successful token renewal.


## Database Design

<!-- ![ER Diagram](assets/erd.png) error -->

### 1. Core Entities

#### User
`id`, `email`, `password_hash` (VARCHAR, **nullable** – supports external or token-based authentication providers), `role` (VARCHAR), `status` (VARCHAR), `created_at` (TIMESTAMPTZ), `first_name`, `last_name`, `start_semester`, `field_of_study`, `failed_attempts`, `locked_until` (TIMESTAMPTZ)

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
* **Constraints:** Enforces `CHECK (end_time > start_time)` and a PostgreSQL `EXCLUDE USING gist` constraint to prevent overlapping active bookings for the same resource.

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

Status: design proposal for T-003, ready for team review.

### Purpose and Scope

This concept describes how the mobile app on the smartphone communicates with the door terminal over BLE to authorize access and record check-in/check-out. The terminal is assigned to the booked resource. The flow follows the backend's ticket approach.

#### The idea in short

Before check-in, the app fetches a signed ticket from the backend. The terminal can check this ticket completely offline. With a challenge-response step, the app proves that it holds the matching private key, so simply copying the ticket to another smartphone is not enough. If everything succeeds, the terminal saves the event and issues a signed receipt, which the app uploads to the backend. Check-out works the same way, with its own ticket.

**Not in scope:** BLE and firmware implementation, mobile classes, final data formats and cryptographic algorithms, detailed synchronization, and performance optimization.

### Actors and Prerequisites

Three parties are involved. Only the first two take part in the BLE exchange.

| Actor | Role |
|---|---|
| Mobile app on the smartphone | BLE central / GATT client. Verifies the terminal, sends the ticket, proves the device key, and uploads receipts. |
| Door terminal (assumed ESP32) | BLE peripheral / GATT server. Checks tickets offline, saves events, and returns signed receipts. |
| Backend | Registers devices, issues tickets, and processes receipts. It takes no part in the BLE exchange. |

Before a check-in or check-out can start, four things must be in place:

1. The smartphone has a registered key pair. The private key stays in protected storage; the backend links the public key to the user.
2. The app holds a backend-signed ticket for this device key, booking, resource, action, and time window.
3. The terminal knows its terminal and resource IDs and the backend's verification key. It has its own signing key, a reliable clock, and storage that survives a restart.
4. The app has trusted information to verify the terminal.

The terminal never needs a name or email. It only checks the ticket and the device's proof. BLE advertising and addresses help the app find the terminal, but they do not prove identity.

Before issuing a ticket, the backend checks the user's role, booking ownership, and account/device status. In the proposed backend flow, only the booking owner can check in or out with their registered device.

The proposed design decisions are summarized below.

### Normal Flow: Check-in and Check-out

Check-in and check-out use the same exchange. Each needs its own ticket.

#### The five steps

1. **Connect and verify the terminal.** The app finds the terminal over BLE and connects. The terminal proves that it is the trusted terminal for this resource, and both sides set up a protected connection. *Message: handshake and terminal proof.*
2. **Send the ticket.** The app sends the signed ticket and an operation ID. The terminal checks the backend signature, resource, action, and time window. *Message: authorization request.*
3. **Prove the device key.** The terminal sends a fresh challenge, and the app signs it with the device key named in the ticket. *Messages: challenge and device proof.*
4. **Save and confirm.** The terminal checks its local state, saves the event, and returns a signed receipt. Only then may it signal access. *Message: signed receipt, or an error result.*
5. **Upload the receipt.** The app checks the terminal's signature and that the receipt matches its own request. It saves the receipt and uploads it when it is online. The backend then updates the booking status.

Under the backend's ticket proposal, a check-out ticket is issued only after the backend has accepted the check-in receipt. The upload in step 5 is therefore required to obtain that ticket.

![BLE connection, identification, check-in and check-out](../assets/ble-normal-flow.png){width=100%}

The message names describe purposes, not final names or formats. GATT characteristics, UUIDs, framing, and packet sizes are left for implementation.

#### What the terminal checks and saves

| Action | Required local state | Result |
|---|---|---|
| `CHECK_IN` | The booking may start. No conflicting use, and this booking was not started before. | One start event. The booking is in use. |
| `CHECK_OUT` | The same booking is currently in use. | One end event. The use is ended. |

The terminal follows three rules:

- **One request at a time.** It finishes one request before it handles the next.
- **Save first, then confirm.** Usage state, event, and receipt are saved together. Success and access are signalled only afterwards, so a restart cannot leave half an update.
- **Same request, same receipt.** If the app repeats a request that was already saved, the terminal returns the original receipt and creates no second event.

The terminal only knows its own local state. Whether a booking is valid centrally is checked by the backend when it issues the ticket.

### Failure and Initial Recovery

Every failed attempt ends in one of three results for the app. Which one depends on how far the exchange got:

- **Aborted:** a connection problem or an unverified terminal stopped the attempt before the app started sending the device proof. The terminal saved no usage event for this attempt.
- **Rejected:** the terminal answered with a rejection. This attempt saved nothing.
- **Unknown:** the app started sending the device proof but holds neither a valid receipt nor a final rejection. The terminal may have saved the event.

![BLE failure handling and initial recovery](../assets/ble-failure-flow.png){width=100%}

#### Failure cases

| Where it fails | Result | What the app does |
|---|---|---|
| Connection problem before the app starts sending the device proof (Bluetooth off, terminal not found, connection drops) | Aborted | Explain the problem. Allow a limited number of retries. |
| Terminal cannot be verified (unsupported version, untrusted terminal, protection fails) | Aborted | Stop before sending the ticket. No automatic retry, no unprotected connection. |
| Terminal rejects the ticket or proof (invalid or expired ticket, wrong resource or action, unreliable clock, late or invalid proof) | Rejected | Show the reason. Request a new ticket if it expired. |
| Terminal rejects the action (conflicting use, check-out without active use, full event queue, or a storage failure that clearly saved nothing) | Rejected | Do not show success or signal access. |
| No valid receipt after the app has started sending the device proof (connection lost, response missing, receipt invalid, or a storage failure with an unclear result) | Unknown | Keep the operation ID and follow the unknown-result rules below. |

#### Unknown result

The terminal can save an action only after it has checked the device proof. Before the app starts sending the device proof, a failed attempt is simply unsuccessful. After the app has started sending it, the result stays unknown until the app holds a verified receipt or a final rejection.

This is why the operation ID matters. The app saves it before sending the request and reuses it for every retry of the same action. In the unknown case the app keeps that ID, reconnects, and asks the terminal for the saved receipt. This query only reads; it cannot perform a check-in or check-out. If no receipt comes back, that does not prove that nothing was saved. Whether a later rejection settles the earlier attempt depends on the terminal's history check; the BLE Recovery Proposal in the security appendix defines when a rejection is final. The app never creates a new operation ID to get around this.

The security appendix contains the detailed recovery and retry rules. A failed receipt upload or a backend conflict is handled by the BLE Synchronization Proposal in the same appendix.

### Security Rules

Each rule protects a step of the normal BLE flow.

| Rule | Protects against |
|---|---|
| The terminal proves its identity before the app sends a ticket. | Fake terminals collecting tickets |
| The connection is protected before sensitive data is sent. | Reading or changing tickets and proofs in transit |
| A ticket alone is not enough. The device answers a challenge. | Copied tickets |
| Every challenge is unpredictable, short-lived, and usable once. | Replayed answers |
| Each device proof covers the ticket, operation, action, terminal, resource, and protected session. | A valid proof being reused for another action, terminal, or session |
| The terminal remembers what it has already applied. | The same booking action running twice |
| Keys stay protected. Logs hold no secrets or unnecessary personal data. | Leaked keys and personal data |
| Message size, waiting time, and retries are limited. | Malformed messages creating events or blocking the terminal |

The terminal proof covers a fresh nonce from the app, the protocol version, the expected terminal and resource, and the handshake itself. A static ID or certificate is not enough.

**Known limit:** BLE authentication does not prove physical distance. An attacker could forward the exchange from another place (relay attack). The team has to accept this or assess extra protection.

**Lost or compromised equipment:** Under the backend proposal, a user or Admin can block a lost smartphone, and an Admin can remove a stolen or compromised terminal. The backend then issues no new tickets for the blocked device and rejects receipts from the removed terminal.


### BLE Design Decisions

The proposed flow assumes that one terminal manages local resource use and that the terminal hardware is an ESP32. Both assumptions need team confirmation.

#### Proposed design decisions

| Decision | Reason |
|---|---|
| Action-specific tickets | A check-in ticket must not allow check-out. |
| Save state, event, and receipt together before success | All three stay consistent after a restart. |
| Keep the operation ID on retries | A retry must not create a second event. |
| Recover with the original device key | The receipt can be fetched again without allowing a new action. |
| Protect the connection before sending a ticket | Tickets and proofs cannot be read or changed. |
| Same terminal for check-in and check-out | An offline terminal can check the order with its own saved state. |

## Offline Operation and Synchronization

### Authorization While Offline

The terminal works without Wi-Fi or backend access, so it cannot learn about changes immediately:

- A ticket is valid for a few minutes. The exact validity limit must be agreed.
- The terminal needs a reliable clock. Without one, it rejects new actions.
- If a device is blocked or a booking is cancelled after the ticket was issued, the terminal may still accept that ticket until it expires. A later backend rejection cannot undo access already granted.
- The smartphone still needs connectivity to get a new ticket. With a valid ticket already stored, the BLE exchange can run without smartphone connectivity.

Under the current backend proposal, the smartphone must upload its check-in receipt and wait for backend acceptance before obtaining a check-out ticket. Fully offline check-in and check-out therefore need another agreed authorization policy. A check-in ticket never substitutes for a check-out ticket. Physical exit behavior is outside this concept.

### Receipt Upload and Acknowledgment

The smartphone carries signed receipts from the terminal to the backend. A successful BLE exchange confirms the local event; it does not yet confirm that the backend has accepted it. If an upload fails, the app keeps the receipt and shows synchronization pending. The backend checks the terminal signature, event details, and event order, and processes each event once.

The proposed return path is a backend-signed acknowledgment carried by the app. The terminal verifies that acknowledgment before marking its event as synchronized. Conflicts stay available for Admin review; a rejected upload cannot undo access already granted. Pending events survive restarts and are never overwritten to make room. If the queue is full, the terminal rejects new actions that it cannot save.

The BLE synchronization proposal in Detailed Security Documentation gives the acknowledgment and retry details. Its return path after check-out, queue limits, and handling of late receipts still require team agreement.

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
