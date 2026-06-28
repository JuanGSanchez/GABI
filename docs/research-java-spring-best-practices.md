# RESEARCH REPORT — Java / Spring Stack Currency for GABI (RR-1)

Request ID: RR-1 (GABI per-framework best-practices grounding)
Status: COMPLETE
Access date for ALL citations: 2026-06-26
Scope: Java 17 baseline; Spring Boot 4 + Spring AI MCP + Spring Data JPA over Apache Derby + Spring Test. No Python/PySide6/matplotlib content reused.

---

## Summary

- Spring Boot 4 keeps a **Java 17 minimum baseline** (supports Java 17–26). No JDK 21 forced upgrade. Current GA is **Spring Boot 4.1.0** (2026-06-10); last 4.0.x patch is **4.0.6** (2026-04-23). [S1][S2][S3][S9]
- **Spring AI 2.0** is the Spring-Boot-4-compatible line (built on the Boot 4 dependency model; cannot load in a 3.x context) and requires **MCP Java SDK 1.0.0**. MCP server starter: `org.springframework.ai:spring-ai-starter-mcp-server*`; canonical tool-exposure idiom is the **`@McpTool` / `@McpToolParam`** annotation pair. [S5][S6][S7]
- Spring AI RAG building blocks: the **`EmbeddingModel`** abstraction (Ollama/OpenAI/Azure/VertexAI impls) + a **`VectorStore`** (in-JVM `SimpleVectorStore`, or PGVector/Chroma/Redis/Elasticsearch) consumed through the **`QuestionAnswerAdvisor`**. [S8]
- **Apache Derby is RETIRED** (read-only since 2025-10-10; no further releases). For a **Java 17** baseline pin **Derby 10.16.1.1** (10.16 = "Java 17 and higher"); Derby **10.17.1.0 requires Java 21** so it is NOT usable on JDK 17. [S10][S11]
- Build tool: recommend **Maven** with `spring-boot-starter-parent` (greenfield, best-documented, jpackage-friendly fat jar). [S2][S4]
- `jpackage` (JDK-bundled, Java 14+) builds native installers from the Spring Boot fat jar; Spring Boot 3.2+/4 launcher main-class is `org.springframework.boot.loader.launch.JarLauncher`. [S12][S13]
- Tests: `spring-boot-starter-test` (JUnit Jupiter — JUnit 6 underpins Boot 4 testing — Spring Test, AssertJ, Mockito, JSONassert); `@SpringBootTest` + MockMvc/WebTestClient (+ new `RestTestClient`); optional Testcontainers. Coverage gate: **JaCoCo 0.8.16**. [S14][S15][S16]

**KEY VERSION DECISIONS:** Spring Boot **4.1.0** (GA) / fallback 4.0.6 — JDK **17** baseline (works across the whole stack); Spring AI **2.0** + MCP Java SDK **1.0.0**; Apache Derby **10.16.1.1** (for JDK 17; 10.17 needs JDK 21; project retired); build tool **Maven**; JaCoCo **0.8.16**.

---

## 1. Spring Boot 4.x line + Java baseline (JDK 17 vs JDK 21)

**Finding (resolves the 3.x-vs-4.x discrepancy):** Spring Boot 4 does **NOT** require JDK 21. Spring Boot 4.0 (GA 2025-11-20) and 4.1 retain the **Java 17 minimum** established in 3.0, supporting Java 17 up to Java 26. The Spring team kept the 17 floor deliberately ("industry consensus is clearly around a Java 17 baseline") so 3.x apps migrate without a JDK upgrade; Java 25 is the recommended target for best runtime but is not required. [S1][S2][S3]

- Current GA (as of 2026-06-26): **Spring Boot 4.1.0**, released 2026-06-10, Java 17–26, requires Spring Framework 7.0.8+, Maven 3.6.3+, Gradle 8.14+/9.x. [S2][S9]
- Last 4.0.x patch: **4.0.6** (2026-04-23). Note 4.0.x is supported only until 2026-12-31; the 4.1 line is the longer-lived choice. [S3][S9]
- Reliability: HIGH (official Spring docs + Spring blog).

