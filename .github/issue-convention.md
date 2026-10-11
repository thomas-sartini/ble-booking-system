# Issue Convention

GitHub Issues are used for project **Enablers**, **Tasks**, and **Spikes**.

User Stories are maintained in `product-backlog.md` and are **not** created as GitHub Issues.

Use English for issue titles and descriptions.

---

## Issue IDs

### Enabler IDs

Every Enabler uses an ID in the following format:

```text
EN-XX.YY
```

Where:

- `XX` identifies the parent Epic,
- `YY` is assigned sequentially within that Epic.

Examples:

```text
EN-03.01
EN-03.02
EN-06.01
```

Enabler IDs are assigned once and are never reused.

The Enabler ID is independent from the GitHub Issue number.

For example:

```text
EN-03.01
```

may correspond to:

```text
GitHub Issue #27
```

### Task and Spike IDs

Every Task and Spike uses a globally unique task ID in the following format:

```text
T-XXX
```

Examples:

```text
T-001
T-002
T-103
```

Tasks and Spikes share the same global sequential numbering range.

Task IDs are assigned sequentially and are never reused.

The project task ID is independent from the GitHub Issue number. For example, `T-003` may correspond to GitHub Issue `#12`.

A Spike uses the same `T-XXX` ID format as a normal Task and is additionally identified by `[Spike]` in its Issue title.

---

## Issue Titles

### Enabler

Use:

```text
EN-XX.YY <short technical outcome>
```

Example:

```text
EN-03.01 Establish testable mobile foundation
```

### Task

Use:

```text
T-XXX <short imperative description>
```

Example:

```text
T-015 Create initial database migration
```

### Spike

Use:

```text
T-XXX [Spike] <short imperative description>
```

Example:

```text
T-003 [Spike] Define BLE communication flow
```

Keep titles short, specific, and written in English.

---

## Enablers

Use an Enabler when the Sprint should produce a concrete technical outcome that supports current or future product functionality but does not represent a User Story itself.

An Enabler should be scoped so that its outcome can reasonably be completed within a Sprint.

Recommended structure:

```markdown
## Outcome

Describe the concrete technical result that must exist when the Enabler is complete.

## Scope

- Describe the technical area covered by the Enabler
- Keep the scope small enough to complete within the Sprint
- Identify the relevant system components

## Done when

- Define the observable technical completion criteria
- Required project structure or configuration exists
- Relevant automated tests can be executed
- Required documentation or architectural decisions are committed
- The result is committed to the repository when applicable

## Related epic

EP-XX — Epic title

## Related requirements

- FR-XX.XX — Requirement description
- NFR-XX.XX — Requirement description
```

Sections that do not apply may be omitted.

An Enabler may contain or be supported by multiple Tasks and Spikes.

---

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

US-XX.YY — User Story title
```

or:

```markdown
## Related enabler

EN-XX.YY — Enabler title
```

When neither applies, reference the relevant Epic:

```markdown
## Related epic

EP-XX — Epic title
```

Requirements may also be referenced when relevant:

```markdown
## Related requirements

- FR-XX.XX — Requirement description
- NFR-XX.XX — Requirement description
```

Sections that do not apply may be omitted.

---

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

- Expected diagram, model, decision, comparison, or document
- Relevant design decisions
- Assumptions that must be recorded
- Recommendation when applicable

## Out of scope

- Work intentionally excluded from this Spike
- Implementation that will be handled later

## Related backlog item

US-XX.YY — User Story title
```

or:

```markdown
## Related enabler

EN-XX.YY — Enabler title
```

When neither applies, reference the relevant Epic:

```markdown
## Related epic

EP-XX — Epic title
```

Requirements may also be referenced when relevant:

```markdown
## Related requirements

- FR-XX.XX — Requirement description
- NFR-XX.XX — Requirement description
```

Do not use a Spike for implementation work that is already sufficiently understood. In that case, create a normal Task.

---

## Backlog Relationships

Epics and User Stories are defined and maintained in `product-backlog.md`.

GitHub Issues must not be created for User Stories.

Enablers are created and maintained as GitHub Issues and reference their parent Epic.

Each Task or Spike should reference the most specific relevant backlog context:

- reference an Enabler ID when the work directly contributes to that Enabler;
- otherwise, reference a User Story ID when the work directly contributes to that User Story;
- otherwise, reference the relevant Epic ID.

Do not create artificial User Stories only to provide a parent for technical work.

The expected hierarchy is therefore:

```text
Epic
│
├── User Story
│   └── Task / Spike
│
└── Enabler
    └── Task / Spike
```

Tasks and Spikes may reference an Epic directly when no more specific parent is appropriate.

---

## Sprint Assignment

Issues selected for a Sprint must be assigned to the corresponding GitHub Milestone.

Example:

```text
Milestone: Sprint 2
```

The Milestone represents the Sprint.

Do not include the Sprint number in the Issue title.

Enablers selected for a Sprint should be scoped so that their stated outcome can reasonably be completed within that Sprint.

---

## Labels

Use labels to identify the Issue type or technical area when useful.

Examples:

```text
enabler
spike
database
backend
mobile
terminal
web
docs
testing
ci
```

An Enabler must use the `enabler` label.

A Spike must use the `spike` label.

A normal Task does not require a `task` label because the `T-XXX` identifier already identifies it as a project task.

Additional technical-area labels may be added when useful.

<br>
## Closing Issues

Close an Issue only when its stated outcome or completion criteria have been satisfied.

For an Enabler, all completion criteria defined under `Done when` must be satisfied before the Issue is closed.

A parent Enabler should not be closed merely because its child Tasks and Spikes are closed; its own stated technical outcome must also be verified.

If additional work is discovered that is outside the Issue scope, create a new Issue instead of silently expanding the original one.

Assigned IDs are never reused after an Issue is closed, cancelled, superseded, or deleted.
