# Frontend

Vue/Vite client for LaundryLab. Run `npm ci` and `npm run dev`. The development server proxies `/api` to `http://localhost:8080`; set `API_PROXY_TARGET` before starting Vite to use another local backend port.

The student registration page is `/register`; the emailed link opens `/verify?token=...`. Login sends credentials to the backend and routes users by their server-side role. The server stores the session in an HttpOnly cookie; the frontend does not store credentials in local storage.

The backend must be running with MySQL and SMTP configured. For local HTTP development set `AUTH_SECURE_COOKIE=false` on the backend and `PUBLIC_BASE_URL=http://localhost:5173` (or the actual Vite origin). LINE linking and notifications are not implemented yet.
