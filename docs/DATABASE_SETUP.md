# Database setup

ServiceDesk uses MySQL 8.0 or newer, installed directly on your machine.
This setup starts with a fresh database. Hibernate creates the tables and
the application seeds the demo accounts on startup.

## 1. Install MySQL

Install [MySQL Community Server](https://dev.mysql.com/downloads/mysql/) and,
optionally, MySQL Workbench. Remember the root password chosen during setup.

Check it is running:

```powershell
Get-Service *mysql*
```

## 2. Create the database

Open MySQL Workbench as root, or run `mysql -u root -p`, and execute:

```sql
CREATE DATABASE servicedesk_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_cs;
```

## 3. Create the application user

The application defaults to `servicedesk_user`. Give it access only to this
database, including the DDL permissions Hibernate needs:

```sql
CREATE USER 'servicedesk_user'@'localhost' IDENTIFIED BY 'change-me';
GRANT ALL PRIVILEGES ON servicedesk_db.* TO 'servicedesk_user'@'localhost';
```

`utf8mb4` supports Unicode including emoji. The `as_cs` collation keeps text
comparisons sensitive to accents and case, as in the previous database.

## 4. Configure the app

The database password and the JWT secret are read from environment
variables, so no secrets are committed:

```powershell
$env:DB_PASSWORD="change-me"
$env:JWT_SECRET="a-long-random-string-at-least-32-chars"
```

To persist them for future terminals:

```powershell
setx DB_PASSWORD "change-me"
setx JWT_SECRET "a-long-random-string-at-least-32-chars"
```

The default URL is `jdbc:mysql://localhost:3306/servicedesk_db`. Override the
connection details through the environment when needed:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/servicedesk_db"
$env:DB_USERNAME="servicedesk_user"
```

`SPRING_DATASOURCE_USERNAME` also overrides the username through Spring Boot's
standard configuration. Hosted connections should use TLS; see
[DEPLOYMENT.md](DEPLOYMENT.md).

Run the backend with `.\mvnw.cmd spring-boot:run`.

Maven reads environment variables from your shell; it does not automatically
load a `.env` file. If MySQL uses another port, update
`SPRING_DATASOURCE_URL` to match.

## 5. Tables

There is no migration tool in this project, so Hibernate creates the tables
from the entities on startup (`spring.jpa.hibernate.ddl-auto=update`).

Expected tables:

| Table | Purpose |
|---|---|
| `users` | requesters, agents and admins |
| `tickets` | support tickets with requester/assignee links |
| `comments` | messages on tickets |

Verify in MySQL Workbench, or run:

```sql
USE servicedesk_db;
SHOW TABLES;
SELECT id, email, role, active FROM users;
```

## 6. Demo data

On the first start the app seeds three accounts (all with password
`password123`):

| Email | Role |
|---|---|
| `riya@servicedesk.local` | REQUESTER |
| `arjun@servicedesk.local` | AGENT |
| `admin@servicedesk.local` | ADMIN |

The seeder is `src/main/java/com/servicedesk/config/DataSeeder.java`.

## 7. Resetting everything

If you want a clean slate, drop and recreate the database:

```sql
DROP DATABASE servicedesk_db;
CREATE DATABASE servicedesk_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_cs;
```

Stop the backend before resetting the database. Start it again afterwards
and the tables plus demo users will be recreated.

## 8. Switching from the old database

This project now uses a fresh MySQL database; existing PostgreSQL records are
not imported. Clear any old `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_DRIVER_CLASS_NAME`
overrides before starting locally, and set `DB_PASSWORD` for the MySQL user.
Update these overrides in any hosted backend as well.

The old database can remain available until the MySQL setup is verified.

## 9. Tests

`.\mvnw.cmd verify` uses H2 in MySQL compatibility mode. CI also runs the same
suite against the runner's native MySQL service with a disposable database to exercise actual MySQL DDL,
identity columns, enums, boolean values, and foreign keys.

To run those tests locally, create a **separate, empty** `servicedesk_test`
database with the same character set and collation, and grant your test user
access to it. Then run:

```powershell
.\mvnw.cmd verify `
  "-Dspring.datasource.url=jdbc:mysql://localhost:3306/servicedesk_test" `
  "-Dspring.datasource.username=servicedesk_user" `
  "-Dspring.datasource.password=$env:DB_PASSWORD" `
  "-Dspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver"
```

The test profile uses `create-drop`: it recreates and drops the tables. Always
point this command at the disposable test database.

## Troubleshooting

| Error | Fix |
|---|---|
| `Access denied for user` | Check `DB_USERNAME`, `DB_PASSWORD`, and the account's host and database grants. |
| `Unknown database 'servicedesk_db'` | Create the database (step 2) or fix the JDBC URL. |
| `Connection refused` / `Communications link failure` | Start MySQL and verify the host and port (3306 by default). |
| `Public Key Retrieval is not allowed` | Use TLS, or configure a trusted server RSA public key. For an isolated local server only, `allowPublicKeyRetrieval=true&sslMode=DISABLED` can be appended to the JDBC URL. |
| App still tries PostgreSQL | Remove old datasource environment overrides and rebuild the backend. |
| `Failed to determine a suitable driver class` | The datasource URL is missing or malformed. Check `application.properties`. |
