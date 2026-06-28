---
name: checklist
description: >
  Emits the quality/acceptance checklist gate for a completed feature — deriving
  checklist items from the spec's acceptance criteria, verifying task completion,
  and delegating all gate execution to the existing `run-quality-gate` skill.
  Use this skill after implementation is complete, when the user says "run the
  checklist", "final check", "acceptance gate", or "is this feature done". Never
  reimplements the mvn-verify/JaCoCo/secret-grep logic owned by `run-quality-gate`.
  The SDD "implement" stage maps to the existing `gabi-core-dev`, `access-dev`, and
  `rag-dev` agents — no new coding agent is added by this pipeline.
version: 0.1.0
principles_applied:
  inherited:
    - P1 — Source-of-Truth Grounding
    - P2 — Full Determinism
    - P3 — Systematicity
    - P4 — Consistency
    - P5 — Context Budget Discipline
    - P6 — Self-Containment
    - P7 — Reference Hygiene
    - P8 — Principles Inheritance
    - P9 — Role Separation
    - P10 — Exit-Status Determinism
    - P11 — Programmatic Determinism
    - P12 — Maximal-Effort Completeness
    - P13 — Token Economy
  refs:
    - "R17 Engineering Disciplines; R18=P11 Programmatic Determinism: D:/Documentos/Recursos/Recursos IA/Repo Enhancer/repo-enhancer/orchestrator.md CONVENTIONS"
  custom:
    - id: C1
      name: Gate Delegation
      requires: this skill must not run `mvn verify`, grep source files for
        identifier sinks, or measure JaCoCo coverage directly. All gate execution
        is delegated to the `run-quality-gate` skill. A checklist verdict of PASS
        requires `run-quality-gate` to report VERDICT: GREEN. A PASS verdict is not
        emitted if the gate was skipped or returned VERDICT: RED.
      rationale: Single-point gate ownership (P4) — `run-quality-gate` is GABI's
        authoritative gate (mvn verify + JaCoCo scope sanity-check + secret grep);
        a parallel gate risks drift and undermines the invariant guarantee.
---

# Checklist

Emits the acceptance checklist for a completed feature and delegates quality-gate execution to `run-quality-gate`.

## Workflow

### Step 1: Gate — artifacts and analyze output must exist
Verify `.claude/sdd/spec.md` (Status: CLARIFIED) and `.claude/sdd/tasks.md` exist, and that an `analyze` report has been produced (in context or as a file). If any is missing, STOP: "Run `analyze` first."

### Step 2: Derive acceptance checklist items
From `.claude/sdd/spec.md` Acceptance Criteria section, generate one checklist item per criterion:

```
[ ] (FR-N/AC-M) <criterion text>
```

If `.claude/instructions/sdd-constitution.md` is available (load it), also emit one item per constitution-defined invariant (Rules 8–17 and Rule 21). Always include at minimum:

```
[ ] SQL-identifier validation (Rule 8): every identifier sink routed through
    IdentifierValidator.validate(); no raw + name + / String.format interpolation
[ ] Password handling (Rule 9): no raw password concatenation into Derby SET PROPERTY
[ ] Privileged surface gating (Rule 10): no privileged user-admin operation as
    @Tool/@McpTool or REST route; surface is read-only + RAG + health only
[ ] Credential hygiene (Rule 11): no literal credential in configuration files,
    Javadoc, fixtures, or checkpoints; all use ${ENV_VAR:} placeholders
[ ] application.yml single spring: key (Rule 12): exactly one top-level spring: key;
    no --- separator splitting the document
[ ] RAG graceful degradation (Rule 13): exactly one RagService bean per configuration;
    NoOpRagService active when EmbeddingModel is absent
[ ] JaCoCo gate scoping (Rule 14): check rule uses element PACKAGE or CLASS (not BUNDLE);
    ≥0.90 LINE/INSTRUCTION on core; bound to verify phase
[ ] Spring idioms + pinned versions (Rule 15): constructor injection throughout;
    JDK 17 and Derby 10.16.1.1 unchanged; no invented APIs
[ ] In-memory Derby tests (Rule 16): DB tests use InMemoryDerbyConfig; no live
    network Derby; no real embedding model in tests
[ ] No secrets or build artifacts (Rule 17): no credentials or build output committed
[ ] Coverage gate (Rule 21): mvn verify passes with JaCoCo ≥0.90 on core
[ ] Branch policy: all commits on enhancement branch, not main/master
[ ] Quality gate: run-quality-gate reports VERDICT: GREEN
```

