# Ideas and Concepts

- *Present the proposed solution, component designs, and main design choices.*

## Use Cases

### Web Application

- *Include a Use Case Diagram for resource search, booking management, invoice viewing, and administration through the web application.*
- *Show which actions are available to Student, Lecturer, and Admin.*

### Mobile Application

- *Include a Use Case Diagram for login, viewing the current booking or access entitlement, BLE identification, and check-in/check-out.*
- *Show the relevant user roles and distinguish required mobile functions from optional functions.*

## General System Flow

- *Show the overall process from login and booking to BLE check-in, resource use, check-out, and billing.*
- *Include permitted access without a prior booking and indicate where offline operation affects the process.*

## Proposed System Architecture

- *Include a conceptual component diagram with Web, Backend, Database, Shared Mobile Core, Android, iOS, and Door Terminal controlling the door.*
- *Briefly explain each component's responsibility, the proposed module boundaries, and the connections through REST/HTTPS, shared mobile interfaces, and BLE.*

## Backend Design

### Module Structure

- *Show the proposed backend modules, such as accounts, resources, bookings, access, billing, notifications, and audit.*
- *Describe each module's responsibility, dependencies, and main interfaces with other modules.*

### Class Structure

- *Include a proposed Class Diagram showing important domain classes, controllers, services, and repositories, according to the chosen design.*
- *Briefly describe their responsibilities and relationships, focusing on the main classes.*

## Mobile Application Design

### Purpose and Scope

This document defines the current architecture of the mobile application and serves as the architectural baseline for the implementation phase.

The design is based on Kotlin Multiplatform (KMP) and focuses on the shared Mobile Core, its internal modules, and the boundaries to the native Android and iOS applications and the backend.

The internal architecture of the native applications and the BLE protocol are not yet defined and remain TODO items. The backend implementation is outside the scope of this mobile architecture section.

### Component Architecture

The mobile application is structured around a shared Kotlin Multiplatform core, native Android/iOS applications, and the backend.

#### Module Structure

The following diagram summarizes the module boundaries between the shared Kotlin Multiplatform core, the native applications, and the backend. It reflects the component responsibilities described below without introducing platform-specific implementation details.

The shared `Mobile Core` contains application logic, backend communication, and token lifecycle management. Android and iOS consume the shared core through `MobileCoreApi` and remain responsible for native platform functionality. `SecureStorage` is defined as a shared interface and implemented by the native platforms.

The following diagrams summarize the component-level architecture and the module boundaries described above.

![Mobile Application Component Architecture](assets/mobile-component-diagram.png){width=72%}



#### Mobile Core

The `Mobile Core` contains the shared Kotlin Multiplatform logic used by both Android and iOS. Its three components are summarized below.

| Component | Main Responsibilities | Interfaces |
|:--|:--|:--|
| `MobileCoreApplication` | Provide the shared application entry point; coordinate authentication, booking, and resource-access logic; keep platform-specific code separated from shared business logic. | **Provides:** `MobileCoreApi`; **Requires:** `CommunicationGateway` |
| `BackendCommunication` | Perform REST communication; isolate HTTP and transport-specific details; convert between application models and DTOs; attach authentication credentials; coordinate login, token refresh, and logout; use `TokenManager` for locally managed credentials. | **Provides:** `CommunicationGateway`; **Requires:** `REST/API` |
| `TokenManager` | Maintain the short-lived access token; store and retrieve the long-lived refresh token; update and clear locally managed credentials; keep storage independent from platform-specific APIs. | **Provides:** none; **Requires:** `SecureStorage` |

#### External Components

The following components interact with the Mobile Core but are not part of it. Their internal architecture is not modeled in detail at this stage.

| Component | Main Responsibilities | Interfaces |
|:--|:--|:--|
| `Backend` | Provide backend functionality required by the mobile application; authenticate requests; issue, renew, and revoke authentication credentials; provide booking and access-related data; process access and check-out operations. | **Provides:** `REST/API`; **Requires:** none |
| `AndroidApp` | **TODO:** define the native Android architecture, platform-specific responsibilities, protected-storage implementation, and BLE integration. | **TODO** |
| `IOSApp` | **TODO:** define the native iOS architecture, platform-specific responsibilities, protected-storage implementation, and BLE integration. | **TODO** |

#### Native Platform Design — TODO

The internal architecture of the native Android and iOS applications has not yet been defined. The native UI structure, BLE integration, platform-specific protected storage, key management, and concrete implementations of shared interfaces will be documented in a later design step.

