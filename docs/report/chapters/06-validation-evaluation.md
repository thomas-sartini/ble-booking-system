# Validation and Evaluation

<!-- Author: For each component, summarize the main tests actually performed, their expected and observed outcomes, and their pass/fail status, using test IDs and supporting evidence. -->

## Test Environment

<!-- Author: Describe the actual test setup: software versions, web browser, Android/iOS devices, terminal hardware, and relevant test data. -->
<!-- Author: State the online, offline, and concurrent-access conditions and the acceptance criteria defined in Chapter 4. -->

## Web Application Tests

<!-- Author: Report tests for login, resource search, booking management, invoices, and administrator screens. -->
<!-- Author: Include checks for role-dependent actions, input validation, and feedback when an operation succeeds or fails. -->

## Backend and Database Tests

<!-- Author: Report tests for accounts, roles, sessions, resources, booking rules, billing, invoice generation, notifications, and audit records. -->
<!-- Author: Include authorization, concurrent booking requests, repeated processing, data integrity, and preservation of usage and invoice history. -->

## Mobile Application Tests

The shared Kotlin Multiplatform core has 19 prepared test cases, all marked `@Ignore` pending implementation. They specify expected behaviour and provide no functional pass/fail results. The [prepared-test inventory](08-appendices.md#prepared-mobile-tests) records their scope, tools and dependencies.

Backend communication and credential renewal have not yet been tested. Native protected storage, application screens and BLE remain untested on Android and iOS; storage mocks do not demonstrate actual credential protection or persistence.

## Door Terminal Tests

<!-- Author: Report tests for BLE message handling, valid and invalid credentials, replay/tampering attempts, door control, and usage-event recording. -->
<!-- Author: Include offline authorization and expiry, durable event storage, restart recovery, and the terminal's behavior when connectivity is unavailable. -->

## Integration and End-to-End Tests

<!-- Author: Demonstrate the complete flow across components: login, reservation or permitted walk-in access, BLE check-in, usage, check-out, billing, and invoice creation. -->
<!-- Author: Report synchronization after offline use, duplicate prevention, conflict handling, and the effect of revoking access across connected and disconnected components. -->

## Requirements and Expected Results Verification

<!-- Author: Include a summary table linking requirement IDs and ER-01 through ER-22 to test/evidence IDs and fulfilment status; explain partially met, unmet, or untested items. -->
<!-- Author: Embed the complete expected-results matrix in Appendix A.2 and the detailed test cases, actual outcomes, and supporting evidence in Appendix A.4. -->

## Overall Evaluation and Limitations

<!-- Author: Summarize what the results demonstrate about correctness, reliability, and practical usability; include performance measurements or user feedback where actually collected. -->
<!-- Author: Discuss remaining defects and limits of the evaluation, such as device coverage, simulated conditions, or untested scenarios, and their effect on the conclusions. -->
The [expected-results matrix](08-appendices.md#expected-results) retains Planned for ER-01 through ER-22. The terminal selection is based on supplier/manufacturer specifications and lists checks still to perform. The available material therefore does not establish successful end-to-end operation, hardware validation or requirements fulfilment.
