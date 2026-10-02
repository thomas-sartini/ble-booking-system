# Expected results

Record the expected outcomes from the project assignment here. Define how each result will be demonstrated or verified.

| ID | Expected result | Source | Evidence or verification | Status |
|:--|:--|:--|:--|:--|
| ER-01 | A functional prototype of a secure NFC/BLE-based booking system is implemented. | Project assignment | Demonstration of the complete system workflow from authentication to resource usage and billing. | Planned |
| ER-02 | The system supports secure user management with three roles: Student, Lecturer, and Admin. | FR-01 | Test cases proving role creation, authentication, authorization, and restricted access to functions. | Planned |
| ER-03 | Users can authenticate securely using username/email and password, with secure session management. | FR-01.04, FR-01.06, NFR-01.02 | Login/logout tests, session expiration tests, credential revocation tests. | Planned |
| ER-04 | Administrators can manage user accounts, invitations, roles, and account status. | FR-01.02, FR-01.03 | Admin interface demonstration and tests for account activation, deactivation, and role changes. | Planned |
| ER-05 | The system provides resource management including creation, configuration, availability windows, and pricing rules. | FR-03 | Admin creates and modifies resources; verification through database records and UI tests. | Planned |
| ER-06 | Authorized users can search available resources and create bookings according to permissions and availability. | FR-03.03, FR-04.01 | Booking scenarios showing successful reservations and rejection of invalid bookings. | Planned |
| ER-07 | The system prevents conflicting bookings and enforces resource availability and role restrictions. | FR-04.02 | Concurrent booking tests and validation of rejected overlapping reservations. | Planned |
| ER-08 | The booking lifecycle is implemented with states such as confirmed, cancelled, in-use, and completed. | FR-04.04 | State transition tests covering valid and invalid booking changes. | Planned |
| ER-09 | Users can view their own current and previous bookings while administrators can manage bookings. | FR-04.05 | User interface tests verifying correct visibility of booking information. | Planned |
| ER-10 | Mobile identification through BLE/NFC enables secure access to assigned resources. | FR-05.01 | Demonstration of mobile-terminal communication and successful identification flow. | Planned |
| ER-11 | Check-in and check-out operations are recorded and linked to users, resources, and bookings. | FR-05.02 | Test scenario showing resource usage start/end and stored usage events. | Planned |
| ER-12 | Offline terminal operation is supported with local authorization data and later synchronization. | FR-05.03, FR-05.04 | Offline usage simulation, event queue verification, and synchronization tests. | Planned |
| ER-13 | BLE/NFC communication implements security mechanisms against unauthorized access and replay attacks. | NFR-01.03 | Security tests and documentation of authentication handshake and protection mechanisms. | Planned |
| ER-14 | The system automatically calculates usage costs based on defined pricing rules. | FR-06.01 | Test cases comparing usage duration and generated costs. | Planned |
| ER-15 | The system generates unique PDF invoices connected to usage, booking, resource, and user information. | FR-06.03 | Verification of generated PDF invoices and database references. | Planned |
| ER-16 | Payment status management is implemented with Due, Pending, and Paid states. | FR-06.04 | Admin updates payment status and users verify their own invoice status. | Planned |
| ER-17 | All important system events are recorded through an audit trail. | FR-07.03, FR-07.04 | Audit log inspection showing actor, timestamp, action, target, and outcome. | Planned |
| ER-18 | Email notifications are generated for important events such as booking changes, invitations, and invoice updates. | FR-07.01 | Email delivery tests and verification of logged failures. | Planned |
| ER-19 | The application uses a documented software architecture and database model. | Project assignment | Architecture diagrams, database schema documentation, and technical report. | Planned |
| ER-20 | Security requirements are documented and validated, including TLS communication, password hashing, authorization, and audit logging. | NFR-01 | Security documentation and test results. | Planned |
| ER-21 | Functional and security testing is performed using defined test cases. | Project assignment | Test report containing executed scenarios and results. | Planned |
| ER-22 | The final documentation contains system architecture, data model, use cases, security concept, and evaluation results. | Project assignment | Complete project documentation/PDF report. | Planned |