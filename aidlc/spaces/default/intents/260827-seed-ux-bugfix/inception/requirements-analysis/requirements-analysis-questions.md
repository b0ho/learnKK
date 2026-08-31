# Requirements Analysis — Clarifying Questions

Intent: `260827-seed-ux-bugfix` (scope: bugfix, minimal depth)

The two bugs are well-specified. Only one genuine decision remains — how to
remediate the seed data for bug #1. Bug #2 (meeting-list apply state) has a
clear technical fix and no open decision.

---

## Q1. Seed remediation for the CS-study 수료 후보 (bug #1, HIGH)

The demo seed (`V12__seed_demo.sql`) marks CS-study 멘티2 as
`COMPLETION_CANDIDATE` with `attended=3, total=4` (75%), which violates the
80% rule (`a*100 >= 80*S` → `300 >= 320` = false). This lets an admin confirm a
"75% 수료확정" that the normal compute→approve flow can never produce.

Which remediation do you want?

A. Change 멘티2 to `NOT_COMPLETED` (3/4). Faithful to the 75% reality, but the
   demo loses its `COMPLETION_CANDIDATE` example entirely.
B. Make 멘티2 a genuine candidate at 4/4 — bump attendance to all 4 weeks and
   set `attended=4, total=4`, status stays `COMPLETION_CANDIDATE`. Satisfies the
   80% rule, preserves the candidate demo state, minimal SQL change. (멘티1 stays
   4/4 CONFIRMED; the two differ only by judgement state — a clean demo of
   compute→approve.)
C. Change the candidate example to 4/5 (80%) — requires making CS-study a
   5-week/5-session meeting and adding a 5th attendance row (larger cascade).
D. Both bug #1 items and drop the candidate concept differently (specify).
X. Other (please specify)

[Answer]: B

---

## Q2. Anything to add for next time? (memory ritual)

Any prescriptive rule or verification check from this stage worth persisting
for future runs?

A. Nothing to add.
B. Add a rule/sensor (please specify).
X. Other (please specify)

[Answer]: A
