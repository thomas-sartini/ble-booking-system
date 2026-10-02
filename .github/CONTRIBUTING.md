# Contributing

Use English for issue titles and descriptions, branch names, commit messages, pull requests, and technical documentation.

1. Select one assigned GitHub Issue with a unique task ID (`TASK-101` in the examples), following the [Issue Convention](issue-convention.md). GitHub Issues are used only for Tasks and Spikes; User Stories remain in `product-backlog.md`.
corresponding GitHub Milestone.

2. When the Issue requires repository changes, update your local `main` and create a branch following the [Branch Convention](branch-convention.md), including your GitHub username and the same task ID. A Spike only requires a branch when its outcome produces files or documentation that must be committed.

3. Make focused commits following the [Commit Convention](commit-convention.md). Commit messages do not need task or issue references.

4. Open a pull request targeting `main`. Describe the change and how it was checked.

**Never push directly to `main`.** All changes, including documentation and urgent fixes, go through a branch and a pull request.

