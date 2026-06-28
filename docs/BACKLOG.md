# Improvement Backlog — GABI (Java / Spring Boot 4 / Spring AI 2)

- Repo slug: `gabi`
- Working copy: `D:\Documentos\GitHub\GABI`
- Enhancement branch: `enhancement/gabi-20260612`
- Author: the-recommender (planning only — target repo NOT modified). Date: 2026-06-13.
- Inputs: `docs/review-gabi.md` (verification review — CRITICAL defects found), `docs/research-competitive-gabi.md`
  (competitive feature research), `docs/understanding-gabi.md` (repo context).
- Asset-capability tags map to manifest agents: **feature-enhancer** (core/feature/legacy code + deps),
  **access-layer-builder** (MCP @Tool + REST routes over core), **packaging** (jpackage config),
  **testing** (JUnit + JaCoCo), **docs** (documentation). Each item names the precise capability needed.

How to read this doc: every item is self-contained and pick-up-cold ready. Section 1 = bugs/fixes from the
review (CRITICAL first). Section 2 = improvements/features from competitive research + review gaps, ordered by
value/effort. Each item carries a one-line **Asset capability needed:** tag.

---

## SECTION 1 — BUGS & FIXES (ordered by severity)

### GABI-B01 — IdentifierValidator NOT applied to legacy CLI user-admin sinks in UserDerby.addDb
- **Severity:** CRITICAL
- **File:line:** `src/sql/users/UserDerby.java:164, 169, 171-172` (review B-1)
- **Root cause:** The D-4 SQL-identifier-injection fix (`IdentifierValidator`) was applied to the new core
  (`core/LibraryServiceImpl.addUser`) and to `sql/DatabaseBuilder.java:50-65`, but `UserDerby.java` has **zero**
  reference to `IdentifierValidator` (confirmed by full-tree grep). User-supplied `newUser.getName()` flows
  unescaped into `derby.user.<name>` SET PROPERTY (line 164), the `derby.database.fullAccessUsers` list (line 169),
  and `GRANT ALL PRIVILEGES ... TO <name>` (lines 171-172). This is a LIVE path: `UserMenu` (instantiated by
  `LibMenu.java:262`, admin-gated) calls `UserDerby.getInstance().addDb(...)` at `UserMenu.java:188`. The headline
  security fix is bypassed whenever a user is provisioned through the preserved CLI.
- **Fix approach:** Route `newUser.getName()` through `IdentifierValidator` (the same validator used by the core)
  before every interpolation in `addDb`. Validate against the Derby SQL-identifier grammar; reject on failure with
  a non-localized exception lifted to the UI edge. Reuse the existing validator class — do NOT fork a second one.
- **Acceptance criterion:** A unit/integration test feeds `addDb` a username containing a single quote / SQL
  metacharacter (e.g. `bob'; DROP`) and asserts it is rejected before any statement executes; a valid identifier
  still provisions. Grep confirms `IdentifierValidator` is referenced at every sink in `UserDerby.addDb`.
- **Asset capability needed:** harden legacy CLI SQL sink (apply IdentifierValidator in `UserDerby.addDb`) — feature-enhancer; add JUnit tests — testing.

### GABI-B02 — IdentifierValidator NOT applied to legacy CLI user-admin sinks in UserDerby.deleteDB
- **Severity:** CRITICAL
- **File:line:** `src/sql/users/UserDerby.java:318, 320-321` (review B-1, delete sink)
- **Root cause:** `deleteDB` interpolates the stored name `rs1.getString(1)` into the `derby.user.<name>` null
  SET PROPERTY (line 318) and into `REVOKE ALL PRIVILEGES ... FROM <name>` (lines 320-321) with no
  `IdentifierValidator`. Same injection class as B01; reachable from the live CLI admin path
  (`UserMenu.java:337,355` → `UserDerby.deleteDB`). A malicious identifier stored via the unhardened add path
  (B01) re-detonates on delete.
- **Fix approach:** Validate the name read from the result set through `IdentifierValidator` before interpolating
  into the REVOKE / SET PROPERTY statements in `deleteDB`. Treat a stored value failing validation as a data
  integrity error (fail closed). Same validator instance as B01.
- **Acceptance criterion:** Integration test provisions a valid user, deletes it (passes), and a test asserting a
  crafted/invalid stored identifier is rejected by `deleteDB` before any REVOKE executes. Grep confirms
  `IdentifierValidator` at both sinks in `deleteDB`.
- **Asset capability needed:** harden legacy CLI SQL sink (apply IdentifierValidator in `UserDerby.deleteDB`) — feature-enhancer; add JUnit tests — testing.

### GABI-B03 — Duplicate top-level `spring:` key in application.yml guts context startup
- **Severity:** CRITICAL
- **File:line:** `src/main/resources/application.yml:28 & 91` (review B-2)
- **Root cause:** Two top-level `spring:` mappings exist with no `---` document separator. Under SnakeYAML's
  default loader (Boot 4) a duplicate mapping key throws `DuplicateKeyException` at parse time → context fails to
  start. Under a lenient loader the second `spring:` block silently replaces the first, discarding
  `spring.main.web-application-type: none`, `spring.autoconfigure.exclude` (DataSourceAutoConfiguration!), and the
  entire `spring.ai.mcp.server` config. Either outcome breaks the primary artifact.
- **Fix approach:** Merge the two `spring:` blocks into a single mapping so all keys (`main.web-application-type`,
  `autoconfigure.exclude`, `ai.mcp.server`, and whatever lives at line 28) coexist under one root. Preserve every
  child key from both blocks. Do not rely on a `---` separator as the fix (that creates two profile documents);
  the correct fix is one merged `spring:` mapping.
