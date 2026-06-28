# Instruction — Java / Spring Conventions & Security Invariants (GABI)

GABI's Java language/general-conventions instruction — Rules 1–9 cover: SQL-identifier-validation,
password-escaping, privileged-surface gating, credential hygiene, spring.yml uniqueness, RAG graceful
degradation, JaCoCo gate scoping, Spring idioms, and in-memory-Derby test harness.

Every GABI subagent references this file by name; the security-reviewer enforces it as a PASS/FAIL gate.
Scope: all code/config/test edits to `D:\Documentos\GitHub\GABI`
(Java 17 / Maven / Spring Boot 4.0.6 / Spring AI 2.0.0 / Derby 10.16.1.1).

## Principles Applied
Inherited:
- P1 — Source-of-Truth Grounding (each invariant cites the file that embodies it)
- P2 — Full Determinism
- P3 — Systematicity (conditional rules below)
- P4 — Consistency (one invariant set shared across agents)
- P5 — Context Budget Discipline
- P6 — Self-Containment
- P7 — Reference Hygiene
- P8 — Principles Inheritance
- P9 — Role Separation
- P10 — Exit-Status Determinism
- P11 — Programmatic Determinism
- P12 — Maximal-Effort Completeness
- P13 — Token Economy
Custom:
- C-SEC — Security-Invariant Authority: this file is the single in-repo source of GABI's security boundaries;
  a change that compiles and passes its test is still WRONG if it breaks one. (Rationale: the campaign's value
  is the closed injection/exposure surface; a literal edit must not silently re-open it.)

Engineering disciplines: R17 (prompt/context/harness) + R18/P11 (Programmatic Determinism) — cite repo-enhancer/orchestrator.md CONVENTIONS; do not restate the three layers.

