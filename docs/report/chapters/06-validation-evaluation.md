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

- *Report tests for shared mobile logic, backend communication, credential renewal, protected storage, and the main application screens.*
- *Present Android/iOS coverage for BLE discovery, connection, check-in/check-out, permissions, disconnections, and access feedback.*

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
