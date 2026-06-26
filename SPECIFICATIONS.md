# GABI — Product Specifications

Prioritized capability + robustness backlog for **GABI** ("Gestión de Biblioteca" — a Java library
management application: Books, Members, Loans, Users over Apache Derby). This document is the product
specification that GABI's own SDD (spec-driven development) pipeline consumes to IMPLEMENT. Each spec
is concrete and testable; it is product scope, NOT the `.claude` orchestration vocabulary.

## Reading notes / invariants (hold for every spec below)

- **Framework-independent core holds.** Domain logic (Books/Members/Loans/Users rules) lives in a
  pure `core` service with no console, no Swing, no Spring, no JDBC-credential coupling. The console
  UI, the new desktop UI, the REST API, the MCP server, and the AI panel are all thin adapters over
  that one core. No business rule may live only in an adapter.
- **Security invariants hold (non-negotiable, every spec):** (1) SQL **identifier** validation on every
  DDL/admin path; (2) **NO plaintext credential** leakage anywhere (repo, args, logs, fixtures, config,
  checkpoints, memory); (3) **privileged admin surface gating** — user provisioning and grants are
  admin-only on every adapter.
- **Portable Maven build is the prerequisite.** GABI today has no portable build (only IntelliJ `.iml`
  + an absolute local jar path). A reproducible **`pom.xml`** (Spring Boot 4.1.0 parent, JDK 17) is the
  gating prerequisite for the MCP server, the AI panel, jpackage packaging, and the JaCoCo gate. It is
  spec SPEC-03 and every later spec depends on it.
- **Current UI is console-only.** SPEC-01 (centralized popup) and SPEC-02 (AI panel) require a desktop
  (Swing) UI, which is therefore an additive capability (SPEC-18). The console UI is retained as a
  second thin adapter over the same core.
- **Version pins (from research RR-1, `research-java-spring-best-practices.md`, accessed 2026-06-26):**
  Spring Boot **4.1.0** / JDK **17** baseline (fallback Boot 4.0.6); Spring AI **2.0** + MCP Java SDK
  **1.0.0**; Apache Derby **10.16.1.1** (the JDK-17 line — 10.17 needs JDK 21; Derby is EOL/read-only,
  flag a later migration to H2/PostgreSQL); **HikariCP** (Spring Boot default pool); **Maven**
  (`spring-boot-starter-parent`); **JaCoCo 0.8.16**.

Order: centralized popup, AI panel, access layer + foundational build, security/robustness (high),
then packaging, testing, broader features. Priorities: P1 = must-have/headline; P2 = important;
P3 = valuable.

---

## Theme: UX / Centralized Info Popup

### SPEC-01 Centralized widget-info popup (Swing analogue of the FF-Explorer pattern)
- **Priority:** P1
- **Motivation:** Replicate FF-Explorer's centralized info-surface pattern
  (`reference-ff-widget-popup.md`) using the Java/Swing analogue, and fix the reference's three named
  gaps (inline literals, no accessibility, no coverage enforcement). Users get consistent, themeable,
  accessible help on every control; maintainers get exactly ONE source of info text.
- **Scope:** Every interactive component in GABI's desktop UI (SPEC-18) gets its info via ONE
  centralized mechanism: the framework singleton `javax.swing.ToolTipManager.sharedInstance()` as the
  single info surface; a single info registry (a `ResourceBundle`/`.properties` catalogue keyed by a
  stable widget id, integrated with the existing i18n bundles); `UIManager` `ToolTip.background` /
  `ToolTip.foreground` / `ToolTip.font` keys as the single theming point. A `register_info(component,
  key)` helper sets the tooltip text AND the `AccessibleContext` accessible description from the same
  registry entry in one call. No scattered inline `setToolTipText("literal")` anywhere.
