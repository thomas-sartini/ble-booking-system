# Commit Convention

Use [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/). The first line has this format:

```text
<type>(<scope>): <short imperative description>
```

The scope is optional. Keep the subject in English, start the description with a lowercase verb, omit the final period, and aim for a first line of at most 72 characters. Make one logical change per commit.

## Types

| Type | Use for |
|:--|:--|
| `feat` | A new user-facing capability |
| `fix` | A bug fix |
| `docs` | Documentation only |
| `refactor` | Code restructuring without changed behaviour |
| `test` | Tests only |
| `build` | Build tools or dependencies |
| `ci` | CI workflow changes |
| `chore` | Repository maintenance not covered above |

Choose a short scope for the affected area, such as `auth`, `booking`, `billing`, `backend`, `mobile`, `terminal`, `web`, `docs`, or `repo`. Leave it out if no single area fits.

## Longer messages

For a change that needs explanation, add a body after a blank line:

```text
feat(terminal): record NFC check-in

Validate the booking before creating a check-in event.
```

Use `!` and a `BREAKING CHANGE:` footer when an interface or behaviour changes incompatibly:

```text
feat(api)!: change booking response format

BREAKING CHANGE: clients must read resourceId from booking.resource.
```

## Subject examples

| Good | Why |
|:--|:--|
| `docs(report): add validation outline` | Describes a documentation change |
| `test(auth): cover expired login tokens` | Describes tests only |
| `ci(repo): run backend checks on pull requests` | Describes automation |