No native class diagram is included at this stage.


### Detailed Module Design

This section describes the internal structure of the Mobile Core components.

The class diagrams represent the current architectural design and define the main classes, dependencies, and interfaces between the modules.

The diagrams intentionally focus on architectural responsibilities rather than implementation details. Data fields, error models, and platform-specific implementations may be refined during implementation.

#### MobileCoreApplication

The `MobileCoreApplication` module contains the shared application-level logic. It is the entry point used by the native Android and iOS applications and coordinates the specialized services responsible for authentication, bookings, and resource access.

![Shared Mobile Core Class Structure](assets/mobile-core-classes.png){width=95%}

##### MobileCoreApi


###### Purpose {.unnumbered}

`MobileCoreApi` defines the public contract through which the native Android and iOS applications access the shared Mobile Core.

###### Responsibilities {.unnumbered}

- expose the functionality required by the native applications,
- hide the internal organization of the `MobileCoreApplication` module,
- provide a stable boundary between platform-specific code and shared application logic.

###### Dependencies {.unnumbered}

The interface does not depend directly on internal services. Its implementation is provided by `ServiceFacade`.

###### Interface Operations {.unnumbered}


| Operation | Description |
|---|---|
| `login(identifier: String, password: String)` | Starts the authentication flow using the supplied user identifier and password. Authentication credentials returned by the backend remain internal to the Mobile Core. |
| `logout()` | Ends the current session and triggers backend revocation and local credential cleanup. |
| `getActiveBookings() List<Booking>` | Retrieves the active bookings available to the current user. |
| `requestAccess(request: ResourceAccessRequest) ResourceAccessResponse` | Requests authorization to access a resource using an application-level access request. |
| `confirmAccess(confirmation: AccessConfirmation)` | Confirms the result of a previously initiated resource-access operation. |
| `checkOut(resourceId: String)` | Initiates the check-out flow for the specified resource. |

The interface uses application models rather than transport DTOs. This keeps HTTP-specific representations outside the public Mobile Core API.

##### ServiceFacade


###### Purpose {.unnumbered}

`ServiceFacade` is the concrete implementation of `MobileCoreApi` and acts as the single entry point to the services contained in the `MobileCoreApplication` module.

###### Responsibilities {.unnumbered}

- implement the `MobileCoreApi` contract,
- delegate authentication operations to `AuthService`,
- delegate booking operations to `BookingService`,
- delegate resource-access operations to `AccessService`,
- prevent native applications from depending directly on individual application services.

###### Dependencies {.unnumbered}

- `AuthService`
- `BookingService`
- `AccessService`

##### AuthService


###### Purpose {.unnumbered}

`AuthService` contains the application-level logic related to authentication and session lifecycle.

###### Responsibilities {.unnumbered}

- coordinate user login,
- coordinate logout,
- delegate authentication-related backend operations through `CommunicationGateway`,
- remain independent from token storage and refresh implementation details.

###### Dependencies {.unnumbered}

- `CommunicationGateway`

##### BookingService


###### Purpose {.unnumbered}

`BookingService` contains the application logic required to retrieve booking information.

###### Responsibilities {.unnumbered}

- request active booking information,
- expose booking data as application models,
- keep booking-related backend communication outside the native applications.

###### Dependencies {.unnumbered}

- `CommunicationGateway`

##### AccessService


###### Purpose {.unnumbered}

`AccessService` coordinates the application-level resource access lifecycle.

###### Responsibilities {.unnumbered}

- request access to a resource,
- process access confirmation,
- initiate resource check-out,
- delegate backend-related operations through `CommunicationGateway`.

###### Dependencies {.unnumbered}

- `CommunicationGateway`





#### BackendCommunication

The `BackendCommunication` module provides the boundary between the application layer and the backend REST API.

It separates application-level models and operations from HTTP communication, authentication transport, and DTO-specific representations.


![Backend Communication Class Structure](assets/mobile-communication-classes.png){width=100%}

##### CommunicationGateway


###### Purpose {.unnumbered}

`CommunicationGateway` defines the application-facing contract for backend communication.

It allows the `MobileCoreApplication` module to perform backend operations without depending on HTTP clients, REST endpoints, DTO representations, or token-management details.

###### Responsibilities {.unnumbered}

- expose backend-related capabilities using application-level types,
- isolate the `MobileCoreApplication` module from transport-specific details,
- hide authentication credential handling from application services,
- define the boundary between application logic and backend communication.

###### Dependencies {.unnumbered}