**Recommendation for GABI (greenfield, planned Spring Boot 4):** pin **`spring-boot-starter-parent` 4.1.0** with **`<java.version>17</java.version>`**. JDK 17 is valid across the entire planned stack — no Spring-Boot reason to force JDK 21. (Pin 4.0.6 only if a transitive dependency, e.g. an early Spring AI 2.0 build, declares Boot 4.0 specifically — see §2/§4 caveat.) A Spring Boot **3.x** fallback (3.5.x, the final 3.x minor, also JDK 17) is unnecessary here because Boot 4 already supports JDK 17. [S2][S3][S9]

## 2. Spring AI MCP server starter + Java MCP SDK

**Finding:** The Spring-Boot-4-compatible Spring AI line is **Spring AI 2.0** (M-series through 2026; GA targeted late May 2026). It is built on the Boot 4.0 dependency model and **cannot be loaded in a Spring Boot 3.x context** — Boot 4 migration is a prerequisite. Spring AI 2.0 requires **MCP Java SDK 1.0.0** (the official Java MCP implementation donated to Anthropic in Dec 2024). [S5][S6][S7]

**Starter coordinates** (groupId `org.springframework.ai`): [S6]
- `spring-ai-starter-mcp-server` — STDIO transport
- `spring-ai-starter-mcp-server-webmvc` — HTTP (Streamable-HTTP / SSE / stateless), servlet stack — **recommended for GABI** unless reactive is required
- `spring-ai-starter-mcp-server-webflux` — reactive/WebFlux

**Canonical server-exposure idiom** (annotation-based, the documented default in Spring AI 2.0): [S6]
```java
@Component
public class CalculatorTools {
    @McpTool(name = "add", description = "Add two numbers together")
    public int add(
        @McpToolParam(description = "First number", required = true) int a,
        @McpToolParam(description = "Second number", required = true) int b) {
        return a + b;
    }
}
```
Annotations live in `org.springframework.ai.mcp.annotation` (consolidated by Spring AI 2.0-M6, 2026-05-08). `@McpTool` auto-generates the JSON schema; `@Component` beans are auto-scanned/registered. (The older `ToolCallbackProvider` registration path still exists but the annotation idiom is canonical.) This backs the MCP access layer + an `add-mcp-tool` skill. [S5][S6]
- Reliability: HIGH (Spring AI reference docs + Spring blog); GA-date precision MEDIUM (milestone cadence) — see Limitations.

## 3. Spring AI RAG building blocks

**Finding:** Spring AI provides a modular RAG architecture: [S8]
- **Embedding abstraction:** `EmbeddingModel` interface with implementations including `OllamaEmbeddingModel` (local, e.g. `mxbai-embed-large` / `nomic-embed-text`), `OpenAiEmbeddingModel`, `AzureOpenAiEmbeddingModel`, `VertexAiEmbeddingModel`. For a self-contained JDK-17 desktop/server build with no external API, **Ollama** is the local option; OpenAI/Azure for hosted.
- **Vector store (`VectorStore`):** `SimpleVectorStore` — in-JVM, no external service, auto-uses the injected `EmbeddingModel`; ideal for a small local GABI corpus. External options: **PGVector** (PostgreSQL), **Chroma**, **Redis**, **Elasticsearch**, Pinecone. All are pure-Java/JDBC clients with no JDK-21 requirement, so all are JDK-17 compatible.
- **Retrieval idiom:** the **`QuestionAnswerAdvisor`** (Advisor API) queries the `VectorStore` and augments the prompt; custom flows compose the modular RAG components.
- Backs a `rag-dev` agent. Concrete starter recommendation for GABI: `SimpleVectorStore` + Ollama embeddings for the offline path; PGVector when persistence/scale is needed.
- Reliability: MEDIUM-HIGH (Spring AI reference + multiple corroborating 2025–2026 technical articles).

## 4. Spring Data JPA over Apache Derby

