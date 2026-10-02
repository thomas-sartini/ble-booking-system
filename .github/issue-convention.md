# Issue Convention

GitHub Issues are used only for project **Tasks** and **Spikes**.

User Stories are maintained in `product-backlog.md` and are **not** created as GitHub Issues.

Use English for issue titles and descriptions.

## Task IDs

Every Task and Spike uses a globally unique task ID in the following format:

```text
TASK-XXX
```

Examples:

```text
TASK-001
TASK-002
TASK-103
```

Task IDs are assigned sequentially and are never reused.

The project task ID is independent from the GitHub issue number. For example, `TASK-003` may correspond to GitHub Issue `#12`.

## Issue titles

### Task

Use:

```text
TASK-XXX <short imperative description>
```

Example:

```text
TASK-015 Create initial database migration
```

### Spike

Use:

```text
TASK-XXX [Spike] <short imperative description>
```

Example:

```text
TASK-003 [Spike] Define BLE communication flow
```

Keep titles short, specific, and written in English.


## Tasks

Use a Task when the required work is sufficiently understood and can be implemented directly.

Recommended structure:

```markdown
## Goal

Describe what this task should accomplish.

## Work

- Describe the main work required
- Keep implementation steps concise
- Include only work relevant to this issue

## Done when

- Define the observable completion criteria
- Add tests or documentation when required
- Ensure the result is committed to the repository

## Related backlog item

US-XXX — User Story title

## Related requirements

- FR-XX.XX — Requirement description
- NFR-XX.XX — Requirement description
```

If the Task does not belong to a User Story, reference the relevant Epic instead:

```markdown
## Related epic

EP-XX — Epic title
```

Sections that do not apply may be omitted.

## Spikes

Use a Spike when technical uncertainty must be reduced before implementation.

A Spike is a time-boxed investigation with a specific question and a documented outcome.

Recommended structure:

```markdown
## Goal

Describe what knowledge, model, or design decision the Spike should produce.

## Question

State the technical question that must be answered.

## Expected outcome

- Expected diagram, model, decision, or document
- Relevant design decisions
- Assumptions that must be recorded

## Out of scope

- Work intentionally excluded from this Spike
- Implementation that will be handled later

## Related backlog item

US-XXX — User Story title

## Related requirements

- FR-XX.XX — Requirement description
- NFR-XX.XX — Requirement description
```

If the Spike does not belong to a User Story, reference the relevant Epic instead:

```markdown
## Related epic

EP-XX — Epic title
```

Do not use a Spike for implementation work that is already sufficiently understood. In that case, create a normal Task.

## Backlog relationships

Epics and User Stories are defined and maintained in `product-backlog.md`.

GitHub Issues must not be created for User Stories.

Each Task or Spike should reference its relevant backlog context:

- reference a User Story ID when the work directly contributes to that User Story;
- otherwise, reference the relevant Epic ID.

Do not create artificial User Stories only to provide a parent for technical work.

## Sprint assignment

Issues selected for a Sprint must be assigned to the corresponding GitHub Milestone.

Example:

```text
Milestone: Sprint 1
```

The Milestone represents the Sprint. Do not include the Sprint number in the issue title.

## Labels

Use labels to identify the issue type or technical area when useful.

Examples:

```text
spike
database
backend
mobile
terminal
web
docs
```

A Spike must use the `spike` label.

A normal Task does not require a `task` label because the `TASK-XXX` identifier already identifies it as a project task.

## Branches

When an Issue requires repository changes, create a branch following the project Branch Convention.

The branch must contain the same task ID as the Issue.

Example:

```text
Issue:
TASK-015 Create initial database migration

Branch:
feature/thomas/TASK-015-database-migration
```

A Spike requires a branch only when its outcome produces files or documentation that must be committed to the repository.

## Closing Issues

Close an Issue only when its stated outcome or completion criteria have been satisfied.

If additional work is discovered that is outside the Issue scope, create a new Issue instead of silently expanding the original one.
