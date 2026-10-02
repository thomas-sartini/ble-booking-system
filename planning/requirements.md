# Requirements register

## Functional requirements

### FR-01 — Accounts, roles, and sessions

| ID | Requirement | Priority | Version |
|:--|:--|:--|:--|
| FR-01.01 | The system shall support exactly three application roles: Student, Lecturer, and Admin. Admins can manage accounts, resources, payment status, and audit records; Students and Lecturers can use only functions and resources permitted by their role. | Must | 1.0 |
| FR-01.02 | An Admin shall create a personal, single-use, expiring invitation link for a new account. Registration through it shall set the invited identity and role; invalid, expired, or reused links shall be rejected. Public self-registration is out of scope. | Must | 1.0 |
| FR-01.03 | An Admin shall view, update, deactivate, and change the role of an account. A deactivated account shall lose online access and new sessions, subject to the documented offline authorization window. | Must | 1.0 |
| FR-01.04 | Users shall sign in to the web app with username or email and password, and sign out. | Must | 1.0 |
| FR-01.05 | Users shall reset a password through a single-use, expiring recovery link. | Optional | 1.0 |
| FR-01.06 | Following an initial mobile sign-in, the app shall maintain access using a revocable long-lived renewal credential and short-lived access credentials, without storing the password for automatic login. Sign-out, deactivation, or explicit revocation shall prevent renewal. | Must | 1.0 |
| FR-01.07 | The backend shall enforce role rules for every protected operation. An Admin shall configure which resources or time windows Students and Lecturers may book; the UI shall show only permitted actions. | Must | 1.0 |

---

### FR-02 — Web and mobile applications

| ID | Requirement | Priority | Version |
|:--|:--|:--|:--|
| FR-02.01 | The web app shall let authorized users search resources, inspect availability, and create and manage their own bookings; Admins shall have the administration functions defined here. | Must | 1.0 |
| FR-02.02 | A mobile application shall support account sign-in, display of the user's current booking or access entitlement, and BLE communication with the associated resource terminal for check-in and check-out. | Must | 1.0 |
| FR-02.03 | The system shall handle concurrency between mobile and web access to the same terminal. | Must | 1.0 |

---

### FR-03 — Resources and availability

| ID | Requirement | Priority | Version |
|:--|:--|:--|:--|
| FR-03.01 | An Admin shall add and edit resources through the web app, including their description, bookable time windows, role eligibility, associated terminal, and pricing rule. | Must | 1.0 |
| FR-03.02 | An Admin shall remove a resource from future bookings without deleting its booking, usage, or invoice history. | Must | 1.0 |
| FR-03.03 | Authorized users shall see available resources and bookable time windows in the web app, reflecting existing bookings and role restrictions. | Must | 1.0 |
| FR-03.04 | The mobile app shall display resource and booking information. | Optional | 1.0 |
| FR-03.05 | The system shall allow a user to access and use a resource without a prior booking when the resource is currently available and not reserved by another user. | Must | 1.0 |

---

### FR-04 — Bookings and lifecycle

| ID | Requirement | Priority | Version |
|:--|:--|:--|:--|
| FR-04.01 | An eligible user shall create a booking for an available resource and time window and receive an email confirmation. | Must | 1.0 |
| FR-04.02 | The system shall prevent overlapping active bookings of the same resource and enforce role eligibility and availability on creation or change, including concurrent requests. | Must | 1.0 |
| FR-04.03 | A booking owner or authorized Admin shall be able to modify a booking before or during its use; any modification shall recheck availability. A booking may be cancelled only before use begins. A checked-in booking may be modified but not cancelled, while a completed booking shall not be modified or cancelled. | Must | 1.0 |
| FR-04.04 | The system shall record confirmed, cancelled, in-use, and completed states and valid transitions; expiry or no-show shall have an explicit outcome under the agreed policy. | Must | 1.0 |
| FR-04.05 | Users shall see their own current/past bookings; Admins shall inspect bookings for administration, without exposing other users' details to ordinary users. | Must | 1.0 |

---

### FR-05 — BLE access, terminal, and offline operation