- **Acceptance criteria:**
  - A static analysis / unit test asserts **zero** inline string literals passed to `setToolTipText`
    outside the central helper (grep gate: only the helper calls `setToolTipText`).
  - Exactly one info registry exists (one `info.properties` family, locale-aware ES/EN); every key used
    by the UI resolves; a coverage test fails if any interactive widget lacks a registered info key.
  - `ToolTipManager.sharedInstance()` global delay/dismiss values are set in exactly one place; no
    component sets per-instance dismiss timing.
  - Tooltip look comes only from `UIManager` `ToolTip.*` keys; a test asserts no per-component tooltip
    styling override exists.
  - Accessibility: each registered component's `AccessibleContext.getAccessibleDescription()` returns
    the registry text; info is reachable by keyboard focus (a focus/`F1`-style trigger or a `?` help
    affordance), verified by a UI test that focuses a control and reads the accessible description.
  - `register_info(component, key)` is the only registration path; calling it with an unknown key
    throws (fail-fast) and is covered by a test.
- **Notes/Dependencies:** Reference: `reference-ff-widget-popup.md` (§5 Java/Swing binding; §6 gaps to
  improve — inconsistent registry, no accessibility, no coverage enforcement). Depends on SPEC-18
  (desktop UI) and SPEC-13 (i18n catalogue). Reuse the existing `statements*.properties` i18n
  infrastructure for the registry bundle.

---

## Theme: AI Assistant Panel (headline feature)

### SPEC-02 In-app AI assistant side panel (model-agnostic, Spring AI ChatClient)
- **Priority:** P1 (headline)
- **Motivation:** User-requested headline feature: let a user converse with / use an AI model directly
  **inside** GABI — a dockable assistant frame in the desktop UI. This is SEPARATE from and in addition
  to the REST + MCP access layer: the MCP server exposes GABI TO external models (SPEC-06); this panel
  brings a model INTO GABI's UI.
- **Scope:** A dockable side frame in the Swing desktop UI hosting a chat conversation with an AI model.
  Model access goes through **Spring AI 2.0 `ChatClient`** (research RR-1 §2/§3), which is
  **model-agnostic** across major providers (OpenAI, Anthropic, Azure OpenAI, Ollama, etc.). Provider,
  model name, and credentials are selectable/configurable **at runtime** (settings UI + external
  config/env), never hardcoded. The panel supports multi-turn chat, **streaming** token responses, and
  optionally injects context about the current app state/data (e.g. the selected book/member, current
  catalogue counts) into the prompt where useful. Provider errors, timeouts, and rate limits are
  handled gracefully with user-visible, non-fatal messaging.
- **Acceptance criteria:**
  - Provider is swappable at runtime: switching provider/model in settings (OpenAI ↔ Anthropic ↔ Azure
    ↔ Ollama) routes the next message through the new provider with no recompile; a test exercises at
    least two `ChatClient` backends via a mocked/abstracted provider.
  - **No hardcoded secret:** API keys are read only from secure external config or environment (honors
    the no-plaintext-credential invariant); a test/scan asserts no key literal in source or committed
    config; a missing key produces a clear "configure provider" prompt, not a crash.
  - Streaming: assistant responses render incrementally (token/chunk stream via `ChatClient` reactive
    stream); a test asserts partial chunks are delivered to the view before completion.
  - Multi-turn context is preserved within a session (conversation memory); clearing the conversation
    resets it.
  - Context injection: when enabled, the current app-state snapshot (selected entity / counts) is added
    to the prompt; a test asserts the snapshot reaches the prompt builder and that it carries no
    credential or other secret.
  - Graceful failure: provider error / timeout / HTTP 429 rate-limit surfaces a user-readable message
    in the panel, keeps the app responsive, and is logged (no stack trace to the user); covered by tests
    simulating each failure.
  - The panel is dockable/closable and reopenable from the menu; closing it does not affect core app
    function.
  - **Separation:** the assistant panel and the MCP server (SPEC-06) share no transport; a test/arch
    check asserts the panel uses `ChatClient` (outbound to a provider) and not the MCP server endpoint.
- **Notes/Dependencies:** Cite research RR-1 §2–§3 (`research-java-spring-best-practices.md`): Spring AI
  2.0 `ChatClient` is model-agnostic; `EmbeddingModel`/`VectorStore`/`QuestionAnswerAdvisor` available
  if the panel later grows RAG over the catalogue. Depends on SPEC-03 (Maven/Spring Boot), SPEC-08
  (secure credentials), SPEC-18 (desktop UI), SPEC-11 (error handling). Distinct from SPEC-06 (MCP
  server). Reuse the same Spring AI BOM pin as SPEC-06.

---

## Theme: Access Layer (core + REST + MCP) and foundational build