The interface itself has no concrete infrastructure dependency. It is implemented by `BackendCommunication`.

###### Interface Operations {.unnumbered}


| Operation | Description |
|---|---|
| `login(identifier: String, password: String)` | Authenticates the user through the backend. Returned credentials are handled internally by `BackendCommunication` and `TokenManager`. |
| `logout()` | Terminates the current session, requests backend-side revocation, and clears locally managed credentials. |
| `getActiveBookings() List<Booking>` | Retrieves active bookings and exposes them as application `Booking` models. |
| `requestAccess(request: ResourceAccessRequest) ResourceAccessResponse` | Sends a resource-access request and returns the corresponding application-level response. |
| `confirmAccess(confirmation: AccessConfirmation)` | Sends an access confirmation to the backend. |
| `checkOut(resourceId: String)` | Requests completion of resource usage for the specified resource. |

No DTO or token types are exposed through this interface. DTO creation, authentication headers, token refresh, and credential persistence remain internal to the communication and token-management modules.

##### BackendCommunication


###### Purpose {.unnumbered}

`BackendCommunication` implements `CommunicationGateway` and acts as the adapter between application-level operations and the REST client.

It also coordinates authentication-related communication without exposing token mechanics to the application layer.

###### Responsibilities {.unnumbered}

- implement the `CommunicationGateway` contract,
- coordinate calls to `BackendApiClient`,
- convert application models to DTOs before sending requests,
- convert received DTOs back into application models,
- store credentials returned by a successful login through `TokenManager`,
- obtain access and refresh tokens through `TokenManager`,
- refresh the access token when required,
- retry an authenticated operation after a successful refresh,
- clear local credentials when logout completes or renewal is no longer possible.

###### Dependencies {.unnumbered}

- `BackendApiClient`
- `TokenManager`

###### Authentication Behaviour {.unnumbered}


###### Login {.unnumbered}

`BackendCommunication.login()` creates a `LoginRequestDto` from the supplied identifier and password and delegates the HTTP request to `BackendApiClient`.

After successful authentication, the returned `LoginResponseDto` contains the access and refresh credentials. `BackendCommunication` passes these credentials to `TokenManager.storeTokens(...)`. The password is not persisted by the Mobile Core.

###### Token Refresh {.unnumbered}

Token refresh is an internal communication concern and is not exposed through `CommunicationGateway`.

When an authenticated request cannot use the current access token because it is unavailable or rejected as expired, `BackendCommunication` retrieves the refresh token from `TokenManager`, creates a `RefreshTokenRequestDto`, and calls `BackendApiClient.refresh(...)`.

If the refresh succeeds, the returned credentials are stored through `TokenManager` and the original operation may be retried once using the new access token.

If the refresh token is rejected, expired, or revoked, the local credentials are cleared and the authentication failure is propagated to the application layer.

###### Logout {.unnumbered}

During logout, `BackendCommunication` obtains the refresh token and requests backend-side session revocation through `BackendApiClient.logout(...)`. Local credentials are then cleared through `TokenManager`.

##### BackendApiClient


###### Purpose {.unnumbered}

`BackendApiClient` is the low-level REST communication client.

###### Responsibilities {.unnumbered}

- execute HTTP requests,
- send and receive transport DTOs,
- send the access token with protected requests,
- perform login, refresh, and logout transport operations,
- isolate HTTP-specific communication from the rest of the Mobile Core.

###### Dependencies {.unnumbered}

- `HttpClient`

The concrete HTTP client configuration is an implementation concern and is not further defined by this class diagram.

##### LoginRequestDto


###### Purpose {.unnumbered}

`LoginRequestDto` represents the transport data sent to the backend during login.

###### Responsibilities {.unnumbered}

- represent the login request in the REST communication layer,
- carry the supplied identifier and password to the authentication endpoint,
- provide a serializable transport representation for authentication data.

###### Dependencies {.unnumbered}

None are defined in the current architecture.

##### LoginResponseDto


###### Purpose {.unnumbered}

`LoginResponseDto` represents the transport response returned by the backend after successful authentication.

###### Responsibilities {.unnumbered}

- represent the authentication response,
- carry the short-lived access token and long-lived refresh token returned by the backend,
- provide the credentials that are passed to `TokenManager`.

###### Dependencies {.unnumbered}

None are defined in the current architecture.

##### RefreshTokenRequestDto


###### Purpose {.unnumbered}

`RefreshTokenRequestDto` represents the transport request used to renew authentication credentials.

###### Responsibilities {.unnumbered}

