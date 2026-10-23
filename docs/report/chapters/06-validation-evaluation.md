# Validation and Evaluation

- *For each component, summarize the main tests actually performed, their expected and observed outcomes, and their pass/fail status, using test IDs and supporting evidence.*

## Test Environment

- *Describe the actual test setup: software versions, web browser, Android/iOS devices, terminal hardware, and relevant test data.*
- *State the online, offline, and concurrent-access conditions and the acceptance criteria defined in Chapter 4.*

## Web Application Tests

- *Report tests for login, resource search, booking management, invoices, and administrator screens.*
- *Include checks for role-dependent actions, input validation, and feedback when an operation succeeds or fails.*

## Backend and Database Tests

- *Report tests for accounts, roles, sessions, resources, booking rules, billing, invoice generation, notifications, and audit records.*
- *Include authorization, concurrent booking requests, repeated processing, data integrity, and preservation of usage and invoice history.*

## Mobile Application Tests

Automated unit tests have been prepared for the shared Kotlin Multiplatform core using `kotlin.test`, Kotlin’s standard multiplatform testing library. Its platform-independent annotations and assertions allow test code to be shared between Android and iOS. `kotlinx-coroutines-test` provides official support for testing suspending operations through `runTest`. Mokkery was selected for dependency mocking because it supports Kotlin Multiplatform and suspending methods.

Mocks replace dependencies with controlled responses, allowing each class to be tested independently. Application services use a mocked `CommunicationGateway`, while `TokenManager` uses a mocked `SecureStorage`. Tests can therefore specify successful results and errors without a running backend or native storage.

The current suite contains **19 test cases**:

- **AuthService:** four cases for login, logout, and error propagation.
- **BookingService:** three cases for empty and populated booking lists and communication errors.
- **AccessService:** six cases for access requests, confirmations, check-out, and related errors.
- **TokenManager:** six cases for credential retrieval, storage, replacement, and clearing.

All cases are currently marked `@Ignore` pending implementation. They specify expected behaviour but do not yet provide functional pass/fail results. `ServiceFacade` has no dedicated unit tests because, as a facade, it only forwards calls to the corresponding service methods.

Backend communication and credential renewal have not yet been tested. These tests are deferred until the REST requests and responses are defined and implemented. HTTP responses will then be simulated to verify client behaviour independently of a running backend.

Native protected storage, application screens, and BLE functionality remain untested on both Android and iOS. Planned platform tests cover discovery, connection, check-in/check-out, permissions, disconnections, and access feedback. The storage mocks do not verify actual credential protection or persistence on either platform.
## Door Terminal Tests

- *Report tests for BLE message handling, valid and invalid credentials, replay/tampering attempts, door control, and usage-event recording.*
- *Include offline authorization and expiry, durable event storage, restart recovery, and the terminal's behavior when connectivity is unavailable.*

## Integration and End-to-End Tests

- *Demonstrate the complete flow across components: login, reservation or permitted walk-in access, BLE check-in, usage, check-out, billing, and invoice creation.*
- *Report synchronization after offline use, duplicate prevention, conflict handling, and the effect of revoking access across connected and disconnected components.*

## Requirements and Expected Results Verification

- *Include a summary table linking requirement IDs and ER-01 through ER-22 to test/evidence IDs and fulfilment status; explain partially met, unmet, or untested items.*
- *Embed the complete expected-results matrix in Appendix A.2 and the detailed test cases, actual outcomes, and supporting evidence in Appendix A.4.*

## Overall Evaluation and Limitations

- *Summarize what the results demonstrate about correctness, reliability, and practical usability; include performance measurements or user feedback where actually collected.*
- *Discuss remaining defects and limits of the evaluation, such as device coverage, simulated conditions, or untested scenarios, and their effect on the conclusions.*
