---
name: tasks
description: >
  Derives an ordered, dependency-aware task list from a technical implementation
  plan — each task carrying an ID, title, dependency list, owner agent, done
  criterion, test criterion, and status. Produces tasks.md. Use this skill after
  `plan` produces plan.md, when the user says "break this into tasks", "task
  list", "what do we do first", or when the analyze stage reports missing task
  coverage.
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
      name: Dependency-Ordered Tasks
      requires: no task in tasks.md may reference an artifact or capability that
        is produced by a later task. The ordering must be a valid topological sort
        of the dependency graph. A dependency cycle is a planning error and is
        surfaced to the user before tasks.md is written.
      rationale: Out-of-order tasks cause implementation failures when an agent
        attempts work whose prerequisite has not yet been delivered.
---

# Tasks

Derives an ordered, dependency-aware task list from plan.md so each task can be executed sequentially by the owning agent.

## Workflow

### Step 1: Gate — plan.md must exist
Read `.claude/sdd/plan.md`. If it does not exist, STOP: "Run the `plan` skill first."

### Step 2: Decompose into atomic tasks
For each component change in plan.md, derive one or more atomic tasks. A task is atomic if: it produces a single verifiable artifact or change; it can be assigned to one agent; and its done/not-done state is unambiguous.

Assign each task:
- `id` — `T<NNN>` (zero-padded three digits, sequential).
- `title` — imperative verb phrase, 8 words or fewer.
- `depends_on` — list of T-IDs whose output this task requires; `—` if none.
- `owner` — one of: `gabi-core-dev`, `access-dev`, `rag-dev`, `test-author`, `docs-writer`, `packaging-builder`.
- `done` — one-line feature-level done criterion, matching the spec's acceptance criteria where applicable.
- `test` — the specific test(s) that must pass before the task is marked done.
- `status` — `TODO` (all tasks start here; only the owning agent or user may update).

Apply constitution-derived sequencing rules (`.claude/instructions/sdd-constitution.md`):
- If a task touches any SQL-identifier sink (derby.user.* SET PROPERTY, GRANT, REVOKE, DROP/CREATE TABLE name, or any new sink): precede it with an "IdentifierValidator coverage re-check" task (owner: gabi-core-dev; done criterion: "no raw + name + or String.format identifier interpolation remains in any sink; IdentifierValidator.validate called at every entry point") as a prerequisite for every task that modifies that sink.
- If a task adds or modifies a MCP/REST-exposed operation: add an "exposed-surface review" task (owner: access-dev; done criterion: "no privileged admin operation appears as @Tool/@McpTool or REST route; new operation is read-only + RAG semantics or has explicit human authorization") before any other access-layer task is marked done.
- If a task touches credential configuration (configuration.properties, application*.yml, Javadoc examples, test fixtures): add a "credential hygiene check" task (owner: gabi-core-dev; done criterion: "no literal credential in any committed file; all references use ${ENV_VAR:} placeholders") before any task that modifies those files is marked done.
- If a task modifies RAG beans or `RagConfig`: add a "RAG degradation test" task (owner: rag-dev; done criterion: "exactly one RagService bean in every configuration; NoOpRagService active when EmbeddingModel absent; no NoUniqueBeanDefinitionException on startup") before any other RAG task is marked done.
- Test tasks (owner: `test-author`) must always depend on the implementation tasks whose output they verify.

### Step 3: Validate ordering (C1)
Build the dependency graph and check for cycles. If a cycle is detected, STOP: list the cycle task IDs and ask the user to resolve the dependency conflict. Do not write tasks.md until the graph is acyclic.

### Step 4: Write .claude/sdd/tasks.md
Create or overwrite `.claude/sdd/tasks.md` using the output format below. List tasks in valid execution order (all dependencies appear above their dependents).

## Output Format

`.claude/sdd/tasks.md`:

```
# Task List: <Feature Name>
Plan: .claude/sdd/plan.md
Date: <YYYY-MM-DD>

## Tasks
| ID   | Title                                  | Depends on | Owner         | Status | Done criterion     | Test criterion           |
|------|----------------------------------------|------------|---------------|--------|--------------------|--------------------------|
| T001 | …                                      | —          | gabi-core-dev | TODO   | …                  | …                        |
| T002 | …                                      | T001       | test-author   | TODO   | …                  | …                        |
...
```

Summary line emitted after write: `tasks.md written — <N> tasks, topological order validated.`

## Self-Containment Index

This skill package contains everything needed for its complete usage:
- SKILL.md (this file): workflow, output format

External dependencies:
- `.claude/sdd/plan.md` — input artifact.
- `.claude/sdd/spec.md` — consulted for acceptance criteria wording. If missing: derive done/test criteria from plan component descriptions.
- `.claude/instructions/sdd-constitution.md` — consulted for constitution-derived sequencing rules (IdentifierValidator re-check, exposed-surface review, credential hygiene check, RAG degradation test). If missing: note the gap and apply the sequencing rules stated in Step 2 of this skill.

## Sources
- User requirement: SDD pipeline stage-4 skill (tasks) for GABI Group E.
- SDD pipeline: asset-metaprompting `references/software-development.md §2`.
- `references/claude.md §SKILL`; `templates/claude_skill.md`.
- `D:/Documentos/Recursos/Recursos IA/Repo Enhancer/repo-enhancer/orchestrator.md` CONVENTIONS (R17/R18).
