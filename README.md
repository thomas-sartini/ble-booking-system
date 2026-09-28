# BLE Booking System

`ble-booking-system` is a project for the IIP module at Lucerne University of Applied Sciences and Arts (HSLU). It aims to design and prototype a secure system for reserving and using shared resources. Users identify themselves at a terminal via Bluetooth Low Energy (BLE), connecting a reservation to the actual check-in and check-out of a room, device, or other resource.

The project brings together user management, booking, access control, usage tracking, and billing. Its scope and expected outcomes are recorded in the [planning files](planning/README.md); the academic report is written in Markdown and built as [one PDF](docs/README.md).

The original [project assignment](docs/project-assignment.pdf) is kept in `docs/` for reference. This project's implementation focuses on BLE.

## Project status

The repository is being set up for development. The planning files are structures to be completed by the team, and the report chapters contain draft prompts. Features described here are project goals, not a claim that they have been implemented.

## System overview

The intended prototype covers:

- User registration, authentication, and role-based access.
- Resource availability, reservations, and booking management.
- BLE identification at a terminal, with check-in and check-out.
- Usage-based cost calculation, PDF invoices, and payment status.
- Security controls and an audit trail for relevant events.

The final scope, priorities, and verification criteria belong in the [requirements register](planning/requirements.md) and [expected results](planning/expected-results.md).

## Repository layout

| Path | Purpose |
|:--|:--|
| `backend/` | Server-side application and APIs |
| `mobile/` | Mobile application work |
| `terminal/` | BLE terminal integration |
| `web/` | Web application work |
| [`planning/`](planning/README.md) | Requirements, expected results, epics, and user story index |
| [`docs/`](docs/README.md) | Markdown report source and PDF build |

Component-specific setup instructions should be kept in the respective component directories as the implementation takes shape.

## Planning and work tracking

Requirements and expected results are maintained in `planning/`. Epics are described in the [product backlog](planning/product-backlog.md), which also indexes user stories. Detailed user stories, tasks, and spikes are managed as GitHub Issues; tasks and spikes are not duplicated in Markdown.

## Build the report

The report uses Pandoc and XeLaTeX to produce a single PDF. Follow the [report setup guide](docs/README.md) for prerequisites and document structure. From the repository root:

```sh
sh docs/build.sh
```

On Windows PowerShell:

```powershell
.\docs\build.ps1
```

The generated PDF is written to `docs/output/`. The report structure follows the supplied HSLU WIPRO/BAA report guide, valid from autumn semester 2025.
