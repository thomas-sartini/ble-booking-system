# Problem, Research Question, and Vision

## Background and Problem Statement

The BLE Booking System is an IIP project at Lucerne University of Applied Sciences and Arts (HSLU). It aims to connect reservations of shared rooms or equipment with secure identification, actual resource use, and billing. The project combines user management, booking, access control, usage tracking and an audit trail.

## Project Objectives and Questions

The objective is to design and prototype secure resource reservation and BLE check-in/check-out, with usage-based cost calculation, PDF invoices and payment status. The expected outcomes and their proposed verification are recorded in the [expected-results register](08-appendices.md#expected-results).

<!-- Author: Formulate the agreed research questions. -->

## Target Users and Roles

The requirements identify Student, Lecturer and Admin. Students and Lecturers use permitted resources; Admins manage accounts, resources, payment status and audit records. The backend uses the identifiers `STUDENT`, `STAFF` and `ADMIN`; the unresolved terminology is recorded with the [integration conflicts](03-ideas-and-concepts.md#integration-conflicts).

## Scope and Constraints

The intended system comprises a web application, native Android and iOS applications with a shared Kotlin Multiplatform core, a backend, PostgreSQL and a BLE terminal. NFC is excluded by FR-05.01. The terminal prototype is an equipment lending compartment based on the selected Arduino Nano ESP32, within a CHF 200 hardware budget. The terminal checks permissions locally; the smartphone carries tickets and receipts between it and the backend.

## Requirements Overview

The functional requirements cover accounts and sessions, web and mobile access, resources and availability, booking lifecycle, BLE and offline operation, billing, email and audit records. Security requirements cover authenticated encrypted transport, password hashing, credential expiry and revocation, replay resistance, bounded offline authorization and restricted data access.

The register distinguishes Must requirements from Optional password reset (FR-01.05) and mobile resource/booking information (FR-03.04). The [complete register](08-appendices.md#full-requirements-register) preserves the identifiers, priorities and versions. All ER-01 through ER-22 remain recorded as Planned; their status is not a claim of delivery.
