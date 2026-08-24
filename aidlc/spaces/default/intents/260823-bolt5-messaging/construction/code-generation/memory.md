# Code Generation — Stage Diary (Bolt 5 Messaging / U7)

Stage: code-generation | Phase: construction | Unit: bolt5-messaging
Design inherited from: `aidlc/spaces/default/intents/260731-learnkk-crew/construction/U7-messaging/`

## Interpretations

- 2026-08-23T15:40:00Z — {unit-name} resolves to `bolt5-messaging` (mirrors Bolt 3's `bolt3-enrollment` record dir); design source is the parent `learnkk-crew` intent's `U7-messaging/` artifacts, not this intent's construction dir (which is empty by the jump-to-code-generation pattern). Consistent with how Bolt 3 inherited U4 design.
- 2026-08-23T15:40:00Z — Included a `GET /api/messages/recipients` read endpoint. Not one of the four C6 MessageService methods (send/listThreads/getThread/unreadCount), but it directly implements the functional-design FE screen "발신 대상 선택: 권한 있는 상대만 노출" — treated as a UX-supporting refinement; server `send()` re-validates via `canMessage` (403) as the authority.

## Deviations

- (none yet)

## Tradeoffs

- 2026-08-23T15:40:00Z — Cross-module authorization (`canMessage`) composes via MeetingService/EnrollmentService read methods (ADR-007: never join across module tables). Adds a few package-visible parameter-only read methods (meeting ids owned by a mentor, active enrollment relationship) since every existing read is Principal-authorized. Chose service-composition over a shared query to preserve module ownership.
- 2026-08-23T15:40:00Z — "active enrollment" = status `APPLIED` (the only non-cancelled literal in U4's enum). Design S1 warned against a lone `APPLIED` literal breaking post-②; but U4's current enum has no post-start status — `APPLIED` IS the active-relationship literal in this codebase. Documented so a future U4 status expansion revisits this.

## Open questions

- 2026-08-23T15:40:00Z — Polling interval for the unread badge is an FE setting ([assumption] in design). Will pick a reasonable default (e.g. 30s) in the FE.
