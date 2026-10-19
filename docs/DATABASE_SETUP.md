# Database setup

ServiceDesk uses PostgreSQL. This guide walks through setting it up from
scratch, including creating a dedicated database user (which is what you
would do on a real project rather than using the `postgres` superuser).

## 1. Install PostgreSQL

Download it from https://www.postgresql.org/download/ and install with
pgAdmin 4 (the Windows installer includes both). Remember the password you
set for the `postgres` superuser during installation.

Check it is running:

```powershell
Get-Service postgresql*
```

## 2. Create the database

Open **pgAdmin 4**, connect to your server, right-click **Databases** and
choose **Create > Database...**. Name it `servicedesk_db` and save.

Or do it with SQL - right-click the server, open **Query Tool**, and run:

```sql
CREATE DATABASE servicedesk_db;
```

## 3. Create a dedicated user (recommended)

Using the `postgres` superuser for an application is a bad habit. Create a
user that only owns this database:

```sql
CREATE USER servicedesk_user WITH PASSWORD 'change-me';
GRANT ALL PRIVILEGES ON DATABASE servicedesk_db TO servicedesk_user;
```

PostgreSQL 15 and newer also need this, run while connected to
`servicedesk_db`:

```sql
GRANT ALL ON SCHEMA public TO servicedesk_user;
```

Then point the app at that user in `src/main/resources/application.properties`:

```properties
spring.datasource.username=servicedesk_user
```

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

If your database name or host is different, update the URL:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/servicedesk_db
```

## 5. Tables

There is no migration tool in this project, so Hibernate creates the tables
from the entities on startup (`spring.jpa.hibernate.ddl-auto=update`).

Expected tables:

| Table | Purpose |
|---|---|
| `users` | requesters, agents and admins |
| `tickets` | support tickets with requester/assignee links |
| `comments` | messages on tickets |

Verify in pgAdmin by expanding **servicedesk_db > Schemas > public > Tables**.

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
CREATE DATABASE servicedesk_db;
```

Start the app again and the tables plus demo users will be recreated.

## Troubleshooting

| Error | Fix |
|---|---|
| `password authentication failed for user "postgres"` | The `DB_PASSWORD` env var does not match your PostgreSQL password. Reset it in pgAdmin: `ALTER USER postgres WITH PASSWORD 'new-pass';` |
| `database "servicedesk_db" does not exist` | Create the database (step 2) or fix the name in `spring.datasource.url`. |
| `Connection refused` | PostgreSQL is not running. Start the service or pgAdmin's server. |
| `Failed to determine a suitable driver class` | The datasource URL is missing or malformed. Check `application.properties`. |