**Finding — Derby status:** Apache Derby is **retired**: on 2025-10-10 the developers voted the project into a **read-only state**; development and bug-fixing have ended, no further releases, JIRA frozen. Treat Derby as a frozen, end-of-life dependency. [S10][S11]

**Finding — version/Java mapping (critical for the JDK-17 baseline):** [S10][S11]
- **Derby 10.16.1.1** (2022-05-19) — "for **Java 17** and higher" (drops SecurityManager support). **This is the version to pin for a JDK-17 GABI.**
- **Derby 10.17.1.0** (2023-11-10, the final release) — "for **Java 21** and higher"; **does NOT run on Java 17**.
- So on JDK 17 you cannot use 10.17; pin **10.16.1.1**. (Only if GABI moves to JDK 21 would 10.17.1.0 apply.)

**Recommended DataSource / pool setup:** Replace any connection-per-call pattern with a managed pool. Spring Boot's **default pool is HikariCP** (bundled via `spring-boot-starter-data-jpa`/`-jdbc`), so define a single `DataSource` and let Spring auto-configure Hikari. Minimal `application.properties` for an embedded Derby DB:
```properties
spring.datasource.url=jdbc:derby:gabidb;create=true
spring.datasource.driver-class-name=org.apache.derby.jdbc.EmbeddedDriver
spring.jpa.database-platform=org.hibernate.community.dialect.DerbyTenSevenDialect
spring.jpa.hibernate.ddl-auto=update
# HikariCP is the default pool; tune as needed:
spring.datasource.hikari.maximum-pool-size=5
```
Derby driver coordinates: `org.apache.derby:derby:10.16.1.1` (engine), plus `derbyshared` and `derbytools` for 10.16 (10.15+ split the jars). Hibernate's Derby dialect lives in `hibernate-community-dialects`. [S2][S10]
- Reliability: HIGH (official Derby site/downloads) for status/version; MEDIUM for the exact dialect/jar split (corroborate against the pinned Hibernate version shipped by Boot 4.1).

**Forward-looking note (out-of-scope-but-relevant, RS3):** because Derby is EOL, consider migrating GABI's persistence to a maintained embedded engine (H2, or PostgreSQL for server use) in a later iteration; flagged, not actioned here.

## 5. Maven vs Gradle build descriptor

**Finding & recommendation: use Maven.** GABI currently has no portable build (only `.iml` + an absolute jar path). For a greenfield Spring Boot 4 app the lowest-friction, best-documented path is **Maven with `spring-boot-starter-parent`** (Boot 4.1 requires Maven 3.6.3+). Maven's declarative POM + the Spring Boot BOM/parent give reproducible dependency management and a one-line `spring-boot:repackage` fat jar that feeds jpackage (§6). Gradle 8.14+/9.x is fully supported and is preferable for large polyglot/multi-module builds, but adds Kotlin/Groovy DSL surface that a single-module GABI does not need. [S2][S9]

**Minimal Spring Boot starter dependency set (POM excerpt):**
```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>4.1.0</version>
</parent>
<properties><java.version>17</java.version></properties>
<dependencies>
  <dependency><groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId></dependency>          <!-- MVC + MCP webmvc transport -->
  <dependency><groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId></dependency>     <!-- JPA + HikariCP default -->
  <dependency><groupId>org.apache.derby</groupId>
    <artifactId>derby</artifactId><version>10.16.1.1</version></dependency>
  <dependency><groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
</dependencies>
<!-- plus org.springframework.boot:spring-boot-maven-plugin for repackage -->
```
Add the Spring AI BOM (`org.springframework.ai:spring-ai-bom:2.0.x`) under `<dependencyManagement>` to pin Spring AI/MCP versions. [S2][S6][S9]
- Reliability: HIGH (official docs); Spring AI 2.0.x exact patch is milestone-dependent — see Limitations.

## 6. jpackage for a Java 17+ Spring Boot installer

