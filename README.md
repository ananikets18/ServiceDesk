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

## Getting started

You need JDK 21 and PostgreSQL. No Maven install is needed - the repo ships the wrapper.

### 1. Create the database

```sql
CREATE DATABASE servicedesk_db;
```

More detail, including creating a dedicated database user, is in [docs/DATABASE_SETUP.md](docs/DATABASE_SETUP.md).

### 2. Set the environment variables

Secrets are never committed. The app reads them from the environment:

```powershell
$env:DB_PASSWORD="your-postgres-password"
$env:JWT_SECRET="a-long-random-string-at-least-32-chars"
```

To make them permanent, use `setx` and reopen the terminal.

### 3. Run it

```powershell
.\mvnw.cmd spring-boot:run
```

The API starts on http://localhost:8080. Tables and three demo users are created automatically on first start.

### Demo accounts

All three use the password `password123`.

| Email | Role |
|---|---|
| `riya@servicedesk.local` | REQUESTER |
| `arjun@servicedesk.local` | AGENT |
| `admin@servicedesk.local` | ADMIN |

## API

Protected endpoints expect `Authorization: Bearer <token>`.

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/` | public | Quick check that the API is up |
| GET | `/api/v1/health` | public | Health check |
| POST | `/api/v1/auth/register` | public | Register a user |
| POST | `/api/v1/auth/login` | public | Log in and receive a JWT |
| POST | `/api/v1/tickets` | any role | Create a ticket |
| GET | `/api/v1/tickets` | any role | List tickets (requesters see only their own) |
| GET | `/api/v1/tickets/{id}` | any role | Get one ticket |
| PATCH | `/api/v1/tickets/{id}/status` | agent, admin | Change status |
| PATCH | `/api/v1/tickets/{id}/assign` | agent, admin | Assign a ticket |
| POST | `/api/v1/tickets/{id}/comments` | any role | Add a comment |
| GET | `/api/v1/tickets/{id}/comments` | any role | List comments |
| GET | `/api/v1/users/agents` | agent, admin | List agents (for the assign dropdown) |

### Permissions

- **Requester** - creates tickets, sees only their own, can comment on them, cannot change status or assign.
- **Agent** - sees all tickets, can update tickets assigned to them (or still unassigned), and can only assign tickets to themselves.
- **Admin** - full access, including assigning any ticket to any agent.

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

## Frontend

The React app lives in `frontend/`. It covers login, registration, the ticket list, a new-ticket form and a ticket detail page where agents and admins can change the status, assign an agent and everyone can comment.

### Running the frontend

Start the backend first, then:

```powershell
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The Vite dev server proxies `/api` to `http://localhost:8080`, so there is no CORS setup to worry about.

Log in with one of the demo accounts from the section above. What you see depends on the role:

- Requesters see only their own tickets and cannot change status or assign.
- Agents see every ticket and can manage the ones assigned to them.
- Admins can manage anything.

### Building for production

```powershell
cd frontend
npm run build
```

The output goes to `frontend/dist`.

## Swagger UI

With the app running, open http://localhost:8080/swagger-ui.html. Log in first, then click **Authorize** and paste the token to try requests from the browser.

## Postman

Import [docs/ServiceDesk.postman_collection.json](docs/ServiceDesk.postman_collection.json). The login requests save the token automatically, and the create-ticket request saves the new ticket id, so the requests can be run in order. There is also a folder of negative cases (401, 403, 404, 400) to show the error handling.

## Tests

```powershell
.\mvnw.cmd test
```

The tests use an in-memory H2 database, so PostgreSQL does not need to be running. There are 39 tests:

- `TicketServiceTest` - ticket rules and permissions (mocked repositories).
- `AuthServiceTest` - registration and login logic.
- `TicketApiTest` - full HTTP tests through the security filters and database, including negative cases.
- `ServiceDeskApplicationTests` - context load check.

## Roadmap

- [x] Phase 1 - Foundation: project setup and PostgreSQL connection
- [x] Phase 2 - Core APIs: tickets, comments and validation
- [x] Phase 3 - Security: authentication, roles and permissions
- [x] Phase 4 - Testing
- [x] Phase 5 - Documentation, Postman collection and Swagger

## Author

Aniket Shinde
