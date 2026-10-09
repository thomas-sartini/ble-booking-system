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

### Module Structure

- *Show the proposed backend modules, such as accounts, resources, bookings, access, billing, notifications, and audit.*
- *Describe each module's responsibility, dependencies, and main interfaces with other modules.*

### Class Structure

- *Include a proposed Class Diagram showing important domain classes, controllers, services, and repositories, according to the chosen design.*
- *Briefly describe their responsibilities and relationships, focusing on the main classes.*

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

- *Include the proposed ER Diagram with tables/entities, main fields, primary and foreign keys, relationships, and cardinalities.*
- *Explain how users, roles, resources, terminals, bookings, usage events, invoices, payments, and audit records are connected, including important integrity constraints.*

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