- carry the current refresh token to the backend refresh endpoint,
- keep refresh-specific transport data inside the communication layer.

###### Dependencies {.unnumbered}

None are defined in the current architecture.

##### RefreshTokenResponseDto


###### Purpose {.unnumbered}

`RefreshTokenResponseDto` represents the transport response returned after a successful token refresh.

###### Responsibilities {.unnumbered}

- carry the new access token,
- carry a replacement refresh token when refresh-token rotation is applied,
- provide the credentials used to update `TokenManager`.

###### Dependencies {.unnumbered}

None are defined in the current architecture.

##### BookingDto


###### Purpose {.unnumbered}

`BookingDto` represents booking information exchanged with the backend.

###### Responsibilities {.unnumbered}

- represent booking data in the transport layer,
- provide the source representation used when converting backend data into the application `Booking` model.

###### Dependencies {.unnumbered}

None are defined in the current architecture.

##### ResourceAccessRequestDto


###### Purpose {.unnumbered}

`ResourceAccessRequestDto` represents a resource-access request sent through the REST API.

###### Responsibilities {.unnumbered}

- represent resource-access information in transport form,
- keep the backend request representation separate from the application model.

###### Dependencies {.unnumbered}

None are defined in the current architecture.

##### ResourceAccessResponseDto


###### Purpose {.unnumbered}

`ResourceAccessResponseDto` represents the backend response to a resource-access request.

###### Responsibilities {.unnumbered}

- represent the REST response for an access request,
- provide the transport representation used to create a `ResourceAccessResponse`.

###### Dependencies {.unnumbered}

None are defined in the current architecture.

##### AccessConfirmationRequestDto


###### Purpose {.unnumbered}

`AccessConfirmationRequestDto` represents the transport request used to confirm a resource-access operation.

###### Responsibilities {.unnumbered}

- represent access-confirmation data sent to the backend,
- isolate the REST representation from the application-level `AccessConfirmation` model.

###### Dependencies {.unnumbered}

None are defined in the current architecture.

##### CheckOutRequestDto


###### Purpose {.unnumbered}

`CheckOutRequestDto` represents a check-out request in the REST communication layer.

###### Responsibilities {.unnumbered}

- represent check-out information in transport form,
- provide the DTO required by `BackendApiClient` when sending a check-out request.

###### Dependencies {.unnumbered}

None are defined in the current architecture.

##### Booking


###### Purpose {.unnumbered}

`Booking` is the application-level representation of a booking.

###### Responsibilities {.unnumbered}

- represent booking information independently from the REST API,
- provide a shared model that can be used by the `MobileCoreApplication` module.

###### Dependencies {.unnumbered}

None are defined in the current architecture.

##### ResourceAccessRequest


###### Purpose {.unnumbered}

`ResourceAccessRequest` represents an application-level request to access a resource.

###### Responsibilities {.unnumbered}

- contain the information required by the application access flow,
- keep the `MobileCoreApplication` module independent from `ResourceAccessRequestDto`.

###### Dependencies {.unnumbered}

None are defined in the current architecture.

##### ResourceAccessResponse


###### Purpose {.unnumbered}

`ResourceAccessResponse` represents the application-level result of a resource-access request.

###### Responsibilities {.unnumbered}

- expose the access result to the `MobileCoreApplication` module,
- hide the backend-specific `ResourceAccessResponseDto` representation.

###### Dependencies {.unnumbered}

None are defined in the current architecture.

##### AccessConfirmation


###### Purpose {.unnumbered}

`AccessConfirmation` represents an application-level confirmation of a resource-access operation.

###### Responsibilities {.unnumbered}

- represent confirmation data inside the Mobile Core,
- keep access-confirmation logic independent from the REST DTO representation.

###### Dependencies {.unnumbered}

None are defined in the current architecture.



#### TokenManager

The `TokenManager` module encapsulates local authentication credential handling and separates credential storage from backend communication.

The module does not perform REST requests. Token renewal is coordinated by `BackendCommunication`, while `TokenManager` is responsible for storing, retrieving, updating, and clearing credentials.

![Token Management Class Structure](assets/mobile-token-classes.png){width=80%}


##### TokenManager


###### Purpose {.unnumbered}

`TokenManager` manages the authentication credentials used by the Mobile Core without performing backend communication.

###### Responsibilities {.unnumbered}

- maintain the short-lived access token in memory,
- retrieve the current access token when required,
- persist the long-lived refresh token through `SecureStorage`,
- retrieve the refresh token when renewal or logout requires it,
- replace locally managed credentials after login or successful refresh,
- clear both access and refresh credentials when the session ends.