### SPEC-03 Portable Maven build (Spring Boot 4.1.0 / JDK 17) — foundational prerequisite
- **Priority:** P1
- **Motivation:** GABI has no portable build — dependencies live only in `.iml` + the absolute local
  path `C:/Derby/lib/derbyclient.jar`; nothing builds outside one developer's IntelliJ. This is the
  critical-path prerequisite for MCP, the AI panel, packaging, and the coverage gate.
- **Scope:** Introduce a reproducible `pom.xml` at repo root: `spring-boot-starter-parent` 4.1.0,
  `<java.version>17</java.version>`, the Spring AI BOM (`spring-ai-bom` 2.0.x) under
  `dependencyManagement`, pinned Apache Derby 10.16.1.1 (+ `derbyshared`/`derbytools`), Spring starters
  (web, data-jpa), `spring-boot-maven-plugin` for the repackaged fat jar. Convert the IntelliJ module
  layout to the Maven `src/main/java` + `src/main/resources` + `src/test/java` layout. Add `.idea/`,
  `*.iml`, `out/` to `.gitignore`.
- **Acceptance criteria:**
  - `mvn clean package` succeeds on a clean checkout with no machine-local absolute paths and no
    IntelliJ; a repackaged executable fat jar is produced.
  - All dependency versions are pinned via the parent + BOMs (no version-less or SNAPSHOT third-party
    deps except managed milestones); Derby is exactly 10.16.1.1.
  - Resources (`statements*.properties`, config template) resolve from the classpath, not from a CWD
    relative path (ties to SPEC-15).
  - `.gitignore` excludes `.idea/`, `*.iml`, `out/`; a clean `git status` after build shows no stray IDE
    artifacts.
- **Notes/Dependencies:** Cite research RR-1 §1, §4, §5. Gates SPEC-02, SPEC-05, SPEC-06, SPEC-16,
  SPEC-17. Fallback to Boot 4.0.6 only if a Spring AI 2.0 build constrains to 4.0.x (RR-1 Limitations).

### SPEC-04 Framework-independent core service (de-fuse UI / i18n / credentials from DAOs)
- **Priority:** P1
- **Motivation:** Domain logic is fused with the console UI and with per-call credential passing; DAO
  methods take a `User currentUser` + `ResourceBundle rb`, open a new JDBC connection per call, write
  to `System.out`/`System.err`, and throw `RuntimeException(localizedMessage)`. A headless core is the
  prerequisite for REST, MCP, the AI panel, and unit testing.
- **Scope:** Extract a pure `core` service exposing, per entity, the capability set the headless layer
  needs: Books (`listBooks`, `searchBooks(field,text)`, `getBook(id)`, `addBook(title,author)`,
  `deleteBook(id)`, `countBooks`), Members (`listMembers`, `searchMembers`, `getMember`, `addMember`,
  `deleteMember`), Loans (`listLoans`, `createLoan(memberId,bookId)`, `returnLoan(loanId)`,
  `listLoansByMember`), Users (admin: `listUsers`, `addUser`, `deleteUser`). The core owns all business
  rules (duplicate checks, loan limits, "cannot delete a lent book"), takes an injected
  `DataSource`/connection (not per-call credentials), returns locale-free value objects (no i18n in the
  core), and signals failure via typed exceptions (no localized strings, no stdout/stderr).
- **Acceptance criteria:**
  - The `core` module compiles and is unit-tested with **no** dependency on Swing, console I/O, Spring
    web, or `ResourceBundle`; an arch test asserts no `System.out`/`System.err` and no `ResourceBundle`
    reference in `core`.
  - Every business rule currently in a menu or a DAO has a core method + a unit test (duplicate book,
    loan limit, delete-lent-book guard, return-loan).
  - Core methods accept an injected `DataSource` and never call `DriverManager.getConnection` with
    per-call user credentials.
  - Core methods return typed DTOs/value objects and throw typed exceptions; no method returns a
    localized string and none returns bare `null` for a missing entity (use `Optional`).
  - The console UI is refactored to a thin adapter over the core with no behavioral regression
    (covered by adapter tests).
