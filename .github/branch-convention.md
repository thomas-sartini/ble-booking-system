# Branch Convention

Create one short-lived branch for one assigned task. Use:

```text
<kind>/<collaborator>/<task-id>-<short-description>
```

Use the collaborator's GitHub username in lowercase (for example, `thomas` or `alex`) so the owner is clear. Every branch includes its globally unique task ID (`TASK-101`), regardless of whether the work is a feature, fix, or documentation change. The user story ID stays in the backlog and task issue. The exact task-number ranges will be defined in the backlog; these examples are illustrative.

| Kind | Example | Use for |
|:--|:--|:--|
| `feature` | `feature/thomas/TASK-101-user-registration` | A user-facing feature |
| `fix` | `fix/alex/TASK-205-overlapping-bookings` | A bug fix |
| `docs` | `docs/thomas/TASK-307-report-structure` | Documentation |
| `refactor` | `refactor/alex/TASK-408-booking-service` | Behaviour-preserving code work |
| `chore` | `chore/thomas/TASK-509-update-dependencies` | Maintenance |

Use lowercase English words in the description, separated by hyphens. Keep the exact uppercase `TASK-101` format for task IDs. Do not use spaces, underscores, dates, or vague descriptions such as `changes` or `new-feature`. The collaborator segment is the only place for a person's username. Branch kinds and commit types serve different purposes: a `feature/` branch may contain `feat`, `test`, and `docs` commits.

**Never push directly to `main`.** Push only your working branch and open a pull request targeting `main`, including for documentation or urgent fixes.
