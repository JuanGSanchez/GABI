---
name: harden-sql-sink
description: >
  Route a GABI SQL-identifier sink through core/IdentifierValidator (and escape any
  interpolated Derby SET PROPERTY password), then add the injection test that proves it.
  Use this skill when hardening a sink in core/LibraryServiceImpl, sql/users/UserDerby
  (addDb/deleteDB), or sql/DatabaseBuilder against SQL-identifier injection — e.g.
  GABI-B01, GABI-B02, GABI-B07 — or whenever a change adds a new identifier sink. Produces
  a hardened sink plus a passing crafted-input rejection test; the gabi-security-reviewer
  re-checks it.
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
    - id: C-FAILCLOSED
      name: Fail-Closed Validation
      requires: The identifier is validated (and the password escaped/rejected) BEFORE any statement executes; a crafted metacharacter input is rejected, never silently sanitized to a different identifier.
      rationale: Sanitizing-to-pass would let a near-miss identifier through; rejection before execution is the only safe contract for a privileged DDL sink.
---

# Harden SQL Sink

Routes a user-supplied identifier through `core/IdentifierValidator` at every interpolation point of a Derby DDL sink, escapes any interpolated SET PROPERTY password, and adds the test that proves injection is rejected. Reuse the one validator — never fork a second.

## Workflow

### Step 1: Locate the sink
Read the target file at the cited line(s) (`docs/BACKLOG.md` item, e.g. `sql/users/UserDerby.java` addDb ~164/169/171-172, deleteDB ~318/320-321; `core/LibraryServiceImpl.addUser` ~611-612; `sql/DatabaseBuilder.java`). Grep the file for the sink tokens: `SET PROPERTY`, `derby.user.`, `fullAccessUsers`, `GRANT`, `REVOKE`, `CREATE TABLE`, `DROP TABLE`. Confirm the identifier is interpolated raw (`+ name +` / `String.format`) with no existing `IdentifierValidator` reference. If it is already validated, STOP — nothing to harden.

### Step 2: Route every identifier through the validator
Before each interpolation, replace the raw name with `IdentifierValidator.validate(name, "<context>")` (e.g. `"username"`). Apply at EVERY sink in the method, not just the first. Do not change the SQL shape otherwise. For DATA values (not identifiers), keep/convert to `PreparedStatement` parameters — the validator is for identifiers only.

### Step 3: Escape any interpolated password
If the sink interpolates a password into `SET PROPERTY` (GABI-B07 sites), Derby-escape it (double every single quote) or reject by explicit policy via a single shared helper referenced at all three sites (`LibraryServiceImpl`, `UserDerby`, `DatabaseBuilder`). Never concatenate a password raw. Never log the password.

### Step 4: Prove no raw sink remains
Grep the touched file again; confirm no raw `+ <name>() +` / `String.format` identifier interpolation remains at any sink. Confirm no literal credential was introduced.

### Step 5: Add the injection test
Add/extend a JUnit test (in-memory Derby via `core/InMemoryDerbyConfig` + `TestSchemaHelper` for DB paths): feed a crafted identifier (e.g. `bob'; DROP`) and assert it is rejected (an `InvalidIdentifierException`) BEFORE any statement executes; add a valid-identifier happy-path that still provisions. For a password site, feed `pa'ss` and assert the DDL is well-formed or the password is rejected — no statement breakage.

### Step 6: Verify
Run `mvn -q verify`. Confirm the new test passes AND the JaCoCo core gate stays green. If the change dropped core coverage, add tests for the changed lines — never lower the gate.

## Output Format
A report: file:line of each sink hardened; the validator/escaping call inserted at each; grep proof no raw sink remains and no credential introduced; the injection test added and its assertion; `mvn -q verify` + core-gate result. No credential values quoted.

## Examples

### Example 1
**Input:** Harden `UserDerby.addDb` (GABI-B01).
**Output:**
```
Sinks: derby.user.<name> SET PROPERTY (164), fullAccessUsers (169), GRANT ... TO <name> (171-172).
Inserted IdentifierValidator.validate(newUser.getName(), "username") before each interpolation.
Grep: no raw `+ getName() +` sink remains; no credential introduced.
Test: addDb("bob'; DROP") → InvalidIdentifierException before any statement; addDb("validuser") provisions.
mvn -q verify → pass; core gate green.
```

## Self-Containment Index
This skill package contains everything needed for its complete usage:
- SKILL.md (this file): workflow, output format, example.

External dependencies (must be available in the repo/environment):
- `src/main/java/core/IdentifierValidator.java` — the `validate(String, String)` chokepoint (do not fork).
- `src/test/java/core/InMemoryDerbyConfig.java` + `TestSchemaHelper.java` — in-memory Derby test harness.
- `.claude/instructions/java-spring-conventions.md` (rules 1-2) — the sink invariant this skill enforces.
- `mvn` on PATH; JDK 17.

## Sources
- User requirement: a harden-sql-sink skill (route a sink through IdentifierValidator + injection tests) (this task).
- Repo ground truth: `docs/BACKLOG.md` GABI-B01/B02/B07; `core/IdentifierValidator.java`; `sql/users/UserDerby.java`; `sql/DatabaseBuilder.java`; `core/LibraryServiceImpl.java`; `core/InMemoryDerbyConfig.java`.
