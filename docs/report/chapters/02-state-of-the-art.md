# State of Research and Practice

- *Briefly introduce existing solutions and technical foundations relevant to the project, using reliable sources.*

## Existing Booking and Access-Control Systems

- *Present a few relevant examples of systems for resource reservation, physical access, or usage tracking.*
- *Summarize their main features and how they connect booking, access, and billing.*

## BLE Communication

- *Explain the BLE basics needed to understand smartphone-to-terminal communication: discovery, connection, and message exchange.*
- *Briefly distinguish BLE from NFC and relate the discussion to the project's BLE-only scope.*

## Authentication and Security

- *Introduce authentication, role-based authorization, password hashing, session management, and protected mobile credential storage.*
- *Summarize established approaches to secure BLE access, especially credential verification and protection against replay attacks.*

## Offline Access and Synchronization

- *Explain common approaches to local authorization when a terminal cannot reach the backend, including limited credential validity.*
- *Introduce queued usage events, later synchronization, duplicate prevention, and conflict handling.*

## Cross-Platform Mobile Development

- *Introduce the approach of sharing application logic while using native Android and iOS adapters for BLE and protected storage.*
- *Briefly describe Kotlin Multiplatform as a candidate approach relevant to the proposed Shared Mobile Core.*

## Relevance to the Project

- *Summarize which existing approaches and technical foundations the project can build on.*
- *Identify the main gaps or challenges the prototype will address; develop the chosen solution in Chapter 3.*