###### Dependencies {.unnumbered}

- `SecureStorage`

###### Operations {.unnumbered}


| Operation | Description |
|---|---|
| `getAccessToken() String?` | Returns the currently available in-memory access token, or no value if none is available. |
| `getRefreshToken() String?` | Retrieves the persisted refresh token from `SecureStorage`, or no value if no renewable session exists. |
| `storeTokens(accessToken: String, refreshToken: String)` | Replaces the currently managed credentials. The access token is kept in memory and the refresh token is persisted through `SecureStorage`. |
| `clearTokens()` | Removes the in-memory access token and deletes the persisted refresh token. |

##### SecureStorage


###### Purpose {.unnumbered}

`SecureStorage` defines the platform-independent contract used to persist sensitive values securely.

The interface allows the shared Mobile Core to use protected storage without depending directly on Android or iOS storage APIs.

###### Responsibilities {.unnumbered}

- define secure key-value storage operations,
- abstract platform-specific protected storage,
- allow the native Android and iOS applications to provide their respective implementations.

###### Dependencies {.unnumbered}

The interface has no platform dependency inside the Mobile Core. Its concrete implementation is provided externally by the native application.

###### Interface Operations {.unnumbered}


| Operation | Description |
|---|---|
| `save(key: String, value: String)` | Stores the supplied value under the specified key using the platform-protected storage implementation. |
| `get(key: String) String?` | Retrieves the value associated with the specified key. Returns no value if the key is not present. |
| `remove(key: String)` | Removes the value associated with the specified key from secure storage. |

The interface intentionally exposes generic key-value operations. Token-specific decisions, such as which values are stored and which keys are used, remain the responsibility of `TokenManager`.


The following diagram summarizes the classes and dependencies described in this section.



### Authentication and Token Lifecycle

Authentication state is managed jointly by `BackendCommunication` and `TokenManager`.

`BackendCommunication` owns the communication workflow, while `TokenManager` owns the local credential state. Authentication details are not exposed to the native applications or to the application services.

#### Login Flow

1. The native application calls `MobileCoreApi.login(identifier, password)`.
2. `ServiceFacade` delegates the operation to `AuthService`.
3. `AuthService` calls `CommunicationGateway.login(...)`.
4. `BackendCommunication` creates a `LoginRequestDto` and delegates the HTTP operation to `BackendApiClient`.
5. The backend validates the credentials and returns a short-lived access token and a long-lived refresh token.
6. `BackendCommunication` stores the returned credentials through `TokenManager.storeTokens(...)`.
7. The password is discarded after the login operation and is not used for automatic re-authentication.

The complete login sequence is summarized in the following diagram.

![Mobile Login Flow](assets/mobile-login-flow.png){width=100%}

#### Access Token Refresh

Token refresh is transparent to the caller and is not part of `MobileCoreApi` or `CommunicationGateway`.

1. `BackendCommunication` obtains the current access token from `TokenManager`.
2. The access token is used for a protected backend request.
3. If no access token is available, or if the backend rejects it because it has expired, `BackendCommunication` requests the refresh token from `TokenManager`.
4. `BackendCommunication` creates a `RefreshTokenRequestDto` and calls `BackendApiClient.refresh(...)`.
5. When the refresh succeeds, the returned credentials replace the previous credentials through `TokenManager.storeTokens(...)`.
6. The original protected operation may then be retried once with the new access token.
7. If renewal fails because the refresh token is expired, revoked, or invalid, the local credentials are cleared and the caller is informed that authentication is required again.

The complete renewal sequence is summarized in the following diagram.

![Mobile Token Refresh Flow](assets/mobile-token-refresh-flow.png){width=95%}

#### Logout Flow

1. The native application calls `MobileCoreApi.logout()`.
2. The operation reaches `BackendCommunication` through `AuthService` and `CommunicationGateway`.
3. `BackendCommunication` retrieves the refresh token from `TokenManager`.
4. The backend is asked to invalidate the renewable session.
5. `TokenManager.clearTokens()` removes the local access token and the persisted refresh token.

Local credentials are cleared when the user logs out. Backend-side invalidation ensures that the renewal credential can no longer be used to obtain new access credentials.

#### Token and Session Lifecycle


| Credential | Purpose | Lifetime | Local Storage |
|---|---|---|---|
| Password | Initial user authentication | Used only during login | Not persisted |
| Access token | Authenticate protected backend requests | Short-lived | Memory |
| Refresh token | Obtain new access credentials without storing the password | Long-lived and revocable | `SecureStorage` |

