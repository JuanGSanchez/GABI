---
name: plan
description: >
  Produces the technical implementation plan (the how) from a clarified feature
  spec — identifying affected components, defining the implementation strategy,
  and enforcing the layered-architecture and security invariants from
  sdd-constitution.md. Use this skill after `clarify` sets the spec to CLARIFIED,
  when the user says "write the plan", "plan this feature", "how do we implement
  this". Delegates all coding to gabi-core-dev / access-dev / rag-dev / test-author
  agents; writes no code.
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
      name: Core Security Discipline
      requires: the plan must not bypass IdentifierValidator for any SQL-identifier
        sink, expose privileged user-admin operations on the MCP/REST surface, or
        embed literal credentials. Any plan component that violates one of these three
        boundaries is flagged and must be redesigned before tasks are derived.
      rationale: The security triad (sdd-constitution.md Rules 8–11 / C-SEC) is the
        central GABI invariant; a plan that bypasses it silently opens injection sinks
        or leaks credentials — the most damaging possible regressions.
---

# Plan

Derives the technical implementation plan (the how) from a clarified spec, enforcing GABI's layered architecture and all java-spring-conventions.md security invariants.

## Workflow

### Step 1: Gate — spec.md must be CLARIFIED
Read `.claude/sdd/spec.md`. If it does not exist or Status is not CLARIFIED, STOP: "Run `specify` then `clarify` first."

### Step 2: Load governing instructions (just-in-time)
Read `.claude/instructions/sdd-constitution.md` for project-wide gates (9 invariants, layer rules, coverage gate). If the feature touches the access layer or RAG, also read `.claude/instructions/spring-boot-best-practices.md`. Load only these two; do not load other instructions.

If either file is missing, note the gap in plan.md and apply C1 and the default coverage gate (≥0.90 on core) as fallback.

### Step 3: Identify affected components
For each functional requirement in spec.md, determine which layer(s) change:
- **core** (`src/main/java/core/LibraryService`, `LibraryServiceImpl`, `IdentifierValidator`, domain models) — all domain logic; owner: gabi-core-dev.
- **access** (`src/main/java/access/mcp/LibraryMcpTools`, `access/rest/LibraryRestController`) — MCP and REST exposure, delegating to core; owner: access-dev.
- **rag** (`src/main/java/rag/RagService`, `RagServiceImpl`, `RagConfig`) — RAG retrieval and configuration; owner: rag-dev.
- **tests** (`src/test/`) — always affected; owner: test-author.
- **packaging** (`packaging/` + `pom.xml`) — owner: packaging-builder (only if build or dependency changes).

Apply C1 security checks:
- SQL-identifier sink check (sdd-constitution.md Rule 8): does any component change add or touch a sink? If so, confirm IdentifierValidator is invoked at every entry point; flag any raw `+ name +` / `String.format` pattern as a C1 violation requiring redesign.
- Privileged surface check (Rule 10): does any component add a new `@Tool`/`@McpTool` or REST route? Confirm it is read-only + RAG + health semantics. Any write-semantic operation STOP and confirm with the user before including it in the plan.
- Credential check (Rule 11): does any component reference configuration files? Confirm only `${ENV_VAR:}` placeholders appear; no literals.

Apply architecture checks:
- Layered architecture: domain logic must live in `LibraryServiceImpl`, not in `LibraryMcpTools` or `LibraryRestController`.
- RAG degradation (Rule 13): if RAG configuration is modified, confirm `NoOpRagService` fallback remains active when `EmbeddingModel` is absent.
- application.yml integrity (Rule 12): if `application.yml` is modified, confirm exactly one top-level `spring:` key.

### Step 4: Define implementation strategy
For each affected component describe:
- What changes (method additions or modifications in named classes).
- New data contracts (parameter types and return types) if any.
- How default behaviour is preserved for callers that do not use the new capability.
- Owner agent: `gabi-core-dev`, `access-dev`, `rag-dev`, `test-author`, `docs-writer`, or `packaging-builder`.

Keep descriptions at "what changes and why" — no code. Cite existing identifiers (e.g. `LibraryService.search`, `IdentifierValidator.validate`) only if they exist in the repo.

### Step 5: Write .claude/sdd/plan.md
Create or overwrite `.claude/sdd/plan.md` using the output format below.

## Output Format

`.claude/sdd/plan.md`:

```
# Implementation Plan: <Feature Name>
Spec: .claude/sdd/spec.md (CLARIFIED)
Date: <YYYY-MM-DD>

## Affected Components
| Component                                      | Change summary | Owner agent      |
|------------------------------------------------|----------------|------------------|
| src/main/java/core/LibraryServiceImpl          | …              | gabi-core-dev    |
| src/main/java/access/mcp/LibraryMcpTools       | …              | access-dev       |
| src/main/java/rag/RagConfig                    | …              | rag-dev          |
| src/test/java/…                                | …              | test-author      |
...

## Implementation Strategy
### <Component>
<What changes, data contracts, default-preservation approach>

## Architecture Gate Checks (C1 / sdd-constitution.md Rules 8–17)
- SQL-identifier validation (Rule 8): PASS | FAIL — <reason if FAIL>
- Password handling (Rule 9): PASS | N/A — <reason>
- Privileged surface gating (Rule 10): PASS | FAIL — <reason>
- Credential hygiene (Rule 11): PASS | N/A — <reason>
- application.yml single spring: key (Rule 12): PASS | N/A — <reason>
- RAG graceful degradation (Rule 13): PASS | N/A — <reason>
- JaCoCo gate scoping (Rule 14): PASS | N/A
- Spring idioms + pinned versions (Rule 15): PASS | FAIL — <reason>
- No secrets or build artifacts (Rule 17): PASS
- Coverage gate feasibility: <expected coverage delta>

## Component Sequencing
<Which component must be complete before which>
```

Summary line emitted after write: `plan.md written — <N> components, architecture gate checks: PASS|FAIL.`

## Self-Containment Index

This skill package contains everything needed for its complete usage:
- SKILL.md (this file): workflow, output format

External dependencies:
- `.claude/sdd/spec.md` — input artifact (Status: CLARIFIED).
- `.claude/instructions/sdd-constitution.md` — project-wide gates and all invariants; loaded in Step 2. If missing: apply C1 and default coverage gate (≥0.90 on core); note gap.
- `.claude/instructions/spring-boot-best-practices.md` — Spring AI MCP/RAG guidance; load only if a requirement touches access or RAG layers. If missing: apply sdd-constitution.md directly; note gap.
- `.claude/agents/gabi-core-dev.md`, `gabi-access-dev.md`, `gabi-rag-dev.md` — referenced for owner assignment. If any is missing: assign by layer convention (gabi-core-dev for core; access-dev for access; rag-dev for rag).

## Sources
- User requirement: SDD pipeline stage-3 skill (plan) for GABI Group E.
- SDD pipeline: asset-metaprompting `references/software-development.md §2`.
- GABI layer conventions: `src/main/java/` directory structure, CLAUDE.md §Architecture.
- `references/claude.md §SKILL`; `templates/claude_skill.md`.
- `D:/Documentos/Recursos/Recursos IA/Repo Enhancer/repo-enhancer/orchestrator.md` CONVENTIONS (R17/R18).
