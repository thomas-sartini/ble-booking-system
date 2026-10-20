\appendix

# Supplementary Material {#supplementary-material}

- *Embed the supporting material needed for the final submission in this PDF and refer to the relevant appendices from the main chapters.*

## Full Requirements Register

- *Include the final requirements.md content: all functional and non-functional requirements, IDs, priorities, and versions.*
- *Record agreed clarifications or changes, including the BLE-only scope and the finalized booking, billing, and offline-access policies.*

## Full Expected Results and Verification Matrix

- *Include the final expected-results.md content with ER-01 through ER-22, sources, verification evidence, and final statuses.*
- *Explain any approved changes or results that are partially achieved, unmet, or unverified.*

## Product Backlog and Project Management Records

- *Include the full epic and user-story register, with descriptions, acceptance criteria, requirement mappings, and final statuses.*
- *Include relevant planning conventions, identifier rules, responsibilities, milestones, and task outcomes directly in the PDF.*

## Complete Test Plan and Test Report

- *Include detailed test cases by component and for integration, with test IDs, requirement/result mappings, setup, steps, expected and actual outcomes, and statuses.*
- *Embed the supporting evidence, such as screenshots, relevant logs, invoice samples, measurements, and remaining defect records.*

## Installation, Operation, and User Guides

- *Include complete instructions for building, configuring, starting, and connecting the system, including terminal provisioning.*
- *Explain the main Web, Mobile, and Admin operations so the delivered prototype can be reproduced and demonstrated.*

## Detailed Security Documentation

The following proposals support the BLE Access and Security Concept and Offline Operation and Synchronization sections. They define how saved receipts can be recovered and synchronized. The final cryptographic mechanisms, record retention, and automatic retry policy still require team agreement.

### BLE Recovery Proposal

This proposal details the read-only recovery used for an unknown BLE result and adds limited retries. The retry rules need team agreement before implementation.

#### Read-only recovery

Recovery asks the terminal for a saved receipt, using the original device key that was linked to the event. It can work after the ticket has expired. It does not extend permission, create an event, or grant access.

![Read-only recovery of a BLE receipt](../assets/figures/ble-recovery-flow.png){width=100%}

The recovery proof covers a fresh challenge, the purpose "recovery", the operation ID, the terminal, and the protected session. A proof from a normal check-in or check-out therefore cannot be used for recovery, and a recovery proof cannot be used for an action. The terminal checks the proof before it shows any receipt details. It returns the same generic "unavailable" answer whether the record is missing or the proof fails, and it limits attempts. A different device does not automatically get the receipt; a lost device key needs a separate recovery path.

#### Limited retries

If recovery is unavailable, the app may retry the original action a limited number of times. It uses the same operation ID, a valid ticket, and a fresh device proof at the original terminal. The terminal returns the original receipt if the action was already saved, or saves it once if it is still allowed.

- The same operation ID cannot be used with a different booking, resource, action, terminal, or device key.
- A replacement ticket for the same action keeps the operation ID. Only its ticket ID and validity period change. It can be issued only while booking, time, and device are still eligible.
- A rejection is final only if the terminal has checked its complete, reliable saved history and no earlier attempt can still be saved. A rejection before that check, such as an invalid ticket or a failed proof, says nothing about the earlier attempt.

#### When the terminal cannot help

- If a reset or storage loss makes the terminal's saved state unreliable, it blocks all new actions until that state is restored.
- If the record of an operation is older than the agreed retention period, only this operation stays unresolved. A missing record does not prove that it never happened. It is resolved through the backend or an Admin before any retry.

### BLE Synchronization Proposal

The backend proposal uses the smartphone to carry signed receipts from the terminal to the backend. This proposal adds an acknowledgment back to the terminal, as a proposal for FR-05.04 and FR-05.05.

1. The terminal and the app keep events and receipts until they are synchronized. When the smartphone is online, the app uploads the original receipt over an authenticated, encrypted connection.
2. The backend checks the terminal, the signature, the event details, and the order of check-in and check-out. It uses the event ID to process each event once, without duplicate usage records or charges.
3. The backend signs its result: accepted, already accepted, or conflict. The result names the event and the terminal it belongs to. The app carries it back, and the terminal checks the backend signature and that the result matches one of its own stored events.
4. Only "accepted" or "already accepted" marks an event as synchronized. Conflicts are kept for Admin review. A rejected upload does not delete the terminal event or undo access already granted.

If no smartphone is available, events stay queued. If the queue is full, the terminal rejects new actions that it cannot save; it never deletes pending events to make room.