- **Acceptance criterion:** `application.yml` has exactly one top-level `spring:` key (grep/YAML-lint). A context
  smoke test (`@SpringBootTest` or `context loads`) starts successfully, and an assertion confirms
  `spring.main.web-application-type=none`, the `autoconfigure.exclude` list, and `spring.ai.mcp.server` are all
  effective (not dropped).
- **Asset capability needed:** fix Spring config/profiles (merge duplicate `spring:` root in application.yml) — feature-enhancer; add context-load JUnit test — testing.

### GABI-B04 — RAG NoOp fallback wiring fails startup when no model is configured
- **Severity:** HIGH
- **File:line:** `src/main/java/rag/NoOpRagService.java:21,29`, `rag/RagServiceImpl.java:51`, `rag/RagConfig.java:56` (review B-3)
- **Root cause:** Two compounding defects. (1) `NoOpRagService` is `@ConditionalOnMissingBean(name="ragServiceImpl")`
  and its Javadoc claims `RagServiceImpl` is `@Primary`, but `RagServiceImpl` is a plain `@Service` with no
  `@Primary`/`@Profile`. `@ConditionalOnMissingBean` on a component-scanned `@Service` is bean-order-fragile →
  possible `NoUniqueBeanDefinitionException` for `RagService` injected by type into `LibraryServiceImpl`.
  (2) `RagServiceImpl` is unconditional: its constructor needs `SimpleVectorStore` + `ChatClient.Builder`, and
  `RagConfig.simpleVectorStore` requires an `EmbeddingModel` bean (`RagConfig.java:56`). With no model running,
  Spring tries to build `RagServiceImpl` and fails on the missing `EmbeddingModel`/`ChatClient` — the intended
  "NoOp keeps the app booting" graceful degradation never happens.
- **Fix approach:** Make `RagServiceImpl` (and the `RagConfig` beans it depends on) conditional on the model being
  present — e.g. `@ConditionalOnBean(EmbeddingModel.class)` (or a clean `@Profile`/`@ConditionalOnProperty` toggle)
  on `RagServiceImpl` and on `RagConfig.simpleVectorStore`. Make `NoOpRagService` the typed fallback via
  `@ConditionalOnMissingBean(RagService.class)` (by type, not by bean name) so exactly one `RagService` exists in
  every configuration. Correct the stale `@Primary` Javadoc claim.
- **Acceptance criterion:** With NO model configured (no `EmbeddingModel`/`ChatClient`), a `@SpringBootTest`
  context starts and exactly one `RagService` bean is present and is the NoOp; with a model profile active, exactly
  one `RagService` is present and is `RagServiceImpl`. Neither configuration throws
  `NoUniqueBeanDefinitionException` or a missing-dependency failure.
- **Asset capability needed:** fix Spring config/profiles (make RAG beans conditional on EmbeddingModel/model presence) — feature-enhancer; add both-configurations context JUnit tests — testing.

### GABI-B05 — Mis-scoped JaCoCo check rule does not enforce the 0.90 core gate
- **Severity:** HIGH
- **File:line:** `pom.xml:322-346` (rule), specifically `pom.xml:323-331` (review B-4)
- **Root cause:** The `check` rule uses `<element>BUNDLE</element>` with `<includes><include>core/*</include></includes>`.
  JaCoCo `includes`/`excludes` match the element's OWN name; for `BUNDLE` that name is the project/bundle name, not
  class/package names. So `core/*` does not scope the limit to the core package — the rule either matches no bundle
  named `core/*` and is silently skipped (gate never enforced) or applies 0.90 to the whole bundle (fails for the
  wrong reason given UI/legacy packages). The "≥0.90 on core, bound to verify, truly fails the build" requirement
  is not reliably met.
- **Fix approach:** Change the rule element to `PACKAGE` (or `CLASS`) and set the include pattern to the actual core
  package name (e.g. `core` / `core/*` as a PACKAGE-name glob, matching how the core package is named in the JaCoCo
  report). Keep the `LINE`/`INSTRUCTION` ratio limit at 0.90, keep the execution bound to `verify`. Optionally add
  matching `excludes` for legacy/UI packages so they are not dragged into the gate.
- **Acceptance criterion:** A deliberate coverage drop in a `core` class (e.g. an untested branch) makes
  `mvn verify` FAIL with a JaCoCo rule violation naming the core package; full core coverage passes. The rule
  element is `PACKAGE`/`CLASS` (not `BUNDLE`) and the include resolves to the core package in the JaCoCo report.
- **Asset capability needed:** fix JaCoCo rule in pom (BUNDLE→PACKAGE/CLASS scoping to enforce 0.90 core gate) — testing.

### GABI-B06 — GabiCliRunner not profile-gated; server mode hangs on console input
- **Severity:** MEDIUM
- **File:line:** `src/main/java/.../GabiCliRunner.java:27-36`; contradicts `application-server.yml:13` (review B-6)
- **Root cause:** `GabiCliRunner` is an unconditional `@Component implements CommandLineRunner` with no `@Profile`,
  yet `application-server.yml:13` documents "the CommandLineRunner is conditional on the 'cli' profile … suppressed
  in server mode." With the server profile active, the runner still fires and `LibMenu.main` blocks on
  `Scanner(System.in)`, so the documented `--spring.profiles.active=server` (MCP+REST, no CLI) launch hangs on
  console input — a functional mismatch between code and the access-layer contract.
