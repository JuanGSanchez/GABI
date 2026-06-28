# GABI — Gestor Autonomo de Biblioteca Interactivo

GABI is a **Java library management system** backed by an Apache Derby SQL database. It started
life as a command-line CRUD manager for books, members, and loans; the enhancement branch adds:

- A **RAG (retrieval-augmented generation) capability** — natural-language Q&A over the book
  catalogue via Spring AI and a configurable LLM provider.
- A **dual MCP + REST access layer** — 14 tools and a matching REST API so external agents and
  HTTP clients can drive the catalogue and RAG without touching the CLI.
- A **Maven build system** replacing the original IntelliJ-only project model, producing a
  reproducible fat-jar and a self-contained jpackage app-image.

---

## Table of contents

1. [What GABI is](#what-gabi-is)
2. [Prerequisites](#prerequisites)
3. [Build](#build)
4. [Run modes](#run-modes)
5. [RAG provider configuration](#rag-provider-configuration)
6. [Packaged app (jpackage)](#packaged-app-jpackage)
7. [Security](#security)
8. [Project structure](#project-structure)

---

## What GABI is

| Layer | What it does |
|-------|-------------|
| **CLI library manager** | Interactive console menus for Books, Members, Loans, and Users (DB operators). Backed by Apache Derby over JDBC. Entry class: `GabiApplication` → `GabiCliRunner` → `manager.LibMenu`. |
| **RAG / Q&A** | `LibraryService.ask(question)` embeds the question, similarity-searches a `SimpleVectorStore` built from the live catalogue, injects the top matches into a chat prompt, and returns `AnswerWithSources`. `ingest()` projects the Derby book rows into the vector store. |
| **MCP server** | Spring AI MCP server (Streamable-HTTP at `/mcp` + STDIO) exposing 14 tools: `ask`, `reindex`, list/search/get/count books, members, loans, and `health`. Active only under `--spring.profiles.active=server`. |
| **REST API** | Spring WebMVC controllers under `/api` exposing the same operations as the MCP tools. Also active only under the `server` profile. |

---

## Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| **JDK 17** | 17 (minimum) | Spring Boot 4.0 and Derby 10.16.x baseline. Use JDK 17 to get `jpackage`. |
| **Maven** | 3.6.3+ | Or use the included `mvnw` / `mvnw.cmd` wrapper — no local Maven install needed. |
| **Apache Derby network server** | 10.16.x | Required at runtime for the CLI and server modes. GABI is a Derby **network client**; start `NetworkServerControl start` on `localhost:1527` before launching. In-memory Derby (`jdbc:derby:memory:...`) is used for tests only and starts automatically. |
| **Ollama** (optional) | any recent | Required to actually answer RAG questions under the default provider. Install from [ollama.com](https://ollama.com) and pull the models: `ollama pull llama3.2` and `ollama pull nomic-embed-text`. If Ollama is not running, GABI starts but `ask` returns a degraded message. |
| **OpenAI / Anthropic API key** (optional) | — | Needed only when using the `openai` or `anthropic` Spring profiles. Set as environment variables (never committed). |

---

## Build

```bash
# Build the executable fat-jar (skips tests for speed)
./mvnw -q -DskipTests package

# Build and run all tests (152 tests, JaCoCo >=90% core gate)
./mvnw verify
```

The build produces `target/gabi-1.0.0-exec.jar` (Spring Boot fat-jar, ~120 MB including all
Spring AI and Derby network client dependencies).

On Windows use `mvnw.cmd` in place of `./mvnw`.

---

## Run modes

Set `DB_USER` and `DB_PASSWORD` via environment variables before starting. Never hardcode them.

### Mode 1 — CLI library manager (default)

```bash
export DB_USER=<your-derby-user>
export DB_PASSWORD=<your-derby-password>
java -jar target/gabi-1.0.0-exec.jar
```

The interactive console menus start. Authenticate with the credentials you used when initialising
the Derby database. The RAG pipeline is also available from the CLI via the `ask` menu option when
Ollama is running.

### Mode 2 — HTTP server (MCP Streamable-HTTP + REST)

```bash
export DB_USER=<your-derby-user>
export DB_PASSWORD=<your-derby-password>
java -jar target/gabi-1.0.0-exec.jar --spring.profiles.active=server
```

GABI starts embedded Tomcat on port 8080 (override with `--server.port=<port>`):

- MCP Streamable-HTTP endpoint: `http://localhost:8080/mcp`
- REST API: `http://localhost:8080/api/...`
- Health check: `http://localhost:8080/health`

The CLI does not start in this mode.

Add an AI provider profile to also activate RAG (see [RAG provider configuration](#rag-provider-configuration)):

```bash
# OpenAI example
export OPENAI_API_KEY=sk-...
java -jar target/gabi-1.0.0-exec.jar --spring.profiles.active=server,openai
```

### Mode 3 — STDIO MCP (for MCP clients that launch the process)

```bash
export DB_USER=<your-derby-user>
export DB_PASSWORD=<your-derby-password>
java -jar target/gabi-1.0.0-exec.jar \
     --spring.ai.mcp.server.stdio=true \
     --spring.main.web-application-type=none
```

The MCP client (e.g., Claude Desktop) launches GABI as a child process and communicates over
stdin/stdout using JSON-RPC. No HTTP port is opened.

MCP client configuration (Claude Desktop / agent config):

```json
{
  "mcpServers": {
    "gabi": {
      "command": "java",
      "args": [
        "-jar", "/absolute/path/to/gabi-1.0.0-exec.jar",
        "--spring.ai.mcp.server.stdio=true",
        "--spring.main.web-application-type=none"
      ],
      "env": {
        "DB_USER": "<your-user>",
        "DB_PASSWORD": "<your-password>"
      }
    }
  }
}
```

For the HTTP transport variant see `docs/README-access.md`.

---

## RAG provider configuration

GABI supports three LLM providers selectable via Spring profiles. The provider determines which
`ChatModel` and `EmbeddingModel` beans are injected into the RAG pipeline.

| Provider | Profile flag | API key env var | Chat model | Embedding model |
|----------|-------------|-----------------|------------|-----------------|
| **Ollama** (default) | *(none)* | none needed | `llama3.2` (local) | `nomic-embed-text` (local) |
| **OpenAI** | `openai` | `OPENAI_API_KEY` | `gpt-4o` (configurable) | `text-embedding-ada-002` |
| **Anthropic** | `anthropic` | `ANTHROPIC_API_KEY` | `claude-3-5-sonnet` (configurable) | Ollama `nomic-embed-text` |

> Note: Anthropic provides chat only. The `anthropic` profile uses Ollama for embeddings, so a
> local Ollama with `nomic-embed-text` must also be running when using Anthropic.

### Activating a provider

```bash
# Ollama (default — no flag needed)
java -jar target/gabi-1.0.0-exec.jar

# OpenAI
export OPENAI_API_KEY=sk-...
java -jar target/gabi-1.0.0-exec.jar --spring.profiles.active=openai

# Anthropic (chat) + Ollama (embeddings)
export ANTHROPIC_API_KEY=sk-ant-...
java -jar target/gabi-1.0.0-exec.jar --spring.profiles.active=anthropic

# Server mode + OpenAI
export OPENAI_API_KEY=sk-...
java -jar target/gabi-1.0.0-exec.jar --spring.profiles.active=server,openai
```

API keys are read from environment variables only. They are never committed to the repository.
The Ollama base URL defaults to `http://localhost:11434`; override with
`OLLAMA_BASE_URL=http://<host>:<port>`.

### RAG index management

Call `reindex` (MCP tool or `POST /api/reindex`) to build or refresh the vector index from the
current Derby book catalogue. The index is in-memory by default; configure
`gabi.vectorstore.file=<path>` in `application.yml` or via `--gabi.vectorstore.file=<path>` to
persist it across restarts.

If no model backend is available, `ask` returns a degraded message rather than throwing:

> "RAG service is not available (no model running). Start Ollama with 'ollama run llama3.2' or
> set an API key via OPENAI_API_KEY."

---

## Packaged app (jpackage)

GABI can be packaged as a self-contained native app-image (no JVM required on the target machine).

```powershell
# Windows — builds the fat-jar then the app-image
.\packaging\build_windows.ps1

# Skip Maven build if target\gabi-1.0.0-exec.jar already exists
.\packaging\build_windows.ps1 -SkipBuild

# Build an MSI installer instead (requires WiX Toolset 3.x)
.\packaging\build_windows.ps1 -Type msi
```

```bash
# Linux / macOS
./packaging/build_posix.sh        # app-image (default)
./packaging/build_posix.sh deb    # Debian package
./packaging/build_posix.sh rpm    # RPM package
./packaging/build_posix.sh dmg    # macOS DMG
```

The app-image lands at `packaging/bin/GABI/GABI.exe` (Windows) or `packaging/bin/GABI/bin/GABI`
(Linux/macOS) with a bundled trimmed JRE (~60–80 MB) and the fat-jar (~120 MB). Total: ~180–200 MB.

The packaged app supports all three run modes. Pass `--spring.profiles.active=server` as a
command-line argument exactly as you would with `java -jar`. See `packaging/README-packaging.md`
for full details.

External configuration (writable properties) is read from `%USERPROFILE%\.gabi\` (Windows) or
`~/.gabi/` (Linux/macOS) — not from the install directory.

---

## Security

- **DB credentials** — `DB_USER` and `DB_PASSWORD` must be supplied via environment variables.
  The repository does not contain credentials. The original `configuration.properties` placeholder
  values (`admin` / `1234`) are defaults for first-time DB initialisation only; change them before
  production use.
- **API keys** — `OPENAI_API_KEY` and `ANTHROPIC_API_KEY` are read exclusively from environment
  variables and are never committed.
- **User-admin operations** — Derby user provisioning (`addUser`, `deleteUser`, GRANT/REVOKE,
  `derby.user.*`) is exposed only through the CLI. It is intentionally absent from both the MCP
  tool set and the REST endpoints, so the network-accessible surface cannot escalate DB privileges.
- **Identifier validation** — all user-supplied SQL identifiers (user names, table names) pass
  through `IdentifierValidator`, which enforces a strict `[A-Za-z0-9_]` whitelist against SQL
  identifier injection. Data values continue to use `PreparedStatement` parameter binding.
- **Error responses** — `GlobalExceptionHandler` maps all `LibraryException` subtypes to safe
  RFC 9457 `ProblemDetail` responses. Stack traces and DB metadata are never forwarded to clients.

---

## Project structure

```
GABI/
  src/main/java/
    GabiApplication.java          Spring Boot entry point
    GabiCliRunner.java            CommandLineRunner → manager.LibMenu
    manager/                      Interactive CLI menus (LibMenu, BookMenu, ...)
    sql/                          Legacy JDBC DAOs (LibDBBook, LibDBMember, ...)
    tables/                       Domain entities (Book, Member, Loan, User)
    utils/                        Utilities, i18n bundles, configuration
    core/                         Headless core service (LibraryService interface +
                                  LibraryServiceImpl, IdentifierValidator,
                                  LibraryException, AnswerWithSources)
    rag/                          Spring AI RAG pipeline (RagServiceImpl,
                                  NoOpRagService fallback, RagConfig)
    access/
      rest/                       Spring WebMVC REST controllers + GlobalExceptionHandler
      mcp/                        Spring AI MCP tools (LibraryMcpTools)
  src/test/java/                  JUnit 5 + Mockito tests (152 tests, >=90% core coverage)
  src/main/resources/
    application.yml               Main config (CLI mode, Ollama defaults)
    application-server.yml        Server profile (enables Tomcat + MCP + REST)
    application-openai.yml        OpenAI profile
    application-anthropic.yml     Anthropic profile
  docs/
    README-access.md              MCP + REST interface reference
    agent-operating-doc.md        External agent operating guide (this repo)
  packaging/
    build_windows.ps1             jpackage build script for Windows
    build_posix.sh                jpackage build script for Linux/macOS
    README-packaging.md           Packaging guide
    bin/GABI/                     App-image output (generated; not committed)
  pom.xml                         Maven build (Spring Boot 4.0.6, Spring AI BOM 2.0.0,
                                  Derby 10.16.1.1, JaCoCo 0.8.12)
  mvnw / mvnw.cmd                 Maven wrapper
  LICENSE
```
