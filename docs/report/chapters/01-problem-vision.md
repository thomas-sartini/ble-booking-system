# Problem, Research Question, and Vision

## Background and Problem Statement

### Management of Shared Resources

Universities provide students and lecturers with various shared resources, such as equipment, devices, and facilities. Managing these resources requires a system that can organize availability, handle reservations, and control who is authorized to use them.

A digital booking system simplifies resource management, but it does not necessarily guarantee that a reservation corresponds to the actual use of a resource.

### Connection Between Booking and Actual Usage

One of the main challenges is connecting a digital reservation with physical access to a resource.

The system must verify that the person accessing a resource is authorized and that access takes place within the permitted time window. Furthermore, the beginning and end of resource usage must be recorded reliably.

Without a connection between reservations and actual usage, it becomes difficult to ensure traceability and accurately calculate potential usage costs.

### Security and Reliability

Managing shared resources requires particular attention to system security and reliability.

User data must be protected, access to resources must be restricted to authorized users, and information related to reservations and actual usage must be recorded accurately.

The system must also maintain data consistency in the event of errors or interruptions, ensuring that performed operations remain traceable.

## Project Objectives

### Overall Objective

The main objective of the BLE Booking System project is to design and develop a prototype that integrates digital resource reservations, secure physical access control, and usage tracking into a single system.

The central challenge is to connect digital bookings with physical resource access while ensuring security, reliability, and traceability throughout the process.

The prototype aims to support the complete process, from user authentication and resource booking to check-in, check-out, and billing.

### Specific Objectives

To achieve the overall objective, the project focuses on the following goals:

- Provide secure user authentication and role-based access management.
- Enable authorized users to view resource availability and create, modify, or cancel reservations.
- Support access to available resources without a prior reservation when permitted.
- Enable secure resource access through Bluetooth Low Energy (BLE).
- Record check-in and check-out events reliably, including the handling of communication interruptions.
- Calculate usage costs, generate PDF invoices, and manage payment status.
- Support relevant notifications and ensure the traceability of system operations.
- Evaluate the prototype through functional and security testing.

The detailed functional and non-functional requirements are documented in the [Full Requirements Register](08-appendices.md#full-requirements-register). The corresponding expected results and their verification criteria are provided in the [Expected Results and Verification Matrix](08-appendices.md#expected-results).

## Project Vision and Relevance


### Project Vision

The vision of the BLE Booking System is to simplify the use of shared university resources by connecting reservations with physical access.

Users should be able to book a resource, access it with their smartphone, and complete its use through a single system. This approach aims to reduce manual processes and provide a clear record of resource usage.

### Stakeholders and Expected Benefits

The project addresses the needs of different stakeholders involved in the management and use of shared university resources.

**Students and Lecturers** are the primary users of the system. They should be able to find available resources, manage their reservations, and access authorized equipment through a clear and convenient process. 

**Administrators** are responsible for managing user accounts, resources, reservations, and billing information. The system is intended to provide them with centralized management functions and traceable records of relevant activities.


## Scope and Constraints

The project focuses on developing a prototype rather than a production-ready system.

The scope includes a web application, a mobile application, a backend, a database, and a physical access terminal. The prototype is demonstrated through an equipment lending compartment.

The implementation is limited to Bluetooth Low Energy (BLE), excluding NFC. The hardware budget for the physical prototype is CHF 200.

The complete [Original Project Assignment](08-appendices.md#original-project-assignment) is included in the appendices.