- **Fix approach:** Annotate `GabiCliRunner` with `@Profile("cli")` (or `@ConditionalOnProperty`/`@Profile("!server")`
  consistent with the documented contract) so it is instantiated only in CLI mode and suppressed in server mode.
  Ensure the default/no-profile launch still gives the intended console behavior.
- **Acceptance criterion:** Starting with `--spring.profiles.active=server` does NOT instantiate `GabiCliRunner`
  (assert the bean is absent or the runner does not execute) and the context becomes ready without blocking on
  `System.in`; starting with the `cli` profile still launches the console menu.
- **Asset capability needed:** fix Spring config/profiles (profile-gate GabiCliRunner to cli) — feature-enhancer; add server-profile no-CLI JUnit test — testing.

### GABI-B07 — Password interpolated unescaped into Derby SET PROPERTY (new core + legacy mirrors)
- **Severity:** MEDIUM
- **File:line:** `core/LibraryServiceImpl.java:611-612` (primary); mirrors `sql/users/UserDerby.java:164`, `sql/DatabaseBuilder.java:74-75` (review B-5)
- **Root cause:** `LibraryServiceImpl.addUser` validates the username but builds the credential DDL as
  `"'derby.user." + validName + "', '" + pwdValue + "')"` with `pwdValue` raw user input. A password containing a
  single quote breaks the statement — a second-order injection vector on the privileged SET PROPERTY call.
  Passwords cannot be parameterized in this Derby call, so they must be escaped/validated, not concatenated raw.
  Same untreated pattern in `UserDerby.java:164` and `DatabaseBuilder.java:74-75`.
- **Fix approach:** Introduce a password-escaping/validation step (Derby string-literal escaping — double any
  single quote — and/or reject passwords containing disallowed characters) applied at all three sites before
  building the SET PROPERTY DDL. Centralize the escaping helper so the core and the two legacy sites share one
  implementation. Keep the actual secret out of logs and out of any checkpoint/memory record.
- **Acceptance criterion:** A test sets a password containing a single quote (e.g. `pa'ss`) and asserts the
  provisioning DDL is well-formed and the user authenticates (or the password is rejected by an explicit policy) —
  with no statement breakage/injection. The escaping helper is referenced at all three sites (grep).
- **Asset capability needed:** harden core + legacy SQL sink (escape/validate password in SET PROPERTY DDL across core and legacy) — feature-enhancer; add JUnit tests — testing.

### GABI-B08 — Committed dev-default credential pair + docs still advertise `admin 1234` CLI invocation
- **Severity:** MEDIUM
- **File:line:** `src/main/resources/utils/configuration.properties:40,43` (`database-name=admin`,
  `database-password=dev-local-only`); `application.yml:24` and several Javadocs still show `java -jar gabi.jar admin 1234` (review C caveats, D-list)
- **Root cause:** `configuration.properties` ships a committed default credential pair. Not a live secret, but it
  is a credential-as-default-config pattern, and `application.yml:24` plus Javadocs keep advertising the
  `admin 1234` credentials-as-CLI-arg pattern — perpetuating the original plaintext-credential smell the campaign
  was meant to remove.
- **Fix approach:** Replace the committed default credential values with empty env-placeholder semantics
  (`${DB_USER:}` / `${DB_PASSWORD:}` style, matching `application.yml:78-79`) or remove them from
  `configuration.properties` entirely and document an external/secret source. Remove `admin 1234` (and any literal
  credentials) from `application.yml:24` and from Javadoc examples; replace with placeholder/env-var guidance.
- **Acceptance criterion:** Grep over the repo finds no committed literal credential pair in
  `configuration.properties` and no `admin 1234` (or other literal credentials) in `application.yml`/Javadoc; a
  docs/security note explains the env-var/secret-based credential flow. App still starts when credentials are
  supplied via env vars.
- **Asset capability needed:** fix Spring config/profiles (remove committed default creds, env-placeholder them) — feature-enhancer; update credential docs/examples — docs.

### GABI-B09 — GlobalExceptionHandler has no catch-all; non-library errors bypass RFC-9457
- **Severity:** LOW
- **File:line:** `src/main/java/access/rest/GlobalExceptionHandler.java` (review B-7)
- **Root cause:** The handler maps only `LibraryException` subtypes. Non-library runtime errors (e.g.
  `IllegalArgumentException` from `LibraryServiceImpl.getTableName`, or a legacy `RuntimeException`) bypass
  RFC-9457 `ProblemDetail` translation and fall through to Spring's default error page — a small consistency /
  info-leak gap on the REST surface.
- **Fix approach:** Add an `@ExceptionHandler(Exception.class)` (or `RuntimeException.class`) catch-all that returns
  a generic RFC-9457 `ProblemDetail` (500-class) WITHOUT serializing the cause/stack trace — matching the existing
  no-leak policy at `GlobalExceptionHandler.java:64-65`. Keep specific `LibraryException` mappings ahead of the
  catch-all.
- **Acceptance criterion:** A REST test triggering a non-`LibraryException` runtime error receives an RFC-9457
  `ProblemDetail` response (correct content type, generic detail, no stack trace / DB internals in the body), not
  the default Spring error page.
- **Asset capability needed:** add REST exception handler (RFC-9457 catch-all in GlobalExceptionHandler) — access-layer-builder; add REST JUnit test — testing.

### GABI-B10 — Redundant/dead parameter set in listLoansWithDetails
- **Severity:** LOW
- **File:line:** `core/LibraryServiceImpl.java:419-421` (review B-8)
- **Root cause:** `psMember.setInt(1, memberId)` is set, then `psMember.clearParameters()`, then set again — dead,
  confused code (harmless but indicates the method was edited without cleanup).
