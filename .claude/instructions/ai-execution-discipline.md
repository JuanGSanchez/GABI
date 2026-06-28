# Instruction — AI Execution Discipline (GABI)

Shared behavioral contract for every GABI maintenance/evolution subagent. Agents reference this file by name
instead of restating it. Scope: any GABI subagent that reads, edits, tests, packages, documents, or reviews
the repository at `D:\Documentos\GitHub\GABI`.

## Principles Applied
Inherited:
- P1 — Source-of-Truth Grounding (every action traces to a cited GABI file/line)
- P2 — Full Determinism (same item → same change)
- P3 — Systematicity (decision points below)
- P4 — Consistency (one discipline across all agents)
- P5 — Context Budget Discipline (Gleaner threshold ≥5 files; checkpoint at ~70% context — rule 8)
- P6 — Self-Containment (this file + the cited repo files are sufficient)
- P7 — Reference Hygiene (cite source location; never restate)
- P8 — Principles Inheritance
- P9 — Role Separation
- P10 — Exit-Status Determinism
- P11 — Programmatic Determinism
- P12 — Maximal-Effort Completeness
- P13 — Token Economy
Custom:
- C-EXEC — Anti-Programmatic-Execution: the agent must never advance by mechanically "making it run/pass";
  it advances only when the named acceptance condition is demonstrably met. (Rationale: a literal "green
  compile" or "test passes" can hide a wrong edit, a bypassed validator, or a skipped assertion.)

Engineering disciplines: R17 (prompt/context/harness) + R18/P11 (Programmatic Determinism) — cite repo-enhancer/orchestrator.md CONVENTIONS; do not restate the three layers.

<instructions>
  <context>
    GABI is a security-sensitive Java/Spring app with live SQL-identifier-injection surfaces and a coverage
    gate that has been mis-scoped before. A mechanical, assumption-driven edit silently re-opens a closed
    vulnerability or fakes a passing gate. These rules force verify-before-act and acceptance-driven completion.
  </context>

  <rules>
    1. Verify before edit. Read the exact file at the exact line(s) named by the task and confirm the defect or
       precondition is present as described BEFORE any Edit/Write. Never edit a file you have not just read this
       session. If the real code differs materially from the description (line moved, already fixed, different
       shape), STOP and report the discrepancy instead of editing on the assumption.
    2. Check assumptions explicitly. Never pass an ID, identifier, field name, Maven coordinate, Spring property,
       or API symbol you have not seen in the repo or in a cited result. When a value is implied but unknown,
       discover it (grep/read) or ask — never guess. State each assumption you rely on in the report.
    3. STOP and confirm before any irreversible, security-relevant, or ambiguous action. This includes: exposing
       a write on the MCP/REST surface; deleting a file; changing the dependency set or the JDK 17 / Derby
       10.16.1.1 baseline; altering the DB schema or auth model; editing credential config; modifying the JaCoCo
       gate threshold or scope; committing (agents never commit — the orchestrator does). State what you intend
       and why, then wait for explicit authorization.
    4. Acceptance-criteria-driven done (C-EXEC). The task is complete only when its stated acceptance criterion
       is demonstrably met — the encoding test passes AND the JaCoCo core gate stays green — not when a file
       changed or a build merely ran. A green compile without the acceptance test is NOT done.
    5. Minimal change. Make the smallest change that satisfies the stated fix approach. Do not refactor unrelated
       code, rename symbols, reformat untouched lines, or fix a neighbouring item — note it and let the
       orchestrator dispatch it separately. One task per session.
    6. Branch guard. Before any edit, run
       `python "D:\Documentos\Recursos\Recursos IA\Repo Enhancer\bin\git_ops.py" --action status --repo "D:\Documentos\GitHub\GABI"`
       and confirm `branch` is the enhancement branch. If it is `main`/`master`, STOP and report — never edit there.
    7. No secrets, ever. Never write a literal credential (`admin`, `1234`, `dev-local-only`, any key) into source,
       YAML, a Javadoc example, a test fixture, a checkpoint, or a memory record. Credentials remain `${...}`
       env/profile placeholders. Never log or lift a secret into an exception message or REST body.
    8. Context-budget discipline. Load the backlog/understanding/operating docs on demand, not pre-emptively;
       quote only what you need. If completing the task requires reading 5 or more files for context, STOP and
       emit a `GATHERING REQUEST` for the orchestrator to dispatch the-gleaner (threshold = 5); use the returned
       gather file as source of truth. If a version/API fact cannot be grounded in the repo, STOP and emit a
       `RESEARCH REQUEST` rather than inventing the API. At ~70% context, checkpoint per the Agent Checkpoint
       Instruction (`instructions/agent-checkpoint-instruction.md`) — never put a credential in the checkpoint.
    9. Honest reporting. Never claim a build, test, or gate passed that you did not run. Quote acceptance criteria
       verbatim. End with an EXIT STATUS block: COMPLETED only when the acceptance criterion is met and the gate
       is green; PARTIAL (with a checkpoint) otherwise; BLOCKED when a precondition (e.g. enhancement branch) is
       missing; FAILED on an unrecoverable error.
  </rules>

  <conditional_rules>
    - If a security invariant (see java-spring-conventions.md) cannot be held by the minimal change, then STOP and
      report — never ship a partial security fix that leaves a sink unvalidated, a credential committed, or a
      write exposed.
    - If `mvn verify` fails on the JaCoCo core gate after your change, then add the missing test(s) for the lines
      you changed — never lower the threshold or re-scope the gate to hide the shortfall.
  </conditional_rules>
</instructions>

## Sources
- `docs/BACKLOG.md` (GABI-B01…B12, I01…I13 — File:line, Root cause, Fix approach, Acceptance criterion).
- `instructions/agent-checkpoint-instruction.md` (system) — checkpoint procedure.
- `bin/git_ops.py` (system) — branch-status guard; commits are the orchestrator's responsibility.
- `.claude/instructions/java-spring-conventions.md` — the security invariants and Spring/Derby idioms.
