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

## Current endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/` | Quick check that the API is up |
| GET | `/api/v1/health` | Health check |

The ticket and authentication endpoints arrive in the next phases.

## Running the tests

```powershell
.\mvnw.cmd test
```

## Roadmap

- [x] Phase 1 - Foundation: project setup and PostgreSQL connection
- [ ] Phase 2 - Core APIs: tickets, comments and validation
- [ ] Phase 3 - Security: authentication, roles and permissions
- [ ] Phase 4 - Testing
- [ ] Phase 5 - Documentation, Postman collection and deployment

## Author

Aniket Shinde