- **Fix approach:** Remove the redundant `setInt`/`clearParameters`/`setInt` sequence; keep a single correct
  `psMember.setInt(1, memberId)` before execution. Verify no other parameter index relies on the cleared state.
- **Acceptance criterion:** The method body has a single `setInt(1, memberId)` with no intervening
  `clearParameters()`; existing `listLoansByMember`/`listLoansWithDetails` tests still pass.
- **Asset capability needed:** edit headless core service (remove dead parameter code in LibraryServiceImpl) — feature-enhancer.

### GABI-B11 — jpackage app-image present but no checked-in jpackage config/script (reproducibility unverified)
- **Severity:** LOW / GAP
- **File:line:** `packaging/` output tree exists (`packaging/bin/GABI/app/classes/...`); no `jpackage.properties` /
  build profile found in source control (review A-4, D-list)
- **Root cause:** The packaging is evidenced only by build output, not by a checked-in jpackage configuration or
  build script/profile, so the app-image is not reproducibly buildable from source.
- **Fix approach:** Add a checked-in, reproducible jpackage configuration — a Maven `jpackage` profile / plugin
  execution (or a `jpackage.properties` + build script) targeting JDK 17, producing the app-image from the built
  jar + runtime image. Wire it so `mvn -Pjpackage ...` (or the documented command) regenerates `packaging/` output
  deterministically. Ensure the IDE/`out/` artifacts and absolute-jar references are NOT relied upon.
- **Acceptance criterion:** A documented single command builds the jpackage app-image from a clean checkout (no
  IDE, no absolute jar path); the produced launcher starts GABI in the intended mode. The jpackage config is under
  source control.
- **Asset capability needed:** add jpackage build config (checked-in, reproducible, JDK 17 app-image) — packaging.

### GABI-B12 — No standalone documentation set (README / RAG-config / agent-access / packaging / security)
- **Severity:** LOW / GAP
- **File:line:** repo root — no top-level `README.md` rewrite, RAG-config, agent-access, packaging, or security doc
  found; only inline Javadoc + YAML comments (review A-6, D-list)
- **Root cause:** The docs workstream output is not evident as standalone files; documentation lives only as inline
  Javadoc and `application*.yml` comments, so operators have no entry-point doc for run modes, RAG configuration,
  the MCP/REST agent surface, packaging, or security posture.
- **Fix approach:** Produce a standalone docs set: a rewritten top-level `README.md` (build/run, profiles
  cli/server, env-var credentials), a RAG-configuration doc (model profiles Ollama/OpenAI/Anthropic, NoOp fallback
  behavior), an agent-access doc (the 14 MCP `@Tool` ops + REST surface, read-only posture), a packaging doc
  (jpackage from B11), and a short security doc (env-var creds, hardened sinks from B01/B02/B07, read-only exposed
  surface). Keep credentials out of all examples (see B08).
- **Acceptance criterion:** The named docs exist at the repo root/`docs/`, each builds/runs as written from a clean
  checkout, and a reviewer can configure RAG and launch server mode using only the docs. No literal credentials in
  any example.
- **Asset capability needed:** generate documentation set (README + RAG/agent-access/packaging/security docs) — docs.

---

## SECTION 2 — IMPROVEMENTS & FEATURES (ordered by value/effort)

Value/effort legend: **Value** {High|Med|Low} = catalogue/RAG impact for a small Spring AI app;
**Effort** {Low|Med|High} = implementation+wiring cost on this codebase. Ordered best value/effort first.

### GABI-I01 — Source-grounded, citation-bearing RAG answers
- **Value: High · Effort: Low** (the single highest-leverage RAG item; largely advisor/prompt configuration)
- **What it adds:** Constrains generation to retrieved catalogue rows and attaches citations (which book/record
  each claim came from) to every answer, returned via the existing `AnswerWithSources` / `core/AnswerWithSources.java`
  type.
- **Why:** Grounding + citation is the primary hallucination-reduction lever for RAG over a database; it makes
  GABI's `ask` answers trustworthy and traceable to specific catalogue records.
- **Reference:** RAG grounding best practice — https://pmc.ncbi.nlm.nih.gov/articles/PMC12540348/ (research item 10).
- **Modules/files touched:** `rag/RagServiceImpl.java`, `core/AnswerWithSources.java`, the `QuestionAnswerAdvisor`
  configuration in `rag/RagConfig.java`; `access/mcp/LibraryMcpTools.ask` + `access/rest/LibraryRestController` ask route.
- **Rough approach:** Tighten the system/grounding prompt so the model answers ONLY from retrieved rows and refuses
  when retrieval is empty; capture the retrieved `Document` metadata (book/record id) and populate
  `AnswerWithSources` with per-claim citations; surface citations in both the MCP and REST `ask` responses.
- **Acceptance criteria:** For a question answerable from the catalogue, the response includes citations naming the
  source record(s); for an unanswerable question, the model declines rather than fabricating. A test asserts the
  citation list is non-empty and references real retrieved rows.
- **Asset capability needed:** edit headless core/RAG service (grounded citation answers) — feature-enhancer; surface citations on @Tool/REST ask route — access-layer-builder; add RAG JUnit tests — testing.

### GABI-I02 — Faceted / fielded catalogue search (author, subject, format, ISBN, call number)
- **Value: High · Effort: Med** (also the keyword backbone for hybrid retrieval — see I06)
- **What it adds:** Faceted refinement and advanced fielded search over the catalogue (author, subject, format,
  ISBN, call number), beyond today's single `LIKE %seed%` substring match.
