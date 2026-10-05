# Deploying the backend to Render

This repository includes `render.yaml`, which deploys the Dockerfile and checks
`/actuator/health`. The application reads Render's `PORT` environment variable
automatically.

1. Create a Render **Blueprint** from this repository (or create a Docker web
   service using this repository and Dockerfile).
2. Set the secret variables marked as `sync: false` in Render:
   - `SPRING_DATA_MONGODB_URI` — a reachable MongoDB connection string
   - `GEMINI_API_KEY`
   - `APP_AUTH_JWT_SECRET` — a random secret of at least 32 bytes
   - `APP_CORS_ALLOWED_ORIGINS` — your exact Netlify URL, such as
     `https://your-site.netlify.app`
   - `GOOGLE_CLIENT_ID`
3. Deploy the service, then copy its public HTTPS URL into Netlify as
   `VITE_API_BASE_URL` and redeploy the frontend.

`APP_AUTH_COOKIE_SECURE=true` and `APP_AUTH_COOKIE_SAME_SITE=None` are set for
the separate HTTPS Netlify and Render domains. Keep these production values;
the checked-in `.env.example` retains local-development settings.

If you deploy a Netlify preview or a custom domain as well, append each exact
origin to `APP_CORS_ALLOWED_ORIGINS`, separated by commas. Add the same frontend
origins to the Google OAuth client's authorized JavaScript origins.
