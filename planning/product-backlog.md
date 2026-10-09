# Product Backlog

## ID Rules

The following ID formats and numbering rules are used across the project.

- Requirement IDs:
  - Functional requirements: `FR-XX.XX`
  - Non-functional requirements: `NFR-XX.XX`

- Expected result IDs:
  - `ER-XX`

- Epic IDs:
  - `EP-XX`
  - Epic IDs are assigned sequentially.

- User Story IDs:
  - `US-XX.YY`
  - `XX` identifies the parent Epic.
  - `YY` is assigned sequentially within that Epic.

- Enabler IDs:
  - `EN-XX.YY`
  - `XX` identifies the parent Epic.
  - `YY` is assigned sequentially within that Epic.
  - Enablers represent technical outcomes that support current or future product functionality.

- Task and Spike IDs:
  - `T-XXX`
  - Tasks and Spikes share one globally unique sequential number range.
  - A Spike uses the same `T-XXX` identifier and is additionally marked with `[Spike]` in the GitHub Issue title.
  - Example Task: `T-015 Create initial database migration`
  - Example Spike: `T-016 [Spike] Evaluate database triggers`
  - The project task ID is independent from the GitHub Issue number.

- ID assignment rules:
  - IDs are assigned sequentially within their respective numbering scheme.
  - An assigned ID is never reused.
  - Closing, cancelling, superseding, or deleting an item does not make its ID available again.
  - Moving a Task or Spike to another parent does not change its `T-XXX` ID.
  - User Story and Enabler IDs retain their original ID once assigned.

---

## Epics

| Epic ID | Title | Description and intended outcome | Requirement IDs | Status |
|:--|:--|:--|:--|:--|
| EP-01 | Accounts and Access Management | Provide secure account creation, authentication, session management, role management, and role-based authorization for Students, Lecturers, and Admins. | FR-01.01–FR-01.07, NFR-01.01, NFR-01.02, NFR-01.05 | Planned |
| EP-02 | Resource Discovery and Booking | Allow authorized users to discover resources, inspect availability, create and manage bookings, and ensure booking conflicts and role restrictions are correctly enforced. | FR-02.01, FR-03.01–FR-03.05, FR-04.01–FR-04.05 | Planned |
| EP-03 | Mobile and BLE Resource Access | Enable users to use the mobile application and BLE terminal communication for secure resource access, check-in, check-out, offline operation, and synchronization. | FR-02.02, FR-02.03, FR-05.01–FR-05.06, NFR-01.03, NFR-01.04 | Planned |
| EP-04 | Billing and Invoicing | Calculate usage costs, generate traceable PDF invoices, and manage invoice and payment status. | FR-06.01–FR-06.05 | Planned |
| EP-05 | Notifications and Audit Trail | Notify users about relevant system events and provide a secure, traceable audit trail for administrative and security-relevant actions. | FR-07.01–FR-07.04 | Planned |
| EP-06 | Security and System Foundations | Establish cross-cutting security controls and technical foundations required by the web app, mobile app, backend, terminal, and stored data. | NFR-01.01–NFR-01.05 | Planned |

---

## User Stories

User Stories describe functionality or behavior that delivers value to a user or stakeholder.

A User Story should be sufficiently refined and scoped so that it can reasonably be completed within a single Sprint before it is selected for Sprint Planning.

| Story ID | Epic ID | Title | Description | Requirement IDs | Status |
|:--|:--|:--|:--|:--|:--|

---

## Enablers, Tasks and Spikes

Technical Enablers, Tasks, and Spikes are created and managed only as GitHub Issues and are not duplicated in this file.

### Enablers

An Enabler represents a concrete technical outcome required to support current or future product functionality.

Each Enabler shall:

- use an `EN-XX.YY` ID,
- reference its parent Epic,
- define a clear and verifiable technical outcome,
- be scoped so that it can reasonably be completed within a Sprint.

### Tasks

A Task represents concrete implementation or engineering work that is sufficiently understood to be carried out directly.

Each Task shall:

- use a globally unique `T-XXX` ID,
- reference its parent User Story or Enabler when applicable,
- otherwise reference the relevant Epic,
- contribute directly to the completion of its related backlog item,
- define clear completion criteria.

### Spikes

A Spike represents time-boxed research, investigation, or technical exploration used to reduce uncertainty before implementation.

Each Spike shall:

- use a globally unique `T-XXX` ID from the same numbering sequence as normal Tasks,
- include `[Spike]` in the GitHub Issue title,
- reference its parent User Story or Enabler when applicable,
- otherwise reference the relevant Epic,
- define the specific question or uncertainty to be investigated,
- produce a documented conclusion, decision, model, or recommendation.

Tasks and Spikes share the same global `T-XXX` numbering sequence. Assigned IDs are never reused.

Do not maintain a duplicate list of Enablers, Tasks, or Spikes in this file.