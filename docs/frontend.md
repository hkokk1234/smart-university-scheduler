# Frontend

React and TypeScript/Vite use feature folders for authentication, catalog, planning,
schedules, administration, settings and dashboard. React Router handles lazy page loading,
TanStack Query handles server state, Zustand holds session/draft/preferences state, and
FullCalendar renders semester-local weekly meeting templates. Tailwind and Radix-based
shadcn primitives provide responsive navigation, dialogs and form controls.

The frontend reads live API data; no production sample course data is embedded. The dev
server proxies `/api` and `/actuator` to `BACKEND_PROXY_TARGET`. Hosting must proxy these
paths on the same HTTPS origin or configure `VITE_API_BASE_URL` with matching backend CORS.
Frontend environment values are public; never place secrets in `VITE_*` variables.

JWTs live only in memory. Reloading the page requires signing in again. Draft course and
section choices, theme, calendar display and solver preferences persist locally. A different
user resets draft ownership. Private query caches clear on logout. Generated results are
ephemeral. Published meetings cannot be dragged to other times; choose an alternative section
instead.

Generation calls the authenticated Java CSP endpoint and renders its ranked schedules,
score explanations and execution metrics. The Settings page controls a preferred time window,
morning/afternoon preference, free weekdays, preferred maximum campus days and gap weight;
these are passed to the solver on generation. Penalties are normalized before applying weights.
Mandatory unavailable intervals remain a hard constraint available through the API, not the
current Settings UI. Manual partial selections can be saved as drafts.

Run from the repository root:

```sh
npm run dev
npm run typecheck
npm run lint
npm run test --workspace frontend
npm run build
```

Browser cancellation aborts the fetch on navigation. It does not guarantee servlet-thread
interruption; the backend deadline bounds ongoing search. Browser end-to-end testing and
accessibility testing with assistive technology remain distinct from builds and unit checks.
