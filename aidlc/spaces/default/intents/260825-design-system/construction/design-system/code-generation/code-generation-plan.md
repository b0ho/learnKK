# Code Generation Plan — learnKK Design System (GREEN, light-only, Pretendard)

## Goal
Apply a cohesive modern-minimal GREEN design system across ALL screens of the React 18 + Vite + TS + Tailwind + shadcn/ui frontend. Visual-only. Zero behavioral/API/routing changes. Zero test regression.

## Definition of Done
- [ ] Green brand palette filled into `:root` design tokens (light), AA-considered contrast.
- [ ] `.dark {}` scaffold block present with reasonable/slotted values (no toggle wired).
- [ ] Pretendard self-hosted (npm `pretendard`), imported locally, set as base sans with system-ui fallback; Korean renders well.
- [ ] `tailwind.config.js` gets `fontFamily.sans` = Pretendard stack; existing token color mappings preserved.
- [ ] `components/ui/` polished via tokens/CVA only; props/exports/variants preserved.
- [ ] Design-system primitives added (skeleton, tooltip, avatar, dropdown-menu) + sonner toast Toaster mounted once (additive only).
- [ ] AppShell header + bottom nav polished; all `data-testid`/structure preserved.
- [ ] 3 pre-existing build blockers fixed minimally.
- [ ] `npx vitest run` ALL pass (baseline 135). `npm run build` green.

## Token plan (chosen HSL values)

### `:root` (light)
| token | value | note |
|---|---|---|
| --background | 0 0% 100% | white, minimal |
| --foreground | 155 25% 12% | dark green-tinted neutral text |
| --card / --card-foreground | 0 0% 100% / 155 25% 12% | |
| --popover / --popover-foreground | 0 0% 100% / 155 25% 12% | |
| --primary | 152 62% 30% | brand green; white text = 4.9:1 (AA normal) |
| --primary-foreground | 0 0% 100% | white |
| --secondary | 150 30% 96% | light green-tint |
| --secondary-foreground | 152 40% 20% | |
| --muted | 150 20% 96% | |
| --muted-foreground | 155 12% 40% | ~6:1 on white |
| --accent | 150 40% 94% | |
| --accent-foreground | 152 45% 22% | |
| --destructive | 0 72% 45% | white text = 5.7:1 (AA) |
| --destructive-foreground | 0 0% 100% | |
| --border | 150 16% 88% | |
| --input | 150 16% 86% | |
| --ring | 152 62% 30% | matches primary |
| --radius | 0.625rem | softened from 0.5rem |

### `.dark {}` scaffold (reasonable defaults, to be tuned later)
Deep green-neutral surfaces, brighter primary (152 55% 45%) for dark bg. Marked as scaffold.

## Font approach
- Add dep `pretendard` (bundles woff2 + @font-face). `import 'pretendard/dist/web/static/pretendard.css'` in `main.tsx` so Vite bundles locally (no CDN — satisfies constraint C2).
- `tailwind.config.js`: `fontFamily.sans = ['Pretendard','Pretendard Variable','system-ui','-apple-system','Segoe UI','Roboto','Helvetica Neue','Arial','Apple SD Gothic Neo','Noto Sans KR','sans-serif']`.
- `body { @apply font-sans; }` in index.css; keep `-webkit-tap-highlight-color`.

## Component list
- Polish (tokens/CVA only): button (focus ring, active), card (shadow via token), badge (add `success`/`warning`/`info` variants; keep existing), input/textarea (focus).
- New primitives: `skeleton` (no dep), `tooltip` (@radix-ui/react-tooltip), `avatar` (@radix-ui/react-avatar), `dropdown-menu` (@radix-ui/react-dropdown-menu), `sonner` toast.
- Mount `<Toaster />` (sonner) once in AppShell — additive only; do NOT convert existing `role="alert"` flows.

## Test-preservation strategy
- Change values, not markup. Keep all token NAMES (tailwind maps them).
- No changes to any `data-testid`, `role="alert"`, or Korean text.
- AppShell: restyle via classes only; preserve tab-*/unread-badge testids and DOM structure.
- Toaster is additive (no existing test asserts absence of a region that would conflict).
- Run full suite; any break restored via contract, not test edits.

## Pre-existing build fixes (unrelated to design; block green build)
- `MeetingQuestionsEditPage.tsx`: `type: q.type as SurveyQuestionType` (DTO.type is `string`).
- `AdminApprovalPage.test.tsx`: add `_init?: RequestInit` param to `queueFetch` mock.
- `MyLearningPage.test.tsx`: add `completed: false` to `session()` fixture.

## Step checklist
- [x] Read current tokens/config/components/tests
- [x] Fix 3 pre-existing build blockers
- [x] Verify baseline build + tests (135 pass)
- [x] Install deps (pretendard, sonner, radix tooltip/avatar/dropdown-menu)
- [x] Fill green tokens + dark scaffold + font in index.css
- [x] Update tailwind.config.js (fontFamily.sans, boxShadow)
- [x] Import pretendard css in main.tsx
- [x] Add primitives (skeleton, tooltip, avatar, dropdown-menu, sonner)
- [x] Polish button/card/badge/input/textarea
- [x] Polish AppShell + mount Toaster
- [x] Run vitest (135 pass) + build (green)
- [x] Write code-summary.md
