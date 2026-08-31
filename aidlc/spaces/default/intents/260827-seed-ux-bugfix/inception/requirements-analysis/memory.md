<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is maintained by the orchestrator during stage execution. Add observations at the gate ritual, not by editing here directly.

## Interpretations
- 2026-08-27T00:00:00Z — Treated request as minimal-depth bugfix with two well-defined items; user supplied remediation options, so only one genuine decision (seed remediation choice) needs confirmation.
- 2026-08-27T00:00:00Z — Bug #2 confirmed as pure-frontend fix: `enrollmentsApi.listMine()` already exposes the mentee's enrollments (meetingId + status), so applied-state can be pre-marked on list load without any backend change.

## Deviations
- 2026-08-27T00:00:00Z — Skipped full reverse-engineering re-scan; existing CodeKB (2026-08-24) is adequate for a 2-file targeted bugfix.

## Tradeoffs
- 2026-08-27T00:00:00Z — Seed bug #1: NOT_COMPLETED (faithful to 3/4) drops the candidate demo state; 4/4-candidate preserves the candidate state and satisfies the 80% rule with the least SQL cascade. Surfaced as a decision for the user.

## Open questions
- 2026-08-27T00:00:00Z — [RESOLVED] Seed remediation for CS-study 멘티2: user chose B (genuine candidate at 4/4). Q2: nothing to persist.