| ID | Requirement | Priority | Version |
|:--|:--|:--|:--|
| FR-05.01 | Identification at a resource terminal shall use BLE communication with the mobile app and bind access to a specific user, resource, and eligible booking. NFC is outside implementation scope. | Must | 1.0 |
| FR-05.02 | A valid start of use shall create a check-in event, and a valid end of use shall create a check-out event. The status shall be updated after synchronization. | Must | 1.0 |
| FR-05.03 | The terminal shall verify eligible access and record check-in/check-out without Wi-Fi or backend connectivity, using locally available authorization data for a bounded offline validity window. Unknown or expired authorizations shall be rejected. | Must | 1.0 |
| FR-05.04 | The terminal shall queue offline events durably and synchronize them on reconnection without duplicate check-ins, check-outs, or charges. Conflicts shall be flagged for Admin review, not silently overwritten. | Must | 1.0 |
| FR-05.05 | The terminal/mobile contract shall document enrollment, message types, versioning, identifiers, outcomes, error handling, and security handshake; the terminal/backend synchronization contract shall also be documented. | Must | 1.0 |
| FR-05.06 | An Admin shall associate a terminal with a resource and take a lost or compromised terminal out of service. | Must | 1.0 |

---

### FR-06 — Billing and payment status

| ID | Requirement | Priority | Version |
|:--|:--|:--|:--|
| FR-06.01 | The system shall automatically calculate the final cost based on the agreed pricing rule and the finalized usage at check-out. If no check-out is recorded, the cost shall be calculated using the agreed end-of-booking rule. | Must | 1.0 |
| FR-06.02 | The system shall define and apply rules for no-shows, overtime, and missing check-out before billing is enabled. | Must | 1.0 |
| FR-06.03 | The system shall generate one uniquely identified PDF invoice per billable finalized usage, linked to user, booking, resource, calculation, and amount. Repeated processing shall not duplicate it. | Must | 1.0 |
| FR-06.04 | Each invoice shall have a Due, Pending, or Paid status. An authorized Admin shall update it and record when/by whom; users shall see their own invoice status. | Must | 1.0 |
| FR-06.05 | Issued invoice data shall remain traceable to the rate and usage on which it was based; later price/resource changes shall not silently alter its PDF or amount. | Must | 1.0 |

---

### FR-07 — Email and audit trail

| ID | Requirement | Priority | Version |
|:--|:--|:--|:--|
| FR-07.01 | The system shall email affected users when bookings are confirmed, changed, or cancelled, when a resource change affects their booking, and when an invoice or payment status changes. Invitations and password resets shall also be emailed. | Must | 1.0 |
| FR-07.02 | Email failure shall be recorded and retried or surfaced to an Admin without undoing a successful operation. Messages shall not contain passwords, tokens, or unnecessary personal data. | Must | 1.0 |
| FR-07.03 | The system shall log relevant account/role, authentication, resource/booking, BLE access, check-in/out, invoice, payment, and offline reconciliation events with actor, time, action, target, and outcome. | Must | 1.0 |
| FR-07.04 | Admins shall view and filter the audit log; ordinary users shall not access it. Entries shall not be editable through the app. | Must | 1.0 |

---

## Non-functional requirements

### NFR-01 — Security and privacy

| ID | Requirement | Priority | Version |
|:--|:--|:--|:--|
| NFR-01.01 | Online client/server and terminal/server communication shall use authenticated, encrypted transport. Passwords shall be stored only as salted adaptive hashes (for example, Argon2id or bcrypt), never plaintext or reversible. | Must | 1.0 |
| NFR-01.02 | Sessions, invitation/reset links, and mobile renewal credentials shall have defined expiry and revocation. Renewal credentials shall be stored in platform-protected storage, never in logs, and rotated or invalidated according to the chosen session design. | Must | 1.0 |
| NFR-01.03 | BLE identification shall authenticate devices or credentials, protect sensitive exchanges, and resist replay/copied-message attacks; a BLE address or proximity alone shall not grant access. | Must | 1.0 |
| NFR-01.04 | The backend and offline terminal shall enforce their valid authorization policies. Maximum offline credential validity and delay before central revocation takes effect offline shall be defined and documented. | Must | 1.0 |
| NFR-01.05 | Personal, booking, and billing data shall be accessible only to authorized roles. Secrets shall not appear in the repository, email, audit entries, or diagnostics. | Must | 1.0 |