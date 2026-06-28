# GABI — Post-review remediation specifications (2026-06-28)

Remediation backlog produced by the pre-merge critical review of branch
`enhancement/gabi-20260625` (PR #1 → `main`). The delivered product spec list (SPEC-01…SPEC-22)
is archived in `SPECIFICATIONS-archive-20260625.md`; this document is the **active** list and
contains only concrete, acceptance-testable fixes for issues found in that review.

Each spec is grounded in a cited file:line finding. Priorities: **P1** = merge blocker (build
correctness, security invariant, or committed secret); **P2** = correctness/scope gap; **P3** =
verification / confirmation (encode a guard, no functional change).

Security invariants (CLAUDE.md §"Security invariants") are non-negotiable and hold for every spec
below: (1) every SQL-identifier sink routes through `core/IdentifierValidator`; (2) passwords in
Derby `SET PROPERTY` are escaped (doubled single-quotes) or rejected; (3) the MCP/REST surface stays
read-only + RAG + health; (4) no committed plaintext credential.

---

## P1 — Merge blockers

### SPEC-R01 — Repair the Maven wrapper so a clean checkout builds with `./mvnw`
- **Finding:** `.mvn/wrapper/maven-wrapper.jar` is the genuine `maven-wrapper-3.2.0.jar`, but its
  `MANIFEST.MF` has **no `Main-Class`** entry (verified). `mvnw` (line 20) and `mvnw.cmd` invoke
  `java -jar "$WRAPPER_JAR"`, which fails with *"no main manifest attribute in maven-wrapper.jar"*.
  A fresh `git clone` + `./mvnw verify` therefore fails — a merge-readiness blocker (follow-up (a)).
- **Scope:** Correct `mvnw` and `mvnw.cmd` to launch the wrapper the way the wrapper jar requires —
  on the **classpath** with the explicit main class and the project base dir property:
  `java -classpath "<wrapper.jar>" "-Dmaven.multiModuleProjectDirectory=<basedir>" org.apache.maven.wrapper.MavenWrapperMain "$@"`.
  Keep the existing `JAVA_HOME` guard and the `mvn`-on-PATH fallback. Do not re-download or replace
  the (valid) wrapper jar; do not change the pinned Maven 3.9.4 in `maven-wrapper.properties`.
- **Acceptance:**
  - From a pristine export of the branch (no `~/.m2/wrapper` side state assumed for the launch step),
    `./mvnw -version` runs and prints Apache Maven 3.9.4, and `./mvnw -o verify` reaches the build
    (no "no main manifest attribute" error).
  - Both `mvnw` (sh) and `mvnw.cmd` (Windows) use the `-classpath … MavenWrapperMain` form.
  - `git status` stays clean after the wrapper run (no stray tracked files).

### SPEC-R02 — Consolidate the duplicate top-level `spring:` key in `application.yml`
- **Finding:** `src/main/resources/application.yml` declares `spring:` twice (lines 28 and 104).
  YAML last-key-wins → the first block (`spring.main.web-application-type`,
  `spring.main.allow-bean-definition-overriding`, `spring.autoconfigure.exclude`, the entire
  `spring.ai.mcp.server.*`) is silently dropped in favour of the second (`spring.ai.ollama.*`).
  Violates CLAUDE.md invariant #5 (follow-up (b)).
- **Scope:** Merge the two `spring:` blocks into exactly one, preserving every child key
  (`application`, `main`, `autoconfigure`, `ai.mcp.server.*`, `ai.ollama.*`). No setting may be lost
  or changed in value. Keep one top-level `spring:`; do not split with `---`.
- **Acceptance:**
  - `application.yml` contains exactly one top-level `spring:` key (grep gate: one line matching
    `^spring:`).
  - Every previously-present child key is still present with its original value (diff-review).
  - `mvn -o verify` stays green; the context still loads (no MCP/autoconfigure regression).

### SPEC-R03 — Harden the legacy `UserDerby` privileged-admin SQL sinks
- **Finding:** `sql/users/UserDerby` (CLI user-admin path) does **not** use `IdentifierValidator`
  and concatenates raw input into DDL/admin SQL: the Derby user password is interpolated raw into
  `SET PROPERTY 'derby.user.<name>', '<password>'` (line 164) with no escaping; the username and the
  deleted user's name flow unvalidated into `derby.user.<name>` (lines 164, 318); `GRANT`/`REVOKE`
  interpolate `database-name`/table identifiers raw (lines 171–172, 320–321). Violates invariants #1
  and #2, which name `UserDerby` explicitly. (`DatabaseBuilder` is already hardened.)
- **Scope:** Route every identifier reaching a DDL/admin statement in `UserDerby.addDb` and
  `UserDerby.deleteDB` through the single `core.IdentifierValidator` — the new user's name, the
  deleted user's name, `database-name`, and each table name — validating **fail-fast at the top of
  the method, before any connection/SQL**. Escape the `SET PROPERTY` password by doubling single
  quotes (`'` → `''`), or reject it. Reuse the one validator; do not fork a second.
- **Acceptance:**
  - A negative test: `addDb` with a malicious username (e.g. `"x'; DROP TABLE books--"`, quotes,
    spaces) throws `LibraryException.InvalidIdentifierException` and issues no SQL.
  - A password containing a single quote is escaped (doubled) before reaching `SET PROPERTY`, never
    concatenated raw (asserted by a unit test on the escape, or by a targeted code-level check).
  - Valid identifiers still pass; existing provisioning behaviour is unchanged for valid input.
  - `gabi-security-reviewer` boundary-1 check passes for `UserDerby`.

### SPEC-R04 — Remove the committed `admin 1234` plaintext-credential examples
- **Finding:** The plaintext credential pair `admin 1234` is committed as a CLI usage example in
  `GabiApplication.java:18`, `GabiCliRunner.java:23`, and `application.yml:24`. The repo's own
  `gabi-security-reviewer` rule (`.claude/agents/gabi-security-reviewer.md:57`) marks an `admin 1234`
  CLI example as a FAIL (GABI-B08). Violates invariant #4 / SPEC-08.
- **Scope:** Replace every `admin 1234` example with a placeholder form (`<db-user> <db-password>` or
  `$DB_USER $DB_PASSWORD`) in those three files. Do **not** change the schema/owner identifier
  `database-name=admin` in `configuration.properties` — see SPEC-R07.
- **Acceptance:**
  - `git grep -nI "1234"` over `src/` and `*.yml` returns no credential example (only `.claude/`
    rule text that *describes* the forbidden pattern may remain).
  - No literal `<user> <password>` pair appears in any committed source/config example.
  - `mvn -o verify` stays green.

### SPEC-R05 — Suppress the interactive CLI under the `server` profile
- **Finding:** `GabiCliRunner` is annotated `@Profile("!desktop")`, so under the `server` profile it
  still runs and calls `LibMenu.main(args)`, launching the **blocking interactive console** —
  contradicting `application-server.yml`'s own comment ("the CLI … is suppressed in server mode") and
  CLAUDE.md ("Run server mode (MCP + REST, no CLI)"). Regression from the SPEC-18 profile change.
- **Scope:** Gate `GabiCliRunner` so it does **not** activate under `server` (nor `desktop`): change
  the profile expression to `@Profile("!desktop & !server")`. The default (no profile) CLI path and
  the `desktop`/`server` paths keep their current behaviour otherwise.
- **Acceptance:**
  - With `spring.profiles.active=server`, `GabiCliRunner` is not instantiated (no `LibMenu.main`
    invocation, no blocking console read).
  - Default (no active profile) still launches the CLI; `desktop` still launches the Swing UI; both
    unchanged.
  - `mvn -o verify` stays green; a context/profile check (where feasible) confirms the runner bean is
    absent under `server`.

---

## P2 — Correctness / scope gap

### SPEC-R06 — Extend SPEC-20 paginated search to Members
- **Finding:** SPEC-20 paged, multi-field, case-insensitive search was implemented for **Books only**
  (`LibraryService.searchBooksPaged`, REST + MCP `search_books_paged`). Members exposes only the
  legacy `searchMembers(field, text)`. Follow-up (c) directs extending the same pattern to Members.
- **Scope:** Add `searchMembersPaged(String query, int page, int size, sort, dir)` to
  `core.LibraryService`/`LibraryServiceImpl` mirroring `searchBooksPaged` (multi-field over
  name+surname, case-insensitive, `core.search.Page<Member>`, parameterized queries only — no
  identifier interpolation; sort field validated against an allow-list). Expose it on REST
  (`LibraryRestController`, query-param paged route returning page metadata) and MCP
  (`LibraryMcpTools` `search_members_paged` `@Tool`, read-only). Add core + adapter tests.
- **Acceptance:**
  - `searchMembersPaged` returns a `Page<Member>` with page/size/sort honored, case-insensitive,
    matching name OR surname; covered by core tests (incl. paging boundaries and blank-query =
    match-all, matching the Books tests).
  - REST exposes the paged members route with pagination metadata; MCP exposes
    `search_members_paged` (read-only; surface-exclusion check passes — no write/admin added).
  - Sorting uses an allow-listed sort column; no user input is interpolated as a SQL identifier
    (invariant; ties SPEC-07). `mvn -o verify` green, JaCoCo core gate ≥0.90 held.

---

## P3 — Verification (encode a guard, no functional change)

### SPEC-R07 — Confirm AI-panel secret/separation guarantees and record accepted scope
- **Finding (no defect):** Follow-up (d) is satisfied — the in-app AI panel has **no hardcoded key**
  (keys flow from `${OPENAI_API_KEY}`/`${ANTHROPIC_API_KEY}` into the Spring AI `ChatClient`), is
  **ChatClient-only and separate from the MCP path** (`SpringAiChatBackend` never touches an MCP
  endpoint), degrades via `NoOpChatBackend`, and is **stub-tested with no live network**
  (`StubChatBackend`, `AssistantServiceTest`, `AssistantSeparationAndSecurityTest`).
- **Scope:** Verify the existing tests assert (i) no key literal in the assistant source, (ii)
  ChatClient/MCP transport separation, and (iii) stub-only backend. If any of the three is not
  explicitly asserted, add the missing guard test. No production-code change expected.
- **Acceptance:** A test/scan asserts no API-key literal in `ui/assistant/**`; a test asserts the
  panel path uses `ChatBackend`/`ChatClient` and not the MCP server; tests run with a stub backend
  and make no network call. All green under `mvn -o verify`.
- **Accepted scope (no change):** `database-name=admin` in `configuration.properties` is the Derby
  **schema/owner identifier** (the connection user/schema), env-overridable via `DB_USER`, and is
  depended on by the test harness (`TestSchemaHelper.SCHEMA = "admin"`) and the core schema model
  (`LibraryServiceImpl` table qualification). The actual secret — the password — is already
  externalized (blank in config, sourced from `DB_PASSWORD`). Removing the `admin` schema identifier
  is a structural change beyond this remediation pass and is **deferred**, recorded here explicitly.

---

## Summary

- **P1 (5):** SPEC-R01 (mvnw), SPEC-R02 (spring: key), SPEC-R03 (UserDerby sinks),
  SPEC-R04 (admin 1234), SPEC-R05 (server CLI suppression).
- **P2 (1):** SPEC-R06 (members paged search).
- **P3 (1):** SPEC-R07 (AI-panel guard confirmation + accepted-scope record).

**Total: 7 remediation specs.** Areas reviewed with **no real issue**: secret history (clean),
packaging/SPEC-16 (reproducible scripts present), JaCoCo gate scope (correctly PACKAGE-scoped, green),
working-tree cleanliness (stray files gitignored), and the headline specs SPEC-01/02/18 (implemented,
tested) — apart from the specific defects enumerated above.
