# React + TypeScript + Vite

This template provides a minimal setup to get React working in Vite with HMR and some Oxlint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the Oxlint configuration

If you are developing a production application, we recommend enabling type-aware lint rules by installing `oxlint-tsgolint` and editing `.oxlintrc.json`:

```json
{
  "$schema": "./node_modules/oxlint/configuration_schema.json",
  "plugins": ["react", "typescript", "oxc"],
  "options": {
    "typeAware": true
  },
  "rules": {
    "react/rules-of-hooks": "error",
    "react/only-export-components": ["warn", { "allowConstantExport": true }]
  }
}
```

See the [Oxlint rules documentation](https://oxc.rs/docs/guide/usage/linter/rules) for the full list of rules and categories.

## Project scripts

Run from `web/`:

| Command | What |
|---|---|
| `npm ci` | Install dependencies |
| `npm run dev` | Dev server on http://localhost:5173 |
| `npm run build` | Type check and production build |
| `npm run lint` | Oxlint |
| `npm test` | Vitest (React Testing Library + MSW), runs once |
| `npm run test:watch` | Vitest in watch mode |

## Configuration

Copy `.env.example` to `.env.local`.

- `VITE_API_URL`: backend base URL, default `http://localhost:8080`.
- `VITE_USE_MOCKS`: `true` starts the MSW service worker in the browser, so the app runs without a backend.

## Mock API

`src/mocks/` holds MSW handlers that follow `docs/api/booking-api.md`. The same handlers serve the tests (`src/mocks/server.ts`) and the dev server (`src/mocks/browser.ts`).

```bash
VITE_USE_MOCKS=true npm run dev
```

Any non-empty login works. The mock user is a staff member, so all seed resources are visible. The mocks are loaded only when `VITE_USE_MOCKS=true` and are not part of normal bundles. If `public/mockServiceWorker.js` is ever missing, regenerate it with `npx msw init public/ --no-save`.
