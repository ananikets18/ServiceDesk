# Deploying ServiceDesk (free demo on the internet)

This guide puts the backend, database and frontend on the internet for free:

| Piece | Where | Why |
|---|---|---|
| Database | [Neon](https://neon.tech) | Free serverless PostgreSQL |
| Backend (Spring Boot) | [Render](https://render.com) | Free Docker web service |
| Frontend (React) | [Vercel](https://vercel.com) | Free static hosting with HTTPS |

Everything is deployed from this GitHub repository, so once it is set up a
push to `main` is enough to release a new version.

> **This is a demo deployment.** Use fake data only. The app runs with
> Hibernate `ddl-auto=update`, ships with demo accounts whose password is
> `password123`, and has no rate limiting.

---

## Before you start

Push this repository to GitHub (already done if you are reading this from the
repo). There are no secrets in the code: the database password and the JWT
secret are read from environment variables that you set on Render.

Useful commands to create a strong JWT secret (needs 32+ characters):

```powershell
# PowerShell
-join ((48..57) + (97..122) | Get-Random -Count 40 | ForEach-Object { [char]$_ })
```

---

## Step 1 - Create the database on Neon

1. Sign up at https://neon.tech and create a project.
2. Open the project's **Connection string** panel. You will see something like:

   ```
   postgresql://neondb_owner:AbC123xy@ep-cool-name-123456.us-east-2.aws.neon.tech/neondb?sslmode=require
   ```

3. Split it into the JDBC parts Render needs:

   | Render variable | Value from the connection string |
   |---|---|
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://ep-cool-name-123456.us-east-2.aws.neon.tech/neondb?sslmode=require` |
   | `SPRING_DATASOURCE_USERNAME` | `neondb_owner` |
   | `DB_PASSWORD` | `AbC123xy` |

   Note the `jdbc:` prefix added to the URL, and keep `?sslmode=require`.

You do not have to create any tables. Hibernate creates them on first start.

---

## Step 2 - Deploy the backend on Render

1. Sign up at https://render.com and choose **New > Web Service**.
2. Connect your GitHub account and pick the `ServiceDesk` repository.
3. Configure it:
   - **Runtime**: `Docker`
   - **Dockerfile path**: `./Dockerfile` (detected automatically)
   - **Health check path**: `/api/v1/health`
   - **Instance type**: `Free`
4. Under **Environment > Environment Variables** add:

   | Key | Value |
   |---|---|
   | `SPRING_DATASOURCE_URL` | the `jdbc:...` URL from step 1 |
   | `SPRING_DATASOURCE_USERNAME` | `neondb_owner` |
   | `DB_PASSWORD` | your Neon password |
   | `JWT_SECRET` | the long random string you generated |
   | `APP_CORS_ALLOWED_ORIGINS` | leave empty for now, fill in after step 3 |

5. Click **Create Web Service**. Wait for the first build (a few minutes).
6. When it is live, note the URL, for example
   `https://servicedesk-api.onrender.com`. Test it:

   ```
   https://servicedesk-api.onrender.com/api/v1/health
   ```

   On the free plan the service sleeps after ~15 minutes of inactivity, so the
   first request after a break can take 30-60 seconds.

---

## Step 3 - Deploy the frontend on Vercel

1. Sign up at https://vercel.com and choose **Add New > Project**.
2. Import the same `ServiceDesk` repository.
3. Configure it:
   - **Root Directory**: `frontend`
   - **Framework Preset**: `Vite` (build command `npm run build`, output `dist`)
4. Under **Environment Variables** add:

   | Key | Value |
   |---|---|
   | `VITE_API_URL` | `https://servicedesk-api.onrender.com/api/v1` |

   Use the Render URL from step 2 and keep the `/api/v1` at the end. Vite bakes
   this value in at build time, so if you change it you must trigger a new
   deploy.

5. Click **Deploy**. Note your frontend URL, for example
   `https://servicedesk.vercel.app`.

---

## Step 4 - Let the backend trust the frontend (CORS)

The browser blocks requests between different domains unless the backend says
they are allowed.

1. Back on Render, edit the `APP_CORS_ALLOWED_ORIGINS` environment variable and
   set it to your Vercel URL, for example:

   ```
   https://servicedesk.vercel.app
   ```

   (Several origins can be separated by commas.)

2. Save. Render redeploys automatically.

Now open the Vercel URL, log in with a demo account and create a ticket.

### Demo accounts (password `password123`)

| Email | Role |
|---|---|
| `riya@servicedesk.local` | requester |
| `arjun@servicedesk.local` | agent |
| `admin@servicedesk.local` | admin |

---

## Deploying new changes

- **Backend**: push to `main` in the repo that Render watches. Render rebuilds
  the Docker image and redeploys automatically.
- **Frontend**: push to `main`. Vercel rebuilds and redeploys automatically.

The `CI` workflow in `.github/workflows/ci.yml` runs all backend tests and the
frontend build on every push and pull request, so broken changes are caught
before they reach a host.

---

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| Frontend shows "Could not load..." and the browser console says CORS | `APP_CORS_ALLOWED_ORIGINS` on Render does not exactly match the Vercel URL (check `https` and no trailing slash) |
| Requests time out the first time | Render free instance was asleep; wait ~30-60s and retry |
| 401 immediately after logging in | `JWT_SECRET` too short (must be 32+ characters) |
| Database errors on startup | `SPRING_DATASOURCE_URL` is missing the `jdbc:` prefix or `?sslmode=require` |
| Login fails for a demo account | The database was reset; the seeder only runs when the table is empty |

---

## Cleanup

Everything here is on a free tier. To stop using resources, delete the Render
web service, the Vercel project and the Neon project. Nothing in this setup
costs money by default.
