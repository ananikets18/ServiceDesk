# ServiceDesk - Resume Project Entry

Copy the block you need into the **Projects** section of your resume. Keep the
tech-stack line (ATS systems scan for those keywords) and 3-5 bullets.

---

## Full version (use if you have space)

**ServiceDesk - IT Service Request and Issue Tracking System** | Capstone Project

**Tech:** Java 21, Spring Boot, Spring Security, Spring Data JPA, MySQL, JWT,
REST API, React, Vite, Tailwind CSS, GitHub Actions, Swagger (OpenAPI),
JUnit 5, Mockito, Maven, Git

- Built a full-stack IT help desk application that manages support tickets through a
  defined lifecycle (Open, In Progress, Resolved, Closed) for three user roles:
  Requester, Agent, and Admin.
- Developed 15 secured REST API endpoints with Spring Boot using a layered
  architecture (Controller, Service, Repository), DTOs, Bean Validation, and a
  centralized exception handler that returns consistent JSON errors.
- Implemented stateless JWT authentication and role-based authorization with
  Spring Security and BCrypt password hashing, including ownership rules so
  requesters can only view and edit their own tickets.
- Modeled the domain with Spring Data JPA / Hibernate (User, Ticket, Comment) on
  MySQL, enforcing status-transition rules and admin user management
  (create, activate, and deactivate accounts).
- Wrote 67 automated tests with JUnit 5, Mockito, and MockMvc against an in-memory
  H2 database, covering success and failure paths (400, 401, 403, 404, 409).
- Built a responsive single-page frontend with React, React Router, and Tailwind
  CSS, using Axios interceptors to attach JWT tokens and handle session expiry.
- Added a GitHub Actions CI pipeline that runs the full test suite on H2 and
  MySQL and builds the frontend on
  every push and pull request.
- Documented the API with Swagger / OpenAPI and a Postman collection, and
  prepared deployment instructions for a Java executable JAR and MySQL.

---

## Short version (3 bullets, if space is tight)

**ServiceDesk - IT Service Request and Issue Tracking System** | Capstone Project
Tech: Java 21, Spring Boot, Spring Security, MySQL, React, GitHub Actions

- Built a full-stack IT ticket management system with JWT authentication and
  role-based access for Requesters, Agents, and Admins.
- Created 15 secured REST endpoints with validation, centralized error handling,
  and a status workflow, backed by 67 JUnit and Mockito tests.
- Automated the tests and build with a GitHub Actions CI pipeline.

---

## One-line version (for a summary line or portfolio card)

Full-stack IT ticketing system built with Spring Boot, MySQL, and React
featuring JWT authentication, role-based access control, a ticket status
workflow, and CI with GitHub Actions.

---

## ATS keywords included

Java, Spring Boot, Spring Security, Spring Data JPA, Hibernate, MySQL,
REST API, JSON, JWT, authentication, authorization, role-based access control
(RBAC), DTO, Bean Validation, exception handling, React, Vite, Tailwind CSS,
Axios, React Router, CI/CD, GitHub Actions, Swagger,
OpenAPI, Postman, JUnit 5, Mockito, MockMvc, H2, Maven, Git, unit testing,
integration testing, full-stack, backend, frontend.

---

## Numbers to keep accurate

| Fact | Value |
|---|---|
| REST API endpoints | 15 |
| Automated tests | 67 |
| User roles | 3 |
| Ticket statuses | 4 |
| Frontend pages | 6 |

Note: only list a cloud deployment once the live demo works
end-to-end from the browser. Until then, drop that bullet or say
"deployment-ready". Everything else above is verified by the code and test suite.
