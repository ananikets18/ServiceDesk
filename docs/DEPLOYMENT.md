# Deploying ServiceDesk

The backend runs as an executable JAR on a Java 21 host. It connects to a
MySQL 8.0+ database, and the React frontend can be served by a static host.

This is a demo application: Hibernate updates the schema automatically and
the application seeds demo accounts with password `password123`.

## 1. Prepare MySQL

Create a fresh database and application user using
[DATABASE_SETUP.md](DATABASE_SETUP.md). For a managed database, use the host,
port, account grants, and network access rules supplied by your provider.
The application user needs DDL permissions for Hibernate to create tables.

For a remote connection, use TLS. An example JDBC URL is:

```text
jdbc:mysql://mysql.example.com:3306/servicedesk_db?sslMode=VERIFY_IDENTITY
```

Use the actual database hostname. `VERIFY_IDENTITY` requires a trusted
certificate matching it. If your provider supplies a private CA, configure
a Java trust store following its instructions and the
[Connector/J TLS guide](https://dev.mysql.com/doc/connector-j/en/connector-j-reference-using-ssl.html).

## 2. Build the backend

Install JDK 21 and build from the repository root:

```powershell
.\mvnw.cmd -B verify
```

On Linux or macOS:

```bash
chmod +x mvnw
./mvnw -B verify
```

The executable JAR is `target/servicedesk-0.0.1-SNAPSHOT.jar`. Copy it to
your Java 21 host if you build on another machine.

## 3. Configure and start the backend

Set these environment variables in the host's service configuration or the
shell that starts Java:

| Variable | Value |
|---|---|
| `SPRING_DATASOURCE_URL` | the MySQL JDBC URL from step 1 |
| `SPRING_DATASOURCE_USERNAME` | your MySQL application user |
| `DB_PASSWORD` | the application user's password |
| `JWT_SECRET` | a random secret of at least 32 characters |
| `APP_CORS_ALLOWED_ORIGINS` | your frontend origin, such as `https://servicedesk.example.com` |
| `PORT` | optional HTTP port; defaults to `8080` |

Environment variables must be available to the Java process. A `.env` file
is not loaded automatically. Remove any old PostgreSQL datasource overrides.

Start the application from the directory containing the JAR:

```bash
java -jar servicedesk-0.0.1-SNAPSHOT.jar
```

Hibernate creates the tables on first start. Configure the host's process
manager to keep Java running and restart it when needed. For public access,
configure your hosting platform or reverse proxy to serve HTTPS and forward
requests to the application's HTTP port.

Check the backend at `https://api.example.com/api/v1/health`, using your
actual API hostname.

## 4. Build and publish the frontend

Set the API URL before building. For example, in PowerShell:

```powershell
cd frontend
$env:VITE_API_URL="https://api.example.com/api/v1"
npm ci
npm run build
```

Publish `frontend/dist` to your static host. Configure it to serve
`index.html` for client-side routes. If deploying through a host's build
pipeline, set the project root to `frontend`, the build command to
`npm run build`, the output directory to `dist`, and `VITE_API_URL` in its
build environment.

Set the backend's `APP_CORS_ALLOWED_ORIGINS` to the frontend's exact origin
(scheme and hostname, without a trailing slash), then restart the backend.
Vite embeds `VITE_API_URL` at build time, so changing it requires rebuilding
the frontend.

## 5. Verify the deployment

Open the frontend and sign in with `riya@servicedesk.local`, password
`password123`. Create a ticket and confirm it remains after restarting the
backend. The other seeded accounts are `arjun@servicedesk.local` (agent) and
`admin@servicedesk.local` (admin), using the same demo password.

## Updates and CI

The GitHub Actions workflow runs backend tests on H2 and a native MySQL
service, and builds the frontend. Deployment is separate: rebuild and
replace the JAR, restart the backend, and publish the new frontend build.
Keep `JWT_SECRET` consistent across restarts to preserve existing tokens.

## Troubleshooting

| Symptom | Check |
|---|---|
| Backend does not start | Java 21, required environment variables, and startup logs |
| Database connection fails | MySQL service, JDBC URL, credentials, network access, and TLS configuration |
| Browser reports CORS errors | `APP_CORS_ALLOWED_ORIGINS` exactly matches the frontend origin |
| Frontend cannot reach the API | `VITE_API_URL` includes `/api/v1` and was set before building |
| Page refresh returns 404 | The static host forwards client-side routes to `index.html` |