### Design Decisions and Assumptions

This section records the main architectural decisions and assumptions that define the current Mobile Core design.

#### Design Decisions

| Decision | Rationale |
|---|---|
| Shared application logic is implemented in Kotlin Multiplatform. | Authentication, booking, backend communication, and token-related logic can be shared between Android and iOS while platform-specific functionality remains native. |
| Native applications access the shared core only through `MobileCoreApi`. | A single public boundary reduces coupling between native code and the internal service structure of the Mobile Core. |
| Backend communication is isolated behind `CommunicationGateway`. | Application services remain independent from REST, HTTP, serialization, DTO, and token-management concerns. |
| DTOs are kept inside the communication layer. | Transport representations can evolve without directly affecting the application-facing API and models. |
| Authentication communication is coordinated by `BackendCommunication`. | Login, refresh, and logout require interaction with the backend and therefore belong to the communication boundary rather than to application services or local token storage. |
| `TokenManager` does not perform backend communication. | The module has a single responsibility: manage local authentication credentials and their storage lifecycle. |
| The access token is kept in memory while the refresh token is persisted securely. | The access credential is short-lived, while the long-lived renewal credential requires platform-protected storage. |
| Secure storage is represented by a platform-independent interface. | Android and iOS use different protected-storage mechanisms while the shared core remains platform-independent. |
| BLE communication remains outside the Mobile Core. | BLE interaction depends on native platform APIs and is handled by the Android and iOS applications. |

#### Assumptions

- The backend provides REST endpoints for login, token refresh, session revocation, and the operations required by `CommunicationGateway`.
- Successful login returns a short-lived access token and a long-lived refresh token.
- Successful refresh returns a new access token and may rotate the refresh token; when rotation is used, the replacement token is stored immediately.
- Android and iOS provide concrete implementations of `SecureStorage`.
- The internal architecture of the Android and iOS applications will be designed separately.
- The BLE protocol and terminal interaction contract are not defined by this document.
- DTO fields, error representations, and detailed endpoint contracts may be refined when the backend contract is finalized.
- The current check-out flow uses `resourceId` at the application boundary. `CheckOutRequestDto` allows the transport request to be extended later without changing the current application interface.
- An authenticated backend operation is retried at most once after a successful access-token refresh to avoid uncontrolled retry loops.

## Door Terminal Design

### Module Structure

- *Show the proposed firmware modules for BLE communication, credential verification, door control, local storage, and event handling.*
- *Explain their responsibilities, dependencies, and interfaces with the mobile app and the selected synchronization route.*

### Class and Data Structure Design

- *Include a proposed diagram of the important firmware classes or data structures, according to the chosen C/C++ design.*
- *Show how protocol messages, authorization data, terminal state, and queued usage events relate to the firmware modules.*

## Database Design

- *Include the proposed ER Diagram with tables/entities, main fields, primary and foreign keys, relationships, and cardinalities.*
- *Explain how users, roles, resources, terminals, bookings, usage events, invoices, payments, and audit records are connected, including important integrity constraints.*

## Booking and Usage Lifecycle

- *Show the proposed states and transitions for bookings and resource usage.*
- *Summarize the main policies for booking conflicts, walk-in access, no-shows, overtime, and missing check-out.*

## BLE Access and Security Concept

- *Explain the proposed mobile-to-terminal identification and authorization flow, from credential verification to door access and check-in/check-out.*
- *Summarize the security approach: role restrictions, protected credentials and keys, replay protection, and handling of lost or compromised devices.*

## Offline Operation and Synchronization

- *Explain how the terminal should authorize access and retain usage events while offline, within a defined authorization validity window.*
- *Describe the proposed route for later synchronization, including duplicate prevention and how conflicting events should be handled.*

## User Interface Design

### Web Application UI

- *Include wireframes for the main web screens: login, resource search, bookings, invoices, and administration.*
- *Show navigation and the actions available to each role, including important confirmation and error states.*

### Mobile Application UI

- *Include wireframes for the main mobile screens: login, current booking/access entitlement, BLE connection, and check-in/check-out.*
- *Show navigation and feedback for successful or denied access, disconnection, and offline operation.*

## Design Choices

- *Summarize the important choices in architecture, technologies, mobile integration, and BLE communication, with their reasons.*
- *Record unresolved design questions; describe the final implemented structures and any changes in Chapter 5.*
