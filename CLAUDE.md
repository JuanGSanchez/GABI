# GABI — repository guide for Claude

GABI is a Java command-line library-catalogue manager (books, members, loans, DB users) with a
greenfield RAG Q&A capability, exposed headlessly over a Spring AI MCP + REST access layer.

## Principles Applied

Inherited:
- P1 — Source-of-Truth Grounding (every claim below traces to the cited file)
- P2 — Full Determinism
- P3 — Systematicity
- P4 — Consistency (same invariants the in-repo agents enforce)
- P5 — Context Budget Discipline
- P6 — Self-Containment (paths/commands resolve in this repo)
- P7 — Reference Hygiene (every referenced path exists on the enhancement branch)
- P8 — Principles Inheritance
- P9 — Role Separation
- P10 — Exit-Status Determinism
- P11 — Programmatic Determinism
- P12 — Maximal-Effort Completeness
- P13 — Token Economy

Custom:
- C1 — Invariant Authority: this file is the single in-repo statement of GABI's security invariants and gate commands; the `gabi-maintainer` and `gabi-operator` agents enforce them, this file records them. (Rationale: one source of truth prevents drift between the agents and ad-hoc sessions.)

Engineering disciplines: R17 (prompt/context/harness) + R18/P11 (Programmatic Determinism) — cite repo-enhancer/orchestrator.md CONVENTIONS; do not restate the three layers.
Deployment: deployment_target: claude_code (real deployed .claude/ tree). This file is the orchestration/operating-contract carrier (no orchestrator agent).
SessionStart contract: relies on the user-level global hook $HOME\.claude\hooks\claude-orchestration-contract.py; no per-project copy created or required.

## Stack
- Java 17 (baseline — do not bump). Apache Derby 10.16.1.1 (last Java-17 line — do not bump).
- Maven: Spring Boot parent 4.0.6, Spring AI BOM 2.0.0. Build: `mvn` / `mvnw`.
- Spring AI RAG: `SimpleVectorStore` + `QuestionAnswerAdvisor`; providers Ollama (default) / OpenAI / Anthropic via profiles; `NoOpRagService` fallback when no model.
- Access layer: `@Tool` MCP beans (`access/mcp/LibraryMcpTools.java`) + REST RFC-9457 (`access/rest/LibraryRestController.java`), both delegating to one shared `core/LibraryService`.
- Tests: JUnit 5 + JaCoCo (`check` bound to `verify`); in-memory Derby via `InMemoryDerbyConfig`/`TestSchemaHelper`.
- Packaging: jpackage app-image (JDK 17).

## Layout
- `src/main/java/core/` — shared headless service + `IdentifierValidator` (the security chokepoint).
- `src/main/java/access/mcp/` + `access/rest/` — the exposed read-only + RAG + health surface.
- `src/main/java/rag/` — `RagServiceImpl`, `NoOpRagService`, `RagConfig`.
- `src/manager/` + `src/main/java/sql/` — legacy CLI/DAO path (includes the privileged user-admin sinks in `sql/users/UserDerby.java` + `sql/DatabaseBuilder.java`).
- `src/main/resources/application*.yml` — base + `server`/`openai`/`anthropic` profiles.

## Security invariants (do NOT violate)
1. Every SQL-identifier sink (`derby.user.<name>`, `GRANT`, `REVOKE`, `fullAccessUsers`, `SET PROPERTY`) routes through `core/IdentifierValidator` — in the core AND the legacy CLI path (`UserDerby`, `DatabaseBuilder`). Reuse the one validator; never fork a second.
2. Passwords interpolated into Derby `SET PROPERTY` are escaped (double single quotes) or rejected — never concatenated raw.
3. Privileged user-admin (`addUser`/`deleteUser`/GRANT/REVOKE/`derby.user.*`) is NEVER on the MCP/REST surface. That surface stays read-only + RAG (`ask`/`reindex`) + `health`. New write features stay CLI/admin-gated unless a human explicitly authorizes a curated exposed write.
4. DB creds and API keys live only as `${DB_USER:}` / `${DB_PASSWORD:}` / `${OPENAI_API_KEY}` / `${ANTHROPIC_API_KEY}` placeholders — never commit, log, fixture, or checkpoint a literal credential (no `admin`/`1234`/`dev-local-only`).
5. `application.yml` has exactly one top-level `spring:` key (merge child keys; do not split with `---`).
6. RAG degrades gracefully: exactly one `RagService` bean in every configuration (NoOp when no `EmbeddingModel`, `RagServiceImpl` with a model profile); no startup failure on a missing model.
7. The JaCoCo `check` rule scopes the ≥0.90 LINE/INSTRUCTION limit to the actual core package (element `PACKAGE`/`CLASS`, not `BUNDLE`), bound to `verify`; a real core shortfall must fail `mvn verify`. Never lower or re-scope the gate to hide a shortfall.

## Gate / verify commands
- Full gate (tests + JaCoCo core gate): `mvn -q verify`
- Build only (skip tests): `mvn -q -DskipTests package`
- Run server mode (MCP + REST, no CLI): `java -jar target/gabi-*-exec.jar --spring.profiles.active=server` (set `DB_USER`/`DB_PASSWORD` in env first).
- Health probe: `curl http://localhost:8080/health`

## Work on this repo
- Branch off `main` to an enhancement branch; never commit on `main`/`master`.
- Backlog: `docs/BACKLOG.md` (item IDs `GABI-B01`…`GABI-I13`). To IMPLEMENT a backlog item, use the `gabi-maintainer` subagent. To DRIVE the running catalogue/RAG service (ask questions, search, count), use the `gabi-operator` subagent.

## SDD Pipeline (CLAUDE.md is the coordination point — no orchestrator agent)
CLAUDE.md sequences the SDD pipeline for every change: `specify` → `clarify` → `plan` → `tasks` → `analyze` → implement → `checklist` (skills at `.claude/skills/`).
Constitution gates: `.claude/instructions/sdd-constitution.md`. Best-practices instructions: `.claude/instructions/java-spring-conventions.md`, `.claude/instructions/spring-boot-best-practices.md`.

## In-repo Claude agents

Active:
- `.claude/agents/gabi-core-dev.md` — headless-core/Derby DAO/config developer.
- `.claude/agents/gabi-access-dev.md` — MCP @Tool + REST access-layer developer.
- `.claude/agents/gabi-rag-dev.md` — Spring AI RAG pipeline developer.
- `.claude/agents/gabi-test-author.md` — JUnit 5 test author and JaCoCo core-gate custodian.
- `.claude/agents/gabi-packaging-builder.md` — jpackage/Maven packaging engineer.
- `.claude/agents/gabi-docs-writer.md` — documentation author.
- `.claude/agents/gabi-security-reviewer.md` — read-only PASS/FAIL security gate.
- `.claude/agents/gabi-operator.md` — read-only operator driving the MCP/REST access layer.

Retired:
- `.claude/agents/gabi-maintainer.md` — RETIRED; superseded by the specialist agents above.

## Sources
- `docs/review-gabi.md` (B-1…B-8, security section C, severity tally), `docs/backlog-gabi.md` / in-repo `docs/BACKLOG.md` (item IDs + acceptance criteria), `docs/understanding-gabi.md` (stack/layout), `docs/agent-operating-doc.md` (access-layer ops), and the cited `src/`/`pom.xml`/`application*.yml` files. The two in-repo agent assets enforce these invariants at runtime; this file records them.
