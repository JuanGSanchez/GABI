---
name: run-quality-gate
description: >
  Run GABI's full quality gate and report a single GREEN/RED verdict: `mvn -q verify`
  (tests + JaCoCo core gate), a JaCoCo scope sanity-check (the ≥0.90 core rule is element
  PACKAGE/CLASS, not the silently-passing BUNDLE), and a committed-secret grep. Use this
  skill before committing a GABI change, after a maintenance agent lands an item, or on
  demand ("run the quality gate", "is GABI green?"). Read-only except for running the
  build; it makes no source edits.
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
    - "R17 Engineering Disciplines — cite repo-enhancer/orchestrator.md CONVENTIONS."
  custom:
    - id: C-NOHIDE
      name: No-Hidden-Pass
      requires: A GREEN verdict is asserted only when mvn verify passes AND the JaCoCo check rule is confirmed scoped via element PACKAGE/CLASS (a BUNDLE-with-include-core/* rule is reported RED as a non-enforcing gate, GABI-B05) AND the secret grep is clean.
      rationale: A passing build over a gate that can never fail, or with a committed secret, is a false GREEN; the skill must catch the mis-scoped gate itself.
---

# Run Quality Gate

Runs the full gate and returns one verdict. Makes no edits.

Execution backend for the SDD pipeline `analyze` (`.claude/skills/analyze/SKILL.md`) and `checklist` (`.claude/skills/checklist/SKILL.md`) skills — those skills delegate deterministic build/coverage/secret verification here.

## Workflow

### Step 1: Branch check
Run `python "D:\Documentos\Recursos\Recursos IA\Repo Enhancer\bin\git_ops.py" --action status --repo "D:\Documentos\GitHub\GABI"` and note the branch (informational; the gate runs regardless, but report if on main/master).

### Step 2: JaCoCo scope sanity-check
Read the `check` execution in `pom.xml` (~lines 315-349). Confirm the rule element is `PACKAGE` or `CLASS` with an include resolving to the core package, LINE/INSTRUCTION ratio ≥0.90, bound to `verify`. If the element is `BUNDLE` with `<include>core/*</include>`, report RED — the gate is mis-scoped (GABI-B05) and does not enforce; the rest of the run is advisory until it is fixed by gabi-test-author.

### Step 3: Run the build gate
Run `mvn -q verify` via Bash. Capture: tests pass/fail, and the JaCoCo `check` outcome (a real core shortfall must fail here once Step 2 is GREEN).

### Step 4: Committed-secret grep
Grep the tree (source, `application*.yml`, `utils/configuration.properties`, Javadoc, fixtures) for literal credential patterns: `admin`, `1234`, `dev-local-only`, `database-password\s*=\s*\S`, `sk-`, and key-shaped literals outside `${...}`. Any hit → RED (GABI-B08).

### Step 5: Verdict
GREEN only if: `mvn -q verify` passed AND the JaCoCo rule is correctly scoped (Step 2) AND the secret grep is clean. Otherwise RED, listing each failing check with evidence (test name / coverage shortfall / mis-scope / file:line of secret).

## Output Format
```
QUALITY GATE — GABI
Branch:            <branch>
JaCoCo scope:      GREEN (PACKAGE/CLASS) | RED (BUNDLE mis-scope — GABI-B05)
mvn -q verify:     PASS | FAIL — <tests / coverage detail>
Secret grep:       CLEAN | RED — <file:line>
VERDICT: GREEN | RED
Next: <which agent fixes each RED item>
```

## Examples

### Example 1
**Input:** Run the quality gate.
**Output:**
```
Branch: enhancement/gabi-20260612
JaCoCo scope: GREEN (element CLASS, include core.*, LINE 0.90, bound to verify)
mvn -q verify: PASS (52 tests, core LINE 0.93)
Secret grep: CLEAN
VERDICT: GREEN
```

## Self-Containment Index
This skill package contains everything needed for its complete usage:
- SKILL.md (this file): workflow, output format, example.

External dependencies (must be available in the repo/environment):
- `pom.xml` — the JaCoCo `check` rule under audit.
- `bin/git_ops.py` (system) — branch status.
- `.claude/instructions/java-spring-conventions.md` (rules 4, 7) — secret + gate-scope invariants.
- `mvn` on PATH; JDK 17.

## Sources
- User requirement: a run-quality-gate skill (mvn verify + JaCoCo scope check + secret grep) (this task).
- Repo ground truth: `pom.xml` (check rule ~315-349); `docs/BACKLOG.md` GABI-B05, B08; `CLAUDE.md` §Gate/verify commands; `src/main/resources/application*.yml`, `utils/configuration.properties`.
