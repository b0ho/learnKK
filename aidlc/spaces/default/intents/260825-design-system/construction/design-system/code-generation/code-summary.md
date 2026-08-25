# Code Summary — learnKK Design System (GREEN, light-only, Pretendard)

Branch: `design-system` (NOT committed). Visual-only. Zero test regression.

## Result
- `npx vitest run`: **135 passed / 135** (28 files) — same as baseline, no regression.
- `npm run build` (tsc -b && vite build): **green**. Pretendard woff2/woff bundled locally into `dist/assets/` (no external CDN — satisfies constraint C2).
- `eslint` on all changed/new files: clean.

## Files changed
| File | Change |
|---|---|
| `frontend/src/index.css` | Filled GREEN `:root` light tokens; added status tokens (success/warning/info); added `.dark {}` scaffold block (not wired); softened `--radius` to 0.625rem; `body` now uses `font-sans` + antialiasing; kept `-webkit-tap-highlight-color`. |
| `frontend/tailwind.config.js` | Added `fontFamily.sans` = Pretendard stack (system-ui + Korean fallbacks); added `success/warning/info` color mappings; added subtle `boxShadow` scale. Kept all existing token color mappings. |
| `frontend/src/main.tsx` | `import 'pretendard/dist/web/static/pretendard.css'` (self-hosted, Vite-bundled). |
| `frontend/src/components/ui/button.tsx` | Token/CVA polish: shadow-sm on default/destructive/outline, active states, subtle active scale. Variants/sizes/props/exports unchanged. |
| `frontend/src/components/ui/badge.tsx` | Added `success`/`warning`/`info` variants. Existing default/secondary/destructive/outline unchanged. |
| `frontend/src/components/ui/input.tsx` | shadow-sm, transition, hover border via ring token. Markup/props unchanged. |
| `frontend/src/components/ui/textarea.tsx` | Same polish as input. Markup/props unchanged. |
| `frontend/src/routes/AppShell.tsx` | Sticky translucent header (brand-green wordmark), sticky bottom nav with subtle top shadow + active-tab top accent bar via `after:`. Mounted `<Toaster />` (additive). ALL `data-testid` (tab-*, unread-badge, app-content) + DOM structure preserved. |
| `frontend/package.json` | Added deps: `pretendard@1.3.9`, `sonner@2.0.8`, `@radix-ui/react-tooltip@1.2.16`, `@radix-ui/react-avatar@1.2.6`, `@radix-ui/react-dropdown-menu@2.1.24`. |

## New files (design-system primitives)
- `frontend/src/components/ui/skeleton.tsx` — no new dep; additive loading placeholder.
- `frontend/src/components/ui/tooltip.tsx` — Radix tooltip, token-styled.
- `frontend/src/components/ui/avatar.tsx` — Radix avatar, token-styled.
- `frontend/src/components/ui/dropdown-menu.tsx` — Radix dropdown-menu, token-styled.
- `frontend/src/components/ui/sonner.tsx` — token-styled Toaster (light theme), mounted once in AppShell.

## Token values chosen (light `:root`)
- `--primary: 152 62% 30%` (brand GREEN) — white `--primary-foreground` ≈ **4.9:1** (AA normal text).
- `--foreground: 155 25% 12%`; `--background: 0 0% 100%`.
- `--secondary: 150 30% 96%` / fg `152 40% 20%`; `--muted: 150 20% 96%` / fg `155 12% 40%`; `--accent: 150 40% 94%` / fg `152 45% 22%`.
- `--destructive: 0 72% 45%` — white text ≈ **5.7:1** (AA); `--border: 150 16% 88%`; `--input: 150 16% 86%`; `--ring: 152 62% 30%`.
- Status: `--success` (green), `--warning: 38 92% 42%`, `--info: 205 70% 40%`, each with white foreground.
- `--radius: 0.625rem`.

## Font wiring
Pretendard self-hosted via the `pretendard` npm package; the **variable** font CSS (`pretendard/dist/web/variable/pretendardvariable.css`) imported in `main.tsx` so Vite bundles a single `PretendardVariable` woff2 (~2MB, all weights) locally — no external CDN (C2), and far lighter than the 9 static weights (mobile-first NFR1). Set as base `fontFamily.sans` (`'Pretendard Variable'` first, then `'Pretendard'`, `system-ui`, Korean fallbacks `Apple SD Gothic Neo`/`Noto Sans KR`/`Malgun Gothic`); `body` applies `font-sans`.

## How test contracts were preserved
- Only token VALUES changed; token NAMES kept (tailwind.config maps them) so no class churn.
- No `data-testid`, `role="alert"`, or Korean loading/empty/error strings were altered.
- State UI was NOT componentized — restyling happens purely through tokens/utility classes, so all exact-string / testid / role assertions still hold.
- Toaster is additive; existing `role="alert"` error flows untouched (no conversion to toasts).
- AppShell restyle is class-only; tab-*/unread-badge/app-content testids and structure intact (AppShell.test.tsx passes).

## Pre-existing build fixes (unrelated to design; were blocking a green build on base branch)
1. `MeetingQuestionsEditPage.tsx` — `toDrafts` now casts `q.type as SurveyQuestionType` (DTO `type` is `string`). Import added.
2. `AdminApprovalPage.test.tsx` — `queueFetch` mock signature widened to `(url, _init?: RequestInit)` so `c[1]?.method` type-checks.
3. `MyLearningPage.test.tsx` — `session()` fixture now includes `completed: false` (required by `MeetingSessionResponse`).

## Deferred
- **Dark theme**: `.dark {}` block is a scaffold with reasonable starting values but is NOT wired to any toggle; tune contrast before enabling.
- Newly added primitives (tooltip/avatar/dropdown-menu) are available for future use; only skeleton + sonner Toaster are lightly wired. Existing loading states left as-is intentionally to protect test contracts.
- `npm audit` reports pre-existing advisories (not introduced by scope); left untouched.