- **Notes/Dependencies:** Understanding doc §"Core capability candidates" + defects 5,6,7. Gates
  SPEC-05, SPEC-06, SPEC-17. Depends on SPEC-10 (DataSource), SPEC-13 (i18n moved to edges).

### SPEC-05 REST API over the core service
- **Priority:** P1
- **Motivation:** Expose the framework-independent core over HTTP so external clients (and the future
  packaged app) can drive library operations programmatically.
- **Scope:** A Spring MVC REST layer (`spring-boot-starter-web`) mapping core capabilities to resource
  endpoints for Books, Members, Loans, and (admin-gated) Users. Thin controllers: validate input,
  delegate to the core, map typed core exceptions to HTTP status codes, serialize DTOs to JSON.
  i18n applied at this edge only.
- **Acceptance criteria:**
  - CRUD + search endpoints exist for Books, Members, Loans; admin endpoints for Users.
  - Controllers contain no business rule (arch test: controllers call only core + mappers).
  - Validation failures → 400 with a structured error body; not-found → 404; privileged user endpoints
    require admin and return 403 otherwise (ties SPEC-09); covered by MockMvc/`RestTestClient` tests.
  - No credential or secret appears in any response body or error payload (test asserts).
  - OpenAPI/endpoint docs generated or hand-authored; each endpoint has at least one integration test.
- **Notes/Dependencies:** Cite RR-1 §5, §7. Depends on SPEC-03, SPEC-04. Pairs with SPEC-06 (same core).

### SPEC-06 MCP server exposing GABI to external models (Spring AI MCP)
- **Priority:** P1
- **Motivation:** Expose GABI's core capabilities as MCP tools so external AI models/agents can operate
  the library. This is the outbound counterpart to SPEC-02 (which brings a model into GABI).
- **Scope:** A Spring AI 2.0 MCP server (`spring-ai-starter-mcp-server-webmvc`, MCP Java SDK 1.0.0)
  exposing core capabilities as MCP tools via the `@McpTool` / `@McpToolParam` annotation idiom. Read
  tools (list/search/get/count) for Books/Members/Loans; mutating tools (add/delete/createLoan/
  returnLoan) with validation; user-admin tools gated as privileged. Tools delegate to the core; no
  business logic in tool methods.
- **Acceptance criteria:**
  - The server starts under Spring Boot 4 and registers one MCP tool per exposed core capability;
    `@McpTool` auto-generates each tool's JSON schema.
  - Each tool delegates to the core and carries no business rule (arch test).
  - An MCP client integration test invokes representative read and mutating tools and asserts correct
    results and schema.
  - Privileged user-admin tools reject non-admin callers (ties SPEC-09); covered by a test.
  - No plaintext credential is exposed through any tool input/output (test asserts).
  - **Separation:** the MCP server and the SPEC-02 assistant panel share no endpoint/transport (arch
    check).
- **Notes/Dependencies:** Cite RR-1 §2 (`@McpTool`/`@McpToolParam`, MCP Java SDK 1.0.0). Depends on
  SPEC-03, SPEC-04. Distinct from SPEC-02.

---

## Theme: Robustness / Security (high)

### SPEC-07 SQL identifier validation on all DDL / admin paths
- **Priority:** P1
- **Motivation:** Table/field/user names are concatenated into SQL via `String.format` in
  `DatabaseBuilder` and `UserDerby` (GRANT/REVOKE, Derby `SET PROPERTY`, `derby.user.<name>`); a user
  name flows unescaped into DDL — an injection surface. Data-row CRUD already uses `PreparedStatement`.
- **Scope:** Centralize a strict identifier validator (whitelist: `[A-Za-z][A-Za-z0-9_]*`, bounded
  length, optional explicit allow-list of known table/column names) applied to every identifier that
  reaches a DDL/admin statement. Reject or safely quote; never interpolate raw user input into DDL.
- **Acceptance criteria:**
  - Every code path that builds DDL/admin SQL (`DatabaseBuilder`, user provisioning, GRANT/REVOKE, `SET
    PROPERTY`) routes its identifiers through the single validator; an arch test asserts no `String.
    format`/concatenation into a DDL statement bypasses it.
  - Injection attempts in a user name (`"; DROP TABLE ..."`, quotes, whitespace, Unicode tricks) are
    rejected with a typed error; covered by parameterized negative tests.
  - Valid identifiers pass unchanged; existing provisioning still works (positive tests).
