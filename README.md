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
