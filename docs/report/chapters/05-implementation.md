# Implementation

<!-- Author: Describe the system actually built, its important implementation decisions, and any changes from the design in Chapter 3. -->

## Final System Architecture

<!-- Author: Show the final component architecture and the technologies actually selected for Web, Backend, Database, Shared Mobile Core, Android, iOS, and Door Terminal. -->
<!-- Author: Explain important changes from the proposed architecture and how the implemented components connect. -->

## Web Application

<!-- Author: Describe the implemented modules/components, their responsibilities, and how the application communicates with the backend. -->
<!-- Author: Include screenshots of the main user and administrator screens, explaining navigation, role-dependent actions, and error feedback. -->

## Backend

### Modules and Classes

<!-- Author: Show the implemented backend module structure and the important classes, including their responsibilities, dependencies, and internal interfaces. -->
<!-- Author: Explain important implementation decisions or changes from the proposed design; include updated diagrams where needed. -->

### Main Functions

<!-- Author: Explain how accounts, roles, sessions, resources, terminals, bookings, and usage are handled, including availability checks and concurrent requests. -->
<!-- Author: Describe cost calculation, PDF invoices, payment status, email notifications, and audit logging, including the agreed exceptional-case policies. -->

## Database and Persistence

The report documents local PostgreSQL provisioning through Docker Compose and schema initialization through Flyway. The migration is `backend/src/main/resources/db/migration/V1__initial_schema.sql`; later changes are intended as new versioned migrations. JPA validates the schema rather than creating it. The detailed [local setup](08-appendices.md#local-database-setup) includes configuration and lifecycle commands.

The container image, environment separation, port mapping and named-volume rationale are documented with that procedure. The trigger decision belongs to [database design](03-ideas-and-concepts.md#database-trigger-evaluation).

## Mobile Application

### Shared Mobile Core

<!-- Author: Describe the implemented shared modules and important classes for REST communication, sessions, protocol messages, and access logic. -->
<!-- Author: Explain the interfaces connecting shared logic to native BLE and protected-storage adapters. -->

### Android Application

<!-- Author: Describe the Android implementation, its integration with the shared core, and its BLE and protected-storage adapters. -->
<!-- Author: Include main screen examples and explain permissions, connection handling, access feedback, and relevant platform limitations. -->

### iOS Application

<!-- Author: Describe the iOS implementation, its integration with the shared core, and its BLE and protected-storage adapters. -->
<!-- Author: Include main screen examples and explain permissions, connection handling, access feedback, and relevant platform limitations. -->

## Door Terminal

### Hardware and Firmware Structure

<!-- Author: Describe the actual terminal hardware, door/actuator connection, and firmware modules, classes, or data structures. -->
<!-- Author: Explain their responsibilities, local storage, initialization, and recovery after restart or interrupted operations. -->

### BLE Communication and Door Control

<!-- Author: Explain how the terminal processes BLE messages, verifies authorization, and controls the door. -->
<!-- Author: Describe how check-in/check-out events are created and stored, including denied access and communication failures. -->

## Interfaces and Communication

<!-- Author: Document the main REST endpoints and shared-core/native adapter interfaces, including data models, authentication, and errors. -->
<!-- Author: Describe the implemented BLE contract and terminal-to-backend synchronization route. -->

## Offline Operation and Synchronization

<!-- Author: Explain the implemented offline authorization window, credential expiry, durable event queue, and delayed effect of central revocation. -->
<!-- Author: Describe synchronization, retries, duplicate prevention, and conflict handling, including how repeated events avoid duplicate usage records or charges. -->

## Main Interaction Flows

<!-- Author: Include technical Sequence Diagrams for the main implemented flows: login, booking, BLE check-in, and check-out with billing. -->
<!-- Author: Show offline synchronization and important alternative/error flows using the actual components and interfaces. -->

## Security Implementation

<!-- Author: Describe the actual controls for encrypted communication, password hashing, sessions, role enforcement, and protected mobile credentials/keys. -->
<!-- Author: Explain BLE replay protection, device/terminal revocation, restricted data access, and protection of secrets and audit records. -->

## Setup and Deployment

<!-- Author: Summarize how the components are built, configured, connected, and started, including terminal provisioning. -->
<!-- Author: Include complete setup and operating instructions in Appendix A.5 so the delivered prototype can be reproduced. -->

## Decisions, Difficulties, and Limitations

<!-- Author: Explain the important technical trade-offs and integration problems encountered, with the solutions chosen. -->
<!-- Author: State known implementation limitations and unfinished functions; evaluate their impact in Chapter 6 and discuss future work in Chapter 7. -->

## Implementation-Source Discrepancies {#implementation-discrepancies}


<a id="tbl-05-01"></a>

| Topic | Documentation and repository evidence |
|---|---|
| PostgreSQL container | T-014's setup text specifies `postgres:16-alpine` and `/var/lib/postgresql/data`; the later initialization paragraph specifies PostgreSQL 18, and `docker-compose.yml` uses `postgres:18-alpine` with `/var/lib/postgresql`. Both accounts are retained in the setup appendix. |
| Initial schema | The old initialization paragraph names `sample_bookings`; V1 instead creates `user`, `resource`, `terminal`, `booking`, `device`, `refresh_token`, `one_time_token`, `receipt`, `invoice`, `availability` and `audit_log`. The example name does not describe the supplied migration. |
| Overlap scope | The design describes an exclusion constraint for active bookings. V1's exclusion has no status predicate, so its declared scope also includes cancelled/completed rows. This is a source discrepancy, not an executed database result. |
| Table ownership names | The backend narrative uses plural table names in one ownership table and singular names in its package table. V1 uses singular names. The technical reference preserves both source conventions explicitly. |
| Trigger decision maturity | The original trigger decision says all domain responsibilities are implemented in services. No implementation account or execution evidence accompanies that broad claim; it is retained as a design responsibility, not proof that those functions work. |
| CI and schema initialization | The test concept names `database/init.sql` and CI triggers for `backend/` or `database/`. The repository instead supplies Flyway V1, and `.github/workflows/backend.yml` filters `backend/**` and the workflow itself. Branch protection cannot be established from that file. |
| Deployment | T-014 anticipates a cloud-managed production database; the backend test concept says the prototype is not deployed and specifies no automatic deployment. The available material does not establish a production deployment. |
| Mobile setup | `mobile/README.md` mentions `sharedUI` and a `:sharedUI:testAndroidHostTest` task. The supplied project contains `sharedLogic`, `androidApp` and `iosApp`; the guide retains only applicable commands and preserves this discrepancy here. |

: Implementation-Source Discrepancies — topic, documentation and repository evidence {#tbl-05-01}


The mobile architecture describes the current core as a structural foundation, and T-016 identifies its access-service methods as placeholders. The terminal documents define hardware selection and proposed software interfaces; they do not document implemented firmware. Detailed implementation descriptions remain author tasks in the corresponding source sections.