- **Why:** ILS table stakes; doubles as the keyword half of hybrid RAG retrieval (I06) for free, and improves exact
  lookups (IDs/ISBNs/titles) the vector store handles poorly.
- **Reference:** Koha OPAC faceted/advanced search — https://ebooks.inflibnet.ac.in/lisp5/chapter/koha-open-source-integrated-library-software/ (research item 5).
- **Modules/files touched:** `core/LibraryService(Impl)` (new `searchBooks(facets/fields)` overload), `sql/reservoirs/LibDBBook.java`
  (parameterized fielded queries — PreparedStatement), `access/mcp/LibraryMcpTools` + `access/rest/LibraryRestController` search routes.
- **Rough approach:** Add a typed search-criteria object (field → value, plus facet requests); build parameterized
  queries (no identifier injection — reuse IdentifierValidator for any column/field selection); return facet counts
  alongside results; expose as a read-only MCP `@Tool` + REST route.
- **Acceptance criteria:** A fielded search by ISBN / author returns exactly matching records; a faceted search
  returns facet buckets with counts; all field/column selection is validated (no raw identifier interpolation).
- **Asset capability needed:** edit headless core service (faceted/fielded search) — feature-enhancer; add @Tool MCP bean + REST route — access-layer-builder; add JUnit tests — testing.

### GABI-I03 — ISBN lookup + metadata enrichment on cataloguing
- **Value: High · Effort: Med**
- **What it adds:** On adding a book by ISBN, auto-fill title, authors, publisher, publish date, page count,
  subjects, and cover image from free APIs (Open Library / Google Books, no key); validate/clean the ISBN first.
- **Why:** Slashes manual data entry, raises catalogue data quality, and enriches the rows the RAG assistant
  retrieves over.
- **Reference:** Open Library / Google Books / isbnlib — https://pypi.org/project/isbnlib/ ;
  https://apileague.com/articles/book-apis/ (research item 1; honorable mention: multi-source aggregation).
- **Modules/files touched:** `core/LibraryService(Impl)` (`addBookByIsbn`), a new `core/enrichment/` client over
  Open Library/Google Books (Spring `RestClient`/`WebClient`), `tables/Book.java` (new metadata fields if extended),
  `sql/reservoirs/LibDBBook.java` schema/insert, MCP `@Tool` + REST route.
- **Rough approach:** Validate/normalize ISBN; query Open Library first, fall back to Google Books, degrade
  gracefully if both miss; map response to `Book` (extend schema for new fields); expose `addBookByIsbn` —
  **note this is a WRITE op, so respect the read-only exposed-surface posture** (keep it CLI/admin-side unless the
  campaign decides to expose curated writes). Pin the HTTP client deps via the dependency-audit tool.
- **Acceptance criteria:** Adding a known ISBN populates title/author/publisher/date from the API; an unknown/invalid
  ISBN is rejected or degrades to manual entry without failing the call; external calls are timeout-bounded and
  covered by a mocked test.
- **Asset capability needed:** edit headless core service + add enrichment HTTP client (+ dependency-audit for new deps) — feature-enhancer; optionally add @Tool/REST route honoring read-only posture — access-layer-builder; add mocked JUnit tests — testing.

### GABI-I04 — Role-based access control (staff / admin / member)
- **Value: High · Effort: Med**
- **What it adds:** Distinguishes staff/admin/member roles and gates operations by role (today admin is gated only
  by `currentUser == database-name` at `UserMenu`/`LibMenu.java:262`).
- **Why:** ILS table stakes and a security/correctness improvement; prerequisite for safely exposing any write or
  reporting surface and for circulation reporting (I05). Directly relevant to the privileged user-admin path the
  review flagged (B01/B02).
- **Reference:** FOLIO / Koha role segmentation — https://libraryguides.missouri.edu/folio/reporting ;
  https://ebooks.inflibnet.ac.in/lisp5/chapter/koha-open-source-integrated-library-software/ (research item 7).
  Note: research limitation — FOLIO RBAC specifics were not fully confirmed; treat the role model as a design
  recommendation and re-verify if implemented verbatim.
- **Modules/files touched:** `core/LibraryService(Impl)` (role checks on privileged ops), `tables/User.java` (role
  field), `sql/users/UserDerby.java` + schema (role storage), `manager/UserMenu.java` (replace the ad-hoc admin
  gate), access layer (role-aware exposure decisions).
- **Rough approach:** Introduce a `Role` enum and persist it on `User`; centralize an authorization check in the
  core (not in menus); map Derby grants/roles to app roles; keep all user-admin OFF the exposed MCP/REST surface
  (preserve the review's D-3/D-4 PASS).
- **Acceptance criteria:** A non-admin user is denied user-admin / privileged operations at the core boundary
  (tested); admin retains access; the exposed MCP/REST surface still excludes all user-admin (regression test of
  the read-only posture).
- **Asset capability needed:** edit headless core service (RBAC model + authorization checks) — feature-enhancer; add JUnit authorization tests — testing.

### GABI-I05 — Circulation reporting / analytics
- **Value: Med · Effort: Med**
- **What it adds:** Read-only circulation reports/analytics — checkouts, returns, renewals, overdues, holds ratios,
  active loans per member — over the Loans/Books/Members schema.
- **Why:** ILS table stakes; pure read aggregation that fits the existing read-only exposed surface and gives the
  RAG assistant structured numbers to answer operational questions.
- **Reference:** FOLIO reporting / Koha circulation reports — https://libraryguides.missouri.edu/folio/reporting ;
  https://ebooks.inflibnet.ac.in/lisp5/chapter/koha-open-source-integrated-library-software/ (research item 7;
  honorable mention: member-record-centric circulation view).