- **Notes/Dependencies:** Understanding doc defect 4. Applies across SPEC-04/05/06 admin paths.

### SPEC-08 No plaintext credential leakage (secure config + secrets)
- **Priority:** P1
- **Motivation:** `configuration.properties` commits plaintext `database-name=admin` /
  `database-password=1234`; credentials are also passed as CLI args and held in the `User` object.
  Adding REST/MCP/AI multiplies the leak surface (logs, responses, fixtures, AI provider keys).
- **Scope:** Remove all plaintext secrets from the repo. DB credentials and AI provider API keys come
  only from environment variables or an external (gitignored) secrets file / OS credential store, read
  via Spring config with placeholders. Provide a committed `configuration.properties.template` with no
  real values. Ensure no secret is logged, returned in any REST/MCP response, embedded in the AI prompt
  context, or written to checkpoints/memory.
- **Acceptance criteria:**
  - No real credential literal exists in any committed file; a build-time scan/test fails if a
    `password=`/API-key-shaped literal is found in tracked sources/config.
  - The app boots from env/external secrets; a missing secret yields a clear configuration error, not a
    plaintext fallback.
  - A log-scrubbing test asserts credentials/keys never appear in log output or exception messages.
  - REST/MCP responses and AI prompt context are asserted free of secrets (ties SPEC-02, SPEC-05/06).
  - The committed template carries placeholders only; the real secrets file is gitignored.
- **Notes/Dependencies:** Understanding doc defect 3. Cross-cuts SPEC-02, SPEC-05, SPEC-06, SPEC-12,
  SPEC-15. Invariant: keep these values out of every generated asset, fixture, checkpoint, and memory.

### SPEC-09 Privileged admin surface gating (uniform across adapters)
- **Priority:** P1
- **Motivation:** User provisioning, grants/revokes, and DB bootstrap are privileged side-effecting
  operations gated today only by an inline console check (`currentUser == database-name`). Each new
  adapter (REST, MCP) must enforce the same gate, not re-implement or bypass it.
- **Scope:** Centralize an authorization check in the core for privileged operations (user admin, DB
  build, grant/revoke). Every adapter (console, desktop UI, REST, MCP, AI-panel-driven actions) routes
  privileged calls through it. Non-admin callers are denied uniformly.
- **Acceptance criteria:**
  - All privileged operations share one authorization gate in the core; an arch test asserts no adapter
    performs a privileged DAO/admin call without it.
  - REST returns 403, MCP rejects, console/desktop deny, for non-admin callers; each covered by a test.
  - Admin callers succeed; positive + negative tests for each adapter.
- **Notes/Dependencies:** Understanding doc (`UserMenu` admin gating, defect 4 context). Depends on
  SPEC-04; enforced by SPEC-05/06.

### SPEC-10 DataSource + HikariCP pooling (replace connection-per-call)
- **Priority:** P1
- **Motivation:** Every DAO method opens/closes its own `DriverManager` connection with the caller's
  credentials — unfit for a server-side access layer and untestable headlessly.
- **Scope:** Introduce a single managed `DataSource` (Spring Boot default **HikariCP**) injected into
  the core/DAOs. Remove per-call `DriverManager.getConnection(url, name, password)`. Configure pool
  size and the Derby (10.16.1.1) connection per research RR-1 §4 (embedded or network as chosen).
- **Acceptance criteria:**
  - DAOs/core obtain connections only from the injected `DataSource`; an arch test asserts no
    `DriverManager.getConnection` call remains in the data layer.
  - Connections are returned to the pool (try-with-resources); a load test asserts no connection leak
    under repeated operations.
  - Pool size is configurable; a test verifies the configured `maximum-pool-size` is honored.
- **Notes/Dependencies:** Understanding doc defect 6; RR-1 §4. Depends on SPEC-03; gates SPEC-04/05/06.

### SPEC-11 Error handling (typed exceptions, no null returns, graceful adapters)
- **Priority:** P2
- **Motivation:** `searchDetailDB` returns `null` and `countDB` can return `null` on SQLException —
  latent NPEs; DAOs throw `RuntimeException(localizedMessage)`. Adapters need predictable, typed errors.
