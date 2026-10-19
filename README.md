# ServiceDesk

An IT service request and issue tracking system. Employees raise support tickets and agents pick them up, update them and resolve them. This is my capstone project for practising Java, Spring Boot, REST APIs and PostgreSQL.

## Tech stack

- Java 21
- Spring Boot 3.5 (Spring Web, Spring Data JPA / Hibernate, Spring Security)
- PostgreSQL
- JWT (jjwt) for stateless authentication
- Maven
- JUnit 5 and Mockito

## Before you start

- JDK 21 installed
- PostgreSQL running locally
- No Maven install needed - the repo ships the Maven wrapper

## Getting it running

1. Create the database in PostgreSQL:

   ```sql
   CREATE DATABASE servicedesk_db;
   ```

2. Set the two environment variables the app needs. Secrets are kept out of the repo, so the database password comes from `DB_PASSWORD` and the JWT signing key from `JWT_SECRET` (at least 32 characters):

   ```powershell
   $env:DB_PASSWORD="your-password"
   $env:JWT_SECRET="a-long-random-string-at-least-32-chars"
   ```

   To save them permanently instead, run `setx DB_PASSWORD "your-password"` and `setx JWT_SECRET "..."`, then reopen your terminal.

3. If your PostgreSQL username or database name is different, update `src/main/resources/application.properties`.

4. Start the app:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   It runs on http://localhost:8080.

## Endpoints

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/` | public | Quick check that the API is up |
| GET | `/api/v1/health` | public | Health check |
| POST | `/api/v1/auth/register` | public | Register a new user |
| POST | `/api/v1/auth/login` | public | Log in and receive a JWT |
| POST | `/api/v1/tickets` | any role | Create a ticket |
| GET | `/api/v1/tickets` | any role | List tickets (requesters see only their own) |
| GET | `/api/v1/tickets/{id}` | any role | Get one ticket |
| PATCH | `/api/v1/tickets/{id}/status` | agent, admin | Change a ticket's status |
| PATCH | `/api/v1/tickets/{id}/assign` | agent, admin | Assign a ticket |
| POST | `/api/v1/tickets/{id}/comments` | any role | Add a comment |
| GET | `/api/v1/tickets/{id}/comments` | any role | List a ticket's comments |

Protected endpoints expect the token in a header:

```
Authorization: Bearer <token>
```

### Demo accounts

Seeded on first start (password for all three is `password123`):

| Email | Role |
|---|---|
| `riya@servicedesk.local` | REQUESTER |
| `arjun@servicedesk.local` | AGENT |
| `admin@servicedesk.local` | ADMIN |

### Permissions in short

- **Requester** - creates tickets, sees only their own, can comment on them, cannot change status or assign.
- **Agent** - sees all tickets, can change the status of tickets assigned to them (or still unassigned), and can only assign tickets to themselves.
- **Admin** - full access, including assigning any ticket to any agent.

### Example flow

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

Every error uses the same shape:

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

## Running the tests

```powershell
.\mvnw.cmd test
```

## Roadmap

- [x] Phase 1 - Foundation: project setup and PostgreSQL connection
- [x] Phase 2 - Core APIs: tickets, comments and validation
- [x] Phase 3 - Security: authentication, roles and permissions
- [ ] Phase 4 - Testing
- [ ] Phase 5 - Documentation, Postman collection and deployment

## Author

Aniket Shinde