### Step 3: Verify task completion
Read `.claude/sdd/tasks.md`. For each task, check its Status field:
- `DONE` — satisfied.
- `TODO` or `IN PROGRESS` — checklist item flagged as incomplete.

Add one checklist item per task:
```
[ ] T001: <title> — <DONE|TODO|IN PROGRESS>
```

A checklist with any non-DONE task cannot yield a PASS verdict.

### Step 4: Delegate gate execution (C1)
Invoke the `run-quality-gate` skill. Record its full output verbatim under the "Quality Gate" heading. Do not filter, reinterpret, or re-run it.

If `run-quality-gate` is unavailable, record:
```
Quality Gate: BLOCKED — run-quality-gate skill not found; verdict cannot be determined.
```
and halt with BLOCKED status.

### Step 5: Emit checklist verdict
- `CHECKLIST: PASS` — all acceptance criteria items checked, all tasks DONE, run-quality-gate reports `VERDICT: GREEN`.
- `CHECKLIST: FAIL` — otherwise; list each failing item.
- `CHECKLIST: BLOCKED` — run-quality-gate unavailable.

Note: The "implement" stage of the SDD pipeline is executed by the existing `gabi-core-dev`, `access-dev`, and `rag-dev` agents. This checklist skill does not add or replace those agents — it gates their output.

## Output Format

```
CHECKLIST: <Feature Name>
Verdict: PASS | FAIL | BLOCKED
Date: <YYYY-MM-DD>

## Acceptance Criteria
- [x/ ] (FR-1/AC-1) <criterion text>
- [x/ ] (FR-1/AC-2) <criterion text>
...

## Task Completion
- [x/ ] T001: <title> — DONE | TODO | IN PROGRESS
...

## Quality Gate
<run-quality-gate output verbatim>

## Constitution Gates
- [x/ ] SQL-identifier validation (Rule 8)
- [x/ ] Password handling (Rule 9)
- [x/ ] Privileged surface gating (Rule 10)
- [x/ ] Credential hygiene (Rule 11)
- [x/ ] application.yml single spring: key (Rule 12)
- [x/ ] RAG graceful degradation (Rule 13)
- [x/ ] JaCoCo gate scoping (Rule 14)
- [x/ ] Spring idioms + pinned versions (Rule 15)
- [x/ ] In-memory Derby tests (Rule 16)
- [x/ ] No secrets or build artifacts (Rule 17)
- [x/ ] Coverage gate ≥0.90 on core (Rule 21)
- [x/ ] Branch policy

Verdict: CHECKLIST: PASS | FAIL | BLOCKED
Failing items: <list or "none">
```

## Self-Containment Index

This skill package contains everything needed for its complete usage:
- SKILL.md (this file): workflow, output format

External dependencies:
- `.claude/sdd/spec.md` — acceptance criteria source; must exist.
- `.claude/sdd/tasks.md` — task completion status; must exist.
- `run-quality-gate` skill (`.claude/skills/run-quality-gate/SKILL.md`) — invoked in Step 4; must be present for a PASS verdict. If missing: halt with BLOCKED.
- `.claude/instructions/sdd-constitution.md` — constitution-level gate items (Rules 8–17, 21); loaded in Step 2 if available. If missing: use the default invariant checklist items stated in Step 2; note gap.

## Sources
- User requirement: SDD pipeline stage-6 skill (checklist) for GABI Group E.
- SDD pipeline: asset-metaprompting `references/software-development.md §2`.
- `references/claude.md §SKILL`; `templates/claude_skill.md`.
- `D:/Documentos/Recursos/Recursos IA/Repo Enhancer/repo-enhancer/orchestrator.md` CONVENTIONS (R17/R18).