<instructions>
  <context>
    The review found live SQL-identifier-injection sinks in the legacy CLI path, a mis-scoped coverage gate, a
    startup-gutting duplicate YAML key, and committed dev-default credentials. These invariants are the campaign's
    load-bearing boundaries. They are derived from `docs/BACKLOG.md` (GABI-B01…B08), `CLAUDE.md` §Security
    invariants, and the cited source files.
  </context>

  <rules>
    1. Every SQL-identifier sink goes through `core/IdentifierValidator.validate(name, context)`. Identifier sinks
       are `derby.user.<name>` SET PROPERTY, `derby.database.fullAccessUsers`, GRANT, REVOKE, and DROP/CREATE
       TABLE name interpolation — in the core (`core/LibraryServiceImpl.addUser`/`deleteUser`) AND in the legacy
       CLI path `sql/users/UserDerby.java` (addDb + deleteDB sinks, GABI-B01/B02) AND `sql/DatabaseBuilder.java`.
       Reuse the one `IdentifierValidator`; never fork a second. After touching a sink, grep the file to prove no
       raw `+ name +` / `String.format` identifier interpolation remains.
    2. Passwords interpolated into Derby `SET PROPERTY` are Derby-string-escaped (double every single quote) or
       rejected by explicit policy — never concatenated raw (GABI-B07: core `LibraryServiceImpl.addUser` + mirrors
       in `UserDerby`/`DatabaseBuilder`). Values that are DATA use `PreparedStatement` parameters; only true
       identifiers go through `IdentifierValidator`.
    3. The privileged Derby user-admin is NEVER on the exposed surface. `addUser`/`deleteUser`/GRANT/REVOKE/
       `derby.user.*` must never appear as an `@Tool` in `access/mcp/LibraryMcpTools.java` or a route in
       `access/rest/LibraryRestController.java`. The exposed surface stays read-only + RAG (`ask`/`reindex`) +
       `health` (14 ops). Any new write-semantic feature stays CLI/admin-gated unless a human explicitly
       authorizes a curated exposed write — STOP and confirm before exposing any write.
    4. Credentials live only as `${DB_USER:}` / `${DB_PASSWORD:}` / `${OPENAI_API_KEY}` / `${ANTHROPIC_API_KEY}`
       placeholders. Never commit/log/fixture/checkpoint a literal credential. When touching
       `configuration.properties`, `application*.yml`, or a Javadoc example, leave placeholders, not values
       (GABI-B08: remove the committed `admin`/`dev-local-only` pair and the `admin 1234` CLI example).
    5. `application.yml` has exactly one top-level `spring:` key. Merge child keys under one root; do NOT split
       with a `---` separator (that creates two profile documents and is not the fix) (GABI-B03: duplicate at
       lines ~28 and ~91 guts startup).
    6. RAG degrades gracefully: exactly one `RagService` bean in every configuration — typed `NoOpRagService` via
       `@ConditionalOnMissingBean(RagService.class)` when no `EmbeddingModel`; `RagServiceImpl` (and its
       `RagConfig` vector-store beans) conditional on the model being present (e.g. `@ConditionalOnBean(
       EmbeddingModel.class)`). No `NoUniqueBeanDefinitionException`, no startup failure on a missing model
       (GABI-B04).
    7. The JaCoCo `check` rule scopes the ≥0.90 LINE/INSTRUCTION limit to the actual core package via element
       `PACKAGE`/`CLASS` (not `BUNDLE` with `include core/*`, which JaCoCo matches against the bundle name —
       GABI-B05, silently never fails), bound to `verify`. A real core shortfall MUST fail `mvn verify`. Never
       lower or re-scope the gate to hide a shortfall.
    8. Spring idioms: constructor injection (no field `@Autowired`); JDK 17 baseline and Derby 10.16.1.1 pinned
       (do NOT bump either). Repo ground-truth API set: `SimpleVectorStore.builder(embeddingModel)`,
       `QuestionAnswerAdvisor.builder(vectorStore)`, `@Tool`/`@ToolParam` from
       `org.springframework.ai.tool.annotation`, `MethodToolCallbackProvider`. Never invent a Spring AI / Boot /
       Derby / JaCoCo API, property, annotation, or Maven coordinate — confirm from existing repo usage or emit a
       `RESEARCH REQUEST`.
    9. Tests use deterministic in-memory Derby. DB-touching tests use the existing `core/InMemoryDerbyConfig`
       (`jdbc:derby:memory:gabiTest;create=true`, `@Primary` test DataSource) + `core/TestSchemaHelper`; wiring/
       config items use a `@SpringBootTest` context-load test. Seed deterministic fixtures; no live network Derby
       (`:1527`) and no real model backend in tests.
  </rules>

  <conditional_rules>
    - If a change adds a user-supplied column/field selection (e.g. faceted search), then validate it through
      `IdentifierValidator` and bind values with `PreparedStatement` parameters — never interpolate either raw.
    - If an exposed read capability is added, then add it in BOTH `LibraryMcpTools` (`@Tool` + `@ToolParam`) AND
      `LibraryRestController` (1-to-1 route), both delegating to the same `core/LibraryService` method, read/RAG
      semantics only.
    - If a feature has write semantics (ISBN add, holds, fines, notices, MARC import), then keep it CLI/admin-
      gated and regression-test the read-only exposed posture; do not expose it without explicit authorization.
  </conditional_rules>
</instructions>

## Sources
- `CLAUDE.md` §Security invariants, §Stack, §Gate / verify commands.
- `docs/BACKLOG.md` — GABI-B01…B08 (invariants 1–7), acceptance criteria.
- `src/main/java/core/IdentifierValidator.java` (the chokepoint: `validate(String, String)`, `^[A-Za-z0-9_]+$`).
- `src/main/java/sql/users/UserDerby.java`, `src/main/java/sql/DatabaseBuilder.java` (legacy sinks).
- `src/main/java/access/mcp/LibraryMcpTools.java`, `access/rest/LibraryRestController.java` (read-only surface).
- `src/main/resources/application.yml` (duplicate `spring:` at lines 28 & 91; env placeholders 78-79).
- `src/main/java/rag/RagConfig.java`, `RagServiceImpl.java`, `NoOpRagService.java` (RAG fallback wiring).
- `pom.xml` (JaCoCo `check` rule lines 315-349; Boot 4.0.6; Spring AI BOM 2.0.0; Derby 10.16.1.1).
- `src/test/java/core/InMemoryDerbyConfig.java`, `TestSchemaHelper.java` (in-memory Derby test harness).
