# ServiceDesk

An IT service request and issue tracking system. Employees raise support tickets and agents pick them up, update them and resolve them. This is my capstone project for practising Java, Spring Boot, REST APIs and PostgreSQL.

## Tech stack

- Java 21
- Spring Boot 3.5 (Spring Web, Spring Data JPA / Hibernate)
- PostgreSQL
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

2. Set the database password as an environment variable. We keep secrets out of the repo, so the app reads the password from `DB_PASSWORD`:

   ```powershell
   $env:DB_PASSWORD="your-password"
   ```

   To save it permanently instead, run `setx DB_PASSWORD "your-password"` and reopen your terminal.

3. If your PostgreSQL username or database name is different, update `src/main/resources/application.properties`.

4. Start the app:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   It runs on http://localhost:8080.

## Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/` | Quick check that the API is up |
| GET | `/api/v1/health` | Health check |
| POST | `/api/v1/tickets` | Create a ticket |
| GET | `/api/v1/tickets` | List all tickets |
| GET | `/api/v1/tickets/{id}` | Get one ticket |
| PATCH | `/api/v1/tickets/{id}/status` | Change a ticket's status |
| PATCH | `/api/v1/tickets/{id}/assign` | Assign a ticket to an agent |
| POST | `/api/v1/tickets/{id}/comments` | Add a comment |
| GET | `/api/v1/tickets/{id}/comments` | List a ticket's comments |

On first start the app seeds three demo users (a requester, an agent and an admin) so the ticket endpoints can be tried out. Authentication and role checks come in Phase 3, so for now requests pass the `requesterId` / `authorId` directly.

Example:

```json
POST /api/v1/tickets
{
  "requesterId": 1,
  "title": "Laptop will not boot",
  "description": "Blue screen on startup since this morning",
  "priority": "HIGH"
}
```

## Running the tests

```powershell
.\mvnw.cmd test
```

## Roadmap

- [x] Phase 1 - Foundation: project setup and PostgreSQL connection
- [x] Phase 2 - Core APIs: tickets, comments and validation
- [ ] Phase 3 - Security: authentication, roles and permissions
- [ ] Phase 4 - Testing
- [ ] Phase 5 - Documentation, Postman collection and deployment

## Author

Aniket Shinde