- **Modules/files touched:** `core/LibraryService(Impl)` (report queries), `sql/reservoirs/LibDBLoan.java`
  (aggregation queries), MCP `@Tool` + REST report routes.
- **Rough approach:** Add parameterized aggregation queries (counts by date range, member, status); return typed
  report DTOs; expose as read-only MCP `@Tool` + REST routes (fits current posture, no write exposure needed).
- **Acceptance criteria:** A report endpoint returns correct counts against a seeded in-memory Derby dataset (e.g.
  N active loans, M overdues); queries are parameterized; the routes are read-only.
- **Asset capability needed:** edit headless core service (report aggregations) — feature-enhancer; add @Tool MCP bean + REST route — access-layer-builder; add JUnit tests — testing.

### GABI-I06 — Hybrid retrieval: keyword (BM25) + semantic (vector) with α-weighting
- **Value: High · Effort: High**
- **What it adds:** Combines sparse keyword and dense semantic retrieval with a tunable α (~0.3 keyword-leaning for
  IDs/ISBNs/exact titles, ~0.7 semantic-leaning for "recommend me / how do I" questions), instead of vector-only
  retrieval.
- **Why:** Catches what either retriever misses alone; the keyword side reuses the fielded search backbone from I02,
  so the ILS and RAG roadmaps reinforce each other. Highest-impact RAG retrieval upgrade.
- **Reference:** RAG hybrid-search best practice — https://www.meilisearch.com/blog/hybrid-search-rag ;
  https://redis.io/blog/hybrid-search-benefits-rag-systems/ (research item 8). Limitation: α-range is directional
  vendor guidance — tune empirically on the catalogue.
- **Modules/files touched:** `rag/RagServiceImpl.java`, `rag/RagConfig.java` (retriever composition), the keyword
  index (reuse I02 fielded search over `LibDBBook`), `core/AnswerWithSources.java`.
- **Rough approach:** Run vector retrieval (existing `SimpleVectorStore`) AND keyword retrieval (I02 fielded/`LIKE`
  search), merge/normalize scores with a configurable α (`@ConfigurationProperties`), de-duplicate by record id,
  feed the merged top-k into the advisor. Keep it behind the model-present condition from B04. Confirm whether
  Spring AI 2.0 offers a native hybrid retriever before hand-rolling — flag for The Researcher.
- **Acceptance criteria:** A query with an exact ISBN/ID returns the exact record (keyword wins); a conceptual query
  returns semantically relevant records (vector wins); α is configurable and a test asserts both retrieval paths
  contribute to the merged result set.
- **Asset capability needed:** edit headless core/RAG service (hybrid retriever + α config) — feature-enhancer; RESEARCH REQUEST (Spring AI 2.0 native hybrid-retriever support vs hand-rolled, JDK 17) — the-researcher; add retrieval JUnit tests — testing.

### GABI-I07 — Incremental reindex on catalogue change (+ ANN-accelerated retrieval)
- **Value: Med · Effort: Med**
- **What it adds:** Keeps the vector/keyword index fresh as Books/Loans change via incremental updates rather than
  full rebuilds, with approximate-nearest-neighbour retrieval and caching of frequent queries for low latency.
- **Why:** Without it the RAG index goes stale after every catalogue mutation; the existing `reindex` op
  (`LibraryMcpTools.reindex`) is presumably a full rebuild. Incremental keeps answers current cheaply.
- **Reference:** RAG indexing best practice — https://www.meilisearch.com/blog/hybrid-search-rag (research item 11).
  Limitation: incremental-vs-full specifics were thin in research — treat as a design recommendation.
- **Modules/files touched:** `rag/RagServiceImpl.java` (incremental upsert/delete on `SimpleVectorStore`), the
  write paths in `core/LibraryServiceImpl` (add/delete book/member/loan → trigger index delta), the `reindex` tool.
- **Rough approach:** On each catalogue write, upsert/delete the affected document in the vector store (and keyword
  index) instead of rebuilding; keep `reindex` as the full-rebuild fallback; add a small query cache for hot
  questions. Note `SimpleVectorStore` is in-memory — confirm its upsert/delete semantics; flag a persistent store
  as a follow-up if needed.
- **Acceptance criteria:** Adding/deleting a book updates retrieval results without a full `reindex`; a test asserts
  a newly added book is retrievable after the incremental update and a deleted one is not.
- **Asset capability needed:** edit headless core/RAG service (incremental upsert/delete on write paths) — feature-enhancer; add reindex JUnit tests — testing.

### GABI-I08 — Holds / reservations with a holds queue
- **Value: Med · Effort: High**
- **What it adds:** Lets members place holds (title/item level) with automatic capture and a holds queue; surfaces
  "holds to pull" and "holds waiting pickup."
- **Why:** Core ILS circulation feature; meaningfully extends the Loans model toward a real library workflow.
- **Reference:** Koha holds — https://ebooks.inflibnet.ac.in/lisp5/chapter/koha-open-source-integrated-library-software/ (research item 2).
- **Modules/files touched:** new `tables/Hold.java` + schema table, `core/LibraryService(Impl)` (place/cancel/
  capture hold, queue ordering), `sql/reservoirs/` new Hold DAO, `manager/` CLI menu, MCP/REST routes (write ops —
  respect read-only exposed-surface posture; likely keep writes CLI/admin-side).
