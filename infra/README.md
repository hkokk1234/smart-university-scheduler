# Infrastructure

The root `compose.yaml` starts PostgreSQL for local development. The frontend and backend
run on the host for fast Vite updates and Java debugging. PostgreSQL is bound to localhost
and uses a named volume. `docker compose down` preserves the database; adding `--volumes`
permanently deletes its data.

Change database credentials before the first startup. PostgreSQL initialization variables
apply only to a new volume; editing `.env` does not change credentials in an existing database.
Use database administration commands to rotate existing credentials.

For production, build the backend JAR and frontend static bundle, provide secrets through
the deployment environment, and serve the frontend behind an HTTPS reverse proxy that
routes `/api` and `/actuator/health` to the backend. Do not run Vite's development or preview
server in production. This Compose file is for local development, not a production deployment.
No production hosting provider is selected by this scaffold.
