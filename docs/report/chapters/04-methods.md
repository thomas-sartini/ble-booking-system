# Methods

## Development Approach

The backend concept specifies developing tests alongside code, writing business-rule and security tests first, then implementing and refactoring. T-015 and T-016 are spikes documenting hardware selection and proposed terminal boundaries. These records describe the development approach and design outcomes, not a completed system evaluation.

## Planning and Backlog Management

Requirements and expected results are maintained in `planning/`. The backlog records epics and a user-story index; detailed stories, acceptance criteria, enablers, tasks and spikes are managed as GitHub Issues. A spike is time-boxed research that produces a documented conclusion or recommendation. The [planning register](08-appendices.md#product-backlog-and-project-management-records) embeds the available ID rules and epic records.

## Team Organization and Milestones

<!-- Author: Add agreed responsibilities and milestones. -->

## Version Control and Reviews

The backend concept calls for one feature per branch, local `./mvnw verify`, pull-request CI and review of the important test cases. It also specifies a green pipeline before merge and no automatic deployment. The [full workflow](08-appendices.md#backend-test-plan) preserves these intended rules. The actual workflow file and the unverified branch-protection claim are distinguished in [Chapter 5](05-implementation.md#implementation-discrepancies).

## Testing and Evaluation Method

The backend test plan separates isolated unit tests, HTTP integration tests against PostgreSQL, and architecture checks. The mobile plan uses `kotlin.test`, `kotlinx-coroutines-test` and Mokkery, with mocked communication and protected storage. Terminal checks cover mechanics, electrical stability, BLE, clock backup and restart recovery. The [test-plan appendix](08-appendices.md#test-plans-and-available-evidence) provides the complete available inventories. Chapter 6 reports the available evidence and its limits.
