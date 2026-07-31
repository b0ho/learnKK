---
name: learning-crew-local-mvp
depth: Standard
keywords: []
description: Design-only workflow for the learnKK learning-crew platform — ideation and inception through detailed design, stopping before implementation
skeleton: off
runner: false
---

# learning-crew-local-mvp scope

A design-only workflow for the learnKK (러닝 크루) learning-crew mobile
web-view platform. It runs the full ideation and inception ceremony —
through detailed design — and then STOPS before implementation. The point
of this scope is to produce a complete, reviewed design that three
developers can carry into their own per-team implementation workflows.

## Why these stages, why skip those

Ideation runs at full strength on purpose. `market-research` is EXECUTE
because the team needs competitor and reference research before committing
to the product shape. `feasibility` is EXECUTE to confirm the mobile
web-view approach is viable before design work compounds on top of it.
`team-formation` is EXECUTE because the work is partitioned across three
developers, and that split has to be decided before delivery planning can
sequence it.

Inception runs through detailed design: `requirements-analysis`,
`user-stories`, `refined-mockups`, `application-design`, `units-generation`,
`delivery-planning`, `functional-design`, and `nfr-requirements` all
EXECUTE so that every Bolt is specified down to a level a developer can
implement independently. `practices-discovery` EXECUTE captures the
conventions the three implementation streams will share.

Everything from implementation onward is deferred. `code-generation`,
`build-and-test`, and `ci-pipeline` SKIP because implementation is handed
off to three separate per-team workflows — one developer per assigned Bolt.
`nfr-design` and `infrastructure-design` SKIP here; those decisions belong
with the teams that will build against them. The entire operation phase
(`deployment-pipeline`, `environment-provisioning`, `deployment-execution`,
`observability-setup`, `incident-response`, `performance-validation`,
`feedback-optimization`) SKIPs — a design-only workflow never reaches
production. `approval-handoff` and `reverse-engineering` SKIP: this is a
greenfield build with no existing codebase to map, and the ideation→inception
handoff is carried inline.

## Membership

Not inferable — `keywords: []`, so this scope resolves only by explicit
`--scope learning-crew-local-mvp`. Initialization, the full ideation set
(minus approval-handoff), and inception through detailed design run;
construction implementation and the whole operation phase are deferred to
the per-developer implementation workflows.