- **Rough approach:** Add a `holds` table (member, book/item, placed-date, status, queue position); implement
  FIFO queue + capture-on-return logic in the core; reporting views for pull/waiting lists; expose read views
  (queue/pull lists) on the read-only surface, keep write (place/cancel) appropriately gated.
- **Acceptance criteria:** Placing a hold enqueues it FIFO; returning a held book captures the next hold; pull/
  waiting lists reflect correct state in a seeded test.
- **Asset capability needed:** edit headless core service (holds model + queue logic) — feature-enhancer; add read-view @Tool MCP bean + REST route — access-layer-builder; add JUnit tests — testing.

### GABI-I09 — Automatic overdue/fine calculation (+ write-off)
- **Value: Med · Effort: Med**
- **What it adds:** Computes overdue fines automatically from loan due dates, supports payment recording and
  write-offs against a member's account.
- **Why:** ILS table stakes; the Loan model already carries `dateLoan`, so a due-date + fine rule is a natural,
  mostly self-contained addition.
- **Reference:** Koha fines — https://ebooks.inflibnet.ac.in/lisp5/chapter/koha-open-source-integrated-library-software/ (research item 3).
- **Modules/files touched:** `tables/Loan.java` (due date), `core/LibraryService(Impl)` (fine calc, payment,
  write-off), `sql/reservoirs/LibDBLoan.java` + a fines/account table, read-only report routes.
- **Rough approach:** Add a configurable loan period + daily fine rate (`@ConfigurationProperties`); compute fines
  from `dateLoan` + period vs return/current date; persist account ledger entries; expose read-only fine/account
  views, keep payment/write-off as gated writes.
- **Acceptance criteria:** A loan past its due date accrues the correct fine for a given date; a recorded payment /
  write-off zeroes or reduces the balance; computation is covered against a seeded dataset.
- **Asset capability needed:** edit headless core service (fine/account model + calculation) — feature-enhancer; add read-only @Tool MCP bean + REST route — access-layer-builder; add JUnit tests — testing.

### GABI-I10 — Cross-encoder reranking of retrieved rows
- **Value: Med · Effort: High**
- **What it adds:** Re-scores query+passage jointly after first-stage retrieval to fix close-but-wrong hits
  (~100–200 ms latency for higher precision@k and fewer wrong citations).
- **Why:** Directly improves citation correctness (pairs with I01); a measurable precision lever once hybrid
  retrieval (I06) is in place.
- **Reference:** RAG reranking best practice — https://blog.thegenairevolution.com/article/cross-encoder-reranking-the-low-cost-fix-for-rag-misses (research item 9).
- **Modules/files touched:** `rag/RagServiceImpl.java`, `rag/RagConfig.java` (rerank stage between retrieval and
  advisor), possibly a new reranker model dependency.
- **Rough approach:** After first-stage (vector/hybrid) retrieval, run a cross-encoder rerank over the top-N
  candidates, keep top-k for the advisor; make the reranker optional/conditional (model-present, like B04).
  Confirm whether Spring AI 2.0 exposes a reranking abstraction or whether an external model is needed —
  Researcher item.
- **Acceptance criteria:** For a query with a known best record, reranking promotes that record above a
  lexically-close distractor; the rerank stage is behind a config toggle and adds bounded latency.
- **Asset capability needed:** edit headless core/RAG service (rerank stage) — feature-enhancer; RESEARCH REQUEST (Spring AI 2.0 reranking support / model options on JDK 17) — the-researcher; add rerank JUnit tests — testing.

### GABI-I11 — Automated due-date / overdue notices (email or SMS)
- **Value: Med · Effort: Med**
- **What it adds:** Sends configurable due-soon and overdue notices to members automatically via a scheduled job,
  with a REST/MCP trigger.
- **Why:** ILS table stakes; builds directly on the due-date/fine model (I09) and the member contact data.
- **Reference:** Koha notices — https://ebooks.inflibnet.ac.in/lisp5/chapter/koha-open-source-integrated-library-software/ (research item 4).
- **Modules/files touched:** `tables/Member.java` (contact field), `core/LibraryService(Impl)` (notice generation),
  a new scheduled component (`@Scheduled`), an email/SMS sender abstraction, a read-only "notices due" MCP/REST view.
- **Rough approach:** Add member contact info; a `@Scheduled` job (or on-demand trigger) computes due-soon/overdue
  loans and dispatches notices through a pluggable sender (log-only sender as the default/no-config fallback);
  expose a read-only "pending notices" view + a trigger op. Pin any mail dependency via dependency-audit.
- **Acceptance criteria:** The job selects exactly the due-soon/overdue loans for a seeded dataset and routes them
  to the (mocked) sender; with no sender configured it logs without failing; a test asserts correct selection.
- **Asset capability needed:** edit headless core service + scheduled notice job (+ dependency-audit for mail dep) — feature-enhancer; add trigger/view @Tool MCP bean + REST route — access-layer-builder; add JUnit tests — testing.

### GABI-I12 — MARC import/export (bibliographic interchange)
- **Value: Low · Effort: High**
- **What it adds:** Imports bib records from external databases and exports MARC for interchange, so the catalogue
  isn't a data silo.
- **Why:** Real-ILS interoperability; lowest value/effort for a small training app (heavy format work, niche
  benefit) — schedule last.
- **Reference:** Koha (MARC-compliant) — https://ebooks.inflibnet.ac.in/lisp5/chapter/koha-open-source-integrated-library-software/ (research item 6).
- **Modules/files touched:** new `core/marc/` import/export module + a MARC4J-style dependency, `core/LibraryService(Impl)`
  (bulk import/export), `sql/reservoirs/LibDBBook.java` (bulk insert), CLI/REST import-export routes.
