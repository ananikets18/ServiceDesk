# ServiceDesk

An IT service request and issue tracking system built with Spring Boot. Employees raise support tickets, agents pick them up and resolve them, and admins keep an eye on everything. I built it as my capstone project to practise Java, Spring Boot, REST APIs, PostgreSQL and testing.

## What it does

- **Accounts** - register and log in. Passwords are hashed with BCrypt and sessions are stateless, using JWT tokens.
- **Tickets** - create, list and view tickets with a priority (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) and a status (`OPEN` -> `IN_PROGRESS` -> `RESOLVED` -> `CLOSED`).
- **Assignment** - tickets can be assigned to agents.
- **Comments** - requesters and agents can discuss a ticket.
- **Roles** - requesters, agents and admins each see and can do different things.
- **Validation and errors** - every endpoint validates its input and every error comes back in the same JSON shape.

## Tech stack

| Layer | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Web | Spring Web (REST) |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL |
| Security | Spring Security + JWT (jjwt) |
| Validation | Bean Validation |
| Build | Maven (wrapper included) |
| Testing | JUnit 5, Mockito, MockMvc, H2 |
| Docs | springdoc OpenAPI (Swagger UI) |
| Frontend | React 19 + Vite + Tailwind CSS |

## Project layout

```
src/main/java/com/servicedesk
├── config        application setup (demo data, OpenAPI)
├── controller    HTTP endpoints
├── dto           request/response objects + validation
├── entity        User, Ticket, Comment and enums
├── exception     custom exceptions and the global handler
├── repository    Spring Data interfaces
├── security      JWT filter, user details, security config
└── service       business rules
```

## API

Protected endpoints expect `Authorization: Bearer <token>`.

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/` | public | Quick check that the API is up |
| GET | `/api/v1/health` | public | Health check |
| POST | `/api/v1/auth/register` | public | Register a user |
| POST | `/api/v1/auth/login` | public | Log in and receive a JWT |
| POST | `/api/v1/tickets` | requester | Create a ticket |
| GET | `/api/v1/tickets` | any role | List tickets (requesters see only their own) |
| GET | `/api/v1/tickets/{id}` | any role | Get one ticket |
| PATCH | `/api/v1/tickets/{id}` | requester (own), admin | Edit the title and description |
| PATCH | `/api/v1/tickets/{id}/status` | agent, admin | Change status (one step at a time) |
| PATCH | `/api/v1/tickets/{id}/assign` | agent, admin | Assign a ticket |
| POST | `/api/v1/tickets/{id}/comments` | any role | Add a comment |
| GET | `/api/v1/tickets/{id}/comments` | any role | List comments |
| GET | `/api/v1/users/agents` | agent, admin | List active agents (for the assign dropdown) |
| GET | `/api/v1/users` | admin | List every user |
| POST | `/api/v1/users` | admin | Create a user with a chosen role |
| PATCH | `/api/v1/users/{id}/deactivate` | admin | Deactivate an account |
| PATCH | `/api/v1/users/{id}/activate` | admin | Reactivate an account |

### Status workflow

A ticket moves through `OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED`, one step at a time. It can also be sent back one step if more work is needed (`IN_PROGRESS -> OPEN`, `RESOLVED -> IN_PROGRESS`). `CLOSED` is final. Any other jump is rejected with a `400` that lists the allowed next statuses.

### Permissions

- **Requester** - the only role that can raise tickets. Sees only their own, can edit and comment on them while they are open, but cannot change status or assign.
- **Agent** - works tickets but cannot raise or edit them. Sees all tickets, can move the status of tickets assigned to them (or still unassigned), and can only assign tickets to themselves.
- **Admin** - oversees everything: can edit, update and assign any ticket, and manages the users (create accounts, activate/deactivate them), but does not raise tickets either.

### Example

```json
POST /api/v1/auth/login
{ "email": "riya@servicedesk.local", "password": "password123" }
```

```json
POST /api/v1/tickets
Authorization: Bearer <token>

{
  "title": "Laptop will not boot",
  "description": "Blue screen on startup since this morning",
  "priority": "HIGH"
}
```

### Error shape

```json
{
  "timestamp": "2026-10-19T11:22:09",
  "status": 400,
  "error": "Validation Failed",
  "message": "One or more fields are invalid",
  "path": "/api/v1/tickets",
  "fieldErrors": { "title": "title must not be blank" }
}
```
