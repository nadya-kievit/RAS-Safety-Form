# RAS Safety Form

## Supabase photo storage

Safety-form photos are stored in a private Supabase Storage bucket. Configure
these server-only values in the repository `.env` file for local development,
or as environment variables in deployment:

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_SECRET_KEY=sb_secret_your-server-key
SUPABASE_BUCKET=safety-photos
```

`SUPABASE_URL` may also be copied from the Data API screen with a `/rest/v1`
suffix; the backend normalizes it to the project URL. Never expose the secret
key through a `VITE_*` variable or commit `.env`.

The bucket should remain private. The backend uploads validated images and
returns signed URLs that expire after 15 minutes. Each safety form supports up
to five JPEG, PNG, WebP, or GIF images of at most 10 MB each.

## Persistent login sessions

Authenticated sessions are stored in PostgreSQL and remain valid for 30 days of
inactivity. Before starting the updated backend, apply the `SPRING_SESSION` and
`SPRING_SESSION_ATTRIBUTES` definitions in `backend/sql/schema.sql` to the
database.

The session cookie works over local HTTP by default. In an HTTPS deployment,
set this server environment variable so browsers only transmit it securely:

```properties
SESSION_COOKIE_SECURE=true
```

## First-login password changes

Accounts created by an administrator are marked as requiring a password change.
The user must replace the temporary password before any other authenticated API
route or application page can be used.

For an existing database, preserve existing accounts while making future
accounts require a password change:

```sql
ALTER TABLE users
ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
ALTER COLUMN must_change_password SET DEFAULT TRUE;
```

The current `backend/sql/schema.sql` already includes the column with the
correct default for newly created databases.