- **Scope:** Replace null-on-error and null-on-missing with `Optional`/typed results; define a typed
  exception hierarchy in the core (not-found, validation, conflict, persistence). Each adapter maps the
  hierarchy to its idiom (REST status, MCP error, console/desktop message, AI panel message).
- **Acceptance criteria:**
  - No core/DAO method returns bare `null`; missing entities use `Optional`; covered by tests.
  - SQL/persistence failures raise a typed core exception, never a silent `null`; covered by tests.
  - Each adapter maps every core exception type to a defined user-facing outcome (tested per adapter).
- **Notes/Dependencies:** Understanding doc defect 7. Depends on SPEC-04.

### SPEC-12 Structured logging framework (replace System.out/err)
- **Priority:** P2
- **Motivation:** Direct `System.out`/`System.err` printf throughout; no levels, no structure, and a
  channel through which secrets could leak.
- **Scope:** Introduce SLF4J + the Spring Boot default Logback. Replace data-layer/core stdout/stderr
  with logger calls at appropriate levels. Console UI presentation output stays at the UI edge (not via
  the logger). Configure a log format and ensure scrubbing of secrets (ties SPEC-08).
- **Acceptance criteria:**
  - No `System.out`/`System.err` remains in `core`/data layers (arch test); UI presentation output is
    confined to the UI adapter.
  - Log levels are used meaningfully (error/warn/info/debug); a config controls verbosity.
  - A test asserts no credential/secret reaches log output.
- **Notes/Dependencies:** Understanding doc defect 10. Depends on SPEC-03; pairs with SPEC-08.

### SPEC-13 i18n separation (move ES/EN text out of the data layer)
- **Priority:** P2
- **Motivation:** DAOs require a `ResourceBundle` and emit localized strings; i18n is fused into
  persistence, blocking headless reuse and clean testing.
- **Scope:** Move all localization to the UI/adapter edges. The core returns locale-free codes/values;
  console, desktop UI, REST, and MCP each localize using the `statements*.properties` bundles (ES/EN).
  The info registry (SPEC-01) shares this i18n infrastructure.
- **Acceptance criteria:**
  - No `ResourceBundle` reference in `core`/data layers (arch test, also asserted by SPEC-04).
  - All user-facing strings resolve from the ES and EN bundles at the edges; a test asserts both locales
    resolve every key with no missing-key fallbacks.
- **Notes/Dependencies:** Understanding doc defect 5. Underpins SPEC-01, SPEC-04.

### SPEC-14 Input validation at every adapter edge
- **Priority:** P2
- **Motivation:** Console input is validated ad hoc (`checkOptionInput`, `checkString` regex); REST/MCP/
  AI inputs need consistent validation to protect the core and the DB.
- **Scope:** Define validation rules in the core (or shared validators) for entity fields (title/author/
  name/surname length, allowed chars, required fields, id ranges) and apply them uniformly at each
  adapter edge before reaching the core. Reject invalid input with a typed validation error.
- **Acceptance criteria:**
  - Each entity field has explicit validation rules with positive/negative tests.
  - Console, desktop, REST, MCP each reject invalid input consistently (parameterized tests across
    adapters).
  - Validation errors are typed (SPEC-11) and localized at the edge (SPEC-13).
- **Notes/Dependencies:** Understanding doc (`Utils` validation; defect 4). Depends on SPEC-04/11.

### SPEC-15 Classpath + external-writable config (fix relative-path I/O)
- **Priority:** P2
- **Motivation:** `readProperties()` and the `database-isbuilt` write-back hardcode
  `src/utils/configuration.properties` relative to CWD — breaks under any packaged artifact (jar/
  jpackage) and any CWD other than the repo root.
- **Scope:** Read config from the classpath; write mutable state (e.g. `database-isbuilt`) to a defined
  external writable location (user config dir), not into `src/`. No source-tree writes at runtime.
- **Acceptance criteria:**
  - Config loads from the classpath in a packaged jar (test runs from a jar / non-repo CWD).
  - Runtime state writes go to an external writable path; a test asserts no write under `src/`.
  - Works under jpackage layout (ties SPEC-16).
- **Notes/Dependencies:** Understanding doc defect 2. Gates SPEC-16; depends on SPEC-03.

---

## Theme: Packaging