**Finding:** `jpackage` is bundled with the JDK (JEP 343, GA in JDK 16; usable from JDK 14 via incubator) and produces platform-native installers/executables from a fat jar. It is **not cross-platform**: build Windows `exe`/`msi` on Windows, `dmg`/`pkg` on macOS, `deb`/`rpm` on Linux. [S12][S13]

**Key flags:** `--type` (`app-image`|`exe`|`msi`|`dmg`|`pkg`|`deb`|`rpm`), `--input` (dir holding the jar), `--main-jar`, `--main-class`, `--name`, `--app-version`, `--icon`, and Windows extras `--win-menu --win-shortcut`. [S12][S13]

**Spring Boot specifics:** point `--main-jar` at the `spring-boot-maven-plugin`-repackaged fat jar. The launcher main-class for **Spring Boot 3.2+ / 4.x** is **`org.springframework.boot.loader.launch.JarLauncher`** (older docs show `org.springframework.boot.loader.JarLauncher` — pre-3.2). Example:
```
jpackage --type exe --input build/libs --name GABI \
  --main-jar gabi-1.0.0.jar \
  --main-class org.springframework.boot.loader.launch.JarLauncher \
  --app-version 1.0.0 --icon gabi.ico --win-menu --win-shortcut
```
Optionally use `--runtime-image` from `jlink` to bundle a trimmed JDK 17 runtime so end users need no installed JRE. [S12][S13]
- Reliability: HIGH for the tool/flags (Oracle JDK 17 jpackage docs); MEDIUM for the exact Boot launcher path — verify against the produced jar's `MANIFEST.MF` `Main-Class`.

## 7. JUnit 5 + Spring Test + JaCoCo

**Finding — test stack:** `spring-boot-starter-test` aggregates JUnit Jupiter, Spring Test, Spring Boot Test, AssertJ, Hamcrest, Mockito, JSONassert, JsonPath. For Spring Boot 4, the testing foundation moves to **JUnit 6** (GA 2025-09); JUnit 4 is in maintenance and Spring Framework 7 deprecates it in favour of `SpringExtension`/Jupiter. [S14][S15]
- `@SpringBootTest` loads the full context; add `@AutoConfigureMockMvc` for **MockMvc** (controller/web-slice testing, pair with `@WebMvcTest` for slices). [S15]
- **WebTestClient** for full integration / WebFlux; Spring Boot 4 adds **`RestTestClient`** (fluent REST testing, modern replacement for the soon-deprecated `TestRestTemplate`). [S14]
- **Testcontainers** (optional) for integration tests against a real DB/service via `@Testcontainers` + Spring Boot's `@ServiceConnection`; not needed for embedded Derby unit tests but useful if GABI later targets PostgreSQL/PGVector.

**Finding — coverage gate:** pin **JaCoCo `jacoco-maven-plugin` 0.8.16** (latest, build `0.8.16.202606240819`, 2026-06); supports Java 17+ with bytecode filtering up to javac 26. Wire `prepare-agent` + `report` (and `check` for the coverage threshold gate); do not set surefire/failsafe `forkCount=0`/`forkMode=never` or the javaagent is dropped and coverage is lost. [S16]
- Reliability: HIGH (Maven Central + JaCoCo change history + Spring testing references).

---

## Limitations

- **Spring AI 2.0 exact GA / patch version:** sources converge on a Spring Boot 4-only Spring AI 2.0 with a late-May-2026 GA target and milestones through M6 (2026-05-08), but a precise GA patch number was not pinned from the official release index in this pass. Pin via the `spring-ai-bom` and verify the latest 2.0.x at build time. (C1: surfaced rather than asserted.)
- **Spring Boot 4.1.0 vs Spring AI 2.0 compatibility:** Spring AI 2.0 is documented as built on the "Boot 4.0 dependency model." It is expected to work on 4.1.x, but official confirmation that Spring AI 2.0.x lists 4.1.0 was not located. Conservative fallback: pin Spring Boot **4.0.6** if a Spring AI 2.0 build constrains to 4.0.x.
- **Derby Hibernate dialect / jar split:** the exact dialect class and the 10.16 jar split (`derby`+`derbyshared`+`derbytools`) should be confirmed against the Hibernate ORM version shipped by the chosen Spring Boot 4.1 release.
- **Spring Boot fat-jar launcher path** for jpackage should be verified from the produced jar manifest (3.2+ uses `...loader.launch.JarLauncher`).

