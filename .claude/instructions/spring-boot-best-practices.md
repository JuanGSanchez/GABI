# Instruction: Spring Boot / Spring AI Best Practices (GABI)

## Principles Applied
Inherited: P1 (source grounding — every directive references a source from RR-1; no API name or version number invented beyond RR-1's scope; citations carry traceable URLs), P2 (determinism — named build idioms, DI patterns, and test wiring; binary choices between patterns where applicable; no ambiguous "it depends" phrasing), P3 (systematicity — decision points enumerated in conditional_rules; each pattern has a named choice condition), P4 (consistency — same idioms across every gabi-core-dev/access-dev/rag-dev/test-author/packaging-builder session; one best-practices instruction for this framework stack), P6 (self-contained — all directives, source citations, and API-currency caveats stated here; RR-1 path and sources are explicit), P7 (reference hygiene — all [S*] cites resolve to RR-1 §Sources; hook names resolve to CLAUDE.md §Hooks; no filler), P8 (this block is the P8 expression for this asset), P9 Role Separation (this instruction governs the Spring Boot 4 / Spring AI MCP / Spring Data JPA(Derby) / Spring Test framework layer; Java language conventions and security invariants remain in java-spring-conventions.md; this instruction does not duplicate Rules 1–9), P10 Exit-Status Determinism (n/a — this instruction does not mandate an exit-status output format; agents operating under it return EXIT STATUS per CLAUDE.md Operating contract), P11 Programmatic Determinism (directives direct agents toward annotation-driven wiring and explicit JPA patterns rather than ad-hoc imperative configuration; security hooks enforce the injection/credential/surface boundaries mechanically; R18/P11 canonical definition: `repo-enhancer/orchestrator.md` CONVENTIONS, do not restate), P12 Maximal-Effort Completeness (all technical areas from RR-1 are covered: version pins, layered architecture + DI, JPA/Derby/HikariCP, Spring AI MCP server, Spring AI RAG, Maven build + fat-jar, jpackage, JUnit 5/Spring Test/JaCoCo), P13 Token Economy (cite [S*] IDs from RR-1 rather than restating source text; terse directives). Engineering Disciplines (R17): canonical definition at `repo-enhancer/orchestrator.md` CONVENTIONS; prompt layer = grouped numbered directives with positive/negative examples; context layer = load this instruction only when the relevant agent touches its governed layer (just-in-time); harness layer = `sql_identifier_sink_guard.py`, `no_secrets_guard.py`, `privileged_admin_surface_guard.py` enforce security boundaries at the tool-use level; coverage_gate_reminder.py enforces the JaCoCo gate.

Custom:
- C1 — Research Grounding: every directive in this instruction references at least one source from RR-1 (`docs/research-java-spring-best-practices.md`, compiled 2026-06-26); no Spring AI, Boot, Derby, JaCoCo, or jpackage API, version number, or behavioral claim may be stated without a [S*] citation that resolves to RR-1 §Sources.

Scope: applies to gabi-core-dev (`src/main/java/core/` — LibraryService/LibraryServiceImpl + JPA/Derby), access-dev (`src/main/java/access/` — LibraryMcpTools, LibraryRestController, Spring AI MCP), rag-dev (`src/main/java/rag/` — RagService/RagConfig/RagServiceImpl), test-author (`src/test/` — Spring Test + JaCoCo), and packaging-builder (`packaging/` — Maven fat-jar + jpackage) on Spring Boot 4.0.6 (current `pom.xml`) / recommended upgrade target 4.1.0 [S9], Spring AI 2.0.0, Derby 10.16.1.1, JDK 17 [S1][S2]. Does not govern `.claude/` assets — governed by java-spring-conventions.md and sdd-constitution.md. Java language and security invariant authority remains in java-spring-conventions.md; where a framework idiom conflicts with a security invariant, the invariant wins (java-spring-conventions.md C-SEC).

<instructions>
  <context>
    GABI's pom.xml is ALREADY PRESENT at the repo root (Spring Boot 4.0.6,
    Spring AI BOM 2.0.0, Derby 10.16.1.1). [S2][S9]

    The current boot version in pom.xml is 4.0.6 (last 4.0.x patch, supported
    through 2026-12-31) [S3][S9]. The recommended upgrade target is
    spring-boot-starter-parent 4.1.0 (GA 2026-06-10; longer-lived 4.1 line;
    Java 17–26) [S2][S9]. Spring AI 2.0 is built on the Boot 4.0 dependency
    model; upgrade to 4.1.x only after confirming Spring AI 2.0.x compatibility
    (see Limitations / C1 caveat below). [S5][S6][S7]

    Version matrix (RR-1):
      spring-boot-starter-parent  4.1.0 (target) / 4.0.6 (current / fallback)
      <java.version>              17  (minimum and current baseline)  [S1][S2]
      spring-ai-bom               2.0.x (pin via dependencyManagement)  [S6]
      Apache Derby                10.16.1.1 (Java 17; 10.17 needs Java 21)  [S10][S11]
      jacoco-maven-plugin         0.8.16  [S16]

    Limitation (C1): Spring AI 2.0 exact GA patch and its Boot 4.1.0
    compatibility were not pinned from the official release index at research
    time. Pin via spring-ai-bom and verify the latest 2.0.x at build time.
    Fallback: pin Boot 4.0.6 if a Spring AI 2.0 build explicitly constrains to
    the 4.0.x line. [S5][S6][S7]

    Derby is RETIRED (read-only since 2025-10-10; no further releases). Pin
    10.16.1.1 for JDK 17. Consider migrating to a maintained embedded engine
    (H2, or PostgreSQL for server use) in a future iteration. [S10][S11]

    Source for all technical claims: docs/research-java-spring-best-practices.md
    (RR-1, compiled 2026-06-26). [S*] citations below resolve to RR-1 §Sources.
  </context>

  <rules>
    <!-- Layered architecture + dependency injection -->

    1. Enforce the three-layer architecture: (a) core (`LibraryService`
       interface + `LibraryServiceImpl`) owns all domain logic; (b) access
       layer (`LibraryMcpTools`, `LibraryRestController`) delegates every
       operation to `LibraryService` — it contains no business logic; (c) RAG
       layer (`RagService`/`RagServiceImpl`) is injected into the core where
       AI retrieval is needed. Under no circumstances duplicate domain logic
       in the access or RAG layers. [S2]

    2. Use constructor injection for every Spring-managed bean. Never use
       field `@Autowired`. Declare all required collaborators as `final`
       fields, assigned in the constructor. This is the enforced idiom in
       java-spring-conventions.md Rule 8 and the pattern Spring itself
       recommends. [S2]

    3. For optional/conditional dependencies (e.g., `EmbeddingModel` may not
       be present), use `@ConditionalOnBean` / `@ConditionalOnMissingBean` on
       the `@Configuration` class or `@Bean` method — not `@Autowired(required
       = false)`. This preserves type safety and startup-failure visibility.
       [S2] (See java-spring-conventions.md Rule 6 for the `RagService`
       fallback pattern.)

    <!-- Spring Data JPA over Derby + HikariCP -->

    4. Define a single `DataSource` bean and let Spring Boot auto-configure
       HikariCP (the default pool bundled with `spring-boot-starter-data-jpa`).
       Do NOT open a raw `DriverManager.getConnection()` per call — replace
       any connection-per-call pattern with a managed pool. [S2][S10]
       Minimal `application.yml` for embedded Derby:
         spring:
           datasource:
             url: jdbc:derby:gabidb;create=true
             driver-class-name: org.apache.derby.jdbc.EmbeddedDriver
             hikari:
               maximum-pool-size: 5
           jpa:
             database-platform: org.hibernate.community.dialect.DerbyTenSevenDialect
             hibernate:
               ddl-auto: update
       Keep exactly one top-level `spring:` key (java-spring-conventions.md
       Rule 5 / GABI-B03). [S2][S10]

    5. Pin Derby 10.16.1.1 in `pom.xml`. Do NOT use Derby 10.17.1.0 — it
       requires Java 21 and does not run on JDK 17 (GABI's pinned baseline).
       [S10][S11]

    6. The Derby Hibernate dialect for Boot 4 / Hibernate ORM bundled by 4.x
       is `org.hibernate.community.dialect.DerbyTenSevenDialect` (from
       `hibernate-community-dialects`). Confirm the exact dialect class against
       the Hibernate ORM version shipped by the chosen Boot release before
       committing. [S2][S10]

    <!-- Spring AI MCP server exposure -->

    7. Use `spring-ai-starter-mcp-server-webmvc` (HTTP Streamable-HTTP / SSE,
       servlet stack) as the MCP server starter for GABI unless reactive
       WebFlux is explicitly required. [S6]

    8. The canonical annotation idiom for MCP tool exposure in Spring AI 2.0
       is `@McpTool` + `@McpToolParam` from
       `org.springframework.ai.mcp.annotation`. `@McpTool` auto-generates the
       JSON schema; `@Component` beans are auto-scanned and registered. [S6]
       Example (see also the `add-mcp-tool` skill):
         @Component
         public class LibraryMcpTools {
             @McpTool(name = "searchBooks",
                      description = "Search the library catalogue by query")
             public List<Book> searchBooks(
                 @McpToolParam(description = "Search query", required = true)
                 String query) {
                 return libraryService.search(query);
             }
         }
       The existing repo currently uses `@Tool`/`@ToolParam` from
       `org.springframework.ai.tool.annotation` via `MethodToolCallbackProvider`
       (java-spring-conventions.md Rule 8 ground-truth). New tools added by
       the `add-mcp-tool` skill use `@McpTool`/`@McpToolParam`. Confirm which
       annotation path is in the current `pom.xml` spring-ai-starter import
       before adding new tools. [S6]

    9. The exposed surface stays read-only + RAG (`ask`/`reindex`) + `health`.
       Privileged user-admin operations must never appear as `@Tool`/`@McpTool`
       or REST routes (java-spring-conventions.md Rule 3 /
       `privileged_admin_surface_guard.py`). [S6]

    <!-- Spring AI RAG building blocks -->

    10. The GABI RAG stack for an offline/local setup: `OllamaEmbeddingModel`
        + `SimpleVectorStore` (in-JVM, no external service, auto-uses the
        injected `EmbeddingModel`). For hosted or persistent setups: swap to
        `OpenAiEmbeddingModel` and `PGVector` / `Chroma` / `Redis` — all
        JDK-17-compatible. [S8]

    11. Wire the retrieval advisor as follows:
          @Bean
          @ConditionalOnBean(EmbeddingModel.class)
          public ChatClient chatClientWithRag(
                  ChatClient.Builder builder,
                  VectorStore vectorStore) {
              return builder
                  .defaultAdvisors(
                      QuestionAnswerAdvisor.builder(vectorStore).build())
                  .build();
          }
        Provide a `@ConditionalOnMissingBean(RagService.class)` `NoOpRagService`
        as the fallback (java-spring-conventions.md Rule 6). [S8]

    12. Use `SimpleVectorStore.builder(embeddingModel).build()` (current repo
        API per java-spring-conventions.md Rule 8). Do not use deprecated
        constructors. Confirm from existing `RagConfig.java` usage before
        invoking any Spring AI VectorStore or Advisor API. [S8]

    <!-- Maven build + fat-jar (pom.xml is present) -->

    13. The `spring-boot-maven-plugin` repackages the jar into a self-executing
        fat jar with `spring-boot:repackage` (or bound to `package` phase).
        This is the prerequisite for jpackage. Declare it under `<build>` in
        `pom.xml`. [S2][S4]

    14. Pin Spring AI and the MCP SDK via the Spring AI BOM under
        `<dependencyManagement>`. Specify the BOM version explicitly so the
        build is reproducible:
          <dependencyManagement>
            <dependencies>
              <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>2.0.x</version>   <!-- verify latest 2.0.x at build -->
                <type>pom</type>
                <scope>import</scope>
              </dependency>
            </dependencies>
          </dependencyManagement>
        Do not pin individual Spring AI artifact versions outside the BOM.
        [S6][S9]

    <!-- jpackage for a Spring Boot 4 native installer -->

    15. Point `--main-jar` at the `spring-boot-maven-plugin`-repackaged fat
        jar. The launcher main-class for Spring Boot 3.2+ / 4.x is
        `org.springframework.boot.loader.launch.JarLauncher` (not the older
        `org.springframework.boot.loader.JarLauncher`). Verify this against
        the produced jar's `MANIFEST.MF` `Main-Class` entry before committing
        the jpackage script. [S12][S13]

    16. jpackage is NOT cross-platform: build Windows `exe`/`msi` on Windows,
        `dmg`/`pkg` on macOS, `deb`/`rpm` on Linux. Key flags:
          jpackage --type exe --input target --name GABI \
            --main-jar gabi-<version>.jar \
            --main-class org.springframework.boot.loader.launch.JarLauncher \
            --app-version <version> --icon gabi.ico \
            --win-menu --win-shortcut
        Optionally add `--runtime-image` from a `jlink`-trimmed JDK 17 runtime
        so end users need no installed JRE. [S12][S13]

    <!-- JUnit 5 + Spring Test + JaCoCo -->

    17. `spring-boot-starter-test` (scope `test`) aggregates JUnit Jupiter,
        Spring Test, Spring Boot Test, AssertJ, Mockito, JSONassert. Declare
        no individual JUnit/Mockito versions — the Boot BOM manages them.
        [S14][S15]

    18. Use `@SpringBootTest` to load the full application context for
        integration tests (wiring/config correctness). For controller-slice
        tests use `@WebMvcTest` + `@AutoConfigureMockMvc`. Use `MockMvc` for
        HTTP-layer assertions. [S14][S15]

    19. DB-touching tests use `core/InMemoryDerbyConfig` (`@Primary` test
        DataSource, `jdbc:derby:memory:gabiTest;create=true`) +
        `core/TestSchemaHelper`. Seed deterministic fixtures; never connect to
        a live Derby server or a real embedding model in a test.
        (java-spring-conventions.md Rule 9.) [S14][S15]

    20. Wire JaCoCo via `jacoco-maven-plugin` 0.8.16. The `check` goal (bound
        to `verify`) must use element `PACKAGE` or `CLASS` (not `BUNDLE` with
        `include core/*`, which silently never fails — GABI-B05) with ratio
        `LINE`/`INSTRUCTION` ≥ 0.90 on the core package. Do NOT set
        `forkCount=0` or `forkMode=never` in the Surefire plugin — that drops
        the JaCoCo javaagent and produces zero coverage. [S16]
  </rules>

  <conditional_rules>
    - If adding a new MCP tool: use `@McpTool`/`@McpToolParam` (Rule 8); invoke
      the `add-mcp-tool` skill; confirm the annotation import path against the
      current pom.xml spring-ai-starter; ensure the new tool stays within the
      read-only + RAG + health surface (java-spring-conventions.md Rule 3).
      [S6]
    - If adding a RAG capability: wire `@ConditionalOnBean(EmbeddingModel.class)`
      on `RagServiceImpl` and its `RagConfig` beans; confirm `NoOpRagService`
      fallback remains present via `@ConditionalOnMissingBean(RagService.class)`
      (Rules 10–12; java-spring-conventions.md Rule 6). [S8]
    - If upgrading from Boot 4.0.6 to 4.1.0: update `spring-boot-starter-parent`
      version; verify Spring AI 2.0.x compatibility against the spring-ai-bom
      before committing (C1 caveat); run `mvn verify` immediately after upgrade.
      Fallback: revert to 4.0.6 if Spring AI constrains to the 4.0.x line.
      [S2][S3][S5][S9]
    - If adding a DataSource or JPA entity: use constructor injection for the
      `JpaRepository`; let HikariCP manage the pool (Rule 4); confirm the Derby
      dialect class against the Hibernate ORM version in the active Boot release
      (Rule 6). [S2][S10]
    - If building a jpackage installer: run `mvn package spring-boot:repackage`
      first; verify `MANIFEST.MF Main-Class` is
      `org.springframework.boot.loader.launch.JarLauncher`; then run jpackage
      (Rules 15–16). The build must be clean before packaging. [S12][S13]
    - If the JaCoCo gate drops below 0.90 at any point: halt, report the
      shortfall, and escalate; do not advance or merge (Rule 20;
      java-spring-conventions.md Rule 7). [S16]
  </conditional_rules>

  <examples>
    <example>
      <input>Add a Spring AI MCP tool that exposes the library search
      operation and returns a list of matching book titles.</input>
      <output>
        CORRECT — @McpTool annotation + access-layer delegation (Rules 1, 8, 9):

          @Component
          public class LibraryMcpTools {

              private final LibraryService libraryService;

              public LibraryMcpTools(LibraryService libraryService) {
                  this.libraryService = libraryService;     // constructor injection (Rule 2)
              }

              @McpTool(name = "searchBooks",
                       description = "Search library catalogue by keyword")
              public List&lt;String&gt; searchBooks(
                  @McpToolParam(description = "Search keyword", required = true)
                  String query) {
                  return libraryService.search(query)       // delegate to core (Rule 1)
                         .stream().map(Book::title).toList();
              }
          }

        INCORRECT:
          @Autowired LibraryService libraryService;         // field injection (Rule 2)
          // inline business logic in the MCP method       // logic in access layer (Rule 1)
          @Tool(name = "addUser", ...)                      // privileged op exposed (Rule 9)
      </output>
    </example>
    <example>
      <input>Configure JaCoCo coverage gate for the GABI core package
      to enforce 90% line coverage.</input>
      <output>
        CORRECT — element PACKAGE/CLASS, bound to verify (Rule 20) [S16]:

          &lt;plugin&gt;
            &lt;groupId&gt;org.jacoco&lt;/groupId&gt;
            &lt;artifactId&gt;jacoco-maven-plugin&lt;/artifactId&gt;
            &lt;version&gt;0.8.16&lt;/version&gt;
            &lt;executions&gt;
              &lt;execution&gt;&lt;id&gt;prepare-agent&lt;/id&gt;&lt;goals&gt;&lt;goal&gt;prepare-agent&lt;/goal&gt;&lt;/goals&gt;&lt;/execution&gt;
              &lt;execution&gt;&lt;id&gt;report&lt;/id&gt;&lt;phase&gt;verify&lt;/phase&gt;&lt;goals&gt;&lt;goal&gt;report&lt;/goal&gt;&lt;/goals&gt;&lt;/execution&gt;
              &lt;execution&gt;
                &lt;id&gt;check&lt;/id&gt;&lt;phase&gt;verify&lt;/phase&gt;&lt;goals&gt;&lt;goal&gt;check&lt;/goal&gt;&lt;/goals&gt;
                &lt;configuration&gt;
                  &lt;rules&gt;&lt;rule&gt;
                    &lt;element&gt;PACKAGE&lt;/element&gt;          &lt;!-- PACKAGE or CLASS; not BUNDLE --&gt;
                    &lt;includes&gt;&lt;include&gt;core.*&lt;/include&gt;&lt;/includes&gt;
                    &lt;limits&gt;
                      &lt;limit&gt;&lt;counter&gt;LINE&lt;/counter&gt;
                        &lt;value&gt;COVEREDRATIO&lt;/value&gt;&lt;minimum&gt;0.90&lt;/minimum&gt;&lt;/limit&gt;
                    &lt;/limits&gt;
                  &lt;/rule&gt;&lt;/rules&gt;
                &lt;/configuration&gt;
              &lt;/execution&gt;
            &lt;/executions&gt;
          &lt;/plugin&gt;

        INCORRECT:
          &lt;element&gt;BUNDLE&lt;/element&gt;                    // silently never fails (GABI-B05)
          &lt;includes&gt;&lt;include&gt;core/*&lt;/include&gt;&lt;/includes&gt;  // BUNDLE+include = no-op gate
          // forkCount=0 in surefire                    // drops JaCoCo javaagent (Rule 20)
      </output>
    </example>
  </examples>
</instructions>

<!--
  SOURCES:
  - User requirement: Spring Boot / Spring AI / Derby / Spring Test best-practices
    instruction for GABI (Group E, step 18).
  - docs/research-java-spring-best-practices.md (RR-1, 2026-06-26):
    all technical claims and [S*] citations ([S1]–[S16]).
  - .claude/instructions/java-spring-conventions.md Rules 1–9: security invariants
    (governs; not duplicated here), Derby version pin, Spring idiom ground truth.
  - CLAUDE.md §Architecture, §Hooks, §Stack.
  - asset-metaprompting/references/software-development.md §3:
    best-practices instruction grouped structure and grounding requirement.
  - templates/claude_instruction.md: structural template.
  - repo-enhancer/orchestrator.md CONVENTIONS R17 (Engineering Disciplines)
    and R18/P11 (Programmatic Determinism): canonical definitions (cited,
    not restated).
-->
