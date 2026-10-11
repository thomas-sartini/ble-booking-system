# Implementation

- *Describe the system actually built, its important implementation decisions, and any changes from the design in Chapter 3.*

## Final System Architecture

- *Show the final component architecture and the technologies actually selected for Web, Backend, Database, Shared Mobile Core, Android, iOS, and Door Terminal.*
- *Explain important changes from the proposed architecture and how the implemented components connect.*

## Web Application

- *Describe the implemented modules/components, their responsibilities, and how the application communicates with the backend.*
- *Include screenshots of the main user and administrator screens, explaining navigation, role-dependent actions, and error feedback.*

## Backend

### Modules and Classes

- *Show the implemented backend module structure and the important classes, including their responsibilities, dependencies, and internal interfaces.*
- *Explain important implementation decisions or changes from the proposed design; include updated diagrams where needed.*

### Main Functions

- *Explain how accounts, roles, sessions, resources, terminals, bookings, and usage are handled, including availability checks and concurrent requests.*
- *Describe cost calculation, PDF invoices, payment status, email notifications, and audit logging, including the agreed exceptional-case policies.*

## Database and Persistence

### Database Container Setup (Spike T-014)

To ensure a consistent, isolated, and reproducible database environment across all developer machines, the database infrastructure is defined using Docker Compose.

#### Technical Configuration
The technical prototype uses PostgreSQL 16 Alpine as the lightweight relational database container. The container setup is declared in `docker-compose.yml` at the project root:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: ble_booking_db_container
    environment:
      POSTGRES_DB: ble_booking_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgrespassword
    ports:
      - "5433:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```
### Key Architecture & Configuration Decisions

* **Container Image:** `postgres:16-alpine` was chosen to minimize resource consumption and speed up local initialization time.
* **Port Mapping:** The container exposes port `5432` internally, mapped to host port `5433` (`5433:5432`) to prevent conflicts with pre-existing local PostgreSQL services on developers' machines.
* **Persistence:** A named Docker volume (`postgres_data`) is mapped to `/var/lib/postgresql/data` inside the container, ensuring data persists across container restarts and updates.
* **Development Environment:** Standard developer credentials (`postgres` / `postgrespassword`) are pre-configured for seamless local integration before external configuration or environment files are introduced in later stages.

### Execution & Management

Developers can manage the database lifecycle locally via Docker CLI:

* **Start Database:** `docker compose up -d`
* **Stop Database:** `docker compose down`

## Mobile Application

### Shared Mobile Core

- *Describe the implemented shared modules and important classes for REST communication, sessions, protocol messages, and access logic.*
- *Explain the interfaces connecting shared logic to native BLE and protected-storage adapters.*

### Android Application

- *Describe the Android implementation, its integration with the shared core, and its BLE and protected-storage adapters.*
- *Include main screen examples and explain permissions, connection handling, access feedback, and relevant platform limitations.*

### iOS Application

- *Describe the iOS implementation, its integration with the shared core, and its BLE and protected-storage adapters.*
- *Include main screen examples and explain permissions, connection handling, access feedback, and relevant platform limitations.*

## Door Terminal

### Hardware and Firmware Structure

- *Describe the actual terminal hardware, door/actuator connection, and firmware modules, classes, or data structures.*
- *Explain their responsibilities, local storage, initialization, and recovery after restart or interrupted operations.*

### BLE Communication and Door Control

- *Explain how the terminal processes BLE messages, verifies authorization, and controls the door.*
- *Describe how check-in/check-out events are created and stored, including denied access and communication failures.*

## Interfaces and Communication

- *Document the main REST endpoints and shared-core/native adapter interfaces, including data models, authentication, and errors.*
- *Describe the implemented BLE contract and terminal-to-backend synchronization route.*

## Offline Operation and Synchronization

- *Explain the implemented offline authorization window, credential expiry, durable event queue, and delayed effect of central revocation.*
- *Describe synchronization, retries, duplicate prevention, and conflict handling, including how repeated events avoid duplicate usage records or charges.*

## Main Interaction Flows

- *Include technical Sequence Diagrams for the main implemented flows: login, booking, BLE check-in, and check-out with billing.*
- *Show offline synchronization and important alternative/error flows using the actual components and interfaces.*

## Security Implementation

- *Describe the actual controls for encrypted communication, password hashing, sessions, role enforcement, and protected mobile credentials/keys.*
- *Explain BLE replay protection, device/terminal revocation, restricted data access, and protection of secrets and audit records.*

## Setup and Deployment

- *Summarize how the components are built, configured, connected, and started, including terminal provisioning.*
- *Include complete setup and operating instructions in Appendix A.5 so the delivered prototype can be reproduced.*

## Decisions, Difficulties, and Limitations

- *Explain the important technical trade-offs and integration problems encountered, with the solutions chosen.*
- *State known implementation limitations and unfinished functions; evaluate their impact in Chapter 6 and discuss future work in Chapter 7.*