---

## Sources

- [S1] BSWEN — "What is the Minimum Java Version Required for Spring Boot 4?" — https://docs.bswen.com/blog/2026-03-04-spring-boot-4-java-version/ — accessed 2026-06-26
- [S2] Spring — "System Requirements :: Spring Boot" (4.1.0) — https://docs.spring.io/spring-boot/system-requirements.html — accessed 2026-06-26
- [S3] InfoQ — "Spring Framework 7 and Spring Boot 4 Deliver API Versioning, Resilience, and Null-Safe Annotations" — https://www.infoq.com/news/2025/11/spring-7-spring-boot-4/ — accessed 2026-06-26
- [S4] Marco Behler — "Spring and Spring Boot Versions" — https://www.marcobehler.com/guides/spring-and-spring-boot-versions — accessed 2026-06-26
- [S5] javacodegeeks — "MCP for Java Developers: A Practical Tutorial With Spring AI and the MCP Java SDK" — https://www.javacodegeeks.com/2026/04/mcp-for-java-developers-a-practical-tutorial-with-spring-ai-and-the-mcp-java-sdk.html — accessed 2026-06-26
- [S6] Spring — "MCP Server Boot Starter :: Spring AI Reference" — https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html — accessed 2026-06-26
- [S7] HeroDevs — "Spring AI 2.0 Is Coming Soon. Your Boot 4.0 Migration Does Not Have to Start Tomorrow." — https://www.herodevs.com/blog-posts/spring-ai-2-0-is-coming-soon-your-boot-4-0-migration-does-not-have-to-start-tomorrow — accessed 2026-06-26
- [S8] Spring AI RAG / Vector Store reference & corroborating guide — https://docs.spring.io/spring-ai/reference/api/vectordbs.html (and SivaLabs, "Spring AI RAG using Embedding Models and Vector Databases", https://www.sivalabs.in/blog/spring-ai-rag-using-embedding-models-vector-databases/) — accessed 2026-06-26
- [S9] Spring Blog — "Spring Boot 4.1.0 available now" (2026-06-10) — https://spring.io/blog/2026/06/10/spring-boot-4/ ; "Spring Boot 4.0.6 available now" — https://spring.io/blog/2026/04/23/spring-boot-4-0-6-available-now/ — accessed 2026-06-26
- [S10] Apache Derby — Downloads (release/Java mapping; retirement notice) — https://db.apache.org/derby/derby_downloads.html — accessed 2026-06-26
- [S11] Apache Derby — "Apache Derby 10.17.1.0 Release" / project status — https://db.apache.org/derby/releases/release-10_17_1_0.cgi and https://db.apache.org/derby/ — accessed 2026-06-26
- [S12] Oracle — "Packaging Overview" (JDK 17 jpackage) — https://docs.oracle.com/en/java/javase/17/jpackage/packaging-overview.html — accessed 2026-06-26
- [S13] howtodoinjava — "JPackage: Create MSI/EXE Installer for Java App" — https://howtodoinjava.com/devops/jpackage-plugin-example/ — accessed 2026-06-26
- [S14] rieckpil — "What's New for Testing in Spring Boot 4 and Spring Framework 7" — https://rieckpil.de/whats-new-for-testing-in-spring-boot-4-0-and-spring-framework-7/ — accessed 2026-06-26
- [S15] Baeldung — "Testing in Spring Boot" — https://www.baeldung.com/spring-boot-testing — accessed 2026-06-26
- [S16] Maven Repository — "org.jacoco » jacoco-maven-plugin" (0.8.16) + JaCoCo Change History — https://mvnrepository.com/artifact/org.jacoco/jacoco-maven-plugin and https://www.jacoco.org/jacoco/trunk/doc/changes.html — accessed 2026-06-26

---
END OF RESEARCH REPORT