### SPEC-16 Cross-platform packaging via jpackage
- **Priority:** P2
- **Motivation:** Distribution today is "open in IntelliJ and Run" — no jar, no installer. Users need a
  native installable artifact.
- **Scope:** Produce platform-native installers from the Spring Boot repackaged fat jar via `jpackage`
  (JDK-bundled). Document per-OS builds (Windows exe/msi, macOS dmg/pkg, Linux deb/rpm; jpackage is not
  cross-compiling). Optionally bundle a trimmed JDK 17 runtime via `jlink` (`--runtime-image`).
- **Acceptance criteria:**
  - A documented `jpackage` invocation builds a runnable installer/app-image from the fat jar with
    `--main-class org.springframework.boot.loader.launch.JarLauncher` (verify against the produced jar
    `MANIFEST.MF`).
  - The packaged app launches and reaches the main UI/console with config loaded from the classpath/
    external location (ties SPEC-15) and a reachable Derby DB.
  - The build is reproducible from `mvn clean package` + the documented jpackage step; no machine-local
    absolute paths.
- **Notes/Dependencies:** Cite RR-1 §6. Depends on SPEC-03, SPEC-15.

---

## Theme: Testing

### SPEC-17 JUnit 5 + Spring Test suite with JaCoCo coverage gate
- **Priority:** P2
- **Motivation:** Tests are entirely absent (no test source root, no JUnit, no coverage). Every spec
  above asserts behavior that must be verified; a coverage gate prevents regressions.
- **Scope:** Add `spring-boot-starter-test` (JUnit Jupiter, Spring Test, Mockito, AssertJ). Unit-test
  the core (business rules, validation, exceptions); slice/integration-test the adapters (MockMvc /
  `RestTestClient` for REST, an MCP client test for MCP, UI tests for the popup/AI panel where
  feasible). Wire JaCoCo 0.8.16 (`prepare-agent` + `report` + `check`) with a coverage threshold gate.
- **Acceptance criteria:**
  - `mvn test` runs the suite green on a clean checkout; core business rules each have a test.
  - JaCoCo produces a report and the `check` goal **fails the build** below the configured threshold
    (set an explicit minimum, e.g. core ≥ 80% line coverage); surefire/failsafe forking is not disabled
    (javaagent intact).
  - Adapter tests exist for REST (MockMvc/`RestTestClient`), MCP (client invocation), and the security
    specs (SPEC-07/08/09 negative cases).
  - No test fixture contains a real credential (ties SPEC-08).
- **Notes/Dependencies:** Cite RR-1 §7 (JUnit Jupiter on Boot 4, JaCoCo 0.8.16). Depends on SPEC-03,
  SPEC-04; verifies all other specs.

---

## Theme: Features (broad expansion)

### SPEC-18 Swing desktop UI shell (menus mirroring the console; hosts popup + AI panel)
- **Priority:** P1
- **Motivation:** GABI is console-only today, but the headline popup (SPEC-01) and AI panel (SPEC-02)
  require a desktop UI. A Swing UI is therefore an additive capability and the host for both.
- **Scope:** A Swing desktop application over the same core (SPEC-04) with menus mirroring the existing
  console structure (Books, Members, Loans, Users-admin), localized (ES/EN, SPEC-13). It hosts the
  centralized info popup (SPEC-01) and the dockable AI assistant panel (SPEC-02). The console UI is
  retained as a parallel thin adapter.
- **Acceptance criteria:**
  - The desktop UI performs all current console operations (CRUD + search for each entity, admin user
    ops) against the core, with no business logic in the UI (arch test).
  - Menu actions, validation (SPEC-14), and error display (SPEC-11) work in both ES and EN.
  - The UI hosts a working info-popup mechanism (SPEC-01) and a dockable AI panel (SPEC-02).
  - Both console and desktop adapters pass equivalent behavioral tests over the shared core.
- **Notes/Dependencies:** Depends on SPEC-04, SPEC-13; gates SPEC-01, SPEC-02.

### SPEC-19 Loan due dates, overdue tracking, and return workflow
- **Priority:** P2
- **Motivation:** Loans currently store only a loan date; a real library tracks due dates and overdue
  loans. High-value domain expansion grounded in the Loans entity.