- **Rough approach:** Add a MARC parsing/serialization library; map MARC fields ↔ `Book` (+ enriched metadata from
  I03); implement bulk import (validate, dedupe) and export; keep import as a gated write op. Pin the MARC dep via
  dependency-audit. Re-verify exact MARC behavior against the Koha manual if implemented verbatim (research limitation).
- **Acceptance criteria:** A sample MARC file imports to correct `Book` records; export produces a valid MARC file
  re-importable round-trip; import validation rejects malformed records. Covered by a fixture-based test.
- **Asset capability needed:** edit headless core service + MARC import/export module (+ dependency-audit for MARC dep) — feature-enhancer; add gated import/export REST route — access-layer-builder; add JUnit tests — testing.

### GABI-I13 — Multi-evidence answer refinement / discrepancy check
- **Value: Low · Effort: High**
- **What it adds:** Retrieves multiple supporting rows and runs a refinement/verification pass that flags
  contradictions before answering.
- **Why:** Further hallucination reduction (MEGA-RAG reports >40% with multi-evidence + reranker + discrepancy
  refinement), but it stacks on I01/I06/I10 and is the most speculative for a small app — schedule after the core
  RAG stack lands.
- **Reference:** MEGA-RAG (peer-reviewed) — https://pmc.ncbi.nlm.nih.gov/articles/PMC12540348/ (research item 12).
  Limitation: figures are directional / public-health domain — validate empirically.
- **Modules/files touched:** `rag/RagServiceImpl.java`, `rag/RagConfig.java` (multi-evidence retrieval + refinement
  prompt/pass), `core/AnswerWithSources.java` (discrepancy flag).
- **Rough approach:** Retrieve a wider candidate set, run a verification/refinement LLM pass that cross-checks
  evidence rows and flags contradictions; only then answer (with citations from I01). Gate behind model presence
  (B04) and a config toggle; mind the extra latency/cost on a small app.
- **Acceptance criteria:** Given intentionally contradictory seeded rows, the answer flags the discrepancy (or
  declines) rather than asserting one side confidently; the refinement pass is config-toggleable.
- **Asset capability needed:** edit headless core/RAG service (multi-evidence refinement pass) — feature-enhancer; add refinement JUnit tests — testing.

---

## Sequencing & dependency notes (for the orchestrator)

- **Bugs before features.** GABI-B01…B05 (CRITICAL/HIGH) gate a working artifact: B03 (YAML) and B04 (RAG wiring)
  must land before any RAG feature (I01/I06/I07/I10/I13) can be exercised; B01/B02/B07 close the live injection
  surface; B05 makes the coverage gate real so all later `testing` work is actually enforced.
- **Feature dependencies:** I02 (fielded search) is a prerequisite for I06 (hybrid retrieval). I09 (due dates/fines)
  precedes I11 (notices). I01 (grounded citations) precedes I10/I13 (rerank/refinement). I04 (RBAC) should precede
  exposing any new write surface (I03/I08/I09/I11/I12).
- **Embedded requests for routing:**
  - RESEARCH REQUEST (the-researcher): Spring AI 2.0 native hybrid-retriever support (I06); Spring AI 2.0 reranking
    abstraction / model options on JDK 17 (I10). Re-validate against JDK 17 baseline.
  - GATHERING REQUEST (the-gleaner): none required at planning time — each fix/feature item names ≤4 concrete files;
    if an executing agent must read ≥5 files for a given item, dispatch the-gleaner per the threshold (5).
  - ASSET REQUEST (the-metaprompter): none — all capabilities map to existing manifest agents (feature-enhancer,
    access-layer-builder, packaging, testing, docs). The in-repo agent asset (`.claude/agents/gabi-operator.md`)
    already exists and PASSED review; refresh it only if the exposed surface changes (e.g. I05 report routes).
- **Read-only exposed-surface invariant:** review section C confirms user-admin and all writes are correctly OFF
  the MCP/REST surface (D-3/D-4 PASS). Any feature with write semantics (I03/I08/I09/I11/I12) must preserve this —
  keep writes CLI/admin-gated or behind RBAC (I04), and regression-test the read-only posture.
- **Security hygiene:** keep the plaintext/dev-default credentials (B08) out of all generated assets, test
  fixtures, checkpoints, and memory records (per understanding-doc checkpoint rule).

---

## Distinct asset-capability tags (summary)
1. harden legacy/core SQL sink (IdentifierValidator + password escaping) — feature-enhancer (B01, B02, B07)
2. fix Spring config/profiles (YAML merge, RAG conditionals, profile gating, creds) — feature-enhancer (B03, B04, B06, B08)
3. fix JaCoCo rule in pom (BUNDLE→PACKAGE/CLASS) — testing (B05)
4. edit headless core service (dead-code cleanup, ILS + RAG features) — feature-enhancer (B10, I01–I13)
5. add @Tool MCP bean + REST route (search/report/holds/fines/notices/exceptions) — access-layer-builder (B09, I02, I05, I08, I09, I11, I12)
6. add Jpackage build config — packaging (B11)
7. generate documentation set — docs (B12, B08 docs)
8. add JUnit tests — testing (B01–B07, B09, I01–I13)
9. dependency-audit for new deps (HTTP/mail/MARC) — feature-enhancer (I03, I11, I12)
10. RESEARCH REQUEST (Spring AI 2.0 hybrid/rerank vs JDK 17) — the-researcher (I06, I10)
