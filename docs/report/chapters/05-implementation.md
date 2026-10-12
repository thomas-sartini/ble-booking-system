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

### Database Trigger Evaluation (Spike T-013)

During the architectural design phase of the persistence layer, the application of database triggers was evaluated to determine whether system responsibilities (e.g., integrity checks, audit logging, or state transitions) should be delegated to the database level.

#### Evaluation & Trade-offs
* **Database Triggers (Pros & Cons):** While triggers guarantee data-level consistency regardless of the accessing client and reduce network overhead, they introduce significant architectural drawbacks. They hide core business logic within the database schema, complicate unit and integration testing, obscure debugging workflows, and introduce risks during database migrations.
* **Application Logic (Pros & Cons):** Handling system responsibilities within the Java/Spring Boot application layer keeps domain rules explicit, fully testable, and version-controlled within the primary codebase.

#### Architectural Decision
**Decision:** The team decided **against using database triggers** in the system architecture. All domain responsibilities, including booking availability validation, concurrent access controls, audit logging, and lifecycle timestamps, are explicitly implemented within the application service layer (Java / Spring Boot). 

This decision ensures strict separation of concerns, high maintainability, and seamless end-to-end testability across the entire booking lifecycle.

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
