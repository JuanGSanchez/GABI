---
name: add-mcp-tool
description: >
  Add a new read-only/RAG capability to GABI's exposed surface as a Spring AI MCP @Tool
  bean AND its 1-to-1 REST route, both delegating to one core/LibraryService method, with a
  mandatory surface-exclusion check and a passing test. Use this skill when exposing a new
  read or RAG operation — e.g. a faceted search route (GABI-I02), a circulation report
  route (GABI-I05), or surfacing RAG citations on ask (GABI-I01). It refuses to expose any
  write or user-admin operation. Pairs with the gabi-access-dev agent.
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
    - id: C-READONLY
      name: Read-Only-Surface Guard
      requires: The skill exposes ONLY read/RAG semantics; it stops and refuses if the operation is a write or user-admin, and it greps the surface after the change to prove no addUser/deleteUser/GRANT/REVOKE/derby.user.*/write op is present.
      rationale: The exposed MCP/REST surface is GABI's D-3/D-4 read-only boundary; this skill must not be the path that breaches it.
---

# Add MCP Tool

Adds a read-only/RAG operation to GABI's exposed surface in both transports together, with a surface-exclusion check and a test.

## Workflow

### Step 1: Confirm the operation is read/RAG only
State the operation's semantics. If it is a write (add/update/delete) or user-admin (addUser/deleteUser/GRANT/REVOKE/derby.user.*), STOP and refuse — recommend CLI/admin gating per `java-spring-conventions.md` rule 3. Proceed only for read/RAG semantics.

### Step 2: Confirm the core method exists
Read `core/LibraryService` (+ `LibraryServiceImpl`); confirm the method the tool/route will delegate to exists and is read/RAG. If missing, STOP and request it from gabi-core-dev (or gabi-rag-dev for RAG) — do not inline core logic in the access layer.

### Step 3: Add the @Tool bean
In `access/mcp/LibraryMcpTools.java`, add an `@Tool`-annotated method (annotation from `org.springframework.ai.tool.annotation`) with `@ToolParam` descriptions for each input, delegating to the core method. Match the existing tools' naming/style. For any user-supplied field/column selection, route it through `IdentifierValidator` and bind values with `PreparedStatement` (in the core).

### Step 4: Add the 1-to-1 REST route
In `access/rest/LibraryRestController.java`, add the matching `@GetMapping` (read) or `@PostMapping` (RAG ask/reindex only) route delegating to the SAME core method, returning the same DTO. Keep RFC-9457 error handling intact.

### Step 5: Surface-exclusion check (mandatory, C-READONLY)
Grep `LibraryMcpTools` + `LibraryRestController` for `addUser|deleteUser|GRANT|REVOKE|derby\.user|@PutMapping|@DeleteMapping` and any write verb; confirm the surface is still read + RAG + health only. If anything write/user-admin appears, STOP and revert.

### Step 6: Add the test
Add a test asserting the new MCP tool and REST route return the expected payload for a seeded in-memory-Derby dataset, and a regression test asserting the read-only posture (no write/user-admin exposed). Request from gabi-test-author or add a minimal one.

### Step 7: Verify
Run `mvn -q verify`; confirm tests pass and the core gate is green. Note that `gabi-operator` and `docs/README-access.md` need a refresh (surface changed) — flag it.

## Output Format
A report: operation + semantics (read/RAG confirmed); core method delegated to; the @Tool bean + REST route added; surface-exclusion grep proof; test added; `mvn -q verify` + core-gate result; operator/docs-refresh flag.

## Examples

### Example 1
**Input:** Expose faceted book search as a read-only tool (GABI-I02 exposure).
**Output:**
```
Semantics: read — OK to expose.
Core: LibraryService.searchBooks(criteria) confirmed (added by gabi-core-dev).
MCP: @Tool search_books_faceted(@ToolParam field, @ToolParam text, @ToolParam facets) → core; field validated via IdentifierValidator.
REST: GET /api/books/search/faceted → same core method, BookDto + facet counts.
Surface grep: read + RAG + health only; no write/user-admin.
Test: faceted search returns correct buckets on seeded data; read-only posture regression passes.
mvn -q verify → pass; core gate green. Flag: refresh gabi-operator + docs/README-access.md.
```

## Self-Containment Index
This skill package contains everything needed for its complete usage:
- SKILL.md (this file): workflow, output format, example.

External dependencies (must be available in the repo/environment):
- `src/main/java/access/mcp/LibraryMcpTools.java`, `access/rest/LibraryRestController.java`, `access/rest/dto/*`.
- `src/main/java/core/LibraryService.java` (+ Impl) — the delegated core methods.
- `src/main/java/core/IdentifierValidator.java` — for field/column validation.
- `.claude/instructions/java-spring-conventions.md` (rule 3 + conditionals) — the read-only surface invariant.
- `mvn` on PATH; JDK 17.

## Sources
- User requirement: an add-mcp-tool skill (new @Tool + REST route + tests with surface-exclusion check) (this task).
- Repo ground truth: `docs/BACKLOG.md` GABI-I01/I02/I05, B09; `access/mcp/LibraryMcpTools.java`; `access/rest/LibraryRestController.java`; `access/rest/dto/*`; `core/LibraryService.java`; `docs/agent-operating-doc.md` (the 14-op read-only surface).
