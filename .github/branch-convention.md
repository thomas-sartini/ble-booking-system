# Branch Convention

Create one short-lived branch for one logical change.

For implementation work linked to a GitHub Task or Spike, use:

```text
<kind>/<collaborator>/<task-id>-<short-description>
```

For planning changes that do not require a GitHub Task or Spike, use:

```text
planning/<collaborator>/<short-description>
```

Use the collaborator's GitHub username in lowercase (for example, `thomas` or `alex`) so the owner is clear.

For task-based branches, include the globally unique task ID (`TASK-101`). The User Story ID stays in the backlog and is not used in the branch name.

| Kind | Example | Use for |
|:--|:--|:--|
| `feature` | `feature/thomas/TASK-101-user-registration` | A user-facing feature |
| `fix` | `fix/alex/TASK-205-overlapping-bookings` | A bug fix |
| `planning` | `planning/thomas/update-product-backlog` | Requirements, expected results, backlog, or other planning changes |
| `docs` | `docs/thomas/TASK-307-report-structure` | Technical or academic documentation |
| `refactor` | `refactor/alex/TASK-408-booking-service` | Behaviour-preserving code work |
| `chore` | `chore/thomas/TASK-509-update-dependencies` | Maintenance |

## Planning branches

Planning-only changes may be made without creating a GitHub Task or Spike first.

Use:

```text
planning/<collaborator>/<short-description>
```

Examples:

```text
planning/thomas/update-requirements
planning/alex/define-expected-results
planning/thomas/refine-product-backlog
planning/alex/update-epics
```

Use a task-based branch instead when the planning work is itself tracked by a GitHub Issue:

```text
planning/thomas/TASK-015-refine-product-backlog
```

Planning branches are intended for changes to files under `planning/`, such as requirements, expected results, epics, user stories, backlog structure, and related planning decisions.

Use lowercase English words in the description, separated by hyphens. Do not use spaces, underscores, dates, or vague descriptions such as `changes` or `updates`.

For task-based branches, keep the exact uppercase `TASK-101` format.

The collaborator segment is the only place for a person's username. Branch kinds and commit types serve different purposes: a `feature/` branch may contain `feat`, `test`, and `docs` commits.

**Never push directly to `main`.** Push only your working branch and open a pull request targeting `main`, including for planning, documentation, or urgent fixes.
