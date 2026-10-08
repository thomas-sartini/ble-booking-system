# Branch Convention

Create one short-lived branch for one logical change.

For implementation work linked to a GitHub Task or Spike, use:

```text
<kind>/<collaborator>/<task-id>-<short-description>
```

For planning or documentation changes that do not require a GitHub Task or Spike, use:

```text
planning/<collaborator>/<short-description>
docs/<collaborator>/<short-description>
```

Use the collaborator's GitHub username in lowercase (for example, `thomas` or `alex`) so the owner is clear.

For task-based branches, include the globally unique task ID (`T-101`). The User Story ID stays in the backlog and is not used in the branch name.

| Kind | Example | Use for |
|:--|:--|:--|
| `feature` | `feature/thomas/T-101-user-registration` | A user-facing feature |
| `fix` | `fix/alex/T-205-overlapping-bookings` | A bug fix |
| `planning` | `planning/thomas/update-product-backlog` | Requirements, expected results, backlog, or other planning changes |
| `docs` | `docs/thomas/documentation-outline` | Technical or academic documentation, with or without a Task |
| `refactor` | `refactor/alex/T-408-booking-service` | Behaviour-preserving code work |
| `chore` | `chore/thomas/T-509-update-dependencies` | Maintenance |

## Planning and documentation branches

Planning-only and documentation-only changes may be made without creating a GitHub Task or Spike first when the work is not tracked by an Issue.

Use:

```text
planning/<collaborator>/<short-description>
docs/<collaborator>/<short-description>
```

Examples:

```text
planning/thomas/update-requirements
planning/alex/define-expected-results
planning/thomas/refine-product-backlog
planning/alex/update-epics
docs/thomas/documentation-outline
docs/alex/update-user-guide
```

Use a task-based branch instead when the planning or documentation work is tracked by a GitHub Issue:

```text
planning/thomas/T-015-refine-product-backlog
docs/thomas/T-307-report-structure
```

Planning branches are intended for changes to files under `planning/`, such as requirements, expected results, epics, user stories, backlog structure, and related planning decisions.

Documentation branches are intended for technical or academic documentation, such as report chapter outlines, documentation structure, and content updates. Do not create a Task solely to name a documentation branch.

Use lowercase English words in the description, separated by hyphens. Do not use spaces, underscores, dates, or vague descriptions such as `changes` or `updates`.

For task-based branches, keep the exact uppercase `T-101` format.

The collaborator segment is the only place for a person's username. Branch kinds and commit types serve different purposes: a `feature/` branch may contain `feat`, `test`, and `docs` commits.

**Never push directly to `main`.** Push only your working branch and open a pull request targeting `main`, including for planning, documentation, or urgent fixes.