- **Scope:** Add a due-date to loans (configurable loan period), compute overdue status, list overdue
  loans, and surface overdue indicators in UI/REST/MCP. Enforce loan rules (a member's max concurrent
  loans, no second active loan of the same book) in the core.
- **Acceptance criteria:**
  - Creating a loan sets a due date from a configurable period; `returnLoan` clears active status.
  - `listOverdueLoans()` (core) returns loans past due; exposed via REST/MCP and shown in the UI.
  - Loan-limit and duplicate-active-loan rules are enforced in the core with tests.
- **Notes/Dependencies:** Understanding doc (Loans entity, loan limit rule). Depends on SPEC-04; needs a
  DB schema/migration for the due-date column.

### SPEC-20 Catalogue search and pagination enhancement
- **Priority:** P3
- **Motivation:** Search today is SQL `LIKE %seed%` over single name columns; users need
  multi-field, case-insensitive, paginated search as the catalogue grows.
- **Scope:** Extend core search to multi-field (title+author / name+surname), case-insensitive, with
  pagination + sorting; expose via REST (query params) and MCP (tool params) and the UI.
- **Acceptance criteria:**
  - Core search supports multiple fields, case-insensitivity, page/size/sort; covered by tests.
  - REST and MCP expose the search params; pagination metadata is returned.
  - Search uses parameterized queries only (no identifier interpolation; ties SPEC-07).
- **Notes/Dependencies:** Understanding doc (`searchTB`/`searchUser`). Depends on SPEC-04/05/06.

### SPEC-21 Reports and data export (CSV/JSON)
- **Priority:** P3
- **Motivation:** Library operators benefit from exportable reports (current loans, overdue list,
  catalogue, member activity). Adds product value and feeds the AI panel's context.
- **Scope:** Core report builders (catalogue, active loans, overdue, loans-per-member) and export to
  CSV/JSON; expose via REST download endpoints and a UI export action.
- **Acceptance criteria:**
  - Each report has a core builder with a test asserting content correctness.
  - REST endpoints return CSV and JSON; the UI offers an export action.
  - Exports contain no credentials/secrets (ties SPEC-08).
- **Notes/Dependencies:** Depends on SPEC-04, SPEC-19 (overdue). Optional input to SPEC-02 context.

### SPEC-22 Optional RAG over the catalogue (Spring AI EmbeddingModel + VectorStore)
- **Priority:** P3
- **Motivation:** Grow the AI panel from plain chat into retrieval-augmented answers grounded in GABI's
  catalogue/ingested docs — the greenfield R2.a capability, kept optional and clearly separated.
- **Scope:** Ingest catalogue (and optional documents) into a Spring AI `VectorStore` (in-JVM
  `SimpleVectorStore` for the offline path; PGVector when persistence/scale is needed) using an
  `EmbeddingModel` (Ollama local, or OpenAI/Azure hosted), and answer questions via the
  `QuestionAnswerAdvisor` inside the AI panel (SPEC-02). Model-agnostic; no hardcoded keys.
- **Acceptance criteria:**
  - `ingest(source)` populates the vector store; `ask(question)` returns an answer grounded in ingested
    content with cited sources; covered by tests with a mocked embedding/vector backend.
  - Embedding provider is configurable at runtime (Ollama ↔ OpenAI ↔ Azure), no hardcoded key
    (SPEC-08).
  - RAG is optional and isolated: disabling it leaves plain chat (SPEC-02) and all other features
    functional.
- **Notes/Dependencies:** Cite RR-1 §3 (`EmbeddingModel`/`VectorStore`/`QuestionAnswerAdvisor`). Depends
  on SPEC-02, SPEC-03; greenfield (no existing AI code per understanding doc).

---

## Spec count by priority

- **P1 (11):** SPEC-01, SPEC-02, SPEC-03, SPEC-04, SPEC-05, SPEC-06, SPEC-07, SPEC-08, SPEC-09, SPEC-10,
  SPEC-18.
- **P2 (8):** SPEC-11, SPEC-12, SPEC-13, SPEC-14, SPEC-15, SPEC-16, SPEC-17, SPEC-19.
- **P3 (3):** SPEC-20, SPEC-21, SPEC-22.

**Total: 22 specs — P1 = 11, P2 = 8, P3 = 3.**
